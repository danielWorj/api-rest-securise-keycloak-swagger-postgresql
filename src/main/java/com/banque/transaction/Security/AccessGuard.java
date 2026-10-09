package com.banque.transaction.Security;

import com.banque.transaction.Compte.CompteRepository;
import com.banque.transaction.Depot.DepotRepository;
import com.banque.transaction.Retrait.RetraitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Appelé depuis @PreAuthorize("@accessGuard.xxx(#param)"). Le personnel passe toujours ; un CLIENT seulement sur ses données. */
@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {
    // Cette classe permet de vérifier si l'utilisateur courant
    // a le droit d'accéder à certaines ressources (compte, depot, retrait) en fonction de son rôle (staff ou client) et de son identité.

    //Pour cela on va injecter le CurrentUser qui contient les informations de l'utilisateur courant,
    // ainsi que les repositories des entités Compte, Depot et Retrait pour vérifier si l'utilisateur courant est le propriétaire de la ressource.
    private final CurrentUser me;
    private final CompteRepository compteRepository;
    private final DepotRepository depotRepository;
    private final RetraitRepository retraitRepository;

    //
    public boolean isSelfOrStaff(UUID clientId) {
        return me.isStaff() || me.id().equals(clientId);
    }

    //
    public boolean canAccessCompte(UUID compteId) {
        return me.isStaff() || compteRepository.existsByIdAndClientId(compteId, me.id());
    }

    public boolean canAccessDepot(UUID depotId) {
        return me.isStaff()
                || depotRepository.existsByIdAndClientId(depotId, me.id())            // il est le déposant
                || depotRepository.existsByIdAndCompteClientId(depotId, me.id());     // ou le propriétaire du compte
    }

    public boolean canAccessRetrait(UUID retraitId) {
        return me.isStaff() || retraitRepository.existsByIdAndClientId(retraitId, me.id());
    }
}