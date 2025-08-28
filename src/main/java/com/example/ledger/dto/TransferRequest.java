package com.example.ledger.dto;

import java.math.BigDecimal;

public record TransferRequest(String transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
}
