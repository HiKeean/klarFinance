package com.api.klarfinance.transjakarta.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.model.Loan;
import com.api.klarfinance.fin.service.LoanService;
import com.api.klarfinance.transjakarta.dto.request.PurchaseTicketRequest;
import com.api.klarfinance.transjakarta.dto.response.TicketResponse;
import com.api.klarfinance.transjakarta.model.TransjakartaTicket;
import com.api.klarfinance.transjakarta.repository.TransjakartaTicketRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Mockup beli tiket Transjakarta (konfirmasi user 2026-09-10) - GENERIK, gak ada pilih halte/rute
 * (GTFS+maps dibuang di sesi lanjutan yang sama - trip_id-nya gak reliable, lihat
 * transjakarta-feature knowledge). Nasabah beli qty N tiket sekaligus, dipotong dari plafond umum
 * langsung (gak ada kuota terpisah kayak QRIS), harga flat, tenor+agregasi bulanan ditangani
 * LoanService#purchaseTransjakartaTicket. Gak ada integrasi scan-gate beneran - status
 * used/belum ditandai MANUAL sama nasabah sendiri di app (toggleUsed), bukan validasi real.
 */
@Service
@RequiredArgsConstructor
public class TransjakartaTicketService {

    /** Tarif flat Transjakarta (konfirmasi user - GTFS punya fare_attributes.txt tapi gak ada
     * fare_rules.txt buat mapping ke rute/waktu spesifik, jadi gak reliable dipakai otomatis).
     * Berlaku buat rute standar - TIDAK berlaku buat rute jauh (mis. Bogor-PIK2, Blok M-PIK2),
     * disclaimer ditampilkan di Android, BUKAN divalidasi di sini (gak ada data rute buat cek). */
    private static final BigDecimal TICKET_PRICE = BigDecimal.valueOf(3500);
    private static final int MAX_QTY_PER_PURCHASE = 10;

    private final TransjakartaTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final LoanService loanService;

    @Transactional
    public List<TicketResponse> purchase(PurchaseTicketRequest request, Principal principal) {
        User nasabah = currentNasabah(principal);
        int qty = request.getQty() == null ? 0 : request.getQty();
        if (qty <= 0 || qty > MAX_QTY_PER_PURCHASE) {
            throw new IllegalArgumentException("qty harus antara 1 dan " + MAX_QTY_PER_PURCHASE);
        }

        BigDecimal totalAmount = TICKET_PRICE.multiply(BigDecimal.valueOf(qty));
        // Satu panggilan buat SELURUH qty (bukan per-tiket) - bunga dihitung sekali atas total,
        // matematis sama hasilnya (persentase flat, gak ada compounding) tapi lebih efisien dan
        // semua tiket dari 1 pembelian otomatis numpuk ke Loan bulan berjalan yang sama.
        Loan loan = loanService.purchaseTransjakartaTicket(nasabah, totalAmount);

        return IntStream.range(0, qty)
                .mapToObj(i -> ticketRepository.save(TransjakartaTicket.builder()
                        .ticketCode(UUID.randomUUID().toString())
                        .user(nasabah)
                        .loan(loan)
                        .amount(TICKET_PRICE)
                        .used(false)
                        .build()))
                .map(ticket -> toResponse(ticket, loan))
                .toList();
    }

    public List<TicketResponse> myTickets(Principal principal) {
        User nasabah = currentNasabah(principal);
        return ticketRepository.findByUser_IdOrderByPurchasedAtDesc(nasabah.getId()).stream()
                .map(ticket -> toResponse(ticket, ticket.getLoan()))
                .toList();
    }

    /** Toggle used/belum - MOCKUP, nasabah sendiri yang tandain (konfirmasi user "dari pencet
     * used atau belumnya saja"), gak ada validasi gate beneran. */
    @Transactional
    public TicketResponse toggleUsed(String ticketCode, Principal principal) {
        User nasabah = currentNasabah(principal);
        TransjakartaTicket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new IllegalArgumentException("Tiket tidak ditemukan"));
        if (!ticket.getUser().getId().equals(nasabah.getId())) {
            throw new IllegalStateException("Tiket ini bukan milik Anda");
        }
        ticket.setUsed(!ticket.isUsed());
        ticketRepository.save(ticket);
        return toResponse(ticket, ticket.getLoan());
    }

    private TicketResponse toResponse(TransjakartaTicket ticket, Loan loan) {
        LocalDateTime dueDate = YearMonth.parse(loan.getBillingCycle())
                .plusMonths(1)
                .atDay(LoanService.TRANSJAKARTA_DUE_DAY)
                .atStartOfDay();

        return TicketResponse.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .amount(ticket.getAmount())
                .used(ticket.isUsed())
                .purchasedAt(ticket.getPurchasedAt())
                .loanId(loan.getId())
                .billingCycle(loan.getBillingCycle())
                .dueDate(dueDate)
                .build();
    }

    private User currentNasabah(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null || !"NASABAH".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only Nasabah can buy Transjakarta tickets");
        }
        return user;
    }
}
