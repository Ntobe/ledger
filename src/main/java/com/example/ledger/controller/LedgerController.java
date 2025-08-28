package com.example.ledger.controller;

import com.example.ledger.dto.TransferRequest;
import com.example.ledger.dto.TransferResponse;
import com.example.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        try {
            ledgerService.applyTransfer(request);
            return ResponseEntity.ok(new TransferResponse("SUCCESS","Transfer successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TransferResponse("FAILURE", "Transfer failed: " + e.getMessage()));
        }
    }
}

