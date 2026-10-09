package com.banque.transaction.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

/**
 * Conteneurs partagés par TOUS les tests d'intégration (démarrés une seule fois par JVM).
 * Les versions d'image sont celles de docker-compose.yaml.
 */
public final class Containers {

    public static final String REALM = "banque";

    public static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    public static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
                    .withCommand("start-dev", "--import-realm")
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    // le realm du projet (voir <testResources> dans le pom.xml)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/realm-banque.json", 0644),
                            "/opt/keycloak/data/import/realm-banque.json")
                    .withExposedPorts(8080)
                    // /realms/banque ne répond 200 qu'une fois le realm importé
                    .waitingFor(Wait.forHttp("/realms/" + REALM)
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static {
        // Démarrage en parallèle : on gagne ~15-20 s
        Startables.deepStart(POSTGRES, KEYCLOAK).join();
    }

    /** URL du realm telle que vue depuis la JVM de test (= valeur du claim "iss" des jetons). */
    public static String realmUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + REALM;
    }

    private Containers() {
    }
}
