# Lancer les services et les tests

Prérequis : Docker + Docker Compose (obligatoire aussi pour les tests, qui démarrent de vrais conteneurs PostgreSQL et Keycloak). JDK 21 seulement si tu lances Maven en dehors de Docker.

---

## 1. Préparer la configuration

```bash
cp .env.example .env
# puis adapter les valeurs dans .env (mots de passe, ports, URLs)
```

---

## 2. Lancer tous les services

### Tout en une commande (API + Keycloak + 2 bases PostgreSQL)

```bash
docker compose up --build
```

En arrière-plan :

```bash
docker compose up --build -d
```

| Service | URL |
|---|---|
| API / Swagger UI | http://localhost:8282/swagger-ui.html |
| Keycloak (console admin) | http://localhost:8585 |

### Tout + observabilité (Prometheus, Loki, Alloy, Grafana)

```bash
docker compose -f docker-compose.yaml -f docker-compose.observability.yaml up --build -d
```

| Service | URL |
|---|---|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

### Lancer les services un par un (dans cet ordre)

```bash
docker compose up -d postgres keycloak-db      # 1. les bases de données
docker compose up -d keycloak                  # 2. le serveur d'identité
docker compose up -d --build api               # 3. l'API
```

### Production

```bash
docker compose -f docker-compose.prod.yaml pull api
docker compose -f docker-compose.prod.yaml up -d
```

### Utile

```bash
docker compose ps                  # état et santé des services
docker compose logs -f api         # logs de l'API en direct
docker compose down                # arrêter (garde les données)
docker compose down -v             # arrêter ET supprimer les données
```

---

## 3. Lancer les tests, partie par partie

Tous les tests sont des tests d'intégration (`*IT`), exécutés par Maven Failsafe. La commande `mvn test` n'en lance aucun : il faut utiliser `verify`.

Chaque commande démarre ses conteneurs de test automatiquement (la première fois, le téléchargement des images peut être long).

| Partie | Commande |
|---|---|
| Démarrage de l'application (contexte complet) | `./mvnw verify -Dit.test=TransactionApplicationIT` |
| Base de données / repository (unicité, propriété, verrou) | `./mvnw verify -Dit.test=CompteRepositoryIT` |
| Sécurité (401, jeton falsifié, matrice des droits) | `./mvnw verify -Dit.test=SecuriteIT` |
| Parcours bancaire (dépôt, retrait, validations, suppressions) | `./mvnw verify -Dit.test=ParcoursBancaireIT` |
| Concurrence (5 retraits simultanés) | `./mvnw verify -Dit.test=RetraitsConcurrentsIT` |

Un seul test précis :

```bash
./mvnw verify -Dit.test=ParcoursBancaireIT#depotPuisRetrait
```

Sous Windows, remplace `./mvnw` par `mvnw.cmd`.

---

## 4. Lancer tous les tests en une seule commande

```bash
./mvnw verify
```

Les rapports sont dans `target/failsafe-reports/`.

Pour repartir d'un build propre :

```bash
./mvnw clean verify
```

---

## 5. Ordre recommandé

1. `./mvnw verify -Dit.test=TransactionApplicationIT` : l'application démarre.
2. `./mvnw verify -Dit.test=CompteRepositoryIT` : la base se comporte comme prévu.
3. `./mvnw verify -Dit.test=SecuriteIT` : la sécurité est en place.
4. `./mvnw verify -Dit.test=ParcoursBancaireIT` : les règles métier fonctionnent.
5. `./mvnw verify -Dit.test=RetraitsConcurrentsIT` : les soldes résistent à la concurrence.
6. `./mvnw verify` : tout d'un coup, comme la CI.
7. `docker compose up --build` : lancer les services.
