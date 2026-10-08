package com.banque.transaction.Client;

import com.banque.transaction.ServerResponse.ServerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
public class ClientControllerImpl implements  ClientControllerInt{
    @Autowired
    private ClientRepository clientRepository;
    @Override
    public ResponseEntity<List<Client>> findAllClient() {
        return ResponseEntity.ok(this.clientRepository.findAll());
    }
    @Override
    public ResponseEntity<Client> findById(String id) {
        return ResponseEntity.ok(this.clientRepository.findById(id).orElse(null));
    }
    @Override
    public ResponseEntity<ServerResponse> deleteClient(String id) {
        this.clientRepository.deleteById(id);
        return ResponseEntity.ok(new ServerResponse("Client supprime avec success",true));
    }
}
