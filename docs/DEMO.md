# HoneyLMS — Scénario de démonstration

Démo de fin de stage (**vendredi 23 octobre 2026**). Durée visée : **12 minutes** + questions.
Fil conducteur : *une même formation vue par les trois rôles* — l'étudiante apprend et rend son
devoir, le formateur corrige et fait évoluer son cours, l'administratrice pilote la plateforme.

---

## 1. Préparation

### La veille

```bash
git checkout main && git pull
docker compose down -v                 # repartir d'une base propre (supprime les données !)
docker compose up -d --build           # base + backend (jeu de démo) + front nginx
./scripts/demo-prepare.sh              # ressource PDF + dépôt de Camille « à corriger »
cd frontend && E2E_BASE_URL=http://localhost:8000 npm run e2e:docker   # 6 tests verts
```

> Les tests E2E créent une étudiante « Emma Test… » désactivée : sans gravité, mais pour une
> liste d'utilisateurs impeccable, refaire `down -v` / `up` / `demo-prepare.sh` **après** les tests.

### 30 minutes avant

- [ ] `docker compose ps` : 3 conteneurs `running` / `healthy`.
- [ ] Navigateur en **fenêtre privée** (aucune session résiduelle), zoom 110 %, sur **http://localhost:8000**.
- [ ] Trois onglets prêts : étudiante, formateur, admin (chacun sa fenêtre privée, car la session
      est stockée par onglet dans `sessionStorage`).
- [ ] Un fichier PDF sur le bureau pour le dépôt en direct.
- [ ] Notifications du poste coupées.

### Comptes (mot de passe commun : `Honey2026!`)

| Rôle | Email | Personne |
|---|---|---|
| Étudiante | `etudiant@honeylms.test` | Camille Martin |
| Étudiant | `etudiant2@honeylms.test` | Lucas Petit |
| Formateur | `formateur@honeylms.test` | Paul Durand (Anglais, Excel, Word) |
| Formatrice | `formatrice@honeylms.test` | Sofia Garcia (Espagnol, Italien, EDUCTOUR) |
| Administratrice | `admin@honeylms.test` | Alice Bernard |

---

## 2. Déroulé

### Acte 1 — L'étudiante (4 min) · Camille

| ⏱ | Action | Ce qu'on montre / dit |
|---|---|---|
| 0:00 | Connexion | Écran aux couleurs Honey Group ; rôle → page d'accueil adaptée. |
| 0:30 | **Mes cours** | Bandeau « Reprendre là où vous en étiez », progression par cours. |
| 1:00 | Reprendre → leçon | Plan du cours, compteurs par module, contenu en texte brut (XSS impossible). |
| 1:45 | Module 1 · *Se présenter* → **Télécharger** la ressource | Téléchargement sécurisé (JWT, jamais dans l'URL). |
| 2:15 | **Marquer comme terminée** | La progression se met à jour (leçons publiées uniquement, la note ne compte pas). |
| 2:45 | Module 2 · *Rédiger un email de relance* → **Devoir** | Consigne, date limite, statut **Rendu** (dépôt préparé). |
| 3:30 | Montrer « Remplacer le fichier » | Avertissement : remplacer réinitialise la correction. *Ne pas remplacer.* |

### Acte 2 — Le formateur (4 min) · Paul

| ⏱ | Action | Ce qu'on montre / dit |
|---|---|---|
| 4:00 | **Mes formations** | Statut publié/brouillon, inscrits, « 1 dépôt à corriger ». |
| 4:30 | **Corrections** → *Rédiger un email…* | Rendus / inscrits, tri par date limite, filtre par formation. |
| 5:00 | Camille → télécharger → note **15,5** + commentaire → Enregistrer | Note facultative /20, virgule acceptée, contrôles côté client **et** serveur. |
| 6:00 | Onglet Camille → recharger le devoir | **Corrigé par Paul Durand**, note et commentaire. |
| 6:45 | Mes formations → Anglais → **Gérer** → leçon 4 du module 2 (brouillon) | Éditeur : contenu, ressource, **Publier**. |
| 7:30 | Onglet Camille → recharger la leçon | La nouvelle leçon apparaît ; le % recalculé (règle « publié uniquement »). |

### Acte 3 — L'administratrice (2 min) · Alice

| ⏱ | Action | Ce qu'on montre / dit |
|---|---|---|
| 8:00 | **Utilisateurs** | Filtres par rôle, recherche sans accents ; on ne peut pas se désactiver soi-même. |
| 8:30 | Désactiver **Lucas** → tenter sa connexion → réactiver | « Ce compte est désactivé ». |
| 9:15 | **Formations** → Créer « Allemand A2 » → attribuer Sofia | Brouillon par défaut, attribution des formateurs actifs. |

### Conclusion technique (2–3 min)

- Architecture : Angular 22 (standalone, signals) · Spring Boot 4 / Java 21 · PostgreSQL + Flyway ·
  JWT stateless · monolithe modulaire · Docker (nginx non-root + backend + base).
- Qualité : sprints d'une semaine, Conventional Commits, PR + CI obligatoire sur `main`,
  tests unitaires (JUnit, Vitest) et **6 tests E2E Playwright** sur ces mêmes parcours.
- Sécurité : contrôles d'accès côté serveur (rôles + périmètre formateur), CSP, uploads filtrés,
  aucun HTML interprété.
- Limites assumées du MVP : pas de dépublication/suppression de modules et leçons, jeton valide
  jusqu'à 24 h après désactivation, écrans Classes et création de devoir hors périmètre.

---

## 3. Plan B

| Incident | Réaction |
|---|---|
| Un conteneur ne démarre pas | `docker compose logs backend --tail=50` ; sinon basculer en mode dev : `npm start` → http://localhost:4200 |
| Base dans un état inattendu | `docker compose down -v && docker compose up -d --build && ./scripts/demo-prepare.sh` (≈ 2 min) |
| Réseau / poste indisponible | Rejouer la démo via le rapport Playwright (`npm run e2e:report`) et les captures d'écran du dossier |
| Question sur un point non livré | Renvoyer au Dossier de Conception : backlog, risques et dette documentés |
