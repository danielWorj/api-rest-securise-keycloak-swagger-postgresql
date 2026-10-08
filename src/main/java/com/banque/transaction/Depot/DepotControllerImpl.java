package com.banque.transaction.Depot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DepotControllerImpl implements DepotControllerInt {

    private final DepotService depotService;

    @Override
    public ResponseEntity<DepotDto> createDepot(DepotDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depotService.create(dto));
    }

    @Override
    public ResponseEntity<List<DepotDto>> findAllDepot() {
        return ResponseEntity.ok(depotService.findAll());
    }

    @Override
    public ResponseEntity<DepotDto> findById(UUID id) {
        return ResponseEntity.ok(depotService.findById(id));
    }

    @Override
    public ResponseEntity<List<DepotDto>> findByCompte(UUID compteId) {
        return ResponseEntity.ok(depotService.findByCompte(compteId));
    }

    @Override
    public ResponseEntity<List<DepotDto>> findByClient(UUID clientId) {
        return ResponseEntity.ok(depotService.findByClient(clientId));
    }
}