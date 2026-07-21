package com.bankplatform.notification.application.port;

import com.bankplatform.notification.domain.DeliveryAttempt;
import com.bankplatform.notification.domain.Notification;

public interface NotificationRepository {

    Notification save(Notification notification);

    void saveDeliveryAttempt(DeliveryAttempt attempt);
}
