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
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;


    @Transactional
    public void applyTransfer(TransferRequest request) throws InsufficientFundsException {
        if (isTransferIdProcessed(request)) {
            return;
        }

        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();

        // Always lock in consistent order
        Long firstId = (fromId < toId) ? fromId : toId;
        Long secondId = (fromId < toId) ? toId : fromId;

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new EntityNotFoundException("Account " + firstId + " not found"));

        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new EntityNotFoundException("Account " + secondId + " not found"));

        // Map back to from/to
        Account fromAccount = (firstAccount.getId().equals(fromId)) ? firstAccount : secondAccount;
        Account toAccount   = (firstAccount.getId().equals(toId))   ? firstAccount : secondAccount;

        if (!hasSufficientFunds(fromAccount, request.amount())) {
            throw new InsufficientFundsException();
        }

        updateBalances(request, fromAccount, toAccount);
        saveLedgerEntries(request, fromAccount, toAccount);
    }

    private void saveLedgerEntries(TransferRequest request, Account fromAccount, Account toAccount) {
        ledgerRepository.save(new LedgerEntry(request.transferId(), fromAccount.getId(), request.amount(), EntryType.DEBIT, LocalDateTime.now()));
        ledgerRepository.save(new LedgerEntry(request.transferId(), toAccount.getId(), request.amount(), EntryType.CREDIT, LocalDateTime.now()));
    }

    private void updateBalances(TransferRequest request, Account fromAccount, Account toAccount) {
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.amount()));
        toAccount.setBalance(toAccount.getBalance().add(request.amount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
    }

    private static boolean hasSufficientFunds(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) > 0;
    }

    private boolean isTransferIdProcessed(TransferRequest request) {
        return ledgerRepository.findByTransferId(request.transferId()).isPresent() &&
                !ledgerRepository.findByTransferId(request.transferId()).get().isEmpty();
    }
}

