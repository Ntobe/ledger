package com.example.ledger.controller;

import com.example.ledger.dto.AccountResponse;
import com.example.ledger.dto.CreateAccountRequest;
import com.example.ledger.model.Account;
import com.example.ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.initialBalance());
        return ResponseEntity.ok(new AccountResponse(account.getId(), account.getBalance()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(new AccountResponse(account.getId(), account.getBalance()));
    }
}

