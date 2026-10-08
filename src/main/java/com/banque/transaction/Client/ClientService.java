package com.banque.transaction.Client;

import com.banque.transaction.Exceptions.ConflictException;
import com.banque.transaction.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientDto> findAll() {
        return clientRepository.findAll().stream().map(ClientDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ClientDto findById(UUID id) {
        return ClientDto.from(getEntity(id));
    }

    @Transactional
    public ClientDto create(ClientDto dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        if (clientRepository.existsById(id)) {
            throw new ConflictException("Un client avec l'identifiant " + id + " existe déjà");
        }
        Client client = new Client();
        client.setId(id);
        client.setNom(dto.getNom());
        client.setContact(dto.getContact());
        return ClientDto.from(clientRepository.save(client));
    }

    @Transactional
    public ClientDto update(UUID id, ClientDto dto) {
        Client client = getEntity(id);
        client.setNom(dto.getNom());
        client.setContact(dto.getContact());
        return ClientDto.from(client);          // sauvegardé automatiquement (dirty checking)
    }

    @Transactional
    public void delete(UUID id) {
        Client client = getEntity(id);
        clientRepository.delete(client);
        clientRepository.flush();               // force le contrôle des clés étrangères ICI
    }

    /** Utilisé aussi par les autres modules (Compte, Depot, Retrait). */
    @Transactional(readOnly = true)
    public Client getEntity(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }
}