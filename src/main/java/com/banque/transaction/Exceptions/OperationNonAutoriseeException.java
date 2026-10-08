package com.banque.transaction.Exceptions;

/** 403 : le client tente d'agir sur un compte qui n'est pas le sien. */
public class OperationNonAutoriseeException extends RuntimeException {
    public OperationNonAutoriseeException(String message) {
        super(message);
    }
}