package com.banque.transaction.Client;

import lombok.Data;

import java.util.UUID;

@Data
public class ClientDto {
    private UUID id;            // = claim "sub" du JWT Keycloak
    private String nom;
    private String contact;
}
