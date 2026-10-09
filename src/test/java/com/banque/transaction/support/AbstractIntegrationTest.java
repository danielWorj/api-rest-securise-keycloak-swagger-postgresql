package com.banque.transaction.support;

import com.banque.transaction.Client.ClientRepository;
import com.banque.transaction.Compte.CompteRepository;
import com.banque.transaction.Depot.DepotRepository;
import com.banque.transaction.Retrait.RetraitRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // ids des users de keycloak/realm-banque.json (= le "sub" du token)
    protected static final UUID ADMIN_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    protected static final UUID SECRETAIRE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    protected static final UUID CLIENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    protected static final String ADMIN = "admin1";
    protected static final String SECRETAIRE = "secretaire1";
    protected static final String CLIENT = "client1";
    private static final String PASSWORD = "Passw0rd!";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    // un token dure 30 min, donc une seule demande par user
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ClientRepository clientRepository;
    @Autowired protected CompteRepository compteRepository;
    @Autowired protected DepotRepository depotRepository;
    @Autowired protected RetraitRepository retraitRepository;

    // les ports des conteneurs changent à chaque run, on les injecte ici
    @DynamicPropertySource
    static void containersProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", Containers.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", Containers.POSTGRES::getUsername);
        registry.add("spring.datasource.password", Containers.POSTGRES::getPassword);

        String realmUrl = Containers.realmUrl();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> realmUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> realmUrl + "/protocol/openid-connect/certs");
        registry.add("app.keycloak.public-url", () -> realmUrl);
    }

    // conteneurs partagés -> on vide la base avant chaque test (dans l'ordre des FK)
    @BeforeEach
    protected void viderLaBase() {
        retraitRepository.deleteAllInBatch();
        depotRepository.deleteAllInBatch();
        compteRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    // jetons

    protected String rawToken(String username) {
        return TOKENS.computeIfAbsent(username, AbstractIntegrationTest::fetchToken);
    }

    // à mettre tel quel dans le header Authorization
    protected String bearer(String username) {
        return "Bearer " + rawToken(username);
    }

    // vrai login sur keycloak (grant password activé sur le client swagger-ui)
    private static String fetchToken(String username) {
        try {
            String form = "grant_type=password&client_id=swagger-ui"
                    + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                    + "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create(Containers.realmUrl() + "/protocol/openid-connect/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Keycloak a refusé le login de " + username
                        + " : HTTP " + response.statusCode() + " " + response.body());
            }
            return JsonPath.read(response.body(), "$.access_token");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // helpers métier

    // création d'un client en ADMIN (son id doit être le sub du token s'il doit se connecter)
    protected void creerClient(UUID id, String nom) throws Exception {
        mockMvc.perform(post("/api/client/create")
                        .header(AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","nom":"%s","contact":"+237600000000"}
                                """.formatted(id, nom)))
                .andExpect(status().isCreated());
    }

    // création d'un compte en ADMIN, renvoie l'id
    protected UUID creerCompte(UUID clientId) throws Exception {
        String body = mockMvc.perform(post("/api/compte/create")
                        .header(AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"%s"}
                                """.formatted(clientId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    // dépôt fait par la secrétaire
    protected ResultActions deposer(UUID deposantId, UUID compteId, String montant) throws Exception {
        return mockMvc.perform(post("/api/depot/create")
                .header(AUTHORIZATION, bearer(SECRETAIRE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId":"%s","compteId":"%s","montant":%s}
                        """.formatted(deposantId, compteId, montant)));
    }

    // retrait demandé par le user donné (le clientId du body est ignoré par l'API)
    protected ResultActions retirer(String username, UUID compteId, String montant) throws Exception {
        return mockMvc.perform(post("/api/retrait/create")
                .header(AUTHORIZATION, bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"compteId":"%s","montant":%s}
                        """.formatted(compteId, montant)));
    }

    // solde lu directement en base, pas juste la réponse http
    protected BigDecimal solde(UUID compteId) {
        return compteRepository.findById(compteId).orElseThrow().getSolde();
    }
}
