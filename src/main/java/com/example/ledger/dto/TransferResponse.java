package com.example.ledger.dto;

import com.example.ledger.model.TransferStatus;

public record TransferResponse(TransferStatus status, String message) {
}
