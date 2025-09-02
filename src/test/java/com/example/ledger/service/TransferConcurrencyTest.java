package com.example.ledger.service;

import com.example.ledger.dto.TransferRequest;
import com.example.ledger.dto.TransferResponse;
import com.example.ledger.model.Account;
import com.example.ledger.model.TransferStatus;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferConcurrencyTest {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setup() {
        transferRepository.deleteAll();
        accountRepository.deleteAll();

        fromAccount = new Account();
        fromAccount.setBalance(BigDecimal.valueOf(300));
        accountRepository.save(fromAccount);

        toAccount = new Account();
        toAccount.setBalance(BigDecimal.valueOf(100));
        accountRepository.save(toAccount);
    }

    @Test
    void testConcurrentTransfersFromSameAccount() throws InterruptedException, ExecutionException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Future<TransferResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String transferId = UUID.randomUUID().toString();
            futures.add(executor.submit(() -> {
                latch.countDown();
                latch.await();

                TransferRequest request = new TransferRequest(
                        transferId,
                        fromAccount.getId(),
                        toAccount.getId(),
                        BigDecimal.valueOf(250)
                );

                return ledgerService.applyTransfer(request);
            }));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int successCount = 0;
        int failureCount = 0;

        for (Future<TransferResponse> future : futures) {
            TransferResponse response = future.get();
            if (response.status() == TransferStatus.SUCCESS) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        assertEquals(1, successCount, "Only one transfer should succeed");
        assertEquals(1, failureCount, "One transfer should fail due to insufficient funds");

        Account updatedFromAccount = accountRepository.findById(fromAccount.getId()).orElseThrow();
        Account updatedToAccount = accountRepository.findById(toAccount.getId()).orElseThrow();

        assertEquals(0, updatedFromAccount.getBalance().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, updatedToAccount.getBalance().compareTo(BigDecimal.valueOf(350)));
    }
}
