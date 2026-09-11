package com.api.klarfinance.global;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Push notification (FCM) - dipakai buat approve/reject pengajuan ke nasabah (lihat
 * LosApprovalService#bmDecide, satu-satunya titik yang beneran nge-set status final
 * APPROVED/REJECTED). Kegagalan kirim (token gak ada, FirebaseApp belum ke-init karena service
 * account belum di-setup - lihat FirebaseConfig, atau token expired/uninstalled) SENGAJA gak
 * pernah throw - approve/reject adalah aksi bisnis inti yang gak boleh gagal cuma gara-gara
 * notifikasinya gagal terkirim.
 */
@Service
@Slf4j
public class PushNotificationService {

    public void send(String fcmToken, String title, String body) {
        send(fcmToken, title, body, Collections.emptyMap());
    }

    /**
     * @param data structured payload delivered alongside the display notification (e.g.
     *             {@code type=LOAN_APPROVAL, status=APPROVED}) - read by
     *             KlarFirebaseMessagingService#onMessageReceived's {@code message.data} even
     *             while the app is in the foreground, so the client can react (refresh account
     *             state) instead of only showing a system tray notification.
     */
    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        if (!StringUtils.hasText(fcmToken)) {
            log.info("Skip push notification - nasabah belum punya FCM token terdaftar");
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Skip push notification ke token {}... - Firebase belum ke-init (lihat FirebaseConfig)",
                    fcmToken.substring(0, Math.min(8, fcmToken.length())));
            return;
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("Gagal kirim push notification (kemungkinan token stale/uninstalled): {}", e.getMessage());
        }
    }
}
