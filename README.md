# HoneyLMS — Honey Group Academy

Plateforme de formation (LMS) développée pour **Honey Group** dans le cadre du stage CDA
(fin : 23 octobre 2026). Trois rôles :

- **Étudiant** : catalogue, inscription, leçons et ressources, progression, dépôt de devoirs, correction.
- **Formateur** : ses formations, éditeur (modules, leçons, ressources, publication), corrections notées /20.
- **Administrateur** : comptes (activation, création de formateurs), formations et attribution des formateurs.

| Couche | Technologies |
|---|---|
| Front | Angular 22 (standalone, signals), Angular Material 22, Vitest, Playwright |
| Back | Spring Boot 4.0 / Java 21, Spring Security 7 (JWT stateless), JPA, Flyway |
| Données | PostgreSQL 16 ; fichiers sur volume (`/data/uploads`) |
| Exécution | Docker Compose : `postgres` + `backend` + `frontend` (nginx non-root) |
| Qualité | GitHub Actions (CI backend + frontend obligatoire sur `main`), Conventional Commits, PR |

La conception (MCD/MLD, user stories, règles métier, API, risques) est décrite dans le
**Dossier de Conception**.

---

## 1. Démarrage rapide

Prérequis : Docker + Docker Compose, Git. (Pour développer : JDK 21, Node 24 — voir `.nvmrc`.)

```bash
git clone git@github.com:Marcheg-Estranjour/honeylms.git
cd honeylms
docker compose up -d --build        # ≈ 2 min au premier lancement
```

| Service | Adresse |
|---|---|
| Application (front nginx, `/api` relayé) | http://localhost:8000 |
| API backend (accès direct) | http://localhost:8080/api |
| PostgreSQL | `localhost:5432` (base/utilisateur/mot de passe : `honeylms`) |

Le profil Spring `dev` (par défaut en local) charge le **jeu de démo** (`db/seed/R__demo_seed.sql`).
Comptes, tous avec le mot de passe `Honey2026!` :

| Rôle | Email |
|---|---|
| Administratrice | `admin@honeylms.test` |
| Formateur / formatrice | `formateur@honeylms.test` · `formatrice@honeylms.test` |
| Étudiant(e)s | `etudiant@honeylms.test` · `etudiant2@honeylms.test` |

Ressource PDF et dépôt de devoir de démonstration (ils passent par l'API, le SQL ne peut pas
créer de fichiers) : `./scripts/demo-prepare.sh` (curl, jq, python3).

Arrêter : `docker compose down` · tout réinitialiser (**efface les données**) : `docker compose down -v`.

---

## 2. Structure du dépôt

```
honeylms/
├── .github/workflows/      # backend-ci.yml, frontend-ci.yml (checks obligatoires sur main)
├── docker-compose.yml      # postgres + backend + frontend
├── backend/                # Spring Boot — modules : user, course, enrollment, progress,
│                           #   submission, file, trainingclass, teaching, security, common
├── frontend/               # Angular — core/, shared/, features/{auth,catalog,learning,trainer,admin}
│   ├── e2e/                # tests Playwright (parcours étudiant, formateur, admin)
│   └── nginx/              # configuration de l'image de production
├── scripts/                # demo-prepare.sh, make-pdf.py
└── docs/DEMO.md            # scénario de démonstration minuté
```

Détails du front (installation, conventions, charte, E2E, image nginx) : [`frontend/README.md`](frontend/README.md).

---

## 3. Développement au quotidien

```bash
docker compose up -d postgres backend    # base + API (rebuild du backend : ajouter --build backend)
cd frontend && npm start                 # http://localhost:4200, rechargement à chaud, /api → :8080
```

Backend hors Docker (depuis l'IDE ou Maven) : `docker compose up -d postgres` puis
`cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

Workflow Git : une branche par incrément (`feature/…`, `fix/…`, `test/…`, `docs/…`), commits
*Conventional Commits*, PR vers `main`, merge uniquement si les deux CI sont vertes.

---

## 4. Tests

| Commande | Portée |
|---|---|
| `cd backend && ./mvnw test` | Tests unitaires backend (JUnit 5, Mockito) — exécutés par la CI |
| `cd frontend && npx ng test --watch=false` | Tests unitaires front (Vitest, jsdom) — exécutés par la CI |
| `cd frontend && npm run e2e:docker` | Tests de bout en bout Playwright sur la stack locale (voir le README front) |

---

## 5. Base de données et migrations

- Le schéma est piloté **uniquement par Flyway** (`backend/src/main/resources/db/migration`),
  Hibernate est en `ddl-auto: validate`.
- Nouvelle évolution = nouveau fichier `V<n>__<description>.sql`. **Ne jamais modifier une
  migration déjà appliquée.**
- Le jeu de démo est une migration *répétable* (`db/seed/R__demo_seed.sql`), chargée seulement
  avec le profil `dev`.
- Toutes les dates sont des `Instant` (UTC) côté Java, affichées en heure de Paris par le front.

---

## 6. Principales décisions techniques

| Décision | Justification |
|---|---|
| Monolithe modulaire (un module par domaine) | Simplicité de déploiement pour un MVP, frontières claires entre domaines. |
| JWT stateless en `sessionStorage`, pas de refresh token | Simple et suffisant pour le MVP ; limite connue : un compte désactivé garde l'accès jusqu'à l'expiration du jeton (24 h). |
| Contrôles d'accès côté serveur (rôle + périmètre formateur) | Les guards Angular ne servent qu'au confort de navigation. |
| Front et API sur la même origine (proxy de dev / nginx) | Pas de CORS à configurer, CSP simple (`connect-src 'self'`). |
| Contenu des leçons en texte brut | Aucun HTML interprété : pas de risque XSS. |
| Téléchargements via `HttpClient` + objet blob | Le JWT n'apparaît jamais dans une URL. |
| Images Docker multi-étapes, utilisateurs non-root | Images légères, surface d'attaque réduite. |
| Polices auto-hébergées (`@fontsource`) | Aucune requête vers un tiers (RGPD). |
