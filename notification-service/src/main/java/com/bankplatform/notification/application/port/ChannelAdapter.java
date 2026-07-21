package com.bankplatform.notification.application.port;

import com.bankplatform.notification.domain.Channel;
import java.util.UUID;

public interface ChannelAdapter {

    Channel channel();

    DeliveryResult send(UUID recipientUserId, String message);

    record DeliveryResult(boolean success, String failureReason) {
        public static DeliveryResult delivered() {
            return new DeliveryResult(true, null);
        }
    }
}
