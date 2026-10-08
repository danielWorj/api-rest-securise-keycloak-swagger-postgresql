package com.banque.transaction.Exceptions;

/** 404 : l'objet demandé n'existe pas. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}