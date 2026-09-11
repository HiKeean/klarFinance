package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.LimitApplication;
import com.api.klarfinance.los.repository.LimitApplicationRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Assignment real-time antrean Checker (konfirmasi user 2026-08-31, revisi timing lock 2026-08-31)
 * - satu aplikasi cuma boleh dipegang satu Checker dalam satu waktu, di-assign random ke Checker
 * yang "available" (online + gak lagi pegang aplikasi lain). Assignment (siapa pemiliknya) dan
 * lock 2 jam adalah dua fase terpisah: connect WebSocket cuma bikin dia "online" dan boleh
 * ditawarin aplikasi (lihat {@link #lockAndNotify}), tapi timer 2 jam baru beneran mulai begitu
 * checker itu fetch queue-nya buat pertama kali / benar-benar dapat aplikasi buat dicek (lihat
 * {@link #startReviewIfNeeded}) - biar checker yang online tapi belum sempat buka gak kebuang
 * waktu review-nya sia-sia. Semua state assignment/lock hidup di Redis (bukan kolom DB) - lihat
 * {@link CheckerLockExpirationListener} buat sisi event-driven-nya (key-expiry Redis, bukan
 * scheduled job, sesuai permintaan user).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckerAssignmentService {
    public static final Duration LOCK_TTL = Duration.ofHours(2);
    /** Batas waktu checker buka aplikasi yang ditawarin sebelum di-lempar ulang ke checker lain. */
    public static final Duration OFFER_TTL = Duration.ofMinutes(5);
    private static final String ONLINE_SET = "los:checker:online";

    private final StringRedisTemplate redis;
    private final LimitApplicationRepository limitApplicationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    private String lockKey(Integer applicationId) { return "los:checker:lock:" + applicationId; }
    private String currentKey(String checkerIdentity) { return "los:checker:current:" + checkerIdentity; }
    private String ownerKey(Integer applicationId) { return "los:checker:owner:" + applicationId; }
    private String offerKey(Integer applicationId) { return "los:checker:offer:" + applicationId; }

    public void markOnline(String identity) {
        redis.opsForSet().add(ONLINE_SET, identity);
        log.info("Checker {} online", identity);
        assignNextApplicationToChecker(identity);
    }

    public void markOffline(String identity) {
        redis.opsForSet().remove(ONLINE_SET, identity);
        log.info("Checker {} offline", identity);
        releaseIfNeverStarted(identity);
    }

    /**
     * Checker klik tombol "Istirahat" - beda dari disconnect pasif (markOffline, yang MASIH
     * ngebiarin lock jalan sampai 2 jam kalau udah sempat dibuka, lihat {@link
     * #releaseIfNeverStarted}). Ini FULL release: apapun yang lagi dipegang (masih fase offer
     * ATAU udah fase lock) langsung dilepas dan dilempar ke checker available lain, baru online
     * status-nya dicabut. Sengaja gitu karena checker ini secara eksplisit bilang mau berhenti
     * review, bukan sekadar koneksi putus.
     */
    public void takeBreak(String identity) {
        String appIdValue = redis.opsForValue().get(currentKey(identity));
        redis.opsForSet().remove(ONLINE_SET, identity);

        if (appIdValue == null) {
            log.info("Checker {} istirahat (gak lagi pegang aplikasi apapun)", identity);
            return;
        }

        Integer applicationId = Integer.valueOf(appIdValue);
        log.info("Checker {} istirahat sambil pegang aplikasi {} - dilepas paksa, dilempar ke checker lain", identity, applicationId);
        redis.delete(currentKey(identity));
        redis.delete(ownerKey(applicationId));
        redis.delete(offerKey(applicationId));
        redis.delete(lockKey(applicationId));

        limitApplicationRepository.findById(applicationId)
                .filter(app -> EngineStatus.PENDING_CHECKER.equals(app.getStatus()))
                .ifPresent(app -> assignApplicationToRandomChecker(app, identity));
    }

    /**
     * Kalau checker disconnect sebelum sempat buka (fetch queue) aplikasi yang ditawarin ke dia -
     * timer 2 jam belum pernah jalan (lihat {@link #startReviewIfNeeded}) - jadi tawaran itu
     * dilepas dan dilempar random ke checker available lain, bukan nyangkut nunggu dia balik.
     * Kalau timer-nya udah kepajang jalan (dia sempat buka), biarin tetap jalan sampai 2 jam,
     * konsisten sama perilaku lama.
     */
    private void releaseIfNeverStarted(String identity) {
        String appIdValue = redis.opsForValue().get(currentKey(identity));
        if (appIdValue == null) return;

        Integer applicationId = Integer.valueOf(appIdValue);
        if (Boolean.TRUE.equals(redis.hasKey(lockKey(applicationId)))) {
            log.info("Checker {} offline, tapi udah mulai review aplikasi {} - lock tetap jalan sampai 2 jam", identity, applicationId);
            return;
        }

        log.info("Checker {} offline sebelum sempat buka aplikasi {} yang ditawarkan, lempar ke checker available lain", identity, applicationId);
        redis.delete(currentKey(identity));
        redis.delete(ownerKey(applicationId));
        redis.delete(offerKey(applicationId));
        limitApplicationRepository.findById(applicationId)
                .filter(app -> EngineStatus.PENDING_CHECKER.equals(app.getStatus()))
                .ifPresent(app -> assignApplicationToRandomChecker(app, identity));
    }

    public boolean isAvailable(String identity) {
        boolean online = Boolean.TRUE.equals(redis.opsForSet().isMember(ONLINE_SET, identity));
        boolean busy = Boolean.TRUE.equals(redis.hasKey(currentKey(identity)));
        return online && !busy;
    }

    /** Dipanggil begitu ada LimitApplication baru berstatus PENDING_CHECKER. */
    public void assignNewApplication(LimitApplication application) {
        assignApplicationToRandomChecker(application, null);
    }

    /** Dipanggil begitu Checker submit keputusan (approve/reject) - lepas kunci lalu coba kasih dia kerjaan berikutnya. */
    public void release(Integer applicationId) {
        String owner = redis.opsForValue().get(ownerKey(applicationId));
        redis.delete(currentKeyFor(owner));
        redis.delete(lockKey(applicationId));
        redis.delete(ownerKey(applicationId));
        redis.delete(offerKey(applicationId));

        if (owner != null) assignNextApplicationToChecker(owner);
    }

    /** Cek siapa pemilik lock saat ini - dipakai buat validasi endpoint decision (cegah 2 checker rebutan). */
    public Optional<String> currentOwner(Integer applicationId) {
        return Optional.ofNullable(redis.opsForValue().get(ownerKey(applicationId)));
    }

    /** Aplikasi yang lagi dipegang identity ini sekarang (kalau ada). */
    public Optional<Integer> currentAssignment(String identity) {
        String value = redis.opsForValue().get(currentKey(identity));
        return value == null ? Optional.empty() : Optional.of(Integer.valueOf(value));
    }

    /** Dipanggil dari {@link CheckerLockExpirationListener} pas key lock expired (event-driven, bukan scheduler). */
    public void handleExpiry(Integer applicationId) {
        String previousOwner = redis.opsForValue().get(ownerKey(applicationId));
        redis.delete(currentKeyFor(previousOwner));
        redis.delete(ownerKey(applicationId));

        LimitApplication application = limitApplicationRepository.findById(applicationId).orElse(null);
        if (application == null || !EngineStatus.PENDING_CHECKER.equals(application.getStatus())) {
            return; // sudah diputuskan checker tepat sebelum expiry - race condition aman, gak perlu reassign
        }

        log.info("Lock aplikasi {} expired (2 jam) di checker {}, cari checker lain", applicationId, previousOwner);
        if (previousOwner != null) {
            notify(previousOwner, "EXPIRED", applicationId, "Waktu review 2 jam habis. Aplikasi ini dialihkan ke Checker lain.");
        }
        assignApplicationToRandomChecker(application, previousOwner);
    }

    private void assignApplicationToRandomChecker(LimitApplication application, String excludeIdentity) {
        List<String> available = new ArrayList<>();
        Set<String> online = redis.opsForSet().members(ONLINE_SET);
        if (online != null) {
            for (String identity : online) {
                if (identity.equals(excludeIdentity)) continue;
                if (isAvailable(identity)) available.add(identity);
            }
        }
        if (available.isEmpty()) {
            log.info("Gak ada Checker available buat aplikasi {}, tetap di antrean unassigned", application.getId());
            return;
        }
        String chosen = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        lockAndNotify(application.getId(), chosen);
    }

    private void assignNextApplicationToChecker(String identity) {
        if (!isAvailable(identity)) return;
        findOldestUnassigned().ifPresent(application -> lockAndNotify(application.getId(), identity));
    }

    private Optional<LimitApplication> findOldestUnassigned() {
        return limitApplicationRepository.findByStatusOrderByCreatedAtAsc(EngineStatus.PENDING_CHECKER).stream()
                .filter(app -> !Boolean.TRUE.equals(redis.hasKey(ownerKey(app.getId()))))
                .findFirst();
    }

    /**
     * Nawarin aplikasi ke checker - BELUM nyalain lock 2 jam di sini. Lock beneran nyala pas
     * checker itu fetch queue-nya buat pertama kali (lihat {@link #startReviewIfNeeded}), biar
     * checker yang cuma online tapi belum sempat buka gak kebuang waktu review-nya sia-sia.
     */
    private void lockAndNotify(Integer applicationId, String checkerIdentity) {
        redis.opsForValue().set(currentKey(checkerIdentity), String.valueOf(applicationId));
        redis.opsForValue().set(ownerKey(applicationId), checkerIdentity);
        redis.opsForValue().set(offerKey(applicationId), checkerIdentity, OFFER_TTL);
        log.info("Aplikasi {} ditawarkan ke checker {} (harus dibuka dalam {})", applicationId, checkerIdentity, OFFER_TTL);
        notify(checkerIdentity, "ASSIGNED", applicationId, "Ada pengajuan baru buat direview.");
    }

    /**
     * Dipanggil pas checker fetch queue-nya (buka/refresh halaman Approval) - titik ini yang
     * dianggap "checker sudah dapat yang mau dicek", jadi timer 2 jam beneran mulai jalan di sini
     * dan trigger timeout fase "offer" ({@link #offerKey}) dicabut karena udah gak relevan lagi.
     * Idempotent (pakai SETNX) - refresh berkali-kali gak reset timer yang udah jalan.
     */
    public void startReviewIfNeeded(Integer applicationId, String identity) {
        Boolean justStarted = redis.opsForValue().setIfAbsent(lockKey(applicationId), identity, LOCK_TTL);
        if (Boolean.TRUE.equals(justStarted)) {
            redis.delete(offerKey(applicationId));
            redis.expire(currentKey(identity), LOCK_TTL);
            redis.expire(ownerKey(applicationId), LOCK_TTL);
            log.info("Checker {} mulai review aplikasi {}, lock 2 jam jalan", identity, applicationId);
        }
    }

    /**
     * Dipanggil dari {@link CheckerLockExpirationListener} pas key offer ({@link #offerKey})
     * expired (event-driven) - checker yang ditawarin gak buka-buka aplikasinya dalam {@link
     * #OFFER_TTL}, jadi dilempar random ke checker available lain, sama kayak {@link
     * #handleExpiry} tapi buat fase sebelum review mulai.
     */
    public void handleOfferExpiry(Integer applicationId) {
        if (Boolean.TRUE.equals(redis.hasKey(lockKey(applicationId)))) {
            return; // keburu mulai review pas offer mau expired - race aman, gak perlu reassign
        }

        String previousOwner = redis.opsForValue().get(ownerKey(applicationId));
        redis.delete(currentKeyFor(previousOwner));
        redis.delete(ownerKey(applicationId));

        LimitApplication application = limitApplicationRepository.findById(applicationId).orElse(null);
        if (application == null || !EngineStatus.PENDING_CHECKER.equals(application.getStatus())) {
            return;
        }

        log.info("Offer aplikasi {} ke checker {} expired ({}, gak dibuka-buka), cari checker lain", applicationId, previousOwner, OFFER_TTL);
        if (previousOwner != null) {
            notify(previousOwner, "OFFER_EXPIRED", applicationId, "Kamu gak buka aplikasi ini tepat waktu. Aplikasi ini dialihkan ke Checker lain.");
        }
        assignApplicationToRandomChecker(application, previousOwner);
    }

    /**
     * `convertAndSendToUser` gagal diam-diam (gak throw, gak retry) kalau gak ada sesi WS aktif
     * yang ke-registrasi buat identity ini pas saat ini - bisa kejadian race antara
     * `markOnline()` (jalan begitu server terima SessionConnectedEvent) dengan client yang belum
     * selesai subscribe ke `/user/queue/assignment`. Log ini biar ketauan dari server side kalau
     * push-nya kemungkinan besar kelewat, bukan cuma nebak dari sisi frontend.
     */
    private void notify(String identity, String type, Integer applicationId, String message) {
        boolean hasActiveSession = simpUserRegistry.getUser(identity) != null;
        if (!hasActiveSession) {
            log.warn("Push {} ke checker {} (aplikasi {}) kemungkinan kelewat - gak ada sesi WS aktif ke-registrasi saat ini",
                    type, identity, applicationId);
        }
        messagingTemplate.convertAndSendToUser(identity, "/queue/assignment",
                new AssignmentNotification(type, applicationId, message));
    }

    private String currentKeyFor(String identity) {
        return identity == null ? "los:checker:current:__none__" : currentKey(identity);
    }

    public record AssignmentNotification(String type, Integer applicationId, String message) {}
}
