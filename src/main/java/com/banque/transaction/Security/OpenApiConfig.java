package com.banque.transaction.Security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH = "keycloak-oauth2";
    private static final String BEARER = "bearer-jwt";

    @Bean
    OpenAPI openAPI(@Value("${app.keycloak.public-url}") String keycloakUrl) {
        return new OpenAPI()
                .info(new Info().title("API Banque").version("1.0")
                        .description("Dépôts et retraits sécurisés par Keycloak (OAuth2 / OIDC)"))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        // Connexion Keycloak (navigateur, Authorization Code + PKCE)
                        .addSecuritySchemes(OAUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                        .authorizationUrl(keycloakUrl + "/protocol/openid-connect/auth")
                                        .tokenUrl(keycloakUrl + "/protocol/openid-connect/token")
                                        .scopes(new Scopes().addString("openid", "OpenID Connect")))))
                        // Alternative : coller un jeton obtenu avec curl
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}