package com.banque.transaction.Client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {

    @Id
    private UUID id;            // = claim "sub" du JWT Keycloak (non généré par la base)

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 50)
    private String contact;
}