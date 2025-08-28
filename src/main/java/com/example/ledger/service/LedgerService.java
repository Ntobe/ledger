package com.example.ledger.service;

import com.example.ledger.dto.TransferRequest;
import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.model.Account;
import com.example.ledger.model.EntryType;
import com.example.ledger.model.LedgerEntry;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final AccountRepository accountRepo;
    private final LedgerEntryRepository ledgerRepo;


    @Transactional
    public void applyTransfer(TransferRequest request) throws InsufficientFundsException {
        if (isTransferIdProcessed(request)) {
            return;
        }

        Account fromAccount = accountRepo.findById(request.fromAccountId())
                .orElseThrow(() -> new EntityNotFoundException("From account not found"));
        Account toAccount = accountRepo.findById(request.toAccountId())
                .orElseThrow(() -> new EntityNotFoundException("To account not found"));

        if (!hasSufficientFunds(fromAccount, request.amount())) {
            throw new InsufficientFundsException();
        }

        updateBalances(request, fromAccount, toAccount);
        saveLedgerEntries(request, fromAccount, toAccount);
    }

    private void saveLedgerEntries(TransferRequest request, Account fromAccount, Account toAccount) {
        ledgerRepo.save(new LedgerEntry(request.transferId(), fromAccount.getId(), request.amount(), EntryType.DEBIT, LocalDateTime.now()));
        ledgerRepo.save(new LedgerEntry(request.transferId(), toAccount.getId(), request.amount(), EntryType.CREDIT, LocalDateTime.now()));
    }

    private void updateBalances(TransferRequest request, Account fromAccount, Account toAccount) {
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));
        accountRepo.save(fromAccount);
        accountRepo.save(toAccount);
    }

    private static boolean hasSufficientFunds(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) > 0;
    }

    private boolean isTransferIdProcessed(TransferRequest request) {
        return ledgerRepo.findByTransferId(request.transferId()).isPresent() &&
                !ledgerRepo.findByTransferId(request.transferId()).get().isEmpty();
    }
}

