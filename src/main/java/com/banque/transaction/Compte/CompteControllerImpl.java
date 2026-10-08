package com.banque.transaction.Compte;

import com.banque.transaction.ServerResponse.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CompteControllerImpl implements CompteControllerInt {

    private final CompteService compteService;

    @Override
    public ResponseEntity<CompteDto> createCompte(CompteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.create(dto));
    }

    @Override
    public ResponseEntity<List<CompteDto>> findAllCompte() {
        return ResponseEntity.ok(compteService.findAll());
    }

    @Override
    @PreAuthorize("@accessGuard.canAccessCompte(#id)")
    public ResponseEntity<CompteDto> findById(UUID id) {
        return ResponseEntity.ok(compteService.findById(id));
    }

    @Override
    @PreAuthorize("@accessGuard.isSelfOrStaff(#clientId)")
    public ResponseEntity<List<CompteDto>> findByClient(UUID clientId) {
        return ResponseEntity.ok(compteService.findByClient(clientId));
    }

    @Override
    public ResponseEntity<ServerResponse> deleteCompte(UUID id) {
        compteService.delete(id);
        return ResponseEntity.ok(new ServerResponse("Compte supprimé avec succès", true));
    }
}