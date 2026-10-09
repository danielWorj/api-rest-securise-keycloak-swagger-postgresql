package com.banque.transaction;

import com.banque.transaction.support.AbstractIntegrationTest;
import com.banque.transaction.support.Containers;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // si ça passe, postgres + keycloak + spring démarrent bien ensemble
    }

    @Test
    void keycloakEmetDesJetonsAvecLeBonEmetteurEtLesBonsRoles() {
        // on décode le payload du jwt à la main
        String payload = new String(
                Base64.getUrlDecoder().decode(rawToken(ADMIN).split("\\.")[1]), StandardCharsets.UTF_8);

        String issuer = JsonPath.read(payload, "$.iss");
        String sub = JsonPath.read(payload, "$.sub");
        List<String> roles = JsonPath.read(payload, "$.realm_access.roles");

        assertThat(issuer).isEqualTo(Containers.realmUrl());
        assertThat(sub).isEqualTo(ADMIN_ID.toString());
        assertThat(roles).contains("ADMIN");
    }
}
