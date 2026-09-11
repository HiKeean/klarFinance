package com.api.klarfinance.los.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_pefindo_inquiries", schema = "los")
public class PefindoInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String score;

    @Column(name = "col_status")
    private Integer colStatus;

    @Column(name = "col_history_years_ago")
    private Integer colHistoryYearsAgo;

    @Column(name = "pdf_path_file")
    private String pdfPathFile;

    @Column(name = "raw_response")
    private String rawResponse;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
