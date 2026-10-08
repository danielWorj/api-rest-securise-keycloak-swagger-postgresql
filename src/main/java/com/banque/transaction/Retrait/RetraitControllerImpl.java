package com.banque.transaction.Retrait;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RetraitControllerImpl implements RetraitControllerInt {

    private final RetraitService retraitService;

    @Override
    public ResponseEntity<RetraitDto> createRetrait(RetraitDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(retraitService.create(dto));
    }

    @Override
    public ResponseEntity<List<RetraitDto>> findAllRetrait() {
        return ResponseEntity.ok(retraitService.findAll());
    }

    @Override
    public ResponseEntity<RetraitDto> findById(UUID id) {
        return ResponseEntity.ok(retraitService.findById(id));
    }

    @Override
    public ResponseEntity<List<RetraitDto>> findByCompte(UUID compteId) {
        return ResponseEntity.ok(retraitService.findByCompte(compteId));
    }

    @Override
    public ResponseEntity<List<RetraitDto>> findByClient(UUID clientId) {
        return ResponseEntity.ok(retraitService.findByClient(clientId));
    }
}