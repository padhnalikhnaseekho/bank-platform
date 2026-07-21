package com.bankplatform.notification.adapter.out.persistence;

import com.bankplatform.notification.application.port.NotificationRepository;
import com.bankplatform.notification.domain.Channel;
import com.bankplatform.notification.domain.DeliveryAttempt;
import com.bankplatform.notification.domain.Notification;
import com.bankplatform.notification.domain.NotificationId;
import com.bankplatform.notification.domain.NotificationStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;
    private final DeliveryAttemptJpaRepository deliveryAttemptJpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository,
            DeliveryAttemptJpaRepository deliveryAttemptJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.deliveryAttemptJpaRepository = deliveryAttemptJpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = new NotificationEntity(notification.id().value(),
                notification.recipientUserId(), notification.channel().name(), notification.template(),
                notification.message(), notification.status().name(), notification.createdAt());
        NotificationEntity saved = notificationJpaRepository.save(entity);
        return new Notification(NotificationId.of(saved.getId()), saved.getRecipientUserId(),
                Channel.valueOf(saved.getChannel()), saved.getTemplate(), saved.getMessage(),
                NotificationStatus.valueOf(saved.getStatus()), saved.getCreatedAt());
    }

    @Override
    public void saveDeliveryAttempt(DeliveryAttempt attempt) {
        deliveryAttemptJpaRepository.save(new DeliveryAttemptEntity(attempt.id(), attempt.notificationId().value(),
                attempt.success(), attempt.failureReason(), attempt.attemptedAt()));
    }
}
