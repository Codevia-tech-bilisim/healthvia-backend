package com.healthvia.platform.consent.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Netgsm (netgsm.com.tr) XML/HTTP SMS gateway stub.
 * Activate via `sms.provider=netgsm` + set netgsm.username/password/header.
 * Current implementation logs intent; wire the real HTTP call once the merchant
 * contract is signed. Keeps the codebase compiling under any environment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "netgsm")
public class NetgsmSmsProvider implements SmsProvider {

    @Value("${netgsm.username:}")
    private String username;

    @Value("${netgsm.password:}")
    private String password;

    @Value("${netgsm.header:HEALTHVIA}")
    private String header;

    @Override
    public void send(String phoneNumber, String message) {
        log.info("📱 [NETGSM stub: {} / {}] → {} : {}", header, username.isEmpty() ? "no-auth" : "ok", phoneNumber, message);
        // TODO: implement real Netgsm XML/HTTP POST once credentials provisioned.
    }

    @Override
    public String name() {
        return "NETGSM";
    }
}
