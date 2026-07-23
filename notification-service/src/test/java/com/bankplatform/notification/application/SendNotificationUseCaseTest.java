package com.bankplatform.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankplatform.common.event.EventPublisher;
import com.bankplatform.notification.application.port.ChannelAdapter;
import com.bankplatform.notification.application.port.NotificationRepository;
import com.bankplatform.notification.domain.Channel;
import com.bankplatform.notification.domain.DeliveryAttempt;
import com.bankplatform.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseTest {

    @Mock private NotificationRepository notificationRepository;

    @Mock private EventPublisher eventPublisher;

    @Mock private ChannelAdapter emailAdapter;

    @Mock private ChannelAdapter smsAdapter;

    private SendNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        when(emailAdapter.channel()).thenReturn(Channel.EMAIL);
        when(smsAdapter.channel()).thenReturn(Channel.SMS);
        useCase =
                new SendNotificationUseCase(
                        notificationRepository, eventPublisher, List.of(emailAdapter, smsAdapter));
    }

    @Test
    void routesToTheAdapterForTheRequestedChannelAndPublishesNotificationSentOnSuccess() {
        when(emailAdapter.send(any(), any())).thenReturn(ChannelAdapter.DeliveryResult.delivered());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        UUID recipientId = UUID.randomUUID();

        useCase.send(recipientId, Channel.EMAIL, "welcome", "hello");

        verify(emailAdapter).send(recipientId, "hello");
        verify(smsAdapter, org.mockito.Mockito.never()).send(any(), any());
        ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().status().name()).isEqualTo("SENT");
        verify(notificationRepository).saveDeliveryAttempt(any(DeliveryAttempt.class));
        verify(eventPublisher).publish(eq("notification-sent"), eq("Notification"), any(), any());
    }

    @Test
    void publishesNotificationFailedWhenTheAdapterFailsToDeliver() {
        when(smsAdapter.send(any(), any()))
                .thenReturn(new ChannelAdapter.DeliveryResult(false, "provider down"));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase.send(UUID.randomUUID(), Channel.SMS, "otp", "123456");

        ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().status().name()).isEqualTo("FAILED");
        verify(eventPublisher).publish(eq("notification-failed"), eq("Notification"), any(), any());
    }
}
