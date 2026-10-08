package com.banque.transaction.Compte;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CompteDto {

    private UUID id;                    // en lecture seule
    private String numero;              // généré par le serveur
    private BigDecimal solde;           // en lecture seule (R2)
    private LocalDateTime dateCreation; // en lecture seule

    @NotNull(message = "Le client propriétaire est obligatoire")
    private UUID clientId;

    public static CompteDto from(Compte compte) {
        CompteDto dto = new CompteDto();
        dto.setId(compte.getId());
        dto.setNumero(compte.getNumero());
        dto.setSolde(compte.getSolde());
        dto.setDateCreation(compte.getDateCreation());
        dto.setClientId(compte.getClient().getId());
        return dto;
    }
}