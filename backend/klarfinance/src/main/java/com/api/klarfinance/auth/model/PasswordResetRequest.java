package com.api.klarfinance.auth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Permintaan reset password yang muncul dari tombol "Lupa Password" di halaman login (frontend
 * Checker&BM, setelah 3x salah password - lihat LoginPage) - masuk antrean di webadmin buat
 * di-approve/reject Superadmin. Approve -> PasswordResetRequestService generate password baru
 * random + kirim WhatsApp (lihat KirimiWhatsappService#sendPasswordResetMessage).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_password_reset_request", schema = "auth")
public class PasswordResetRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    private String reason;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) {
            status = STATUS_PENDING;
        }
    }
}
