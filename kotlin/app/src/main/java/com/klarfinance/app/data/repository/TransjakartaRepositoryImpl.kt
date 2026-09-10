package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.PurchaseTicketRequestDto
import com.klarfinance.app.data.dto.TicketDto
import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import javax.inject.Inject

class TransjakartaRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : TransjakartaRepository {

    override suspend fun purchaseTickets(qty: Int): Result<List<TransjakartaTicket>> = runCatching {
        val response = apiService.post<List<TicketDto>, PurchaseTicketRequestDto>(
            "api/v1/nasabah/transjakarta/tickets",
            PurchaseTicketRequestDto(qty = qty),
        )
        (response.data ?: emptyList()).mapNotNull(::toTicket)
    }

    override suspend fun myTickets(): Result<List<TransjakartaTicket>> = runCatching {
        val response = apiService.get<List<TicketDto>>("api/v1/nasabah/transjakarta/tickets")
        (response.data ?: emptyList()).mapNotNull(::toTicket)
    }

    override suspend fun toggleUsed(ticketCode: String): Result<TransjakartaTicket> = runCatching {
        val response = apiService.post<TicketDto>("api/v1/nasabah/transjakarta/tickets/$ticketCode/toggle-used")
        toTicket(response.data) ?: throw IllegalStateException("Unexpected response from server")
    }

    private fun toTicket(dto: TicketDto?): TransjakartaTicket? {
        if (dto == null) return null
        return TransjakartaTicket(
            ticketId = dto.ticketId ?: return null,
            ticketCode = dto.ticketCode ?: return null,
            amount = (dto.amount ?: 0.0).toLong(),
            used = dto.used ?: false,
            purchasedAt = dto.purchasedAt ?: "",
            loanId = dto.loanId ?: return null,
            billingCycle = dto.billingCycle ?: "",
            dueDate = dto.dueDate ?: "",
        )
    }
}
