package com.example.ledger.exception;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException() {
        super("Account has insufficient funds.");
    }
}
