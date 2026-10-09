# API REST bancaire sécurisée — Spring Boot · Keycloak · PostgreSQL

Backend de gestion de **clients, comptes, dépôts et retraits**, conçu dès le départ comme un service **déployable en production** et pas comme une simple démo d'API : authentification déléguée à un serveur d'identité (OIDC/JWT), autorisations par rôle **et** par propriété des données, intégrité transactionnelle des soldes, documentation OpenAPI, tests d'intégration sur de vrais conteneurs, CI/CD, déploiement Docker Compose et observabilité (métriques, logs, alertes).

> **Périmètre du projet.** Le domaine métier est volontairement réduit (4 entités) pour que l'attention porte sur ce qui rend un backend *industrialisable* : sécurité, transactions, tests, packaging, déploiement, exploitation. Les limites connues et la feuille de route vers un contexte bancaire réel sont listées à la fin, sans fard.

---

## Sommaire

1. [Vue d'ensemble](#1-vue-densemble)
2. [Stack technique](#2-stack-technique)
3. [Architecture](#3-architecture)
4. [Sécurité](#4-sécurité)
5. [Intégrité des données et transactions](#5-intégrité-des-données-et-transactions)
6. [API et documentation OpenAPI](#6-api-et-documentation-openapi)
7. [Tests](#7-tests)
8. [CI/CD](#8-cicd)
9. [Déploiement : dev, prod, rollback](#9-déploiement--dev-prod-rollback)
10. [Observabilité et exploitation](#10-observabilité-et-exploitation)
11. [Démarrage rapide](#11-démarrage-rapide)
12. [Configuration (variables d'environnement)](#12-configuration-variables-denvironnement)
13. [Limites connues et feuille de route](#13-limites-connues-et-feuille-de-route)

---

## 1. Vue d'ensemble

| Axe | Ce que le dépôt montre concrètement |
|---|---|
| **Backend** | Java 21, Spring Boot 4, architecture REST en couches (controller / service / repository / DTO), JPA/Hibernate, validation Bean Validation, gestionnaire d'erreurs global au format JSON uniforme |
| **Base de données** | PostgreSQL 17, clés UUID, montants en `BigDecimal(19,2)`, contraintes d'unicité et clés étrangères testées, verrouillage pessimiste sur les débits |
| **Sécurité** | Spring Security en *resource server* OAuth2/OIDC, JWT émis par Keycloak, RBAC à 3 rôles, contrôle de propriété des ressources, accès refusé par défaut, API stateless, CORS restreint, conteneur non-root |
| **Industrialisation** | Dockerfile multi-étapes, Docker Compose dev **et** prod séparés, configuration 100 % par variables d'environnement, image publiée sur GHCR, déploiement SSH piloté par la CI |
| **Tests** | Tests d'intégration avec **Testcontainers** (vrais PostgreSQL et Keycloak), matrice de droits, test de concurrence sur les retraits |
| **Exploitation** | Prometheus + Loki + Grafana + Alloy, logs JSON structurés, dashboard provisionné, 8 règles d'alerte, healthchecks sur chaque service |

---

## 2. Stack technique

| Couche | Technologies |
|---|---|
| Langage / framework | Java 21 (Temurin), Spring Boot 4.1, Spring Web MVC, Spring Data JPA (Hibernate), Bean Validation |
| Sécurité | Spring Security, OAuth2 Resource Server (JWT), `@EnableMethodSecurity`, Keycloak 26.2 (OIDC, PKCE) |
| Base de données | PostgreSQL 17 (une base pour l'API, une base distincte pour Keycloak) |
| Documentation | springdoc-openapi (Swagger UI avec bouton *Authorize* branché sur Keycloak) + contrat `openapi.yaml` |
| Tests | JUnit 5, Spring MockMvc, AssertJ, Testcontainers, Maven Failsafe |
| Conteneurs | Docker (multi-stage, utilisateur non-root, `HEALTHCHECK`), Docker Compose |
| CI/CD | GitHub Actions (CI, publication GHCR, déploiement SSH) |
| Observabilité | Spring Actuator + Micrometer, Prometheus, postgres-exporter, Loki, Grafana Alloy, Grafana |

---

## 3. Architecture

```
                 HTTPS (reverse proxy : Nginx / Caddy / Traefik)
                                   │
              ┌────────────────────┴───────────────────┐
              │                                        │
        ┌─────▼─────┐   JWT (OIDC)               ┌─────▼──────┐
        │    API    │◄──────────────────────────►│  Keycloak  │
        │ Spring    │  validation de signature   │  (realm    │
        │ Boot :8282│  via JWKS                  │  banque)   │
        └─────┬─────┘                            └─────┬──────┘
              │ JDBC                                   │ JDBC
        ┌─────▼─────┐                            ┌─────▼──────┐
        │ PostgreSQL│                            │ PostgreSQL │
        │   (API)   │                            │ (Keycloak) │
        └───────────┘                            └────────────┘

   Surcouche optionnelle : Prometheus ─ Loki ─ Alloy ─ Grafana ─ postgres-exporter
```

Points d'architecture assumés :

- **Séparation des responsabilités** : l'API ne gère ni mots de passe ni sessions ; l'identité est entièrement déléguée à Keycloak. Le `sub` du JWT est la clé primaire du `Client` en base.
- **Deux bases isolées** (API / Keycloak), chacune avec son volume, ses identifiants et son healthcheck.
- **Deux URLs Keycloak distinctes** : l'URL publique (celle du claim `iss`) et l'URL interne au réseau Docker (récupération des clés JWKS), configurées séparément.
- **Réseau Docker dédié** ; en production, **aucun port n'est exposé publiquement** : tout est lié à `127.0.0.1` derrière le reverse proxy.

### Structure du projet

```
.
├── src/main/java/com/banque/transaction/
│   ├── Client/ Compte/ Depot/ Retrait/   # un package par domaine : Controller, Service, Repository, Entity, DTO
│   ├── Security/                         # SecurityConfig, convertisseur de rôles Keycloak, AccessGuard, CurrentUser,
│   │                                     # handlers 401/403 au format JSON
│   ├── Exceptions/                       # exceptions métier + GlobalExceptionHandler
│   ├── openapi/                          # configuration OpenAPI/Swagger (OAuth2 + PKCE, erreurs communes)
│   └── ServerResponse/                   # enveloppe de réponse uniforme
├── src/main/resources/
│   ├── application.yaml                  # 100 % piloté par variables d'environnement
│   └── openapi/openapi.yaml              # contrat d'API
├── src/test/java/…                       # tests d'intégration (Testcontainers)
├── keycloak/                             # realms importés au démarrage (dev / prod)
├── observability/                        # Prometheus, alertes, Loki, Alloy, Grafana (dashboards + provisioning)
├── .github/workflows/                    # ci.yml, cd.yml
├── Dockerfile
├── docker-compose.yaml                   # dev
├── docker-compose.prod.yaml              # production
└── docker-compose.observability.yaml     # surcouche monitoring
```

---

## 4. Sécurité

### Authentification
- L'API est un **OAuth2 Resource Server** : elle valide la signature (clés JWKS de Keycloak), l'émetteur (`iss`) et l'expiration de chaque JWT.
- **Stateless** : aucune session, CSRF désactivé car l'authentification par jeton Bearer n'utilise pas de cookie.
- Les réponses **401** (jeton absent/invalide) et **403** (droits insuffisants) sont renvoyées au même format JSON que le reste de l'API (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`).
- Le client Swagger utilise le flux **Authorization Code + PKCE (S256)**, client public.

### Autorisation : RBAC + propriété des données (deux niveaux)

**Niveau 1 : rôles** (`SecurityConfig`, règles par route, `anyRequest().denyAll()` en filet de sécurité) :

| Opération | ADMIN | SECRETAIRE | CLIENT |
|---|:-:|:-:|:-:|
| Créer client / compte | ✅ | ✅ | ❌ |
| Lister tous les clients / comptes / dépôts / retraits | ✅ | ✅ | ❌ |
| Supprimer client / compte | ✅ | ❌ | ❌ |
| Enregistrer un dépôt | ❌ | ✅ | ❌ |
| Effectuer un retrait | ❌ | ❌ | ✅ |
| Consulter ses propres ressources | ✅ | ✅ | ✅ |

Le principe du **moindre privilège** est appliqué jusqu'au détail : un `ADMIN` ne peut pas enregistrer de dépôt, seule la `SECRETAIRE` le peut.

**Niveau 2 : propriété** (`@PreAuthorize` + `AccessGuard`) : un `CLIENT` n'accède qu'à *ses* comptes, dépôts et retraits. Il obtient un `403` sur la ressource d'un autre client même s'il en connaît l'identifiant (protection contre les accès par identifiant, *BOLA/IDOR* dans la terminologie OWASP API Security).

### Autres mesures
- **Secrets hors du code** : aucune valeur en dur dans `application.yaml`, aucune valeur par défaut volontairement ; si une variable manque, l'application **refuse de démarrer** et indique laquelle. Le fichier `.env` n'est pas versionné.
- **CORS** limité à une liste d'origines configurable, méthodes et en-têtes explicitement listés.
- **Swagger désactivable** en production (`SWAGGER_ENABLED=false`).
- **Actuator** : seuls `health` et `info` sont exposés publiquement ; en mode observabilité, les métriques passent par un **port de management séparé, jamais publié**.
- **Image non-root** (`USER spring`), image runtime JRE Alpine minimale.
- **Validation des entrées** : montants strictement positifs, 2 décimales maximum, identifiants requis ; erreurs de désérialisation et de type renvoyées en `400` propre, sans fuite de détails internes.

---

## 5. Intégrité des données et transactions

- **Montants en `BigDecimal(19,2)`**, jamais de flottant.
- **Opérations atomiques** : `@Transactional` sur les services ; les lectures sont en `readOnly = true`.
- **Verrou pessimiste (`SELECT … FOR UPDATE`)** sur le compte lors d'un retrait, pour éviter les doubles débits sous concurrence.
- **Règles métier appliquées côté service** : le solde ne peut pas devenir négatif (`422`), un client ne retire que de son propre compte (`403`), un dépôt peut être fait sur le compte d'un tiers, retraits et dépôts sont **immuables** (pas de modification ni de suppression), suppression d'un compte non vide refusée (`409`).
- **Contraintes en base** : numéro de compte unique, clés étrangères (supprimer un client ayant un compte → `409`).
- `open-in-view: false` pour éviter les accès paresseux hors transaction.

---

## 6. API et documentation OpenAPI

- Swagger UI : `http://localhost:8282/swagger-ui.html` (bouton **Authorize** → connexion Keycloak, PKCE).
- Spécification : `/v3/api-docs` et contrat versionné `src/main/resources/openapi/openapi.yaml`.
- Les erreurs communes (401, 403, 500) sont ajoutées automatiquement à chaque opération, avec le schéma exact renvoyé par le gestionnaire d'erreurs.
- 20 endpoints répartis sur 4 ressources :

| Ressource | Endpoints |
|---|---|
| `/api/client` | `create`, `all`, `findbyid/{id}`, `update/{id}`, `delete/{id}` |
| `/api/compte` | `create`, `all`, `findbyid/{id}`, `findbyclient/{clientId}`, `delete/{id}` |
| `/api/depot` | `create`, `all`, `findbyid/{id}`, `findbycompte/{compteId}`, `findbyclient/{clientId}` |
| `/api/retrait` | `create`, `all`, `findbyid/{id}`, `findbycompte/{compteId}`, `findbyclient/{clientId}` |

---

## 7. Tests

Tous les tests sont des **tests d'intégration** exécutés contre de **vrais conteneurs PostgreSQL 17 et Keycloak 26.2** via Testcontainers (mêmes versions d'images que le Compose), avec de vrais jetons JWT. Aucune base en mémoire, aucun mock de la sécurité : ce qui est testé est ce qui tourne en production.

| Suite | Ce qu'elle vérifie |
|---|---|
| `SecuriteIT` | Sans jeton → 401, jeton falsifié → 401, endpoints publics, **matrice de droits paramétrée** (utilisateur × méthode × URL → statut attendu) |
| `ParcoursBancaireIT` | Parcours complet : dépôt puis retrait (solde final correct), retrait > solde → 422 et solde inchangé, retrait sur le compte d'autrui → 403, isolation des données entre clients, validation des montants, suppression avec contraintes (409) |
| `RetraitsConcurrentsIT` | **5 retraits simultanés de 30 sur un solde de 100 → exactement 3 succès, 2 refus, solde final 10** (preuve du verrouillage) |
| `CompteRepositoryIT` | Unicité du numéro de compte, requêtes de propriété, requête avec verrou |
| `TransactionApplicationIT` | Démarrage du contexte complet |

```bash
./mvnw verify      # lance les IT via Failsafe (nécessite Docker)
```

---

## 8. CI/CD

**`ci.yml`** (push sur `main`, pull requests) :
1. build + `mvn verify` (tous les tests d'intégration, images pré-téléchargées en parallèle) ;
2. publication des rapports de tests en artefact ;
3. build de l'image Docker (sans push) pour valider le Dockerfile ;
4. annulation automatique des exécutions obsolètes (`concurrency`).

**`cd.yml`** (déclenché uniquement si la CI est verte sur `main`, ou manuellement) :
1. build et publication de l'image sur **GHCR**, taguée `sha-<commit>` **et** `latest` ;
2. déploiement par **SSH** sur le serveur (`pull` + `up -d`) via l'environnement GitHub `production`, qui permet d'imposer une **validation manuelle** avant mise en ligne ;
3. un déploiement en cours n'est jamais interrompu (`cancel-in-progress: false`).

Les secrets (hôte, utilisateur, clé SSH, chemin) sont dans les *secrets* GitHub, jamais dans le dépôt.

---

## 9. Déploiement : dev, prod, rollback

Trois fichiers Compose, un par usage, plutôt qu'un fichier unique conditionné :

| | `docker-compose.yaml` (dev) | `docker-compose.prod.yaml` (prod) |
|---|---|---|
| API | build local depuis le Dockerfile | **image immuable** publiée par la CI (`API_IMAGE:API_TAG`) |
| Keycloak | `start-dev`, HTTP | `start`, derrière reverse proxy HTTPS (`KC_PROXY_HEADERS=xforwarded`) |
| Ports | publiés | **liés à `127.0.0.1` uniquement** |
| Redémarrage | `unless-stopped` | `always` |
| Variables | `.env` | `.env` avec **variables obligatoires** (`${VAR:?message}` : échec explicite si absente) |
| Realm | realm de dev avec comptes de test | realm de production distinct, sans comptes de test |

Chaque service a un **healthcheck**, et l'ordre de démarrage est piloté par `depends_on: condition: service_healthy` (bases → Keycloak → API).

### Mise en production (sur le serveur)

```bash
cd /opt/banque        # dossier contenant .env, docker-compose.prod.yaml et keycloak/
docker compose -f docker-compose.prod.yaml pull api
docker compose -f docker-compose.prod.yaml up -d
```

### Rollback

Chaque déploiement correspond à un tag d'image immuable (`sha-<commit>`). Revenir en arrière consiste à redéployer le tag précédent :

```bash
API_TAG=sha-abc1234 docker compose -f docker-compose.prod.yaml up -d api
```

---

## 10. Observabilité et exploitation

Surcouche optionnelle, activable en dev comme en prod :

```bash
docker compose -f docker-compose.prod.yaml -f docker-compose.observability.yaml up -d
```

| Besoin | Outil | Détail |
|---|---|---|
| Métriques | **Prometheus** (rétention 15 j) | API (JVM, HTTP, pool Hikari), Keycloak, PostgreSQL (via `postgres-exporter`) |
| Logs | **Loki** (rétention 7 j) + **Alloy** | Collecte de tous les conteneurs ; logs de l'API en **JSON structuré**, niveau extrait en label |
| Visualisation | **Grafana** | Datasources et dashboard **provisionnés** (`Banque API – vue d'ensemble`) : débit, taux 5xx, latence p95 par endpoint, refus 401/403, heap JVM, pool de connexions, logs WARN/ERROR |
| Alerting | Règles Prometheus | `ApiDown`, taux d'erreurs 5xx > 5 %, latence p95 > 1 s, pic de 401/403 (tentatives d'accès suspectes), pool de connexions saturé, heap JVM > 90 %, `PostgresDown`, `KeycloakDown` |

Bonnes pratiques appliquées : port de management Actuator séparé et non publié, Prometheus/Grafana liés à `127.0.0.1`, Loki sans port publié, mot de passe Grafana obligatoire, inscription Grafana désactivée.

---

## 11. Démarrage rapide

**Prérequis** : Docker + Docker Compose (et JDK 21 uniquement pour lancer hors conteneur).

```bash
git clone https://github.com/danielWorj/api-rest-securise-keycloak-swagger-postgresql.git
cd api-rest-securise-keycloak-swagger-postgresql

cp .env.example .env        # puis adapter les valeurs (voir section 12)
docker compose up --build
```

| Service | URL |
|---|---|
| API / Swagger UI | http://localhost:8282/swagger-ui.html |
| Keycloak (console admin) | http://localhost:8585 |

Les comptes de test (`admin1`, `secretaire1`, `client1`) sont définis dans `keycloak/realm-banque.json`.

Avec l'observabilité :

```bash
docker compose -f docker-compose.yaml -f docker-compose.observability.yaml up --build
# Grafana : http://localhost:3000 · Prometheus : http://localhost:9090
```

Lancer les tests :

```bash
./mvnw verify
```

---

## 12. Configuration (variables d'environnement)

Un fichier `.env.example` est fourni. **Aucune valeur par défaut n'est codée** : une variable manquante empêche le démarrage.

| Variable | Rôle |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Base PostgreSQL de l'API (`DB_HOST`, `DB_PORT` sont forcés par le Compose) |
| `KC_DB_NAME`, `KC_DB_USER`, `KC_DB_PASSWORD` | Base PostgreSQL de Keycloak |
| `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | Compte administrateur Keycloak |
| `KEYCLOAK_PUBLIC_URL` | URL publique de Keycloak (devient le claim `iss`), **sans** `/realms/...` |
| `KEYCLOAK_PORT` | Port publié de Keycloak (8585) |
| `KEYCLOAK_REALM`, `KEYCLOAK_SWAGGER_CLIENT_ID` | Realm et client utilisés par Swagger |
| `SERVER_PORT` | Port de l'API (8282) |
| `JPA_DDL_AUTO`, `JPA_SHOW_SQL` | Comportement Hibernate (voir feuille de route : `validate` + migrations en prod) |
| `SWAGGER_ENABLED` | Active/désactive Swagger UI et `/v3/api-docs` |
| `APP_CORS_ORIGINS` | Origines autorisées (frontends) |
| `GRAFANA_ADMIN_PASSWORD` | Obligatoire avec la surcouche d'observabilité |
| `API_IMAGE`, `API_TAG` | Image et tag déployés (production) |

---

## 13. Limites connues et feuille de route

Ce dépôt est un socle sérieux, pas un cœur bancaire. Voici, honnêtement, ce qui reste à faire pour le rapprocher d'une exigence bancaire de production, **dans l'ordre où je les traiterais** :

| Sujet | Situation actuelle | Prochaine étape |
|---|---|---|
| **Migrations de schéma** | Schéma géré par Hibernate (`JPA_DDL_AUTO`) | Flyway ou Liquibase, `ddl-auto=validate` en UAT/PROD, scripts versionnés et rejouables |
| **Index et performances** | Index implicites (clés primaires, unicité du numéro de compte, clés étrangères) ; listes non paginées | Index ciblés selon les plans d'exécution (`EXPLAIN ANALYZE`), pagination des endpoints `all`/`findby*` |
| **Microsoft SQL Server** | Développé et testé sur PostgreSQL ; le code JPA reste portable | Profil SQL Server (driver, dialecte, Testcontainers MSSQL) pour valider la portabilité |
| **TLS** | Terminé au reverse proxy (config prévue pour `X-Forwarded-*`) ; certificats non fournis dans le dépôt | Exemple de configuration Nginx/Caddy, TLS bout-en-bout vers Keycloak et PostgreSQL, realm avec `sslRequired=external` |
| **AD / LDAP / SSO** | Identité déléguée à Keycloak, qui supporte nativement la fédération LDAP/AD et le SSO | Configurer et documenter une fédération LDAP/AD de démonstration |
| **Audit trail** | Logs applicatifs JSON, alerte sur les pics de 401/403 ; le dépôt enregistre déjà le nom de la secrétaire sur un dépôt | Journal d'audit métier immuable (qui/quoi/quand/avant/après) et événements de sécurité Keycloak exportés vers Loki |
| **Durcissement Keycloak** | Realm de dev simple | Protection brute-force, politique de mots de passe, durée de vie des jetons réduite, rotation des clés |
| **Gestion des secrets** | Variables d'environnement via `.env` non versionné | Docker/Kubernetes secrets ou coffre (Vault) |
| **Tests de sécurité et de charge** | Tests fonctionnels et de sécurité d'intégration | SAST/SCA en CI (CodeQL, Dependabot, OWASP Dependency-Check, Trivy sur l'image), tests de charge (k6/Gatling), passe OWASP API Top 10 |
| **Sauvegarde / restauration, HA** | Volumes Docker persistants, `restart: always` | Procédure `pg_dump`/PITR testée, réplication PostgreSQL, plusieurs instances d'API derrière le proxy |
| **Alerting** | Règles Prometheus visibles dans Grafana | Alertmanager (e-mail/Slack) |
| **Orchestration** | Docker Compose sur serveur unique | Manifests Kubernetes/OpenShift (probes déjà prêtes via Actuator) |
| **Intégrations bancaires** | Hors périmètre de ce dépôt | API Gateway/ESB, Core Banking, KYC/AML via adaptateurs et contrats OpenAPI |

---

## Auteur

[**danielWorj**](https://github.com/danielWorj), développeur backend Java / Spring Boot. Ce dépôt illustre ma façon d'aborder un backend destiné à la production : sécurité d'abord, intégrité des données, tests sur l'infrastructure réelle, automatisation du déploiement et visibilité en exploitation.
