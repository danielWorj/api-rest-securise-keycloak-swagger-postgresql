package com.banque.transaction.Client;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {
    @Id
    private UUID id;            // = claim "sub" du JWT Keycloak
    private String nom;
    private String contact;
}
