package com.example.ledger.dto;

import java.math.BigDecimal;

public record AccountResponse(Long id, BigDecimal balance) {
}
