# OPUS — Documentation fonctionnelle de l'application

**Opérations Policières Unifiées et Structurées**

> Plateforme intégrée de gestion des informations policières, disponible sur **Android** (Kotlin + Jetpack Compose) et **Desktop** (Electron + React + TypeScript), adossée à une **API REST PHP** sans framework et à une base de données **MySQL**.

---

## Table des matières

1. [Objet de l'application](#1-objet-de-lapplication)
2. [Architecture générale](#2-architecture-générale)
3. [Connexion et authentification](#3-connexion-et-authentification)
4. [Rôles et permissions](#4-rôles-et-permissions)
5. [Organisation fonctionnelle (divisions)](#5-organisation-fonctionnelle-divisions)
6. [Navigation principale](#6-navigation-principale)
7. [Écrans transverses](#7-écrans-transverses)
8. [Gestion du personnel](#8-gestion-du-personnel)
9. [Division Sédentaire — Secrétariat](#9-division-sédentaire--secrétariat)
10. [Division Sédentaire — Poste](#10-division-sédentaire--poste)
11. [Division Service Général](#11-division-service-général)
12. [Division Police Judiciaire](#12-division-police-judiciaire)
13. [Cartographie](#13-cartographie)
14. [Administration (Utilisateurs, Rôles, Journal d'audit)](#14-administration-utilisateurs-rôles-journal-daudit)
15. [Notifications et alertes](#15-notifications-et-alertes)
16. [Profil et paramètres](#16-profil-et-paramètres)
17. [Fonctions spéciales téléphone ↔ ordinateur](#17-fonctions-spéciales-téléphone--ordinateur)
18. [Différences d'interface Android / Desktop](#18-différences-dinterface-android--desktop)
19. [Workflows utilisateurs importants](#19-workflows-utilisateurs-importants)
20. [Conceptions techniques transverses](#20-conceptions-techniques-transverses)

---

## 1. Objet de l'application

OPUS est une application métier de **gestion des opérations et de l'information policière** pour un Commissariat (CSP — Police Nationale). Elle numérise les registres et processus de trois divisions opérationnelles :

- **Division Sédentaire** — subdivisée en **Secrétariat** (correspondances, déclarations de perte, personnel, main courante) et **Poste** (passations de service, armement, armes, matériels, matériel roulant, main courante) ;
- **Division Service Général (SG)** — rassemblements journaliers, évènements survenus sur la voie publique, activités (patrouilles et interventions), dispositifs exceptionnels ;
- **Division Police Judiciaire (PJ)** — plaintes, convocations, gardes à vue (GAV), réquisitions, personnes recherchées, objets saisis/trouvés, perquisitions, mandats, arrestations, renseignements judiciaires.

L'application couvre le cycle complet : **saisie (formulaires), consultation (listes et fiches de détail), pièces jointes, notifications, audit et pilotage par dashboards.**

---

## 2. Architecture générale

| Composant | Technologie | Rôle |
|---|---|---|
| `api/` | PHP pur (REST, sans framework) + JWT | Serveur d'API, logique métier, audit, notifications, push FCM |
| `desktop/` | Electron + React + TypeScript + Zustand + Tailwind | Application poste fixe (Windows/macOS/Linux) |
| `android/` | Kotlin + Jetpack Compose + Hilt | Application mobile Android |
| `database/` | Migrations SQL ordonnées (001 → 064) | Schéma MySQL (registres, permissions, notifications, audit…) |

- Les clients communiquent avec l'API via des **jetons JWT** (jeton d'accès + jeton de rafraîchissement).
- Les permissions sont vérifiées **côté client** (`hasPermission(user, module, action)`) ; côté serveur, l'authentification est exigée sur les routes protégées.
- **Toute mutation** (création, modification, suppression) est tracée dans le **journal d'audit** et peut déclencher une **notification**.

---

## 3. Connexion et authentification

### 3.1 Écran de connexion — Desktop

Page en deux panneaux :

- **Panneau gauche (60 %)** : logos institutionnels (Police Nationale / CSP), titre « Bienvenue / OPUS », mention « Opérations Policières Unifiées et Structurées — la plateforme intégrée de gestion des opérations policières ».
- **Panneau droit (40 %)** : logo OPUS, message « Connectez-vous pour accéder au système », et un **sélecteur de mode d'authentification** à deux onglets :

| Onglet | Contenu |
|---|---|
| **Mot de passe** | Champs *Nom d'utilisateur* et *Mot de passe* (avec bouton d'affichage/masquage du mot de passe), bouton **« Se connecter »**, message d'erreur en ligne, lien « Mot de passe oublié ? » (sans action associée). |
| **Téléphone** | Panneau **QR** (`QrLoginPanel`) : l'application Desktop affiche un QR code que l'agent scanne depuis l'application Android pour valider la connexion. |

La fenêtre de connexion possède sa propre barre de titre (contrôles de fenêtre Windows/Linux ou « traffic lights » macOS).

### 3.2 Écran de connexion — Android

Écran « Splash » au démarrage (redirige vers la connexion ou directement vers l'accueil si une session existe), puis :

- Champs **Nom d'utilisateur** et **Mot de passe** (icônes, affichage/masquage, case à cocher de mémorisation),
- Bouton de connexion dégradé,
- Bouton **QR Code** permettant d'ouvrir le **scanner QR** (connexion d'un poste de travail par scan).

### 3.3 Connexion par QR code (flux croisé téléphone ↔ ordinateur)

Le système `qr_auth_requests` gère des **demandes d'authentification à usage unique, à durée de vie limitée**. Le QR code ne contient jamais d'identifiants : uniquement un code de demande aléatoire.

- **Flux 1 — Connexion du Desktop validée par le téléphone** : le poste Desktop affiche un QR code (mode « Téléphone » de l'écran de connexion). L'agent, **déjà connecté sur l'application Android**, scanne le code (entrée *« Connecter un ordinateur — Scanner un QR code »* du menu), approuve ou rejette la demande ; le Desktop récupère alors automatiquement ses jetons et ouvre la session.
- **Flux 2 — Appairage d'un téléphone depuis le Desktop** : un utilisateur connecté sur Desktop ouvre **« Connecter un téléphone »** (bas de la barre latérale) ; un QR code d'appairage est affiché et scanné par le téléphone.

Cycle de vie d'une demande : `pending` → `scanned` → `approved` / `rejected` / `cancelled` / `expired` → `consumed` (jetons récupérés une seule fois). Chaque demande enregistre le type d'appareil, son nom, l'IP et navigateur du demandeur, ainsi que l'utilisateur approbateur.

### 3.4 Mécanique de session (API)

- `POST /auth/login` : vérification du nom d'utilisateur et du mot de passe (bcrypt), contrôle du compte actif (`is_active`), émission d'un **jeton d'accès** et d'un **jeton de rafraîchissement** JWT, mise à jour de la dernière connexion, écriture d'un journal d'audit.
- `POST /auth/refresh` : renouvellement du couple de jetons à partir du jeton de rafraîchissement.
- `GET /auth/me` : profil de l'utilisateur connecté.
- `POST /auth/password` : changement de mot de passe (mot de passe actuel exigé).
- `POST /auth/verify` : **vérification d'identité sans ouverture de session** (nom d'utilisateur + mot de passe → identité uniquement). Utilisé par la **Passation** pour authentifier le chef de poste montant : son mot de passe est vérifié mais jamais stocké.

### 3.5 Redirection après connexion (Desktop)

Selon le rôle, l'utilisateur est dirigé vers :

| Rôle(s) | Destination |
|---|---|
| `SUPER_ADMIN`, `CHIEF`, `STATION_ADMIN` | `/dashboard` (tableau de bord général) |
| `HEAD_SG`, `OFFICER` | `/sg/dashboard` |
| `HEAD_SED`, `RECEPTION`, `CLERK` | `/sedentaire/dashboard` |
| `HEAD_PJ`, `INVESTIGATOR`, `CUSTODY` | `/pj/dashboard` |

*(Seul `SUPER_ADMIN` existe à l'initialisation ; tous les autres rôles sont créés par l'administrateur.)*

Sur Android, la connexion mène à l'écran principal (barre d'onglets inférieure + menu latéral).

---

## 4. Rôles et permissions

### 4.1 Modèle

- Table `roles` : `code` unique, nom d'affichage, description. **Seul le rôle `SUPER_ADMIN` est créé à l'installation** (avec l'utilisateur `admin`). Tous les autres rôles sont définis par l'administrateur depuis l'interface Desktop.
- Table `role_permissions` : pour chaque rôle, une permission par **module**, déclinée en **5 actions** :

| Action | Signification |
|---|---|
| `can_view` | Consulter (listes et détails) |
| `can_create` | Créer un enregistrement |
| `can_edit` | Modifier un enregistrement |
| `can_delete` | Supprimer un enregistrement |
| `can_export` | Exporter |

- Le rôle `SUPER_ADMIN` possède **tous les droits** sans condition. Pour les autres rôles, le menu, les boutons (Nouveau, Modifier, Supprimer, Exporter) et les actions spécifiques ne s'affichent que si la permission correspondante est accordée.

### 4.2 Modules de permission existants

| Code module | Libellé |
|---|---|
| `sedentaire_secretariat_correspondance` | Sédentaire > Secrétariat > Correspondance |
| `sedentaire_secretariat_declaration_perte` | Sédentaire > Secrétariat > Déclaration de perte |
| `sedentaire_secretariat_rapport` | Sédentaire > Secrétariat > Rapport |
| `sedentaire_secretariat_main_courante` | Sédentaire > Secrétariat > Main courante |
| `personnel` | Sédentaire > Secrétariat > Personnel |
| `sedentaire_poste_passation` | Sédentaire > Poste > Passation |
| `sedentaire_poste_armement` | Sédentaire > Poste > Armement |
| `sedentaire_poste_arme` | Sédentaire > Poste > Armes |
| `sedentaire_poste_materiels` | Sédentaire > Poste > Matériels |
| `sedentaire_poste_materiel_roulant` | Sédentaire > Poste > Matériel roulant |
| `sedentaire_poste_situation_gav` | Sédentaire > Poste > Situation GAV |
| `sedentaire_poste_main_courante` | Sédentaire > Poste > Main courante |
| `sedentaire_poste_renseignement` | Sédentaire > Poste > Renseignement |
| `sg_rassemblement_journalier` | Service Général > Rassemblement journalier |
| `sg_evenement_survenu` | Service Général > Évènements survenus |
| `sg_activite` | Service Général > Activité |
| `sg_dispositif_exceptionnel` | Service Général > Dispositif exceptionnel |
| `pj_plainte` | Police Judiciaire > Plainte reçue |
| `pj_enquete` | Police Judiciaire > Registre d'enquête |
| `pj_mandat` | Police Judiciaire > Mandat |
| `pj_convocation` | Police Judiciaire > Convocation |
| `pj_arrestation` | Police Judiciaire > Arrestation |
| `pj_gav` | Police Judiciaire > GAV |
| `pj_requisition` | Police Judiciaire > Réquisition |
| `pj_personne_recherchee` | Police Judiciaire > Personne recherchée |
| `pj_objets` | Police Judiciaire > Objets |
| `pj_perquisition` | Police Judiciaire > Perquisition |
| `pj_deferrement` | Police Judiciaire > Registre de déferrement |
| `pj_renseignement` | Police Judiciaire > Renseignement |
| `cartographie` | Cartographie |
| `users` | Utilisateurs |
| `roles` | Rôles |

Le **code secret** du personnel (PIN individuel, stocké en hash bcrypt, jamais renvoyé par l'API) est distinct du mot de passe applicatif : il sert exclusivement à **vérifier l'identité d'un agent lors de la perception d'une arme** (voir §10.2). Un agent sans compte utilisateur peut donc disposer d'un code secret.

---

## 5. Organisation fonctionnelle (divisions)

```
OPUS
├── Tableau de bord général (SUPER_ADMIN / commandement)
├── Division Sédentaire
│   ├── Dashboard Sédentaire
│   ├── Secrétariat
│   │   ├── Correspondance
│   │   ├── Gestion du personnel
│   │   ├── Déclaration de perte
│   │   ├── Rapport                  (écran « Bientôt disponible »)
│   │   └── Main courante
│   └── Poste
│       ├── Passation
│       ├── Armement
│       ├── Armes
│       ├── Matériels
│       ├── Matériel roulant
│       ├── Situation GAV            (écran « Bientôt disponible »)
│       ├── Main courante
│       └── Renseignement / Envoi de renseignement
│                                  (écran « Bientôt disponible »)
├── Division Service Général
│   ├── Rassemblement Journalier
│   ├── Évènements survenus (sur la voie publique)
│   ├── Activité (Patrouilles et interventions)
│   └── Dispositif exceptionnel
├── Division Police Judiciaire
│   ├── Dashboard PJ
│   ├── Plainte (Entrée + Sortie)
│   ├── Registre d'enquête           (écran « Bientôt disponible »)
│   ├── Mandat
│   ├── Convocation
│   ├── Arrestation
│   ├── GAV (Garde à vue)
│   ├── Réquisition
│   ├── Personne recherchée
│   ├── Objets (Saisi / Trouvé)
│   ├── Perquisition
│   ├── Registre de déferrement      (écran « Bientôt disponible »)
│   └── Renseignement
└── Modules globaux
    ├── Cartographie (Desktop)
    ├── Utilisateurs (Desktop)
    ├── Rôles (Desktop)
    └── Journal d'audit (Desktop, SUPER_ADMIN)
```

---

## 6. Navigation principale

### 6.1 Desktop — barre latérale

Barre latérale rétractable affichant le logo OPUS, organisée en :

- **Section « Navigation »** : entrées filtrées selon les permissions de l'utilisateur, avec sous-menus dépliants animés (chevrons) et mise en surbrillance de la page active. L'arborescence suit exactement la structure du §5. Si l'utilisateur n'a accès qu'à une seule sous-section, celle-ci est promue au premier niveau.
- **Section « Système »** : **Notifications** (avec pastille de compteur de non lues, actualisée toutes les 30 s), **Profil**, **Paramètres**.
- **Pied de barre** : carte utilisateur (initiales, nom, rôle), bouton **« Connecter un téléphone »** (QR d'appairage), bouton **« Déconnexion »** (avec boîte de confirmation).

Éléments d'interface complémentaires :

- **Palette de commandes** (`Ctrl/Cmd + K`) : recherche et exécution de commandes — aller au Dashboard / Notes / Cartographie / Paramètres, changer de thème, basculer la barre latérale ; navigation clavier (flèches, Entrée, Échap).
- **Barre de titre personnalisée** (contrôles de fenêtre) et **barre d'état** en bas de fenêtre.
- **Page « Bientôt disponible »** (*Coming Soon*) pour toute entrée de menu encore non implémentée (Rapport. Situation GAV, Envoi de renseignement, Registre d'enquête, Registre de déferrement).

### 6.2 Android — onglets inférieurs + menu latéral

- **Barre d'onglets inférieure** (4 entrées) :
  1. **Dashboard** — accueil ;
  2. **Notifications** — liste + pastille de non lues ;
  3. **Personnels** — annuaire du personnel (consultation) ;
  4. **Profil** — compte et préférences.
- **Menu latéral (tiroir)** : en-tête utilisateur (photo, nom), sections **Principal** (Dashboard), **Sédentaire** (Dashboard Sédentaire, Secrétariat, Poste), **Division Service Général**, **Division Police Judiciaire** (Dashboard PJ + modules), **Modules globaux** (Cartographie, Utilisateurs, Rôles — écrans d'attente sur Android), **Signature** (Tablette de signature), **Photo** (Capture photo), **Connexion** (Connecter un ordinateur — scanner un QR code). Les entrées sont filtrées par permissions, comme sur Desktop.

Android utilise **les mêmes fonctionnalités métier** que Desktop (mêmes listes, formulaires, détails) avec une présentation adaptée au mobile (cartes, bouton d'action flottant, onglets).

---

## 7. Écrans transverses

### 7.1 Tableau de bord général (Desktop `/dashboard`, Android *Dashboard*)

- **En-tête institutionnel** : logo Police Nationale, message « Bienvenue, Prénom Nom », rôle et grade, logo CSP.
- **4 cartes de statistiques** : *Personnel actif*, *Divisions* (3 — Sédentaire, SG, PJ), *Utilisateurs*, *Activité*.
- **Accès rapide** (rôles de commandement : `SUPER_ADMIN`, `CHIEF`, `STATION_ADMIN`) : raccourcis vers la Division Sédentaire, la Division Service Général, la Division PJ et le Personnel.
- **Carte « Informations du compte »** : nom d'utilisateur, IM, grade, affectation, rôle système, dernière connexion.

### 7.2 Dashboard Sédentaire (`/sedentaire/dashboard`, identique sur Android)

- **6 statistiques** : Correspondances, Personnel actif, Déclarations de perte, Passations, Matériels, Matériel roulant.
- **Actions rapides contextuelles** (selon permissions) : Enregistrer un courrier, Déclaration de perte, Nouvelle passation, Nouveau personnel, Affecter du matériel, Perception véhicule.

### 7.3 Dashboard PJ (`/pj/dashboard`, identique sur Android)

- **12 statistiques** : Plaintes reçues, Registre d'enquête, Mandats, Convocations, Arrestations, Gardes à vue, Réquisitions, Personnes recherchées, Objets, Perquisitions, Registre de déferrement, Renseignements.
- **Actions rapides** (selon permissions) : Nouvelle plainte, Créer un mandat, Nouvelle convocation, Nouvelle arrestation, Enregistrer une GAV, Nouvelle réquisition, Personne recherchée, Objet saisi, Nouvelle perquisition, Nouveau renseignement, etc.

### 7.4 Division Service Général

La division SG ne dispose pas de tableau de bord dédié : son entrée mène directement au **Rassemblement Journalier**.

### 7.5 Patterns communs des écrans métier

Tous les modules CRUD partagent la même ergonomie (référence : Correspondance) :

- **Écran liste** : tableau paginé avec **recherche textuelle**, **filtres** (statut, type, période selon le module), bouton **« Nouveau/Nouvelle… »** (si `can_create`), menu contextuel par ligne : *Voir*, *Modifier* (si `can_edit`), *Supprimer* (si `can_delete`, avec confirmation).
- **Écran formulaire** : création et édition sur la même page (`…/new` et `…/:id/edit`), sections libellées, validation des champs obligatoires, boutons **Enregistrer** / **Annuler**.
- **Écran détail** : fiche complète en lecture, actions *Modifier*, *Supprimer*, retour à la liste.
- **Pièces jointes** : sur la plupart des modules (correspondance, déclaration de perte, main courante, toute la PJ, passation, armement, matériel roulant, évènements, activités) — ajout d'un fichier avec un **titre**, téléchargement, suppression, **visionneuse d'images** intégrée.

---

## 8. Gestion du personnel

Module `personnel` (Sédentaire > Secrétariat). Écran organisé en **3 onglets** : **Liste** (Desktop) / **Personnel**, **Mouvements** / **Mouvement**, **Comportement**.

### 8.1 Fiche personnel

Champs : **IM** (Indice Matricule, unique), **Grade**, **Nom**, **Prénoms**, **Affectation** (Sédentaire / Police Judiciaire / Service Général), **Téléphone**, **Adresse**, **Photo** (avec miniature), **Signature** (image / SVG), **Code secret** (PIN bcrypt — utilisé pour l'armement, jamais affiché). Pièces jointes possibles sur la fiche.

### 8.2 Onglet Mouvements

Suivi administratif des agents : **type de mouvement** (Congé, Permission, Mission, Mutation, Promotion, Suspension, Retraite, Démission, Détachement, Repos, Repos médical, Absent non motivé), **date de départ**, **nombre de jours**, **date de retour**, indicateur **Retour (Oui/Non)**. Pièces jointes par mouvement. Identité de l'agent (IM, grade, service, nom, prénoms) figée sur chaque mouvement.

### 8.3 Onglet Comportement

Notation Positive/Négative avec **date**, **motif**, **décision**. Workflow de validation :

- Un comportement saisi par un agent non administrateur démarre **« En attente »** (`pending`) et déclenche une notification aux administrateurs ;
- Un administrateur peut **Confirmer** (`confirmed`) ou **Rejeter** (`rejected`, avec motif de rejet) ;
- Un comportement saisi par un administrateur est confirmé automatiquement.

### 8.4 Android

- Onglet **Personnels** (barre inférieure) : annuaire consultable (fiche détaillée en lecture).
- Entrée **Gestion du personnel** (menu Sédentaire > Secrétariat) : mêmes 3 onglets avec bouton d'action flottant d'ajout, suppression avec confirmation, confirmation/rejet des comportements.

---

## 9. Division Sédentaire — Secrétariat

### 9.1 Correspondance

Registre du courrier **Entrant** et **Sortant**, chacun avec sa **propre numérotation** (unicité du numéro d'ordre par sens).

| Champ | Détail |
|---|---|
| Date / Heure d'enregistrement | obligatoires |
| Sens | `Entrant` ou `Sortant` |
| Référence (numéro d'ordre) | unique par sens |
| Émetteur / Destinataire | selon le sens |
| Objet | obligatoire |
| Statut | `Enregistré` → `En traitement` → `Traité` → `Archivé` |
| Pièces jointes | fichiers avec titre |

Liste filtrable par sens/statut, recherche, création/modification/suppression selon permissions. Les créations et mises à jour notifient les utilisateurs habilités.

### 9.2 Déclaration de perte

Registre des déclarations de perte avec **numéro d'attestation unique** délivré au déclarant.

Champs : date et heure de déclaration, **identité du déclarant**, **nature de l'objet**, **description de l'objet**, **date présumée de perte**, **lieu présumé de perte**, **n° d'attestation**, **nom de l'agent** ayant reçu la déclaration. Pièces jointes possibles.

### 9.3 Main courante (Secrétariat)

Voir §10.6 — même registre, contexte `Secretariat` (permission `sedentaire_secretariat_main_courante`).

### 9.4 Rapport

Entrée de menu présente, écran **« Bientôt disponible »** (fonctionnalité non implémentée à ce jour).

---

## 10. Division Sédentaire — Poste

### 10.1 Passation

Enregistre la **passation de service** entre le **chef de poste descendant** (utilisateur qui clôt) et le **chef de poste montant** (utilisateur entrant).

- Date et heure de passation ;
- **Le chef montant s'authentifie dans le formulaire** : son nom d'utilisateur et son mot de passe sont vérifiés via `POST /auth/verify` — le mot de passe n'est jamais conservé, seule son identité (grade + nom) est figée sur l'enregistrement ;
- Champs libres : **Instructions de l'autorité**, **Incidents survenus** durant le service.

Les identités des deux chefs sont **figées (snapshot)** au moment de la passation pour garantir l'exactitude historique. La passation est enregistrée par le chef descendant.

### 10.2 Armement (perception / réintégration d'une arme)

Cycle de vie en deux temps :

**Perception** (remise de l'arme) :
- Date et heure de perception ;
- **Agent preneur** : sélection dans le personnel (identité IM + grade + nom figée) ;
- **Vérification d'identité** : l'agent saisit son **code secret** personnel (contrôle bcrypt — le résultat et l'horodatage sont stockés définitivement) ;
- **Signature de l'agent** capturée en SVG (récupérée de la fiche personnel ou dessinée — éventuellement via la **tablette de signature**, voir §17) ;
- Arme (type + matricule, liée au catalogue des armes), **munitions remises**, **secteur / mission**, **état de l'arme à la perception** ;
- Sur Android : **coordonnées GPS** capturées à la perception (obligatoire côté mobile, absent sur Desktop).

**Réintégration** (retour de l'arme) — transition **irréversible** via une action dédiée :
- Date et heure de réintégration, état de retour, **munitions consommées** ;
- Les munitions consommées **décrémentent automatiquement le stock** de l'arme concernée ;
- Sur Android : coordonnées GPS du lieu de retour.

Tant que la réintégration n'est pas faite, l'arme apparaît **« En cours de perception »** ; ensuite **« Réintégrée »**. Les colonnes de vérification/signature/réintégration ne sont modifiables par aucune mise à jour ordinaire.

### 10.3 Armes (catalogue)

- **Types d'arme** : nom unique (ex. Pistolet PA 9 mm, Fusil AK-47), description optionnelle.
- **Armes** : matricule unique, type, **stock de munitions courant** (mis à jour automatiquement lors des réintégrations ou par un enregistrement de consommation dédié). Une arme référencée par des perceptions ou des consommations **ne peut pas être supprimée**.

### 10.4 Matériels

Gestion des **affectations de matériel** aux agents, en deux temps (perception / réintégration) :

- Catalogue des **types de matériel** (Radio, Bâton, Gilet… — nom unique) ;
- **Affectation** : agent (identité figée), date/heure de perception, statut `Assigné` / `Réintégré`, observations ;
- Une affectation peut contenir **plusieurs lignes de matériel**, chacune avec son **état à l'emport** et son **état à la réintégration**.

### 10.5 Matériel roulant

**Perception / réintégration de véhicules** (types **VHL** ou **Moto**) :

- Date/heure de perception, **n° d'immatriculation**, **description du véhicule** ;
- **Agent conducteur** et **chef de bord** (identités figées) ;
- **Kilométrage** et **niveau de carburant** au départ, puis au retour ;
- **Observations techniques** et **défaillances** (champs distincts, renseignés au retour) ;
- Statut `En service` / `Réintégré` ; la réintégration est une transition irréversible, les données de retour ne sont jamais écrasées.

### 10.6 Main courante (Poste et Secrétariat)

Registre des évènements du service, partagé entre les deux contextes (colonne `origine` : `Secretariat` ou `Poste`) avec une permission propre à chaque contexte.

Champs : **date (période)**, **heure précise**, **catégorie**, **description des faits**. Les catégories forment un **catalogue dynamique** géré par l'utilisateur via une boîte de dialogue du formulaire (valeurs initiales : *Entrée/Sortie de tiers*, *Incident au poste*, *Renseignement reçu*). Pièces jointes possibles.

### 10.7 Situation GAV / Envoi de renseignement

Entrées de menu présentes, écrans **« Bientôt disponible »**.

---

## 11. Division Service Général

### 11.1 Rassemblement Journalier

Prise d'armes quotidienne, composée d'un enregistrement principal et de trois listes intégrées :

- **En-tête** : date, heure, **brigade de service**, **officier de permanence**, **inspecteur de permanence**, **chef de poste**, **instructions de l'autorité** ;
- **Situation de prise d'arme** : effectif théorique, présents, absents, motif d'absence ;
- **Répartition des secteurs — Diurne** et **Nocturne** : pour chaque ligne : secteur, effectif engagé, chef d'élément (avec contact), contrôle (avec contact), matériels et armements, missions.

### 11.2 Évènements survenus (sur la voie publique)

Registre des faits constatés : **date**, **heure**, **type d'évènement** (catalogue dynamique géré comme celui de la main courante ; valeurs initiales : *Infraction*, *Incident*, *Accident*, *Autre*), **lieu exact**, **auteur(s) présumé(s)**, **victime(s)**, **témoin(s)**, **mesures prises**. Localisation et pièces jointes possibles.

### 11.3 Activité (Patrouilles et interventions)

Enregistrement par activité :

- Date et heure ;
- **Patrouilles** : 6 combinaisons sélectionnables — *Diurne* / *Nocturne* × *Motorisée* / *Pédestre* / *Portée* — chacune avec son **itinéraire** ;
- **Opération ciblée** (ex. contrôle CIN, contrôle débit de boissons) ;
- **Faits constatés**, **compte-rendu temps réel à la hiérarchie**, **conduite à tenir** (instructions de l'autorité) ;
- **Nature de l'intervention**, **suites données** (ex. conduite au poste, interpellation, RAS) ;
- Sur Android : **position GPS** capturée à l'enregistrement et visualisable sur la carte.

### 11.4 Dispositif exceptionnel

Opérations de sécurité exceptionnelles (visites VIP, manifestations, urgences) :

- **Nature de l'évènement**, **période** (date de début / date de fin — sélecteur de plage de dates) ;
- **Effectif engagé** (liste par secteur) : secteur, chef d'élément avec contact, contrôle avec contact, matériels et armements, missions.

---

## 12. Division Police Judiciaire

Tous les registres PJ supportent les **pièces jointes** génériques. Les numéros officiels sont **auto-générés côté serveur** par famille (séquences dédiées) et restent uniques ; certains sont modifiables par l'utilisateur.

### 12.1 Plainte — Entrée et Sortie

**Plainte Entrée** — trois types partageant le même registre :

| Type | Particularités |
|---|---|
| `ST_PARQUET` | n° ST, partie civile + adresse |
| `PLAINTE_DIRECTE` | partie civile + adresse (pas de n° ST) |
| `RAPPORT_POLICE` | sans partie civile ni n° ST |

Champs : type, date, **n° de dossier auto-généré** (format `N°…/TRIMO/{ST|PD|RP}/{AA}`), n° ST, **OPJ** et **Enquêteur** (sélection dans le personnel), **partie civile** et son adresse, **mise en cause**, **infraction**, **préjudice**, **lieu** et **heure de l'infraction**, observation. L'écran liste combine Entrée et Sortie (onglets).

**Plainte Sortie** — toujours rattachée à **une** plainte Entrée :
- Nature : `DAT` (date de déferrement facultative) ou `DEFERREMENT` (date obligatoire) ;
- **N° auto-généré** au format officiel `N°…/MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/{AA}` ;
- N° TTR, nom du Substitut, date de déferrement, observation.

### 12.2 Convocation

Types `ST_PARQUET` / `PLAINTE_DIRECTE` (mêmes champs ; seul le format du numéro généré diffère : `…/ST|PD/MC/COV/{AA}`). Champs : type, date, **personne convoquée**, adresse, infraction, **personne ayant accusé réception**, n° du dossier rattaché, observation.

### 12.3 Garde à vue (GAV)

Cycle complet : **nom, prénoms, date de naissance, adresse** de la personne gardée à vue ; **enquêteur de permanence**, **OPJ ayant décidé la GAV**, **motif** ; **état de santé**, **droits notifiés**, **personne à contacter** ; dates-heures de **début**, **fin** et **prolongation**.

### 12.4 Réquisition

Types : `TPH`, `Médecin légiste`, `CIM`, `Autre`. Champs : date, **n° auto-généré (REQ)**, n° TTR, nom du Substitut, **affaire concernée**, n° de dossier, OPJ en charge.

### 12.5 Personne recherchée

Fiche minimale : **nom**, **dernière adresse connue**, **motif de la recherche** — enrichie d'un **album photo dédié** (chaque photo possède une légende, une provenance *Caméra* ou *Galerie*, des dimensions, un ordre d'affichage).

### 12.6 Objets (Saisi / Trouvé)

Interface à **deux onglets** :
- **Objet saisi** : n° du dossier concerné, **motif de la saisie**, **type d'objet** (liste prédéfinie), **propriétaire** ;
- **Objet trouvé** : **affaire concernée**, **motif de découverte** (`Réquisition`, `Sur personne`, `Perquisition`), indicateur de **restitution**.

### 12.7 Perquisition

**N° auto-généré (PEQ)** modifiable mais unique, n° TTR, nom du Substitut, **affaire**, **motif**.

### 12.8 Renseignement (PJ)

Note de renseignement judiciaire : **nature de l'infraction**, **date et lieu des faits**, **circonstances**, **préjudices causés**.

### 12.9 Mandat

**N° auto-généré (MAN)** modifiable mais unique ; **objet du mandat** : *Amener*, *Comparution*, *Arrêt*, *Dépôt* ; autorité ayant délivré le mandat ; **personne concernée** (nom, prénom, date et lieu de naissance) ; motif ; qualification de l'infraction ; **OPJ chargé de l'exécution** ; **date, heure et lieu d'exécution** ; observations.

### 12.10 Arrestation

**N° auto-généré (ARR)** modifiable mais unique ; **date et heure** ; **personne arrêtée** ; lieu ; motif ; **policiers ayant procédé à l'arrestation** (un par ligne) ; n° du dossier rattaché ; observations.

### 12.11 Registre d'enquête / Registre de déferrement

Entrées de menu et cartes du dashboard présentes, écrans **« Bientôt disponible »** (non implémentés).

---

## 13. Cartographie

- **Desktop** (`/cartographie`, permission `cartographie`) : carte interactive **Mapbox GL** avec :
  - **Recherche d'adresses** (service OpenStreetMap / Nominatim, en français) ;
  - **Couches** : Plan / Satellite ;
  - **Points d'intérêt** : ajout de marqueurs, import de POI ;
  - **Itinéraires** : dessin d'itinéraire, géolocalisation ;
  - **Données GIS** : export / import.
- **Android** : pas de module cartographique complet (écran d'attente) ; l'application dispose d'un **visualiseur de position GPS** (carte **OSMDroid**) ouvert depuis les fiches disposant de coordonnées (ex. Activité, Armement), affichant le point et ses coordonnées.

---

## 14. Administration (Utilisateurs, Rôles, Journal d'audit)

Disponible sur **Desktop** ; sur Android, *Utilisateurs*, *Rôles* et *Cartographie* affichent des écrans d'attente.

### 14.1 Utilisateurs (`/users`, permission `users`)

Liste des comptes (nom d'utilisateur, agent rattaché, rôle, statut actif, dernière connexion) avec création et édition : choix de la **personne** (personnel), nom d'utilisateur, mot de passe, **rôle**, activation/désactivation. Le **compte administrateur initial est protégé contre la suppression**.

### 14.2 Rôles (`/roles`, permission `roles`)

Création/édition d'un rôle : **code**, **nom**, **description**, et une **matrice de permissions** (modules × 5 actions : Voir / Créer / Modifier / Supprimer / Exporter, avec boutons « tout sélectionner » par ligne).

### 14.3 Journal d'audit (`/audit-logs`, SUPER_ADMIN uniquement)

Traçabilité exhaustive : **utilisateur**, **action** (connexion, déconnexion, création, modification, suppression…), **module**, **entité concernée**, description lisible, **valeurs avant/après** (JSON), **adresse IP** et **agent logiciel**. Consultation filtrable.

---

## 15. Notifications et alertes

### 15.1 Modèle

Chaque notification possède : titre, message, **type** (`info` / `success` / `warning` / `error`), **service d'origine** (`PJ`, `SG`, `Sedentaire`, `System`), un **destinataire** (ou diffusion aux administrateurs si aucun), un **lien** vers le contenu concerné, l'auteur, et un état **lu / non lu**.

Les contrôleurs émettent une notification à chaque **création ou modification** d'un module métier, ciblant les utilisateurs possédant la permission du module (l'auteur de l'action est exclu). L'envoi **push FCM** (Firebase Cloud Messaging) vers les appareils Android enregistrés (`device_tokens`) est isolé : **un échec push ne fait jamais échouer la requête métier**.

### 15.2 Écran Notifications

- **Desktop** (`/notifications`) : liste chronologique, filtres **Toutes / PJ / Service Général / Sédentaire** avec compteurs de non lues, bouton **« Tout marquer comme lu »**, marquage individuel et suppression ; un clic sur une notification ouvre le contenu lié. Pastille de compteur dans la barre latérale (actualisée toutes les 30 s).
- **Android** : onglet *Notifications* de la barre inférieure avec pastille, synchronisée avec le serveur à chaque retour au premier plan ; le **toucher d'une notification push** ouvre directement l'onglet Notifications ou le **contenu lié** (lien profond — ex. un comportement à confirmer ouvre l'onglet concerné de la Gestion du personnel).

### 15.3 Alertes applicatives

Les deux clients affichent des **messages toast** (succès / erreur) pour les actions courantes : connexion réussie, enregistrement, mise à jour de la photo, changement de mot de passe, etc. Les suppressions passent par des **boîtes de dialogue de confirmation**.

---

## 16. Profil et paramètres

### 16.1 Profil

**Desktop** (`/profile`) :
- **Photo de profil** : import de fichier (recadrée au carré + miniature automatique), **prise de photo via un téléphone appairé** (§17), suppression (avec confirmation), visualisation plein écran ;
- **Informations personnelles** : nom, prénoms, IM, téléphone, adresse ;
- **Informations professionnelles** : grade, affectation, rôle ;
- **Changement de mot de passe** : mot de passe actuel + nouveau (≥ 6 caractères) + confirmation ;
- Bouton **« Modifier mes informations »** → formulaire de la fiche personnel.

**Android** (onglet *Profil*) : sections **Compte** (*Gérer le profil*, *Mot de passe & Sécurité*, *Signature*) et **Préférences** (*Thème*, *Couleur du thème*).

### 16.2 Paramètres — Desktop

Page à sections latérales :

| Section | Contenu |
|---|---|
| **Apparence** | 8 thèmes intégrés : *Dark, Light, Clean Light, Warm Light, High Contrast, Ondark, Matrix, Monokai* |
| **Éditeur** | Taille de police |
| **Raccourcis clavier** | Liste des raccourcis (palette `Ctrl+K`, bascule sidebar, thème…) |
| **Créateur de thème** | Édition couleur par couleur (sélecteur HSL + saisie hex/RGB/HSL), aperçu en direct, enregistrement de thèmes personnalisés, **duplication, suppression, export / import JSON** |
| **Notifications / Stockage / Compte / Confidentialité & Sécurité** | Sections de réglages correspondantes |

### 16.3 Paramètres — Android

Choix du **mode de thème** (*Système*, *Clair*, *Sombre*) et de la **couleur du thème** (palette de couleurs d'accent), également accessibles depuis le Profil.

---

## 17. Fonctions spéciales téléphone ↔ ordinateur

Le téléphone sert de **périphérique compagnon** du poste fixe :

| Fonction | Sens | Description |
|---|---|---|
| **Connexion par QR code** | Téléphone → Desktop | Le Desktop affiche un QR code ; le téléphone (connecté) le scanne et approuve la connexion |
| **Connecter un téléphone** | Desktop → Téléphone | QR code d'appairage généré depuis la barre latérale Desktop |
| **Tablette de signature** | Téléphone → Desktop | Le téléphone sert de surface de signature tactile ; la signature (SVG) est transmise au poste (utilisée notamment pour l'Armement et la fiche personnel) |
| **Capture photo** | Téléphone → Desktop | Le téléphone sert d'appareil photo ; la photo est envoyée au poste (photo de profil, etc.) |

Ces canaux passent par des demandes d'appairage/capture adossées au mécanisme QR (codes à usage unique, expiration, approbation explicite).

---

## 18. Différences d'interface Android / Desktop

| Aspect | Desktop | Android |
|---|---|---|
| Conteneur | Fenêtre Electron (barre de titre, barre d'état) | Plein écran Material 3 |
| Navigation | Barre latérale + palette de commandes (`Ctrl+K`) | 4 onglets inférieurs + tiroir latéral |
| Connexion | Mot de passe **ou** QR affiché puis scanné par le téléphone | Mot de passe ; scanner QR pour connecter un poste |
| Dashboards | Général, Sédentaire, PJ | Mêmes dashboards adaptés mobile (grilles de cartes 2 colonnes) |
| Formulaires | Pages avec sections, sélecteurs de date | Formulaires Compose, bouton d'action flottant |
| GPS | Non capturé (pas de matériel GPS) | **Coordonnées GPS sur Armement (perception + réintégration) et Activité**, visualisables sur carte OSMDroid |
| Cartographie | Module complet (Mapbox, couches, POI, itinéraires, GIS) | Écran d'attente ; visualisation ponctuelle d'une position |
| Utilisateurs / Rôles / Audit | Interfaces complètes | Écrans d'attente |
| Paramètres | Page complète (8 thèmes + créateur de thèmes) | Thème Système/Clair/Sombre + couleur d'accent |
| Notifications push | — | **FCM** : notifications système, pastille, liens profonds |
| Signature / photo | Réception via le téléphone appairé | Saisie tactile native |
| Module « Notes » | Éditeur de notes (module hérité, conservé) | Absent |

Sur le fond, **les deux plateformes partagent strictement les mêmes données, règles de gestion et permissions** via l'API commune.

---

## 19. Workflows utilisateurs importants

1. **Connexion classique** : Login → (mot de passe) → redirection selon le rôle → tableau de bord de la division → module.
2. **Connexion QR** : Login Desktop (onglet *Téléphone*) → QR affiché → scan depuis Android (*Connecter un ordinateur*) → approbation → session Desktop ouverte.
3. **Passation de service** : le chef descendant ouvre *Passation → Nouvelle* → le chef montant saisit ses identifiants (vérifiés, non conservés) → saisie des instructions et incidents → enregistrement (identités figées) → notification.
4. **Perception d'arme** : *Armement → Nouvelle perception* → choix de l'agent → **code secret** de l'agent (vérification) → **signature** (fiche ou tablette) → arme + munitions + mission → GPS (mobile) → enregistrement.
   **Réintégration** : fiche Armement → action *Réintégrer* → date/heure, état, munitions consommées → **stock de l'arme décrémenté** automatiquement → statut « Réintégré » (irréversible).
5. **Traitement d'une plainte** : *PJ → Plainte → Nouvelle (Entrée)* → n° de dossier généré → … enquête … → *Nouvelle Sortie* rattachée à l'Entrée (DAT ou Déferrement) → n° officiel généré.
6. **Validation d'un comportement** : saisie par un agent → notification aux administrateurs → *Gestion du personnel → onglet Comportement* → **Confirmer** ou **Rejeter** (avec motif).
7. **Rassemblement journalier** : *SG → Rassemblement → Nouveau* → en-tête (brigade, permanences) → situation de prise d'arme → lignes de répartition Diurne / Nocturne → enregistrement.
8. **Dispositif exceptionnel** : nature de l'évènement + plage de dates → lignes *Effectif engagé* par secteur → consultation / édition.
9. **Notification entrante** : push Android / pastille Desktop → ouverture de *Notifications* → clic → **lien profond** vers la fiche concernée → marquage lu (individuel ou global).
10. **Administration** : création d'un rôle (matrice de permissions) → création d'un utilisateur rattaché à un agent → l'utilisateur ne voit que les modules autorisés ; chaque action est visible dans le **Journal d'audit**.

---

## 20. Conceptions techniques transverses

- **Numérotations officielles** : séquences serveur dédiées par famille (plaintes ST/PD/RP, sorties, convocations, réquisitions, perquisitions, mandats, arrestations) — uniques, générées automatiquement, parfois modifiables.
- **Identités figées (snapshots)** : grade, nom, IM des agents (passation, armement, matériels, véhicules) sont copiés sur l'enregistrement pour préserver l'exactitude historique malgré les évolutions ultérieures du personnel.
- **Catalogues dynamiques** : types d'arme, types de matériel, catégories de main courante, types d'évènements — gérés par les utilisateurs ; les libellés sont stockés verbatim (le renommage ne casse pas l'historique).
- **Restrictions de suppression** : les entités référencées par l'historique (arme utilisée, dossier lié, compte admin initial) sont protégées.
- **Audit systématique** : chaque mutation écrit une entrée `audit_logs` (acteur, action, valeurs avant/après, IP).
- **Notifications non bloquantes** : l'échec d'envoi push FCM n'affecte jamais la requête métier.
- **Sécurité** : mots de passe et codes secrets en **bcrypt** ; sessions **JWT** (accès + rafraîchissement) ; QR d'authentification à usage unique et expirant, sans identifiants embarqués.

---

*Document généré à partir de l'analyse du code source OPUS (API PHP, application Desktop Electron/React, application Android Kotlin/Compose, schéma de base de données v064). Il décrit l'application telle qu'elle existe actuellement, sans fonctionnalités projetées.*
