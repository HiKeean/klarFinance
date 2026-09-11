package com.api.klarfinance.fin.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountResponse {
    private Integer id;
    private String bankCode;
    private String bankAccountNumber;
}
