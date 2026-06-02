# Guide utilisateur SteeveJobs

Ce guide explique comment **utiliser l'application au quotidien** : connexion, navigation, modules métier et bonnes pratiques. Aucune connaissance technique requise.

---

## Sommaire

1. [Première connexion](#1-première-connexion)
2. [Navigation](#2-navigation)
3. [App Center — modules métier](#3-app-center--modules-métier)
4. [Menu latéral](#4-menu-latéral)
5. [Visioconférence](#5-visioconférence)
6. [Support & tickets](#6-support--tickets)
7. [Rôles et permissions](#7-rôles-et-permissions)
8. [FAQ](#8-faq)

---

## 1. Première connexion

1. Lancez **SteeveJobs** (raccourci bureau ou `javafx:run` en dev).
2. Saisissez votre **adresse e-mail** et **mot de passe** fournis par l'administrateur.
3. Après connexion, vous arrivez sur l'**Accueil** avec les modules auxquels vous avez droit.

> Si c'est la **première installation** de l'entreprise, l'administrateur technique exécute une fois le `DatabaseSeeder` pour créer le compte admin initial (voir [Guide d'installation](SETUP.md)).

---

## 2. Navigation

L'interface comporte deux zones :

| Zone | Rôle |
|------|------|
| **Menu latéral (gauche)** | Fonctions transverses : accueil, planning, tickets, documents perso, visio, paramètres |
| **App Center (accueil)** | Tuiles colorées : modules métier selon vos permissions |

Seules les tuiles et entrées de menu **autorisées pour votre rôle** sont visibles.

---

## 3. App Center — modules métier

### Gestion des permissions *(administrateurs)*

- Consultez et modifiez les **rôles** et leurs droits par module.
- Les permissions sont liées aux tuiles de l'App Center (`APP_*_VIEW`).

### Gestion des utilisateurs *(administrateurs)*

- Créer, modifier ou désactiver des **comptes employés**.
- Associer un rôle et un poste à chaque utilisateur.

### Gestion des stocks

- Consulter le **catalogue produits** (référence, stock, seuil d'alerte).
- Mettre à jour les quantités ; les produits en vrac gèrent le poids.

### Gestion commerciale

- Créer des **devis**, **bons de commande** et **factures**.
- Composer les lignes (produits, quantités, remises).
- Générer le **PDF** et l'envoyer par e-mail si le serveur SMTP est configuré.

### Gestion clients

- Annuaire **tiers** : clients et fournisseurs.
- SIRET, coordonnées, contacts — base pour les documents commerciaux.

### Ressources humaines — Fiches de paie

- Consultation et gestion des **fiches de paie** par période.
- Réservé aux profils RH autorisés.

### Calendrier RH

- Vue **planning équipe** pour les responsables RH.
- **Demandes de congé** : l'employé dépose une demande depuis son calendrier ; le RH valide ou refuse depuis cette vue (jauge de congés restants).

### Support

- Liste des **tickets** de l'équipe support / services.
- Filtre par catégorie selon configuration.

---

## 4. Menu latéral

### Accueil

Tableau de bord : raccourci vers les modules autorisés.

### Planning

Calendrier **personnel** : visualisez vos créneaux et déposez une **demande de congé** (dates, motif). Le statut apparaît après validation RH.

### Tickets

- Créer un ticket (objet, description, priorité).
- Suivre l'état : ouvert, en cours, résolu.
- **Fil de messages** en temps réel (WebSocket) avec l'équipe support.

### Mes documents

Documents commerciaux **dont vous êtes l'auteur** ou qui vous sont assignés — consultation sans passer par le module admin commercial.

### Visioconférence

Voir [section dédiée](#5-visioconférence).

### Paramètres

Préférences du compte connecté (profil, options d'affichage selon version).

---

## 5. Visioconférence

### Rejoindre un salon

1. Ouvrez **Visioconférence** dans le menu latéral.
2. **Salon rapide** : saisissez un identifiant sans espace ni accent (ex. `Salle_RH`) puis **Rejoindre**.
3. **Salon planifié** : sélectionnez une ligne dans le tableau et cliquez **Rejoindre la sélection**.

L'application ouvre votre **navigateur** sur la page visio (LiveKit) avec caméra et micro.

### Planifier une réunion

1. Renseignez le **nom du salon** (caractères autorisés : `a-z`, `A-Z`, `0-9`, `_`, `-`).
2. Choisissez **date et heure**.
3. Ajoutez des **invités** via la recherche d'employés.
4. Validez — la réunion apparaît dans la liste ; seuls le créateur et les invités peuvent rejoindre.

### Pendant l'appel (navigateur)

- Contrôles **micro**, **caméra**, **partage d'écran**.
- **Chat** latéral : sur grand écran, les participants restent visibles à gauche ; sur mobile, le chat s'ouvre en overlay.
- **Quitter** ferme l'appel ; un salon **instantané** est supprimé de la base à la fermeture (réutilisable sous le même nom).

### Supprimer un salon *(créateur)*

Icône corbeille sur une ligne du tableau — réservée au **créateur** du salon.

---

## 6. Support & tickets

| Action | Comment |
|--------|---------|
| Créer un ticket | Menu **Tickets** → Nouveau → décrire le problème |
| Répondre | Ouvrir le ticket → zone de message → envoyer |
| Notification | Une pastille ou mise à jour apparaît si le WebSocket est connecté |
| Clôturer | Réservé au support / rôle adapté selon configuration |

Conseil : soyez précis dans le titre ; joignez le module concerné (stock, paie, visio…).

---

## 7. Rôles et permissions

| Profil type | Accès typique |
|-------------|---------------|
| **Employé** | Planning, ses documents, tickets, visio |
| **Commercial** | + Gestion commerciale, clients, stocks (lecture/écriture selon rôle) |
| **RH** | + Fiches de paie, calendrier RH, validation congés |
| **Support** | + Module tickets étendu |
| **Administrateur** | + Utilisateurs, permissions, tous modules |

Si une tuile ou un menu manque, contactez un administrateur pour ajuster votre **rôle** dans *Gestion permissions*.

---

## 8. FAQ

**Je n'arrive pas à me connecter.**  
Vérifiez e-mail / mot de passe. Si le compte est neuf, l'admin doit l'avoir activé.

**La visio ne s'ouvre pas.**  
Vérifiez que le nom de salon est valide (pas d'espaces). Autorisez caméra/micro dans le navigateur.

**Je ne reçois pas les notifications de tickets.**  
L'application doit rester ouverte ; le serveur de notifications (WebSocket) doit être accessible (configuré par l'admin).

**Où sont stockés les PDF ?**  
Sur le serveur de fichiers de l'entreprise (WebDAV), pas sur votre poste — accès via l'application.

**Puis-je utiliser SteeveJobs sans Internet ?**  
Partiellement : modules locaux (BDD) oui ; visio, e-mails et notifications nécessitent l'infrastructure réseau.

---

[← Index documentation](README.md) · [Installation (technique) →](SETUP.md)
