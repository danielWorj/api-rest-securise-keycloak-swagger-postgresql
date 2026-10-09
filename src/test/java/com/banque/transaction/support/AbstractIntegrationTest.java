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

    // --- Identifiants fixes des utilisateurs de keycloak/realm-banque.json (= claim "sub") ---
    protected static final UUID ADMIN_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    protected static final UUID SECRETAIRE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    protected static final UUID CLIENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    protected static final String ADMIN = "admin1";
    protected static final String SECRETAIRE = "secretaire1";
    protected static final String CLIENT = "client1";
    private static final String PASSWORD = "Passw0rd!";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    /** Un jeton vit 30 min dans le realm : on le demande une seule fois par utilisateur. */
    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ClientRepository clientRepository;
    @Autowired protected CompteRepository compteRepository;
    @Autowired protected DepotRepository depotRepository;
    @Autowired protected RetraitRepository retraitRepository;

    /** Branche Spring sur les conteneurs (leurs ports changent à chaque exécution). */
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

    /** Les conteneurs sont partagés : on repart d'une base vide avant chaque test (ordre = clés étrangères). */
    @BeforeEach
    protected void viderLaBase() {
        retraitRepository.deleteAllInBatch();
        depotRepository.deleteAllInBatch();
        compteRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    // ------------------------------------------------------------------ jetons

    protected String rawToken(String username) {
        return TOKENS.computeIfAbsent(username, AbstractIntegrationTest::fetchToken);
    }

    /** Valeur prête pour l'en-tête Authorization. */
    protected String bearer(String username) {
        return "Bearer " + rawToken(username);
    }

    /** Vrai login auprès de Keycloak (grant "password" autorisé pour le client swagger-ui). */
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

    // ------------------------------------------------------------------ helpers métier

    /** Crée un client (en ADMIN). Son id doit être le "sub" du jeton s'il doit se connecter. */
    protected void creerClient(UUID id, String nom) throws Exception {
        mockMvc.perform(post("/api/client/create")
                        .header(AUTHORIZATION, bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","nom":"%s","contact":"+237600000000"}
                                """.formatted(id, nom)))
                .andExpect(status().isCreated());
    }

    /** Crée un compte (en ADMIN) et renvoie son id. */
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

    /** Dépôt enregistré par la secrétaire. */
    protected ResultActions deposer(UUID deposantId, UUID compteId, String montant) throws Exception {
        return mockMvc.perform(post("/api/depot/create")
                .header(AUTHORIZATION, bearer(SECRETAIRE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"clientId":"%s","compteId":"%s","montant":%s}
                        """.formatted(deposantId, compteId, montant)));
    }

    /** Retrait demandé par l'utilisateur donné (le clientId du corps est de toute façon ignoré par l'API). */
    protected ResultActions retirer(String username, UUID compteId, String montant) throws Exception {
        return mockMvc.perform(post("/api/retrait/create")
                .header(AUTHORIZATION, bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"compteId":"%s","montant":%s}
                        """.formatted(compteId, montant)));
    }

    /** Solde lu DIRECTEMENT en base (on vérifie l'état réel, pas seulement la réponse HTTP). */
    protected BigDecimal solde(UUID compteId) {
        return compteRepository.findById(compteId).orElseThrow().getSolde();
    }
}
