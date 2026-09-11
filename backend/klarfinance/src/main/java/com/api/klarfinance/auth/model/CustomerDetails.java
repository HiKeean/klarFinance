package com.api.klarfinance.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.api.klarfinance.dbo.model.Village;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_customer_details", schema = "auth")
public class CustomerDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private String nik;

    private String npwp;

    private String name;

    private String address;

    private String email;

    @Column(name = "foto_ktp")
    private String fotoKtp;

    @Column(name = "foto_kyc")
    private String fotoKyc;

    @Column(name = "address_2")
    private String address2;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /** FCM registration token buat push notification approve/reject (lihat PushNotificationService)
     * - diisi Kotlin app abis login/register (AuthInterceptor pattern yang sama), bisa null kalau
     * belum pernah login sejak fitur ini ada, permission notifikasi ditolak, atau Play Services
     * gak tersedia (mis. device tanpa GMS). */
    @Column(name = "fcm_token")
    private String fcmToken;

    /** Kode unik yang dibagikan nasabah ini ke orang lain untuk fitur referral (lihat
     * ReferralService.generateUniqueCode) - digenerate sekali saat register, tidak pernah berubah. */
    @Column(name = "referral_code", unique = true)
    private String referralCode;

    private LocalDate dob;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="village_id")
    private Village village;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
