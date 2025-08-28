package com.example.ledger.service;

import com.example.ledger.dto.TransferRequest;
import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.model.Account;
import com.example.ledger.model.EntryType;
import com.example.ledger.model.LedgerEntry;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.LedgerEntryRepository;
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
import java.time.LocalDateTime;
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

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private LedgerEntryRepository ledgerRepo;

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
    void testAccountBalanceUpdate() throws Exception {
        // given
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of()));
        when(accountRepo.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepo.findById(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(accountRepo, times(2)).save(accountCaptor.capture());

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
    void testLedgerEntriesUpdate() throws Exception {
        // given
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of()));
        when(accountRepo.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepo.findById(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(ledgerRepo, times(2)).save(ledgerEntryCaptor.capture());

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
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of()));
        when(accountRepo.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepo.findById(TO_ACCOUNT_ID)).thenReturn(Optional.of(toAccount));

        // when then
        assertThrows(InsufficientFundsException.class, () -> ledgerService.applyTransfer(request));
    }

    @Test
    @DisplayName("Given a transfer request with an existing transferId, " +
            "when applyTransfer is invoked, " +
            "then should not process the transfer.")
    void testExistingTransferId() throws InsufficientFundsException {
        // given
        LedgerEntry entry = new LedgerEntry(TRANSFER_ID,
                FROM_ACCOUNT_ID,
                FROM_ACCOUNT_BALANCE,
                EntryType.DEBIT,
                LocalDateTime.now());
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of(entry)));

        // when
        ledgerService.applyTransfer(request);

        // then
        verify(accountRepo, never()).findById(anyLong());
        verify(accountRepo, never()).save(any());
        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and from account ID is not found, " +
            "when applyTransfer is invoked, " +
            "then EntityNotFoundException should be thrown.")
    void fromAccountNotFound() {
        // given
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of()));
        when(accountRepo.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.empty());

        // when then
        assertThrows(EntityNotFoundException.class, () -> ledgerService.applyTransfer(request));
    }

    @Test
    @DisplayName("Given a transfer request with a new transferId, " +
            "and to account ID is not found, " +
            "when applyTransfer is invoked, " +
            "then EntityNotFoundException should be thrown.")
    void toAccountNotFound() {
        // given
        when(ledgerRepo.findByTransferId(TRANSFER_ID)).thenReturn(Optional.of(List.of()));
        when(accountRepo.findById(FROM_ACCOUNT_ID)).thenReturn(Optional.of(fromAccount));
        when(accountRepo.findById(TO_ACCOUNT_ID)).thenReturn(Optional.empty());

        // when then
        assertThrows(EntityNotFoundException.class, () -> ledgerService.applyTransfer(request));
    }
}