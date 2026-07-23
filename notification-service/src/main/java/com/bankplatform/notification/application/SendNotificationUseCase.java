package com.bankplatform.notification.application;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.notification.application.port.ChannelAdapter;
import com.bankplatform.notification.application.port.NotificationRepository;
import com.bankplatform.notification.domain.Channel;
import com.bankplatform.notification.domain.DeliveryAttempt;
import com.bankplatform.notification.domain.Notification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final EventPublisher eventPublisher;
    private final Map<Channel, ChannelAdapter> adaptersByChannel;

    public SendNotificationUseCase(
            NotificationRepository notificationRepository,
            EventPublisher eventPublisher,
            List<ChannelAdapter> channelAdapters) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.adaptersByChannel =
                channelAdapters.stream()
                        .collect(Collectors.toMap(ChannelAdapter::channel, Function.identity()));
    }

    @Transactional
    public void send(UUID recipientUserId, Channel channel, String template, String message) {
        Notification notification =
                Notification.create(recipientUserId, channel, template, message);
        ChannelAdapter.DeliveryResult result =
                adaptersByChannel.get(channel).send(recipientUserId, message);

        if (result.success()) {
            notification.markSent();
        } else {
            notification.markFailed();
        }
        Notification saved = notificationRepository.save(notification);
        notificationRepository.saveDeliveryAttempt(
                DeliveryAttempt.of(saved.id(), result.success(), result.failureReason()));

        String eventType = result.success() ? "notification-sent" : "notification-failed";
        eventPublisher.publish(
                eventType,
                "Notification",
                saved.id().toString(),
                new NotificationOutcomePayload(
                        saved.id().toString(),
                        recipientUserId.toString(),
                        channel.name(),
                        template));
    }
}
