package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class GetLoanHistoryUseCase @Inject constructor(
    private val repository: LoanRepository,
) {
    suspend operator fun invoke(): Result<List<LoanHistoryItem>> = repository.getLoanHistory()
}
