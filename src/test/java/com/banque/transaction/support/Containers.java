package com.banque.transaction.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

// conteneurs partagés par tous les IT, lancés une seule fois par JVM
// (mêmes versions d'images que le docker-compose)
public final class Containers {

    public static final String REALM = "banque";

    public static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    public static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
                    .withCommand("start-dev", "--import-realm")
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    // le realm est copié dans les test-classes par le pom (testResources)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/realm-banque.json", 0644),
                            "/opt/keycloak/data/import/realm-banque.json")
                    .withExposedPorts(8080)
                    // répond 200 seulement quand le realm est importé
                    .waitingFor(Wait.forHttp("/realms/" + REALM)
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static {
        // démarrage en parallèle, ça fait gagner 15-20 s
        Startables.deepStart(POSTGRES, KEYCLOAK).join();
    }

    // url du realm vue depuis la JVM de test, = claim "iss" des tokens
    public static String realmUrl() {
        return "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080) + "/realms/" + REALM;
    }

    private Containers() {
    }
}
