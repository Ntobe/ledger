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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class LedgerServiceTest {
    private static final Long FROM_ACCOUNT_ID = 1L;
    private static final BigDecimal FROM_ACCOUNT_BALANCE = BigDecimal.valueOf(100);
    private static final Long TO_ACCOUNT_ID = 2L;
    private static final BigDecimal TO_ACCOUNT_BALANCE = BigDecimal.valueOf(50);
    private static final String TRANSFER_ID = "tx-123";
    private static final BigDecimal TRANSFER_AMOUNT = BigDecimal.valueOf(40);
    private static final String TRANSFER_SUCCESS_MESSAGE = "Transfer successful";
    private static final String INSUFFICIENT_FUNDS__MESSAGE = "Account has insufficient funds";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private TransferRepository transferRepository;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    @InjectMocks
    private LedgerService ledgerService;

    private Account fromAccount;
    private Account toAccount;
    private TransferRequest request;

    @BeforeEach
    void setup() {
        fromAccount = new Account();
        fromAccount.setId(FROM_ACCOUNT_ID);
        fromAccount.setBalance(FROM_ACCOUNT_BALANCE);

        toAccount = new Account();
        toAccount.setId(TO_ACCOUNT_ID);
        toAccount.setBalance(TO_ACCOUNT_BALANCE);

        request = new TransferRequest(TRANSFER_ID, FROM_ACCOUNT_ID, TO_ACCOUNT_ID, TRANSFER_AMOUNT);
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and from account has sufficient funds, " +
            "when applyTransfer is invoked, " +
            "then the account balances should be updated correctly.")
    void testAccountBalanceUpdate() {
        // given
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(accountRepository, times(2)).save(accountCaptor.capture());

        List<Account> savedAccounts = accountCaptor.getAllValues();
        assertEquals(2, savedAccounts.size());
        assertEquals(FROM_ACCOUNT_ID, savedAccounts.get(0).getId());
        assertEquals(BigDecimal.valueOf(60), savedAccounts.get(0).getBalance());
        assertEquals(TO_ACCOUNT_ID, savedAccounts.get(1).getId());
        assertEquals(BigDecimal.valueOf(90), savedAccounts.get(1).getBalance());
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and from account has sufficient funds, " +
            "when applyTransfer is invoked, " +
            "then the account ledger entries should be saved correctly.")
    void testLedgerEntriesUpdate() {
        // given
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(ledgerEntryRepository, times(2)).save(ledgerEntryCaptor.capture());

        List<LedgerEntry> savedLedgerEntries = ledgerEntryCaptor.getAllValues();
        assertEquals(2, savedLedgerEntries.size());
        assertEquals(EntryType.DEBIT, savedLedgerEntries.getFirst().getType());
        assertEquals(FROM_ACCOUNT_ID, savedLedgerEntries.getFirst().getAccountId());
        assertEquals(TRANSFER_AMOUNT, savedLedgerEntries.getFirst().getAmount());
        assertEquals(TRANSFER_ID, savedLedgerEntries.getFirst().getTransferId());

        assertEquals(EntryType.CREDIT, savedLedgerEntries.get(1).getType());
        assertEquals(TO_ACCOUNT_ID, savedLedgerEntries.get(1).getAccountId());
        assertEquals(TRANSFER_AMOUNT, savedLedgerEntries.get(1).getAmount());
        assertEquals(TRANSFER_ID, savedLedgerEntries.get(1).getTransferId());
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and from account does not has sufficient funds, " +
            "when applyTransfer is invoked, " +
            "then InsufficientFundsException should be thrown.")
    void testInsufficientFunds() {
        // given
        fromAccount.setBalance(BigDecimal.valueOf(10));
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when then
        TransferResponse result = ledgerService.applyTransfer(request);

        assertEquals(INSUFFICIENT_FUNDS__MESSAGE, result.message());
        assertEquals(TransferStatus.FAILURE, result.status());
    }

    @Test
    @DisplayName("Given a transfer request with an existing transferId, " +
            "when applyTransfer is invoked, " +
            "then should not process the transfer.")
    void testExistingTransferId() {
        // given
        Transfer existingTransfer = new Transfer(TRANSFER_ID, TransferStatus.SUCCESS, TRANSFER_SUCCESS_MESSAGE);
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(existingTransfer));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(accountRepository, never()).findById(anyLong());
        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and from account ID is not found, " +
            "when applyTransfer is invoked, " +
            "then AccountNotFoundException should be thrown.")
    void fromAccountNotFound() {
        // given
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.empty());
        when(accountRepository.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.empty());

        // when then
        assertThrows(AccountNotFoundException.class, () -> ledgerService.applyTransfer(request));
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and to account ID is not found, " +
            "when applyTransfer is invoked, " +
            "then AccountNotFoundException should be thrown.")
    void toAccountNotFound() {
        // given
        when(transferRepository.findByTransferId(TRANSFER_ID)).thenReturn(Optional.empty());
        when(accountRepository.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(TO_ACCOUNT_ID)).thenReturn(Optional.empty());

        // when then
        assertThrows(AccountNotFoundException.class, () -> ledgerService.applyTransfer(request));
    }
}