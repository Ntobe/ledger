package com.example.ledger.service;

import com.example.ledger.dto.TransferRequest;
import com.example.ledger.dto.TransferResponse;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.model.Account;
import com.example.ledger.model.EntryType;
import com.example.ledger.model.LedgerEntry;
import com.example.ledger.model.Transfer;
import com.example.ledger.model.TransferStatus;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
import com.example.ledger.repository.TransferRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final TransferRepository transferRepository;


    @Transactional
    public TransferResponse applyTransfer(TransferRequest request) {
        Optional<Transfer> existingRecord = transferRepository.findByTransferId(request.transferId());
        if (existingRecord.isPresent()) {
            return new TransferResponse(existingRecord.get().getStatus(), existingRecord.get().getMessage());
        }

        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();

        // Always lock in consistent order
        Long firstId = (fromId < toId) ? fromId : toId;
        Long secondId = (fromId < toId) ? toId : fromId;

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));

        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        // Map back to from/to
        Account fromAccount = (firstAccount.getId().equals(fromId)) ? firstAccount : secondAccount;
        Account toAccount   = (firstAccount.getId().equals(toId))   ? firstAccount : secondAccount;

        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            TransferStatus status = TransferStatus.FAILURE;
            String message = "Account has insufficient funds";
            transferRepository.save(new Transfer(request.transferId(), status, message));
            return new TransferResponse(status, message);
        }

        updateBalances(request, fromAccount, toAccount);
        saveLedgerEntries(request, fromAccount, toAccount);

        TransferStatus status = TransferStatus.SUCCESS;
        String message = "Transfer successful";
        transferRepository.save(new Transfer(request.transferId(), status, message));

        return new TransferResponse(status, message);
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
}

