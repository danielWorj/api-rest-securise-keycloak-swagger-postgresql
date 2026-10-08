package com.banque.transaction.Client;

import com.banque.transaction.ServerResponse.ServerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/client")
public interface ClientControllerInt {

    @PostMapping("/create")
    ResponseEntity<ClientDto> createClient(@Valid @RequestBody ClientDto dto);

    @GetMapping("/all")
    ResponseEntity<List<ClientDto>> findAllClient();

    @GetMapping("/findbyid/{id}")
    ResponseEntity<ClientDto> findById(@PathVariable("id") UUID id);

    @PutMapping("/update/{id}")
    ResponseEntity<ClientDto> updateClient(@PathVariable("id") UUID id, @Valid @RequestBody ClientDto dto);

    @DeleteMapping("/delete/{id}")
    ResponseEntity<ServerResponse> deleteClient(@PathVariable("id") UUID id);
}