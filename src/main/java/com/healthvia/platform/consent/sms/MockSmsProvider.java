package com.healthvia.platform.consent.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("📱 [MOCK-SMS] → {} : {}", phoneNumber, message);
    }

    @Override
    public void sendWhatsApp(String phoneNumber, String message) {
        log.info("💬 [MOCK-WA] → {} : {}", phoneNumber, message);
    }

    @Override
    public String name() {
        return "MOCK";
    }
}
