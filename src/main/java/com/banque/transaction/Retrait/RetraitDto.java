package com.banque.transaction.Retrait;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RetraitDto {

    private UUID id;                  // en lecture seule
    private LocalDateTime date;       // en lecture seule

    @NotNull(message = "Le client est obligatoire")
    private UUID clientId;

    @NotNull(message = "Le compte est obligatoire")
    private UUID compteId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Digits(integer = 17, fraction = 2, message = "Le montant admet 2 décimales au maximum")
    private BigDecimal montant;

    public static RetraitDto from(Retrait retrait) {
        RetraitDto dto = new RetraitDto();
        dto.setId(retrait.getId());
        dto.setDate(retrait.getDate());
        dto.setClientId(retrait.getClient().getId());
        dto.setCompteId(retrait.getCompte().getId());
        dto.setMontant(retrait.getMontant());
        return dto;
    }
}