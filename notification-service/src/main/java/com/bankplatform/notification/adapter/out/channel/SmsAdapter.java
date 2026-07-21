package com.bankplatform.notification.adapter.out.channel;

import com.bankplatform.notification.application.port.ChannelAdapter;
import com.bankplatform.notification.domain.Channel;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Mock provider — logs the send and always succeeds. No real SMS integration in this phase. */
@Component
public class SmsAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmsAdapter.class);

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public DeliveryResult send(UUID recipientUserId, String message) {
        log.info("[mock sms] to user {}: {}", recipientUserId, message);
        return DeliveryResult.delivered();
    }
}
