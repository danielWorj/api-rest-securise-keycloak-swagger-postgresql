package com.banque.transaction.Depot;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DepotDto {

    private UUID id;                  // en lecture seule
    private LocalDateTime date;       // en lecture seule : posée par le serveur

    @NotNull(message = "Le client est obligatoire")
    private UUID clientId;

    @NotNull(message = "Le compte est obligatoire")
    private UUID compteId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Digits(integer = 17, fraction = 2, message = "Le montant admet 2 décimales au maximum")
    private BigDecimal montant;

    private String secretaire;

    public static DepotDto from(Depot depot) {
        DepotDto dto = new DepotDto();
        dto.setId(depot.getId());
        dto.setDate(depot.getDate());
        dto.setClientId(depot.getClient().getId());
        dto.setCompteId(depot.getCompte().getId());
        dto.setMontant(depot.getMontant());
        dto.setSecretaire(depot.getSecretaire());
        return dto;
    }
}