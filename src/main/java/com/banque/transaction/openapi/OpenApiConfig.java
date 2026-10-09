package com.banque.transaction.openapi;

import com.banque.transaction.ServerResponse.ServerResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String OAUTH = "keycloak-oauth2";
    public static final String BEARER = "bearer-jwt";

    @Bean
    OpenAPI openAPI(@Value("${app.keycloak.public-url}") String keycloakUrl) {

        Components components = new Components()
                // Connexion Keycloak (navigateur, Authorization Code + PKCE)
                .addSecuritySchemes(OAUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("Connexion via Keycloak (Authorization Code + PKCE)")
                        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                .authorizationUrl(keycloakUrl + "/protocol/openid-connect/auth")
                                .tokenUrl(keycloakUrl + "/protocol/openid-connect/token")
                                .scopes(new Scopes().addString("openid", "OpenID Connect")))))
                // Alternative : coller un jeton obtenu avec curl
                .addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Coller un jeton d'accès obtenu auprès de Keycloak"));

        // Enregistre le schéma ServerResponse (utilisé par toutes les réponses d'erreur)
        ModelConverters.getInstance().readAll(ServerResponse.class)
                .forEach(components::addSchemas);

        return new OpenAPI()
                .info(new Info()
                        .title("API Banque")
                        .version("1.0")
                        .description("""
                                Dépôts et retraits sécurisés par Keycloak (OAuth2 / OIDC).

                                **Rôles** : `ADMIN`, `SECRETAIRE`, `CLIENT`
                                - SECRETAIRE : crée les dépôts
                                - CLIENT : effectue ses propres retraits
                                - ADMIN / SECRETAIRE : consultation globale, création de clients et de comptes
                                - ADMIN : suppressions
                                """)
                        .contact(new Contact().name("Équipe Banque").email("contact@example.com")))
                .tags(java.util.List.of(
                        new Tag().name(ApiTags.CLIENTS).description(ApiTags.CLIENTS_DESC),
                        new Tag().name(ApiTags.COMPTES).description(ApiTags.COMPTES_DESC),
                        new Tag().name(ApiTags.DEPOTS).description(ApiTags.DEPOTS_DESC),
                        new Tag().name(ApiTags.RETRAITS).description(ApiTags.RETRAITS_DESC)))
                // Les deux schémas sont des alternatives (OU)
                .addSecurityItem(new SecurityRequirement().addList(OAUTH))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(components);
    }
}