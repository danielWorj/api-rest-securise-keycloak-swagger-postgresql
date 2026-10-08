package com.banque.transaction.Client;

import com.banque.transaction.ServerResponse.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ClientControllerImpl implements ClientControllerInt {

    private final ClientService clientService;

    @Override
    public ResponseEntity<ClientDto> createClient(ClientDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    @Override
    public ResponseEntity<List<ClientDto>> findAllClient() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @Override
    public ResponseEntity<ClientDto> findById(UUID id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Override
    public ResponseEntity<ClientDto> updateClient(UUID id, ClientDto dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }

    @Override
    public ResponseEntity<ServerResponse> deleteClient(UUID id) {
        clientService.delete(id);
        return ResponseEntity.ok(new ServerResponse("Client supprimé avec succès", true));
    }
}