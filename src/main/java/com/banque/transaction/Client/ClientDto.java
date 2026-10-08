package com.banque.transaction.Client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ClientDto {

    private UUID id;            // optionnel à la création (généré s'il est absent)

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @Size(max = 50, message = "Le contact ne doit pas dépasser 50 caractères")
    private String contact;

    public static ClientDto from(Client client) {
        ClientDto dto = new ClientDto();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setContact(client.getContact());
        return dto;
    }
}