package com.example.ledger.repository;

import com.example.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    Optional<List<LedgerEntry>> findByTransferId(String transferId);
}
