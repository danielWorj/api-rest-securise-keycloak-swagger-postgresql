package com.banque.transaction.Security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("currentUser")
public class CurrentUser {

    /** Identifiant Keycloak (claim "sub") = Client.id en base. */
    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    /** Nom d'utilisateur Keycloak, utilisé comme « secrétaire » d'un dépôt. */
    public String username() {
        String username = jwt().getClaimAsString("preferred_username");
        return username != null ? username : jwt().getSubject();
    }

    public boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
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