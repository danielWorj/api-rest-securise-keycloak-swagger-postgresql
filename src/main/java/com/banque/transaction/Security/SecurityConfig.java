package com.banque.transaction.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity                       // active @PreAuthorize
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String SECRETAIRE = "SECRETAIRE";
    private static final String CLIENT = "CLIENT";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                    // API stateless avec jeton Bearer : pas de cookie de session
                .cors(Customizer.withDefaults())                 // utilise le bean CorsConfigurationSource ci-dessous
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public : documentation et santé ---
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Personnel uniquement ---
                        .requestMatchers(HttpMethod.POST, "/api/client/create", "/api/compte/create")
                        .hasAnyRole(ADMIN, SECRETAIRE)
                        .requestMatchers(HttpMethod.GET, "/api/client/all", "/api/compte/all",
                                "/api/depot/all", "/api/retrait/all")
                        .hasAnyRole(ADMIN, SECRETAIRE)

                        // --- Suppressions : ADMIN seulement ---
                        .requestMatchers(HttpMethod.DELETE, "/api/client/**", "/api/compte/**").hasRole(ADMIN)

                        // --- Opérations d'argent ---
                        .requestMatchers(HttpMethod.POST, "/api/depot/create").hasRole(SECRETAIRE)
                        .requestMatchers(HttpMethod.POST, "/api/retrait/create").hasRole(CLIENT)

                        // --- Reste de l'API (consultations/mises à jour par id) : tout rôle bancaire,
                        //     la propriété est vérifiée ensuite par @PreAuthorize + AccessGuard ---
                        .requestMatchers("/api/**").hasAnyRole(ADMIN, SECRETAIRE, CLIENT)

                        // --- Tout le reste est refusé par défaut ---
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;   // principal name = claim "sub" (valeur par défaut, nécessaire pour la propriété)
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}