package com.banque.transaction.Depot;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/depot")
public interface DepotControllerInt {

    @PostMapping("/create")
    ResponseEntity<DepotDto> createDepot(@Valid @RequestBody DepotDto dto);

    @GetMapping("/all")
    ResponseEntity<List<DepotDto>> findAllDepot();

    @GetMapping("/findbyid/{id}")
    ResponseEntity<DepotDto> findById(@PathVariable("id") UUID id);

    @GetMapping("/findbycompte/{compteId}")
    ResponseEntity<List<DepotDto>> findByCompte(@PathVariable("compteId") UUID compteId);

    @GetMapping("/findbyclient/{clientId}")
    ResponseEntity<List<DepotDto>> findByClient(@PathVariable("clientId") UUID clientId);
}