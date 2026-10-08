package com.banque.transaction.Compte;

import com.banque.transaction.ServerResponse.ServerResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/compte")
public interface CompteControllerInt {

    @PostMapping("/create")
    ResponseEntity<CompteDto> createCompte(@Valid @RequestBody CompteDto dto);

    @GetMapping("/all")
    ResponseEntity<List<CompteDto>> findAllCompte();

    @GetMapping("/findbyid/{id}")
    ResponseEntity<CompteDto> findById(@PathVariable("id") UUID id);

    @GetMapping("/findbyclient/{clientId}")
    ResponseEntity<List<CompteDto>> findByClient(@PathVariable("clientId") UUID clientId);

    @DeleteMapping("/delete/{id}")
    ResponseEntity<ServerResponse> deleteCompte(@PathVariable("id") UUID id);
}