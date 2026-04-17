package com.healthvia.platform.consent.sms;

/**
 * SMS transport abstraction. Implementations:
 *   - MockSmsProvider → logs only, returns a fixed OTP in dev.
 *   - NetgsmSmsProvider (stub) → Netgsm HTTP API once credentials are provisioned.
 *   - IletiMerkeziSmsProvider (stub) → alternative Turkish provider.
 *
 * Selection via `sms.provider` property in application.yml.
 */
public interface SmsProvider {
    void send(String phoneNumber, String message);

    /** For channels like WhatsApp that may have a different sender path. */
    default void sendWhatsApp(String phoneNumber, String message) {
        send(phoneNumber, "[WA] " + message);
    }

    String name();
}
