package com.example.ledger.service;

import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.model.Account;
import com.example.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepo;

    public Account createAccount(BigDecimal initialBalance) {
        Account account = new Account();
        account.setBalance(initialBalance);
        return accountRepo.save(account);
    }

    public Account getAccount(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}

