package com.banque.transaction.Depot;

import com.banque.transaction.Security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DepotControllerImpl implements DepotControllerInt {

    private final DepotService depotService;
    private final CurrentUser currentUser;

    /** Rôle SECRETAIRE exigé par SecurityConfig. La secrétaire est lue dans le JWT, jamais dans le corps. */
    @Override
    public ResponseEntity<DepotDto> createDepot(DepotDto dto) {
        dto.setSecretaire(currentUser.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(depotService.create(dto));
    }

    /** Rôles ADMIN / SECRETAIRE exigés par SecurityConfig. */
    @Override
    public ResponseEntity<List<DepotDto>> findAllDepot() {
        return ResponseEntity.ok(depotService.findAll());
    }

    @Override
    @PreAuthorize("@accessGuard.canAccessDepot(#id)")
    public ResponseEntity<DepotDto> findById(UUID id) {
        return ResponseEntity.ok(depotService.findById(id));
    }

    @Override
    @PreAuthorize("@accessGuard.canAccessCompte(#compteId)")
    public ResponseEntity<List<DepotDto>> findByCompte(UUID compteId) {
        return ResponseEntity.ok(depotService.findByCompte(compteId));
    }

    @Override
    @PreAuthorize("@accessGuard.isSelfOrStaff(#clientId)")
    public ResponseEntity<List<DepotDto>> findByClient(UUID clientId) {
        return ResponseEntity.ok(depotService.findByClient(clientId));
    }
}