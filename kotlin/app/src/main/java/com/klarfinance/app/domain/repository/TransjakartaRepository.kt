package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.TransjakartaTicket

interface TransjakartaRepository {
    /** POST api/v1/nasabah/transjakarta/tickets - beli qty tiket sekaligus, balik semua tiket
     * yang baru dibuat. */
    suspend fun purchaseTickets(qty: Int): Result<List<TransjakartaTicket>>

    /** GET api/v1/nasabah/transjakarta/tickets - rincian semua tiket yang pernah dibeli. */
    suspend fun myTickets(): Result<List<TransjakartaTicket>>

    /** POST api/v1/nasabah/transjakarta/tickets/{ticketCode}/toggle-used - mockup, ditandai
     * manual sama nasabah. */
    suspend fun toggleUsed(ticketCode: String): Result<TransjakartaTicket>
}
