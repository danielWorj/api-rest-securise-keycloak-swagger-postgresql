package com.banque.transaction.Security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("currentUser")
public class CurrentUser {

    // Cette classe permet de récupérer les informations de l'utilisateur courant à partir du token JWT.

    /** Identifiant Keycloak (claim "sub") = Client.id en base. */
    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    /** Nom d'utilisateur Keycloak, utilisé comme « secrétaire » d'un dépôt. */
    public String username() {
        String username = jwt().getClaimAsString("preferred_username");
        return username != null ? username : jwt().getSubject();
    }

    // L'utilisateur est considéré comme du personnel s'il a le rôle "ROLE_ADMIN" ou "ROLE_SECRETAIRE".
    public boolean isStaff() {
        //Recuperation du contexte actuel de l'utilisateur authentifié
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //on retourne true si l'utilisateur est authentifié et qu'il a le rôle "ROLE_ADMIN" ou "ROLE_SECRETAIRE"
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SECRETAIRE"));
    }

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new AccessDeniedException("Utilisateur non authentifié");
    }
}