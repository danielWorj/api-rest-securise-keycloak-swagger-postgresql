package com.banque.transaction.Exceptions;

/** 422 : retrait supérieur au solde disponible. */
public class SoldeInsuffisantException extends RuntimeException {
    public SoldeInsuffisantException(String message) {
        super(message);
    }
}