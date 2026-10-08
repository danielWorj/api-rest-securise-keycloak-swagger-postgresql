package com.banque.transaction.Retrait;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/retrait")
public interface RetraitControllerInt {

    @PostMapping("/create")
    ResponseEntity<RetraitDto> createRetrait(@Valid @RequestBody RetraitDto dto);

    @GetMapping("/all")
    ResponseEntity<List<RetraitDto>> findAllRetrait();

    @GetMapping("/findbyid/{id}")
    ResponseEntity<RetraitDto> findById(@PathVariable("id") UUID id);

    @GetMapping("/findbycompte/{compteId}")
    ResponseEntity<List<RetraitDto>> findByCompte(@PathVariable("compteId") UUID compteId);

    @GetMapping("/findbyclient/{clientId}")
    ResponseEntity<List<RetraitDto>> findByClient(@PathVariable("clientId") UUID clientId);
}