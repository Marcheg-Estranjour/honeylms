# HoneyLMS — Frontend

Application Angular de HoneyLMS (Honey Group Academy).

Ce document décrit les **démarches** pour installer, lancer et vérifier le frontend, ainsi que les décisions techniques prises. Il complète le `README.md` racine (backend).

**Références** :
- Dossier de Conception v2.1 : endpoints, DTO, règles de périmètre ;
- Wireframes & Mock-ups : écrans et charte graphique. Les mock-ups haute fidélité (page « Mock-ups ») font foi pour le visuel. Les wireframes (pages Student / Trainer / Admin) font foi pour le contenu et les règles affichées.

---

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Node.js | 24 LTS (Angular 22 accepte `^22.22.3`, `^24.15.0` ou `^26`) | `node -v` |
| npm | fourni avec Node | `npm -v` |
| Angular CLI | 22 | `ng version` |
| Backend HoneyLMS | lancé sur `http://localhost:8080` | `curl http://localhost:8080/actuator/health` |

Installer la CLI une seule fois :

```bash
npm install -g @angular/cli@22
```

---

## 2. Structure cible

```
honeylms/
├── backend/                       # Spring Boot (voir README racine)
├── frontend/
│   ├── proxy.conf.json            # Dev : /api → http://localhost:8080 (pas de CORS)
│   ├── .nvmrc                     # 24
│   ├── public/                    # favicon.ico, honey-group-logo.png, honey-icon-256.png
│   └── src/
│       ├── _theme-colors.scss     # Palettes Material 3 générées depuis la charte
│       ├── styles.scss            # Thème Material + polices + tokens Honey Group
│       └── app/
│           ├── core/              # auth (service, token, guards, interceptors), api, layout
│           ├── shared/            # composants UI réutilisables, pipes
│           ├── features/
│           │   ├── auth/          # connexion, inscription
│           │   ├── catalog/       # catalogue par domaine, détail d'un cours
│           │   ├── learning/      # mes cours, leçon, progression, reprise
│           │   ├── assignments/   # devoir + dépôt (Student)
│           │   ├── trainer/       # mes formations, éditeur de cours, corrections
│           │   └── admin/         # utilisateurs, comptes Trainer, formations
│           └── app.routes.ts
└── .github/workflows/
    ├── backend-ci.yml
    └── frontend-ci.yml
```

Les dossiers `core/`, `shared/` et `features/` sont créés au fil des incréments, pas à l'avance.

Routes et redirection après connexion (wireframe « Connexion ») :

| Rôle | Page d'accueil | Navigation |
|---|---|---|
| STUDENT | `/my-courses` | Catalogue · Mes cours |
| TRAINER | `/trainer/courses` | Mes formations · Corrections |
| ADMIN | `/admin/users` | Utilisateurs · Formations · (Classes) |

---

## 3. Démarches — création du projet (une seule fois)

### Étape 1 — Branche de travail

```bash
git checkout main && git pull
git checkout -b feature/frontend-setup
```

### Étape 2 — Générer l'application

Depuis la racine du dépôt :

```bash
ng new frontend --style=scss --ssr=false --skip-git --ai-config=none
cd frontend
```

Si la CLI pose d'autres questions, garder les réponses par défaut.

### Étape 3 — Ajouter Angular Material et les polices

```bash
ng add @angular/material
npm install @fontsource/nunito-sans @fontsource/young-serif
```

Pour `ng add`, accepter le thème proposé : il sera remplacé à l'étape 6.

Les polices sont **auto-hébergées** via `@fontsource`, plutôt que chargées depuis Google Fonts. Ainsi, aucune adresse IP d'utilisateur n'est transmise à un tiers (RGPD), et la future CSP nginx reste simple.

### Étape 4 — Brancher le proxy de développement

Créer `frontend/proxy.conf.json` :

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "logLevel": "info"
  }
}
```

Puis :

```bash
ng config projects.frontend.architect.serve.options.proxyConfig proxy.conf.json
echo "24" > .nvmrc
```

### Étape 5 — Vérifier et committer le squelette

```bash
npm start                                   # http://localhost:4200 affiche la page Angular
curl -s http://localhost:4200/api/courses   # JSON du catalogue, reçu via le proxy
npx ng test --watch=false                   # test par défaut vert (Vitest)
npm run build                               # build de production sans erreur
```

Vérifier que `frontend/.gitignore` contient `node_modules/`, `dist/` et `.angular/`, puis :

```bash
cd ..
git add frontend
git commit -m "chore(frontend): scaffold Angular 22 app with Material and dev proxy"
```

### Étape 6 — Appliquer la charte Honey Group (mock-ups)

Générer les palettes Material 3 à partir des couleurs des mock-ups :

```bash
cd frontend
ng generate @angular/material:theme-color
# primary  : #1766E8   (Bleu Honey)
# tertiary : #EE7420   (Orange Group)
# autres   : valeurs par défaut ; dossier de sortie : src/
```

Remplacer le contenu de `src/styles.scss` :

```scss
@use '@angular/material' as mat;
@use './theme-colors' as brand;

// Self-hosted fonts (RGPD: no request to Google Fonts)
@use '@fontsource/nunito-sans/400.css';
@use '@fontsource/nunito-sans/600.css';
@use '@fontsource/nunito-sans/700.css';
@use '@fontsource/nunito-sans/800.css';
@use '@fontsource/young-serif/400.css';

html {
  color-scheme: light;
  @include mat.theme((
    color: (
      primary: brand.$primary-palette,
      tertiary: brand.$tertiary-palette,
    ),
    typography: (
      plain-family: ('Nunito Sans', system-ui, sans-serif),
      brand-family: ('Young Serif', Georgia, serif),
    ),
    density: 0,
  ));

  // Honey Group brand tokens (from the mock-ups)
  --hg-blue: #1766e8;        // brand blue, progress bars
  --hg-blue-action: #1459d1; // primary actions (white text: 6.2:1)
  --hg-navy: #0d2350;        // headings, strong backgrounds
  --hg-orange: #ee7420;      // secondary CTA: navy text only (5.2:1), never white text (2.9:1)
  --hg-orange-text: #b8530a; // orange text on white (4.9:1), e.g. "ACADEMY"
  --hg-sun: #f6c21b;         // highlights, active nav underline, progress on navy
  --hg-success: #2e8b3e;     // completed state
  --hg-sand: #fbf8f2;        // page background
  --hg-text: #243049;        // body text
  --hg-muted: #5b6478;       // secondary text (5.6:1 on sand)
  --hg-border: #e6e1d6;      // card borders

  // Category colors (catalogue tags)
  --hg-cat-languages-bg: #e6effd;  --hg-cat-languages-fg: #0f4ab0;
  --hg-cat-office-bg: #fdeee2;     --hg-cat-office-fg: #9a4308;
  --hg-cat-eductour-bg: #e3f4e1;   --hg-cat-eductour-fg: #23702f;

  --mat-sys-surface: var(--hg-sand);
}

body {
  margin: 0;
  font-family: 'Nunito Sans', system-ui, sans-serif;
  color: var(--hg-text);
  background: var(--hg-sand);
}

h1, h2, .hg-serif {
  font-family: 'Young Serif', Georgia, serif;
  font-weight: 400;
  color: var(--hg-navy);
  margin: 0;
}
```

Ressources graphiques :

- copier `honey-group-logo.png` et `honey-icon-256.png` dans `frontend/public/` ;
- remplacer `frontend/public/favicon.ico` ;
- dans `src/index.html` : `<html lang="fr">` et `<title>Honey Group Academy</title>`.

Vérification : `npm start`, puis ajouter temporairement dans `app.html` un `<h1>Catalogue</h1>` et un `<button mat-flat-button>Test</button>`, avec `MatButtonModule` importé dans le composant. Le titre doit s'afficher en Young Serif marine, le bouton en bleu Honey, le fond doit être sable, et l'onglet doit montrer le favicon. Retirer ces éléments, puis :

```bash
cd ..
git add frontend
git commit -m "feat(ui): apply Honey Group brand theme, fonts and logo"
```

### Étape 7 — Remplacer ce README

Remplacer le `frontend/README.md` généré par `ng new` par ce document, puis :

```bash
git add frontend/README.md
git commit -m "docs(frontend): add setup and run instructions"
git push -u origin feature/frontend-setup
```

Ouvrir la Pull Request vers `main`.

---

## 4. Démarches — développement au quotidien

```bash
# 1. Backend + PostgreSQL (depuis la racine)
docker compose up -d

# 2. Frontend (depuis frontend/)
npm start            # http://localhost:4200, rechargement à chaud, /api relayé vers :8080

# 3. Tests
npx ng test          # mode watch
npx ng test --watch=false
```

L'application appelle toujours l'API en **relatif** (`/api/...`). Aucune URL de backend n'est écrite dans le code.

---

## 5. Charte graphique (issue des mock-ups)

| Rôle | Code | Règle d'usage |
|---|---|---|
| Bleu Honey | `#1766E8` | Couleur de marque, barres de progression |
| Bleu action | `#1459D1` | Boutons principaux, texte blanc (6,2 : 1) |
| Marine | `#0D2350` | Titres, bandeaux forts (texte blanc 15,3 : 1) |
| Orange Group | `#EE7420` | CTA secondaire avec **texte marine** (5,2 : 1). Jamais de texte blanc dessus (2,9 : 1, non conforme). |
| Orange texte | `#B8530A` | Texte orange sur blanc (4,9 : 1) |
| Soleil | `#F6C21B` | Soulignement de navigation active, progression sur fond marine |
| Vert succès | `#2E8B3E` | État « terminé », badge « Inscrit » |
| Sable | `#FBF8F2` | Fond de page |
| Polices | Young Serif (titres) · Nunito Sans (texte) | Auto-hébergées |

Écart volontaire avec les mock-ups : sur le bouton « Reprendre » (orange), le texte passe de blanc à marine pour respecter le contraste WCAG AA.

Couleurs des domaines (tags du catalogue) : Langues = bleu, Bureautique = orange, EDUCTOUR = vert. Les cours n'ont pas d'image dans le modèle de données : la vignette de chaque carte affiche l'icône et la couleur de son domaine, comme dans les mock-ups.

---

## 6. Décisions techniques

| Décision | Statut |
|---|---|
| Angular 22, standalone components, signals, lazy loading par domaine | CHOIX TECHNIQUE |
| Frontend dans `frontend/`, même dépôt que le backend, CI séparée filtrée sur `frontend/**` | CHOIX TECHNIQUE |
| Angular Material comme bibliothèque de composants | CHOIX TECHNIQUE (validé) |
| JWT stocké en `sessionStorage`, expiration calculée depuis `expiresInSeconds` (24h, pas de refresh token — Dossier §12.2) | CHOIX TECHNIQUE (validé) |
| Pas de CORS : proxy Angular en dev, nginx sur la même origine en prod | CHOIX TECHNIQUE |
| Pas de fichiers `environment.ts` : API toujours appelée en relatif | CHOIX TECHNIQUE |
| Guards de route = ergonomie uniquement ; l'autorisation est vérifiée côté backend (Dossier §11) | VALIDÉ (principe) / CHOIX TECHNIQUE (implémentation) |
| Interfaces TypeScript des DTO écrites à la main à partir des records Java | HYPOTHÈSE |
| Vitest (tests unitaires), Playwright (E2E) | CHOIX TECHNIQUE |
| Code en anglais, interface en français, sans framework i18n | VALIDÉ |
| Le Trainer ne crée pas de cours, l'Admin crée les cours et les comptes Trainer (pas de bouton « Nouveau cours » côté Trainer) | VALIDÉ |
| Logo fourni par Honey Group | VALIDÉ |
| Palette et polices des mock-ups (dérivées du logo, sans charte officielle) | HYPOTHÈSE (à remplacer si Honey Group fournit une charte) |
| Polices auto-hébergées (`@fontsource`) plutôt que Google Fonts | CHOIX TECHNIQUE (RGPD) |
| Texte marine sur les boutons orange (écart aux mock-ups, accessibilité) | CHOIX TECHNIQUE |
