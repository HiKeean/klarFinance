package com.api.klarfinance.transjakarta.repository;

import com.api.klarfinance.transjakarta.model.TransjakartaTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransjakartaTicketRepository extends JpaRepository<TransjakartaTicket, Integer> {

    @Query("SELECT t FROM TransjakartaTicket t " +
            "JOIN FETCH t.loan l " +
            "WHERE t.user.id = :userId " +
            "ORDER BY t.purchasedAt DESC")
    List<TransjakartaTicket> findByUser_IdOrderByPurchasedAtDesc(@Param("userId") Integer userId);

    @Query("SELECT t FROM TransjakartaTicket t " +
            "JOIN FETCH t.loan " +
            "WHERE t.ticketCode = :ticketCode")
    Optional<TransjakartaTicket> findByTicketCode(@Param("ticketCode") String ticketCode);

    List<TransjakartaTicket> findByLoan_IdOrderByPurchasedAtDesc(Integer loanId);
}
