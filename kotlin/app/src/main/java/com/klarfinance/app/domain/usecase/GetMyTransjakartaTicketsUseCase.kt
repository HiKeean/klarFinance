package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import javax.inject.Inject

class GetMyTransjakartaTicketsUseCase @Inject constructor(
    private val repository: TransjakartaRepository,
) {
    suspend operator fun invoke(): Result<List<TransjakartaTicket>> = repository.myTickets()
}
