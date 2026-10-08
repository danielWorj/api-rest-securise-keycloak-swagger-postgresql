package com.banque.transaction.Retrait;

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
public class RetraitControllerImpl implements RetraitControllerInt {

    private final RetraitService retraitService;
    private final CurrentUser currentUser;

    /**
     * Rôle CLIENT exigé par SecurityConfig.
     * Le clientId reçu dans le corps est ignoré : le retrait est toujours fait
     * pour l'utilisateur du jeton (sub). La règle R4 du service vérifie ensuite
     * que le compte lui appartient.
     */
    @Override
    public ResponseEntity<RetraitDto> createRetrait(RetraitDto dto) {
        dto.setClientId(currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(retraitService.create(dto));
    }

    /** Rôles ADMIN / SECRETAIRE exigés par SecurityConfig. */
    @Override
    public ResponseEntity<List<RetraitDto>> findAllRetrait() {
        return ResponseEntity.ok(retraitService.findAll());
    }

    @Override
    @PreAuthorize("@accessGuard.canAccessRetrait(#id)")
    public ResponseEntity<RetraitDto> findById(UUID id) {
        return ResponseEntity.ok(retraitService.findById(id));
    }

    @Override
    @PreAuthorize("@accessGuard.canAccessCompte(#compteId)")
    public ResponseEntity<List<RetraitDto>> findByCompte(UUID compteId) {
        return ResponseEntity.ok(retraitService.findByCompte(compteId));
    }

    @Override
    @PreAuthorize("@accessGuard.isSelfOrStaff(#clientId)")
    public ResponseEntity<List<RetraitDto>> findByClient(UUID clientId) {
        return ResponseEntity.ok(retraitService.findByClient(clientId));
    }
}