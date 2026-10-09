package com.banque.transaction.parcours;

import com.banque.transaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParcoursBancaireIT extends AbstractIntegrationTest {

    private static final UUID AUTRE_CLIENT_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");

    private UUID compteDeClient1;
    private UUID compteDeLAutre;

    @BeforeEach
    void donneesDeBase() throws Exception {
        creerClient(CLIENT_ID, "Client Un");          // = utilisateur "client1" de Keycloak
        creerClient(AUTRE_CLIENT_ID, "Autre Client"); // client sans compte Keycloak
        compteDeClient1 = creerCompte(CLIENT_ID);
        compteDeLAutre = creerCompte(AUTRE_CLIENT_ID);
    }

    // ---------------------------------------------------------------- dépôts / retraits

    @Test
    @DisplayName("Dépôt de 1000 puis retrait de 300 : solde final 700")
    void depotPuisRetrait() throws Exception {
        deposer(CLIENT_ID, compteDeClient1, "1000.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secretaire").value(SECRETAIRE)); // lu dans le JWT, pas dans le corps

        retirer(CLIENT, compteDeClient1, "300.00")
                .andExpect(status().isCreated());

        assertThat(solde(compteDeClient1)).isEqualByComparingTo("700.00");
        assertThat(depotRepository.count()).isEqualTo(1);
        assertThat(retraitRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("R3 : un dépôt peut être fait sur le compte d'un autre")
    void depotSurLeCompteDUnAutre() throws Exception {
        deposer(CLIENT_ID, compteDeLAutre, "50.00").andExpect(status().isCreated());
        assertThat(solde(compteDeLAutre)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("R5 : retrait supérieur au solde -> 422 et solde inchangé")
    void retraitAuDessusDuSolde() throws Exception {
        deposer(CLIENT_ID, compteDeClient1, "100.00").andExpect(status().isCreated());

        retirer(CLIENT, compteDeClient1, "500.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(false));

        assertThat(solde(compteDeClient1)).isEqualByComparingTo("100.00");
        assertThat(retraitRepository.count()).isZero();   // rien n'a été enregistré (transaction annulée)
    }

    @Test
    @DisplayName("R4 : retrait depuis le compte d'un autre client -> 403")
    void retraitSurLeCompteDUnAutre() throws Exception {
        deposer(AUTRE_CLIENT_ID, compteDeLAutre, "200.00").andExpect(status().isCreated());

        retirer(CLIENT, compteDeLAutre, "10.00").andExpect(status().isForbidden());

        assertThat(solde(compteDeLAutre)).isEqualByComparingTo("200.00");
    }

    // ---------------------------------------------------------------- propriété des données (AccessGuard)

    @Test
    @DisplayName("Un CLIENT lit son compte (200) mais pas celui d'un autre (403)")
    void lectureDeCompte() throws Exception {
        mockMvc.perform(get("/api/compte/findbyid/" + compteDeClient1).header(AUTHORIZATION, bearer(CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compteDeClient1.toString()));

        mockMvc.perform(get("/api/compte/findbyid/" + compteDeLAutre).header(AUTHORIZATION, bearer(CLIENT)))
                .andExpect(status().isForbidden());

        // le personnel, lui, peut tout lire
        mockMvc.perform(get("/api/compte/findbyid/" + compteDeLAutre).header(AUTHORIZATION, bearer(SECRETAIRE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un CLIENT ne peut lister que ses propres comptes")
    void comptesParClient() throws Exception {
        mockMvc.perform(get("/api/compte/findbyclient/" + CLIENT_ID).header(AUTHORIZATION, bearer(CLIENT)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/compte/findbyclient/" + AUTRE_CLIENT_ID).header(AUTHORIZATION, bearer(CLIENT)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- validation

    @Test
    @DisplayName("Validation : montant nul, à 3 décimales ou compte inconnu")
    void validationDesDepots() throws Exception {
        deposer(CLIENT_ID, compteDeClient1, "0").andExpect(status().isBadRequest());
        deposer(CLIENT_ID, compteDeClient1, "10.123").andExpect(status().isBadRequest());
        deposer(CLIENT_ID, UUID.randomUUID(), "10.00").andExpect(status().isNotFound());

        mockMvc.perform(post("/api/depot/create")
                        .header(AUTHORIZATION, bearer(SECRETAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ pas du json"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- suppressions (R8 + clés étrangères)

    @Test
    @DisplayName("R8 : suppression d'un compte avec solde -> 409 ; compte vide -> 200")
    void suppressionDeCompte() throws Exception {
        deposer(CLIENT_ID, compteDeClient1, "10.00").andExpect(status().isCreated());

        mockMvc.perform(delete("/api/compte/delete/" + compteDeClient1).header(AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/compte/delete/" + compteDeLAutre).header(AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isOk());
        assertThat(compteRepository.existsById(compteDeLAutre)).isFalse();
    }

    @Test
    @DisplayName("Clé étrangère PostgreSQL : supprimer un client qui a un compte -> 409")
    void suppressionDeClientAvecCompte() throws Exception {
        mockMvc.perform(delete("/api/client/delete/" + CLIENT_ID).header(AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isConflict());
        assertThat(clientRepository.existsById(CLIENT_ID)).isTrue();
    }
}
