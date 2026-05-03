package com.healthvia.platform.conversation.channel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.healthvia.platform.conversation.entity.Conversation;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Email ⇄ HealthVia. IMAP poll every {@code email.poll.interval-ms} (default
 * 30 s); each unseen message is normalised + handed to
 * {@link MessageInboundService}. Outbound replies go through Spring's
 * {@link JavaMailSender} with In-Reply-To threading so providers like
 * Gmail / Outlook keep the conversation grouped.
 *
 * Disabled unless {@code email.channel.enabled=true} so the backend can boot
 * without IMAP creds in dev / sandbox.
 *
 * Required properties (set in application.properties or env):
 *   email.channel.enabled=true
 *   email.imap.host=imap.gmail.com
 *   email.imap.port=993
 *   email.imap.username=inbox@healthviatech.website
 *   email.imap.password=<app-password>
 *   email.imap.folder=INBOX
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=...
 *   spring.mail.password=...
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 *   spring.mail.properties.mail.smtp.auth=true
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "email.channel.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EmailChannelAdapter implements ChannelAdapter {

    private final MessageInboundService inboundService;
    private final JavaMailSender mailSender;

    @Value("${email.imap.host}")        private String imapHost;
    @Value("${email.imap.port:993}")     private int imapPort;
    @Value("${email.imap.username}")     private String imapUser;
    @Value("${email.imap.password}")     private String imapPassword;
    @Value("${email.imap.folder:INBOX}") private String imapFolder;
    @Value("${email.imap.protocol:imaps}") private String imapProtocol;

    @Value("${spring.mail.username:no-reply@healthviatech.website}")
    private String fromAddress;

    @Override
    public Conversation.Channel channel() {
        return Conversation.Channel.EMAIL;
    }

    /** IMAP polling — Scheduled fixed-delay so previous poll always finishes. */
    @Scheduled(
        fixedDelayString = "${email.poll.interval-ms:30000}",
        initialDelayString = "${email.poll.initial-delay-ms:5000}"
    )
    public void pollInbox() {
        Properties props = new Properties();
        props.put("mail.store.protocol", imapProtocol);
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        try (Store store = session.getStore(imapProtocol)) {
            store.connect(imapHost, imapPort, imapUser, imapPassword);
            try (Folder folder = store.getFolder(imapFolder)) {
                folder.open(Folder.READ_WRITE);
                Message[] unseen = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                log.debug("IMAP poll: {} unseen messages", unseen.length);
                for (Message m : unseen) {
                    try {
                        ingestMessage(m);
                        m.setFlag(Flags.Flag.SEEN, true);
                    } catch (Exception ex) {
                        log.warn("Failed to ingest email: {}", ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("IMAP poll failed (host={}): {}", imapHost, e.getMessage());
        }
    }

    private void ingestMessage(Message m) throws Exception {
        Address[] fromArr = m.getFrom();
        InternetAddress from = (fromArr != null && fromArr.length > 0 && fromArr[0] instanceof InternetAddress ia)
            ? ia : new InternetAddress("unknown@external");
        String fromEmail = from.getAddress() == null ? "unknown@external" : from.getAddress().toLowerCase();
        String fromName = from.getPersonal() != null ? from.getPersonal() : fromEmail;

        String subject = m.getSubject() == null ? "(no subject)" : m.getSubject();
        String text = extractText(m);
        String messageId = headerValue(m, "Message-ID");
        String inReplyTo = headerValue(m, "In-Reply-To");
        String threadId = inReplyTo != null ? inReplyTo : messageId;

        InboundChannelMessage inbound = InboundChannelMessage.builder()
            .channel(Conversation.Channel.EMAIL)
            .externalUserId(fromEmail)
            .externalUserName(fromName)
            .email(fromEmail)
            .externalMessageId(messageId)
            .externalThreadId(threadId)
            .text(text)
            .subject(subject)
            .sentAt(toLocal(m.getSentDate() == null ? null : m.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()))
            .build();

        inboundService.ingest(inbound);
    }

    private static String headerValue(Message m, String name) {
        try {
            String[] vals = m.getHeader(name);
            return (vals == null || vals.length == 0) ? null : vals[0];
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime toLocal(LocalDateTime ldt) {
        return ldt == null ? LocalDateTime.now() : ldt;
    }

    private static String extractText(Part p) throws Exception {
        if (p.isMimeType("text/plain")) {
            Object content = p.getContent();
            return content == null ? "" : content.toString();
        }
        if (p.isMimeType("text/html")) {
            Object content = p.getContent();
            return content == null ? "" : stripHtml(content.toString());
        }
        if (p.getContent() instanceof Multipart mp) {
            // Prefer text/plain part if available
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) return extractText(bp);
            }
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                String t = extractText(bp);
                if (t != null && !t.isBlank()) return t;
            }
        }
        return "";
    }

    private static String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    /* ================================================================
     * Outbound — used by ChannelDispatcher
     * ================================================================ */

    @Override
    public String sendText(OutboundMessage msg) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(msg.getRecipient());
            helper.setSubject(msg.getSubject() != null ? msg.getSubject() : "HealthVia");
            helper.setText(msg.getText() == null ? "" : msg.getText());
            if (msg.getExternalThreadId() != null) {
                mime.setHeader("In-Reply-To", msg.getExternalThreadId());
                mime.setHeader("References", msg.getExternalThreadId());
            }
            mailSender.send(mime);
            String localMid = "<healthvia-" + msg.getInternalMessageId() + "@" + hostFromAddr(fromAddress) + ">";
            log.info("📧 Email sent to {} (subject={})", msg.getRecipient(), msg.getSubject());
            return localMid;
        } catch (Exception e) {
            log.error("Email send failed to {}: {}", msg.getRecipient(), e.getMessage());
            return null;
        }
    }

    private static String hostFromAddr(String addr) {
        int at = addr.indexOf('@');
        return at < 0 ? "healthviatech.website" : addr.substring(at + 1);
    }
}
