# HoneyLMS — Backend

Squelette technique du backend HoneyLMS (Honey Group Academy) — Sprint 1.

Ce document décrit les **démarches** à suivre pour installer, lancer et vérifier le projet, ainsi que les décisions techniques prises à cette étape.

---

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ (ou utiliser le wrapper `./mvnw` si ajouté plus tard) | `mvn -version` |
| Docker & Docker Compose | récents | `docker --version` / `docker compose version` |
| Git | récent | `git --version` |

---

## 2. Structure du dépôt

```
honeylms/
├── .github/workflows/backend-ci.yml   # Pipeline CI (build + tests à chaque push/PR)
├── docker-compose.yml                 # Orchestration locale : PostgreSQL + backend
├── .gitignore
├── README.md                          # Ce document
└── backend/
    ├── pom.xml
    ├── Dockerfile
    ├── .dockerignore
    └── src/
        ├── main/java/com/honeygroup/honeylms/
        │   ├── HoneylmsApplication.java
        │   └── user/
        │       ├── Role.java
        │       ├── RoleCode.java
        │       ├── RoleRepository.java
        │       ├── UserAccount.java
        │       └── UserAccountRepository.java
        └── main/resources/
            ├── application.yml
            └── db/migration/
                ├── V1__create_role.sql
                └── V2__create_user_account.sql
```

Le dossier `frontend/` (Angular) sera ajouté au Sprint 2.

---

## 3. Démarches — première mise en route

### Étape 1 — Cloner et se positionner à la racine
```bash
git clone <url-du-repo>
cd honeylms
```

### Étape 2 — Lancer l'environnement complet avec Docker Compose
```bash
docker compose up --build
```
Cette commande :
1. démarre un conteneur PostgreSQL (`honeylms-postgres`) ;
2. build l'image du backend (multi-stage : Maven → JRE Alpine) ;
3. démarre le backend, qui applique automatiquement les migrations Flyway (V1, V2) au démarrage.

### Étape 3 — Vérifier que tout fonctionne
```bash
curl http://localhost:8080/actuator/health
```
Réponse attendue :
```json
{"status":"UP"}
```

### Étape 4 — Vérifier les migrations en base (optionnel)
```bash
docker exec -it honeylms-postgres psql -U honeylms -d honeylms -c "\dt"
docker exec -it honeylms-postgres psql -U honeylms -d honeylms -c "SELECT * FROM role;"
```
Vous devez voir les tables `role`, `user_account`, `flyway_schema_history`, et 3 lignes dans `role` (STUDENT, TRAINER, ADMIN).

### Étape 5 — Arrêter l'environnement
```bash
docker compose down          # arrête les conteneurs, garde les données
docker compose down -v       # arrête et supprime aussi le volume PostgreSQL (repart de zéro)
```

---

## 4. Démarches — développement au quotidien (sans tout redémarrer dans Docker)

Pendant le développement, il est plus rapide de ne lancer que PostgreSQL dans Docker et de faire tourner le backend directement depuis l'IDE ou Maven.

```bash
# 1. Ne démarrer que PostgreSQL
docker compose up postgres -d

# 2. Lancer le backend en local
cd backend
mvn spring-boot:run
```
Le backend se connecte alors à `jdbc:postgresql://localhost:5432/honeylms` (valeur par défaut dans `application.yml`).

---

## 5. Démarches — tests

```bash
cd backend
mvn clean verify
```
Le pipeline CI (`.github/workflows/backend-ci.yml`) exécute exactement cette commande à chaque push et pull request sur `main`, avec une base PostgreSQL de test éphémère.

---

## 6. Démarches — ajouter une nouvelle migration

1. Créer un nouveau fichier dans `backend/src/main/resources/db/migration/`, nommé `V<numéro>__<description>.sql` (ex. `V3__create_course.sql`).
2. **Ne jamais modifier une migration déjà exécutée** (déjà appliquée en local, en CI, ou en démo) — toute évolution passe par un nouveau numéro de version.
3. Relancer l'application : Flyway applique automatiquement les migrations manquantes au démarrage.

---

## 7. Décisions techniques prises à cette étape

| Décision | Choix retenu | Justification |
|---|---|---|
| Version Spring Boot | **4.0.7** | Spring Boot 3.5 a atteint sa fin de support open-source le 30/06/2026 ; 4.0 est la version recommandée pour tout nouveau projet démarré après cette date. Java 21 LTS est utilisé (minimum requis par Boot 4 : Java 17). |
| Driver Flyway PostgreSQL | `spring-boot-starter-flyway` + `flyway-database-postgresql` | Depuis Flyway 10, le support PostgreSQL est un module séparé ; Spring Boot 4 fournit un starter dédié qui gère la version automatiquement. |
| Stratégie Hibernate | `ddl-auto: validate` | Le schéma est piloté **uniquement** par Flyway (cohérent avec la stratégie de migration définie dans le Dossier de Conception). Hibernate ne doit jamais créer/modifier de tables lui-même. |
| Image Docker backend | Build multi-stage (Maven → JRE Alpine), utilisateur non-root | Image finale légère, et bonne pratique de sécurité (pas d'exécution en root dans le conteneur). |
| CI | GitHub Actions, déclenché uniquement sur les changements dans `backend/` | Évite de relancer le pipeline backend pour des changements qui ne le concernent pas (ex. futurs changements `frontend/`). |

**Point de vigilance** (à garder en tête pour le prochain incrément, l'authentification) : Spring Boot 4.0 introduit une réécriture du DSL de Spring Security. La configuration de sécurité/JWT sera vérifiée contre la documentation Spring Security 7 au moment de son implémentation plutôt que d'être écrite de mémoire, pour éviter une config qui compile mais ne se comporte pas comme prévu.

---

## 8. Prochaine incrément (Sprint 1, suite)

- `RoleCode` + données V1 sont prêts → implémenter `US-AUTH-01/02/03` (registration, login, JWT, activation de compte).
- Ajouter Spring Security + configuration JWT.
- Créer les DTO d'authentification (`RegisterRequest`, `LoginRequest`, `AuthResponse`).
