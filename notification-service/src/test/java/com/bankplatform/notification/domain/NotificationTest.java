package com.bankplatform.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void createsAPendingNotification() {
        UUID recipientId = UUID.randomUUID();

        Notification notification =
                Notification.create(recipientId, Channel.EMAIL, "welcome", "hi");

        assertThat(notification.recipientUserId()).isEqualTo(recipientId);
        assertThat(notification.channel()).isEqualTo(Channel.EMAIL);
        assertThat(notification.status()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void marksSent() {
        Notification notification =
                Notification.create(UUID.randomUUID(), Channel.SMS, "otp", "123456");

        notification.markSent();

        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void marksFailed() {
        Notification notification =
                Notification.create(UUID.randomUUID(), Channel.PUSH, "alert", "text");

        notification.markFailed();

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
    }
}
