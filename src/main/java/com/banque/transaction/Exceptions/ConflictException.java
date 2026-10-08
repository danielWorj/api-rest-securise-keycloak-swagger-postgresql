package com.banque.transaction.Exceptions;

/** 409 : l'opération est incompatible avec l'état actuel des données (doublon, suppression impossible...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}