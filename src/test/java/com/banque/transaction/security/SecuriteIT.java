package com.banque.transaction.security;

import com.banque.transaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecuriteIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("Sans jeton : 401 avec le corps JSON de RestAuthenticationEntryPoint")
    void sansJeton() throws Exception {
        mockMvc.perform(get("/api/client/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @DisplayName("Jeton falsifié : 401")
    void jetonInvalide() throws Exception {
        mockMvc.perform(get("/api/client/all").header(AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Points d'entrée publics : actuator/health et OpenAPI")
    void endpointsPublics() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    /**
     * Matrice des droits : utilisateur, méthode, URL, statut attendu.
     * Les refus (403) interviennent AVANT la validation du corps : un "{}" suffit.
     */
    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @CsvSource({
            // consultation globale : personnel uniquement
            "admin1,       GET,    /api/client/all,   200",
            "secretaire1,  GET,    /api/client/all,   200",
            "client1,      GET,    /api/client/all,   403",
            "client1,      GET,    /api/compte/all,   403",
            "client1,      GET,    /api/depot/all,    403",
            "client1,      GET,    /api/retrait/all,  403",
            // création de client : personnel uniquement
            "client1,      POST,   /api/client/create, 403",
            // dépôt : SECRETAIRE seulement (même pas ADMIN)
            "admin1,       POST,   /api/depot/create,  403",
            "client1,      POST,   /api/depot/create,  403",
            // retrait : CLIENT seulement
            "secretaire1,  POST,   /api/retrait/create, 403",
            "admin1,       POST,   /api/retrait/create, 403",
            // suppression : ADMIN seulement
            "secretaire1,  DELETE, /api/client/delete/00000000-0000-0000-0000-000000000001, 403",
            "client1,      DELETE, /api/compte/delete/00000000-0000-0000-0000-000000000001, 403",
            // tout le reste est refusé par défaut (denyAll)
            "admin1,       GET,    /nimporte/quoi,     403"
    })
    void matriceDesDroits(String username, String method, String url, int statutAttendu) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), url)
                        .header(AUTHORIZATION, bearer(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(statutAttendu));
    }
}
