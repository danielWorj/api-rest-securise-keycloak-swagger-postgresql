package com.banque.transaction.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajoute automatiquement les erreurs communes (401, 403, 500) à toutes les opérations,
 * au format { "message": "...", "status": false } renvoyé par GlobalExceptionHandler.
 */
@Configuration
public class OpenApiErrorsCustomizer {

    @Bean
    OperationCustomizer commonErrors() {
        return (operation, handlerMethod) -> {
            add(operation, "401", "Non authentifié : jeton absent, expiré ou invalide");
            add(operation, "403", "Accès refusé : rôle insuffisant ou ressource d'un autre client");
            add(operation, "500", "Erreur interne du serveur");
            return operation;
        };
    }

    private static void add(Operation operation, String code, String description) {
        if (operation.getResponses() != null && operation.getResponses().containsKey(code)) {
            return; // ne pas écraser une description plus précise posée par @ApiResponse
        }
        operation.getResponses().addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ServerResponse")))));
    }
}