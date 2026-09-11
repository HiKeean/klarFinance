package com.api.klarfinance.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Push notification (FCM) approve/reject pengajuan ke nasabah - lihat PushNotificationService.
 * Service account JSON (Firebase Console > Project Settings > Service accounts) HARUS di-download
 * manual, gak bisa di-generate lewat kode - kalau file-nya belum ada di path yang dikonfigurasi
 * ({@code app.firebase.service-account-path}, default {@code firebase-service-account.json} di
 * root backend/klarfinance/), init di-skip (log warning) daripada gagal start Spring context -
 * biar dev yang belum sempat setup Firebase tetap bisa jalanin backend normal.
 */
@Component
@Slf4j
public class FirebaseConfig {
    @Value("${app.firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        Path path = Path.of(serviceAccountPath);
        if (!Files.exists(path)) {
            log.warn("Firebase service account tidak ditemukan di '{}' (dicari di '{}', working dir proses = '{}') "
                    + "- push notification (FCM) dinonaktifkan. Kalau file-nya sebenarnya sudah ada, working dir "
                    + "proses ini kemungkinan beda dari yang dikira - set app.firebase.service-account-path "
                    + "(atau FIREBASE_SERVICE_ACCOUNT_PATH di .env.dev) ke path absolut.",
                    serviceAccountPath, path.toAbsolutePath(), System.getProperty("user.dir"));
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized dari '{}'", serviceAccountPath);
            }
        } catch (IOException e) {
            log.error("Gagal inisialisasi Firebase Admin SDK dari '{}' - push notification (FCM) dinonaktifkan", serviceAccountPath, e);
        }
    }
}
