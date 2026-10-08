package com.banque.transaction.Depot;

import com.banque.transaction.Client.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "depot")
@Getter
@Setter
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;   // BigDecimal pour l'argent, jamais double

    private LocalDateTime date;
}
