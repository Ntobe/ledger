package com.example.ledger.dto;

import java.math.BigDecimal;

public record CreateAccountRequest(BigDecimal initialBalance) {
}
