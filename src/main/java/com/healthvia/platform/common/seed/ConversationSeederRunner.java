package com.healthvia.platform.common.seed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.healthvia.platform.admin.entity.Admin;
import com.healthvia.platform.admin.repository.AdminRepository;
import com.healthvia.platform.common.enums.UserRole;
import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.Channel;
import com.healthvia.platform.conversation.entity.Conversation.ConversationPriority;
import com.healthvia.platform.conversation.entity.Conversation.ConversationStatus;
import com.healthvia.platform.conversation.repository.ConversationRepository;
import com.healthvia.platform.lead.entity.Lead;
import com.healthvia.platform.lead.entity.Lead.AssignmentMethod;
import com.healthvia.platform.lead.entity.Lead.LeadSource;
import com.healthvia.platform.lead.entity.Lead.LeadStatus;
import com.healthvia.platform.lead.repository.LeadRepository;
import com.healthvia.platform.message.entity.Message;
import com.healthvia.platform.message.entity.Message.DeliveryStatus;
import com.healthvia.platform.message.entity.Message.MessageType;
import com.healthvia.platform.message.entity.Message.SenderType;
import com.healthvia.platform.message.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Demo conversation seeder — for every WhatsApp / Instagram lead created by
 * {@link LeadSeederRunner} we generate a Conversation plus a realistic
 * back-and-forth thread of Messages. This makes the inbox feel alive on a
 * fresh database (chat bubbles, unread counters, channel breakdown charts).
 *
 * Activated by: app.seed.demo-conversations=true. Skips silently if any
 * conversation already exists.
 *
 * Scope intentionally limited to WHATSAPP + INSTAGRAM because those are the
 * channels we want to demo visually right now; other channels can be added
 * later if needed.
 */
@Slf4j
@Component
@Order(60) // after Lead seeder (50)
@RequiredArgsConstructor
public class ConversationSeederRunner implements CommandLineRunner {

    private final LeadRepository leadRepository;
    private final AdminRepository adminRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Value("${app.seed.demo-conversations:false}")
    private boolean enabled;

    private static final Random R = new Random(20260501L);

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.debug("Conversation seeding disabled (app.seed.demo-conversations=false)");
            return;
        }
        long existing = conversationRepository.count();
        if (existing > 0) {
            log.info("Conversations already present ({}), skipping seed.", existing);
            return;
        }

        // Index agents by id so we can stamp assignedAgentName onto every conversation
        Map<String, Admin> agentsById = adminRepository.findAll().stream()
            .filter(a -> a.getRole() == UserRole.AGENT && !a.isDeleted())
            .collect(Collectors.toMap(Admin::getId, a -> a, (a, b) -> a));
        if (agentsById.isEmpty()) {
            log.warn("No AGENT accounts found — conversations will not have an assigned agent.");
        }

        // Ordered agent pool for round-robin distribution. Sorted by id so the
        // assignment is deterministic across seeds (test runs reproduce).
        List<Admin> agentPool = agentsById.values().stream()
            .sorted(Comparator.comparing(Admin::getId))
            .toList();

        List<Lead> targets = leadRepository.findAll().stream()
            .filter(l -> !l.isDeleted())
            .filter(l -> l.getSource() == LeadSource.WHATSAPP || l.getSource() == LeadSource.INSTAGRAM)
            .sorted(Comparator.comparing(Lead::getId)) // stable distribution
            .toList();

        if (targets.isEmpty()) {
            log.info("No WhatsApp / Instagram leads available, nothing to seed.");
            return;
        }

        log.info("🌱 Seeding {} demo conversations (WhatsApp + Instagram)...", targets.size());

        int conversationsSaved = 0;
        int messagesSaved = 0;
        int idx = 0;

        for (Lead lead : targets) {
            try {
                Channel channel = lead.getSource() == LeadSource.WHATSAPP ? Channel.WHATSAPP : Channel.INSTAGRAM;
                List<ScriptedMessage> script = pickScript(lead.getTreatmentTypeId(), lead.getLanguage(), channel);

                // Conversation timeline starts at lead creation
                LocalDateTime base = lead.getCreatedAt() != null ? lead.getCreatedAt() : LocalDateTime.now().minusDays(2);

                // Force round-robin assignment so every agent sees a slice in
                // their "assigned to me" filter, regardless of how the lead
                // seeder originally distributed them (NEW leads were left
                // unassigned and would never show up here otherwise).
                Admin agent = agentPool.isEmpty() ? null : agentPool.get(idx % agentPool.size());
                idx++;

                // Back-propagate the assignment to the Lead so /api/leads filters
                // ("assigned to me", agent stats, leaderboard) stay in sync.
                if (agent != null) {
                    boolean changed = false;
                    if (lead.getAssignedAgentId() == null || !agent.getId().equals(lead.getAssignedAgentId())) {
                        lead.setAssignedAgentId(agent.getId());
                        lead.setAssignedAgentName(agent.getFirstName() + " " + agent.getLastName());
                        lead.setAssignedAt(base);
                        lead.setAssignmentMethod(AssignmentMethod.AUTO);
                        changed = true;
                    }
                    if (lead.getStatus() == LeadStatus.NEW) {
                        lead.setStatus(LeadStatus.ASSIGNED);
                        lead.setStatusChangedAt(base);
                        lead.setStatusChangedBy("seeder");
                        changed = true;
                    }
                    if (changed) {
                        leadRepository.save(lead);
                    }
                }

                Conversation convo = Conversation.builder()
                    .leadId(lead.getId())
                    .assignedAgentId(agent != null ? agent.getId() : null)
                    .assignedAgentName(agent != null ? agent.getFirstName() + " " + agent.getLastName() : null)
                    .channel(channel)
                    .channelConversationId(externalThreadId(channel, lead))
                    .status(pickStatus(script))
                    .subject(buildSubject(channel, lead))
                    .language(lead.getLanguage())
                    .priority(mapPriority(lead))
                    .totalMessages(script.size())
                    .tags(lead.getTags() != null ? new java.util.HashSet<>(lead.getTags()) : Set.of())
                    .treatmentInterest(lead.getInterestedTreatment())
                    .isPinned(false)
                    .build();
                convo.setCreatedAt(base);
                convo.setUpdatedAt(base);

                Conversation saved = conversationRepository.save(convo);

                LocalDateTime cursor = base;
                int unread = 0;
                String lastPreview = "";
                String lastSender = "LEAD";
                LocalDateTime lastAt = base;
                LocalDateTime firstAgentReplyAt = null;

                for (int i = 0; i < script.size(); i++) {
                    ScriptedMessage sm = script.get(i);
                    cursor = cursor.plusMinutes(2L + R.nextInt(35));

                    boolean fromAgent = sm.sender() == SenderType.AGENT;
                    Message msg = Message.builder()
                        .conversationId(saved.getId())
                        .leadId(lead.getId())
                        .senderType(sm.sender())
                        .senderId(fromAgent
                            ? (agent != null ? agent.getId() : null)
                            : lead.getId())
                        .senderName(fromAgent
                            ? (agent != null ? agent.getFirstName() + " " + agent.getLastName() : "Agent")
                            : lead.getFirstName() + " " + (lead.getLastName() != null ? lead.getLastName() : ""))
                        .messageType(sm.type())
                        .content(sm.content())
                        .channel(channel.name())
                        .externalMessageId(channel.name().toLowerCase() + "_msg_" + saved.getId() + "_" + i)
                        .deliveryStatus(fromAgent ? DeliveryStatus.READ : DeliveryStatus.DELIVERED)
                        .deliveredAt(cursor)
                        .readAt(fromAgent ? cursor.plusMinutes(1 + R.nextInt(10)) : null)
                        .isInternalNote(false)
                        .isAutoReply(false)
                        .isEdited(false)
                        .attachments(sm.attachments())
                        .build();
                    msg.setCreatedAt(cursor);
                    msg.setUpdatedAt(cursor);
                    messageRepository.save(msg);
                    messagesSaved++;

                    if (fromAgent && firstAgentReplyAt == null) {
                        firstAgentReplyAt = cursor;
                    }
                    lastPreview = sm.content().length() > 120 ? sm.content().substring(0, 117) + "..." : sm.content();
                    lastSender = fromAgent ? "AGENT" : "LEAD";
                    lastAt = cursor;
                    if (!fromAgent && i >= script.size() - 2) {
                        unread++;
                    }
                }

                saved.setLastMessagePreview(lastPreview);
                saved.setLastMessageAt(lastAt);
                saved.setLastMessageSender(lastSender);
                saved.setUnreadCount(unread);
                saved.setFirstResponseAt(firstAgentReplyAt);
                saved.setUpdatedAt(lastAt);
                conversationRepository.save(saved);

                conversationsSaved++;
            } catch (Exception e) {
                log.warn("Conversation seed failed for lead {}: {}", lead.getId(), e.getMessage());
            }
        }

        log.info("✅ Seeded {} conversations / {} messages.", conversationsSaved, messagesSaved);
    }

    /* ---------------- helpers ---------------- */

    private static String buildSubject(Channel ch, Lead lead) {
        String tName = treatmentLabel(lead.getTreatmentTypeId());
        String chName = ch == Channel.WHATSAPP ? "WhatsApp" : "Instagram DM";
        return chName + " — " + tName + " (" + lead.getCountry() + ")";
    }

    private static String externalThreadId(Channel ch, Lead lead) {
        String prefix = ch == Channel.WHATSAPP ? "wa" : "ig";
        return prefix + "_" + (lead.getPhone() != null ? lead.getPhone().replaceAll("\\D", "") : lead.getId());
    }

    private static ConversationStatus pickStatus(List<ScriptedMessage> script) {
        SenderType last = script.get(script.size() - 1).sender();
        return last == SenderType.LEAD ? ConversationStatus.AGENT_REPLY : ConversationStatus.WAITING_REPLY;
    }

    private static ConversationPriority mapPriority(Lead lead) {
        if (lead.getPriority() == null) return ConversationPriority.NORMAL;
        return switch (lead.getPriority()) {
            case URGENT -> ConversationPriority.URGENT;
            case HIGH -> ConversationPriority.HIGH;
            case LOW -> ConversationPriority.LOW;
            default -> ConversationPriority.NORMAL;
        };
    }

    private static String treatmentLabel(String ttId) {
        if (ttId == null) return "Genel";
        return switch (ttId) {
            case "tt_hair" -> "Saç ekimi";
            case "tt_ivf" -> "Tüp bebek";
            case "tt_dental" -> "Diş tedavisi";
            case "tt_bbl" -> "BBL";
            case "tt_cardio" -> "Kardiyoloji";
            case "tt_rhino" -> "Rinoplasti";
            case "tt_bariatric" -> "Bariatrik cerrahi";
            case "tt_ortho" -> "Ortopedi";
            case "tt_onco" -> "Onkoloji";
            case "tt_lasik" -> "LASIK";
            case "tt_veneer" -> "Hollywood smile / lamine";
            default -> ttId;
        };
    }

    /* ---------------- scripts ---------------- */

    private record ScriptedMessage(SenderType sender, MessageType type, String content,
                                   List<Message.Attachment> attachments) {
        static ScriptedMessage lead(String text) {
            return new ScriptedMessage(SenderType.LEAD, MessageType.TEXT, text, null);
        }
        static ScriptedMessage agent(String text) {
            return new ScriptedMessage(SenderType.AGENT, MessageType.TEXT, text, null);
        }
        static ScriptedMessage leadImage(String caption, String imageUrl) {
            return new ScriptedMessage(SenderType.LEAD, MessageType.IMAGE, caption,
                List.of(new Message.Attachment("photo.jpg", imageUrl, "image/jpeg", 245_000L, imageUrl)));
        }
        static ScriptedMessage agentTemplate(String text) {
            return new ScriptedMessage(SenderType.AGENT, MessageType.TEMPLATE, text, null);
        }
    }

    /**
     * Pick a multi-message script for the given treatment + language.
     * Falls back to a generic English script.
     */
    private static List<ScriptedMessage> pickScript(String ttId, String language, Channel channel) {
        String key = (ttId != null ? ttId : "generic") + "_" + (language != null ? language.toUpperCase() : "EN");
        List<ScriptedMessage> script = SCRIPTS.get(key);
        if (script == null) {
            // Fallback by treatment only (English)
            script = SCRIPTS.get((ttId != null ? ttId : "generic") + "_EN");
        }
        if (script == null) {
            script = SCRIPTS.get("generic_EN");
        }
        // Mix in an image attachment occasionally for visual richness — only for
        // treatments where a "before photo" makes sense.
        if (channel == Channel.WHATSAPP && (("tt_hair").equals(ttId) || ("tt_rhino").equals(ttId)
                || ("tt_bbl").equals(ttId)) && R.nextDouble() < 0.6) {
            List<ScriptedMessage> withPhoto = new ArrayList<>(script);
            withPhoto.add(1, ScriptedMessage.leadImage("Mevcut durum fotoğrafı 📸",
                "https://placehold.co/600x400?text=Patient+Photo"));
            return withPhoto;
        }
        return script;
    }

    private static final Map<String, List<ScriptedMessage>> SCRIPTS = new HashMap<>();
    static {
        // ---------- Saç ekimi ----------
        SCRIPTS.put("tt_hair_EN", List.of(
            ScriptedMessage.lead("Hi! I'm interested in a hair transplant. Saw your Instagram results."),
            ScriptedMessage.agent("Hello! Thanks for reaching out 👋 We'd love to help. Could you share a photo of your hairline and let us know your age?"),
            ScriptedMessage.lead("I'm 34, Norwood 3. Photo attached."),
            ScriptedMessage.agent("Thank you! Based on the photo, ~3500 grafts would give a strong, natural result. Our DHI package is €2,800 — includes 4-night hotel, PRP, transfers, and 1-year follow-up."),
            ScriptedMessage.lead("Does the price include accommodation?"),
            ScriptedMessage.agent("Yes — 4 nights in a 4★ hotel near the clinic, airport transfers and a translator are all included. The only extra is your flight."),
            ScriptedMessage.lead("Great. What dates are available in the next 2 months?")
        ));
        SCRIPTS.put("tt_hair_TR", List.of(
            ScriptedMessage.lead("Merhaba, saç ekimi için bilgi almak istiyorum."),
            ScriptedMessage.agent("Merhaba 👋 Tabii ki, yardımcı olalım. Saç çizginizin fotoğrafını paylaşır mısınız? Yaşınız?"),
            ScriptedMessage.lead("32 yaşındayım, Norwood 3 seviyesinde. Fotoğraf ekte."),
            ScriptedMessage.agent("Teşekkürler. Fotoğrafa göre ~3500 greft uygun. DHI paketimiz 78.000 TL — 4 gece otel, PRP, transferler ve 1 yıllık takip dahil."),
            ScriptedMessage.lead("Otel fiyata dahil mi peki?"),
            ScriptedMessage.agent("Evet, 4 yıldızlı otelde 4 gece konaklama, havalimanı transferleri ve tercüman ücretsiz. Sadece uçak biletiniz size ait."),
            ScriptedMessage.lead("Anladım, önümüzdeki 2 ay içinde hangi tarihler uygun?")
        ));
        SCRIPTS.put("tt_hair_RU", List.of(
            ScriptedMessage.lead("Здравствуйте, интересует пересадка волос."),
            ScriptedMessage.agent("Здравствуйте! 👋 Конечно, поможем. Можете прислать фото линии роста и сказать возраст?"),
            ScriptedMessage.lead("36 лет, Норвуд 4. Фото прилагаю."),
            ScriptedMessage.agent("Спасибо. По фото — рекомендуем ~4000 графтов, метод DHI. Пакет €3,200, включая отель 4 ночи, PRP и трансферы.")
        ));
        SCRIPTS.put("tt_hair_ES", List.of(
            ScriptedMessage.lead("Hola, vi sus resultados en Instagram. Me interesa un trasplante de pelo."),
            ScriptedMessage.agent("¡Hola! Gracias por escribirnos 👋 ¿Podría enviar una foto de su línea capilar y su edad?"),
            ScriptedMessage.lead("38 años, Norwood 3. Foto adjunta."),
            ScriptedMessage.agent("¡Gracias! Recomendamos ~3500 injertos con técnica DHI. El paquete es €2,900 — incluye hotel 4 noches, PRP y traslados.")
        ));

        // ---------- Rinoplasti ----------
        SCRIPTS.put("tt_rhino_EN", List.of(
            ScriptedMessage.lead("Hi, I'm thinking about a nose job. Do you do revisions as well?"),
            ScriptedMessage.agent("Hello! Yes, our surgeon Dr. Demir specializes in both primary and revision rhinoplasty. Could you share a side profile photo?"),
            ScriptedMessage.lead("Sure, attached. I had a previous surgery 5 years ago."),
            ScriptedMessage.agent("Thank you. Revision cases need an in-person consultation but based on the photo it's definitely feasible. Open technique recommended. Package €4,500, 6 nights hotel, all included."),
            ScriptedMessage.lead("Is there much swelling? When can I fly back?"),
            ScriptedMessage.agent("Cast comes off day 7. You can fly home day 8. Visible swelling subsides in 2-3 weeks, final result in ~12 months.")
        ));
        SCRIPTS.put("tt_rhino_AR", List.of(
            ScriptedMessage.lead("السلام عليكم، أرغب في عملية تجميل الأنف"),
            ScriptedMessage.agent("وعليكم السلام 👋 أهلاً بك. هل يمكنك إرسال صورة جانبية للأنف؟"),
            ScriptedMessage.lead("تفضل، الصورة مرفقة. عمري 28."),
            ScriptedMessage.agent("شكراً. التقنية المفتوحة مناسبة لحالتك. السعر 4,200€ شامل الفندق 6 ليالٍ والمواصلات والمتابعة."),
            ScriptedMessage.lead("ما هي فترة التعافي؟"),
            ScriptedMessage.agent("الجبيرة تُزال في اليوم السابع. يمكنك السفر في اليوم الثامن. التورم يختفي خلال 2-3 أسابيع.")
        ));

        // ---------- IVF / Tüp bebek ----------
        SCRIPTS.put("tt_ivf_EN", List.of(
            ScriptedMessage.lead("Hello, my husband and I are exploring IVF abroad. Saw your clinic on Instagram."),
            ScriptedMessage.agent("Hello and welcome 🌷 I'm sorry for what you've been through. Could you share roughly your age and how many cycles you've had so far?"),
            ScriptedMessage.lead("I'm 38, we've had 2 unsuccessful cycles in Germany."),
            ScriptedMessage.agent("Thank you for trusting us. We'll need your last AMH, FSH and any embryology reports. Our IVF + ICSI + PGT-A package is €6,800, includes 14 nights in apartment-hotel for the cycle."),
            ScriptedMessage.lead("Does the price include medication?"),
            ScriptedMessage.agent("Stim medication is roughly €1,200-1,800 extra depending on protocol — we send a detailed cost breakdown after the doctor reviews your reports."),
            ScriptedMessage.lead("Okay, I'll send the reports tonight.")
        ));
        SCRIPTS.put("tt_ivf_DE", List.of(
            ScriptedMessage.lead("Hallo, ich interessiere mich für IVF in der Türkei."),
            ScriptedMessage.agent("Hallo 🌷 schön, dass Sie uns kontaktieren. Können Sie mir Ihr Alter und die Anzahl bisheriger Zyklen mitteilen?"),
            ScriptedMessage.lead("Ich bin 36, 2 erfolglose Zyklen in Berlin."),
            ScriptedMessage.agent("Danke für Ihr Vertrauen. Senden Sie uns bitte Ihre AMH, FSH und ggf. PGT-Berichte. Unser Paket IVF + ICSI + PGT-A liegt bei €6.800, inkl. 14 Nächte Apartment-Hotel.")
        ));
        SCRIPTS.put("tt_ivf_IT", List.of(
            ScriptedMessage.lead("Buongiorno, vorrei informazioni sulla fecondazione in vitro."),
            ScriptedMessage.agent("Buongiorno 🌷 grazie per averci contattato. Ci può dire la sua età e quanti cicli ha già fatto?"),
            ScriptedMessage.lead("Ho 39 anni, 3 cicli in Italia senza successo."),
            ScriptedMessage.agent("La capisco. Il nostro pacchetto IVF + ICSI + PGT-A è €6.800, include 14 notti in residence. Ci invii AMH e FSH per una valutazione personalizzata.")
        ));

        // ---------- Diş tedavisi ----------
        SCRIPTS.put("tt_dental_EN", List.of(
            ScriptedMessage.lead("Hi, need full mouth dental implants. Quote?"),
            ScriptedMessage.agent("Hello! Full mouth (all-on-4 / all-on-6) starts at €4,200 per arch, or €7,800 for both. Includes 5-night hotel and CT scan. Could you share a recent panoramic X-ray?"),
            ScriptedMessage.lead("I don't have one. Can I get it there?"),
            ScriptedMessage.agent("Of course, the panoramic is free at our clinic on arrival day. How many teeth are currently missing or need extraction?"),
            ScriptedMessage.lead("Top — almost all. Bottom — 6 in the back. Any age limit for implants?"),
            ScriptedMessage.agent("Age isn't usually a limit, bone density matters more. Most all-on-4 patients are 55-75. Treatment is 2 visits — first for surgery + temporary teeth (5 days), second for permanent ones (4 months later, 7 days).")
        ));
        SCRIPTS.put("tt_dental_AR", List.of(
            ScriptedMessage.lead("مرحبا، أحتاج زراعة أسنان كاملة"),
            ScriptedMessage.agent("مرحبا 👋 الزراعة الكاملة (all-on-4) من 4,200€ لكل فك أو 7,800€ للفكين، شامل الفندق 5 ليالٍ."),
            ScriptedMessage.lead("هل يشمل السعر التيجان النهائية؟"),
            ScriptedMessage.agent("نعم، تيجان زيركون مع ضمان 10 سنوات. العلاج زيارتان: الأولى للجراحة (5 أيام)، والثانية بعد 4 أشهر للتيجان النهائية (7 أيام).")
        ));

        // ---------- BBL ----------
        SCRIPTS.put("tt_bbl_EN", List.of(
            ScriptedMessage.lead("Hi, interested in BBL. Do you do hybrid BBL too?"),
            ScriptedMessage.agent("Hello! Yes, our team does both traditional BBL and hybrid BBL (with implant + fat). Package starts at €5,200, all-inclusive."),
            ScriptedMessage.lead("Could you send before-after photos?"),
            ScriptedMessage.agent("Sending a few from this month 👇 (I'll DM the album link). What's your current BMI?"),
            ScriptedMessage.lead("BMI is around 27. Hoping for a more dramatic look."),
            ScriptedMessage.agent("That's great — at BMI 27 you have enough donor fat for a solid result. We recommend hybrid BBL for more projection. Recovery is 10-14 days, no sitting flat for 3 weeks.")
        ));
        SCRIPTS.put("tt_bbl_AR", List.of(
            ScriptedMessage.lead("السلام عليكم، أرغب في عملية BBL"),
            ScriptedMessage.agent("وعليكم السلام، أهلاً بك 🌷 لدينا BBL تقليدي و BBL هجين (بدمج السيليكون). السعر يبدأ من 5,200€ شامل كل شيء."),
            ScriptedMessage.lead("هل يمكنك إرسال صور قبل وبعد؟"),
            ScriptedMessage.agent("بالتأكيد، سأرسل ألبوم خاص الآن. ما هو وزنك وطولك حالياً؟")
        ));

        // ---------- Kardiyoloji ----------
        SCRIPTS.put("tt_cardio_AR", List.of(
            ScriptedMessage.lead("السلام عليكم، والدي يحتاج عملية قلب مفتوح"),
            ScriptedMessage.agent("وعليكم السلام، حفظ الله والدك 🌷 يرجى إرسال التقارير الأخيرة (ECG, Echo, Coronary Angiography) لمراجعتها من قبل البروفيسور."),
            ScriptedMessage.lead("سأرسلها الآن. متى يمكن إجراء العملية؟"),
            ScriptedMessage.agent("بعد مراجعة التقارير، يمكننا تحديد موعد خلال 5-7 أيام. التكلفة التقديرية 15,000-18,000€ شامل الإقامة 10 أيام في المستشفى وفندق 4 ليالٍ بعد الخروج."),
            ScriptedMessage.lead("هل يشمل ذلك العناية المركزة؟"),
            ScriptedMessage.agent("نعم، 2-3 أيام في العناية المركزة و7 أيام في الجناح الخاص، كل شيء شامل بدون رسوم إضافية.")
        ));
        SCRIPTS.put("tt_cardio_EN", List.of(
            ScriptedMessage.lead("Hi, my father needs open-heart surgery. Looking for options."),
            ScriptedMessage.agent("Hello — sorry to hear that. Could you share his latest ECG, Echo and angiography for our cardiology team to review?"),
            ScriptedMessage.lead("Sending now. How soon could surgery happen?"),
            ScriptedMessage.agent("After review, typically within 5-7 days. Total cost €15,000-18,000 including 10 days hospital stay (ICU + ward) and post-discharge hotel.")
        ));

        // ---------- Bariatrik ----------
        SCRIPTS.put("tt_bariatric_DE", List.of(
            ScriptedMessage.lead("Hallo, ich interessiere mich für einen Magenbypass."),
            ScriptedMessage.agent("Hallo! Was ist Ihr aktueller BMI? Wir bieten Magenbypass und Schlauchmagen an, ab €4,500 all-inclusive."),
            ScriptedMessage.lead("BMI 38. Mehrere Diäten ohne Erfolg."),
            ScriptedMessage.agent("Bei BMI 38 mit Begleiterkrankungen ist Bypass meist die bessere Option. 5 Nächte Krankenhaus + 2 Nächte Hotel. Erholung 3-4 Wochen.")
        ));
        SCRIPTS.put("tt_bariatric_EN", List.of(
            ScriptedMessage.lead("Hi, looking into gastric sleeve surgery."),
            ScriptedMessage.agent("Hello! Could you share your current BMI and any comorbidities (diabetes, hypertension, sleep apnea)?"),
            ScriptedMessage.lead("BMI 39, mild hypertension."),
            ScriptedMessage.agent("Sleeve is a great option for you. Package €4,500 all-inclusive: 4 nights hospital, 3 nights hotel, dietician follow-up for 12 months over WhatsApp.")
        ));

        // ---------- Ortopedi ----------
        SCRIPTS.put("tt_ortho_DE", List.of(
            ScriptedMessage.lead("Hallo, ich brauche eine Knie-Arthroskopie."),
            ScriptedMessage.agent("Hallo! Können Sie ein aktuelles MRT senden? Arthroskopie-Paket ab €3,800 inkl. 2 Nächte Klinik + 3 Nächte Hotel."),
            ScriptedMessage.lead("MRT folgt. Wie lange ist die Reha?"),
            ScriptedMessage.agent("6 Wochen, davon 2 Wochen mit Krücken. Wir senden einen detaillierten Reha-Plan und können Sie wöchentlich per Video begleiten.")
        ));

        // ---------- LASIK ----------
        SCRIPTS.put("tt_lasik_EN", List.of(
            ScriptedMessage.lead("Hi, considering LASIK. What's the price?"),
            ScriptedMessage.agent("Hello! Femto-LASIK both eyes is €1,400 including pre-op exams. Treatment takes 1 day; you can fly home the day after."),
            ScriptedMessage.lead("Are there any age limits?"),
            ScriptedMessage.agent("18-50 usually. Above 50 we often recommend LASEK or refractive lens exchange instead. What's your current prescription?")
        ));

        // ---------- Hollywood smile / lamine ----------
        SCRIPTS.put("tt_veneer_FR", List.of(
            ScriptedMessage.lead("Bonjour, je voudrais des facettes (Hollywood smile)."),
            ScriptedMessage.agent("Bonjour 👋 Notre forfait Hollywood smile (16 ou 20 facettes en porcelaine émax) commence à €3,200, hôtel 5 nuits inclus."),
            ScriptedMessage.lead("Quelle est la garantie ?"),
            ScriptedMessage.agent("10 ans sur les facettes en porcelaine. Le traitement prend 5-7 jours sur place — empreintes le jour 1, pose finale le jour 5.")
        ));

        // ---------- Generic fallback ----------
        SCRIPTS.put("generic_EN", List.of(
            ScriptedMessage.lead("Hi, I'd like more information about your treatments."),
            ScriptedMessage.agent("Hello and welcome to HealthVia 👋 Which treatment are you considering? We can send a detailed package with photos and timeline."),
            ScriptedMessage.lead("Just exploring for now. What's most popular?"),
            ScriptedMessage.agent("Our top three are hair transplant, dental implants and rhinoplasty. All packages include hotel, transfers and follow-up. Happy to share details on whichever interests you.")
        ));
    }
}
