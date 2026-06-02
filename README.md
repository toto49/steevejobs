<div align="center">

# SteeveJobs

**Suite de gestion PME — RH, commerce, support & visioconférence**

Application de bureau **JavaFX** pour le quotidien des équipes : plannings, documents commerciaux, tickets, paie et visio.

<br>

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-4780bc?style=flat-square&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8+-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![CI](https://img.shields.io/github/actions/workflow/status/toto49/steevejobs/build.yml?style=flat-square&logo=githubactions&logoColor=white&label=CI)](https://github.com/toto49/steevejobs/actions)

<br>

[**Guide utilisateur**](docs/GUIDE_UTILISATEUR.md) · [**Installation**](docs/SETUP.md) · [**Architecture**](docs/ARCHITECTURE.md) · [**Contribuer**](CONTRIBUTING.md)

</div>

---

## Sommaire

- [À propos](#à-propos)
- [Fonctionnalités](#fonctionnalités)
- [Démarrage rapide](#démarrage-rapide)
- [Documentation](#documentation)
- [Stack technique](#stack-technique)
- [Structure du projet](#structure-du-projet)
- [Contribuer & licence](#contribuer--licence)

---

## À propos

SteeveJobs centralise les outils internes d'une PME dans une interface unique :

- **App Center** — tuiles modulaires selon les droits de chaque utilisateur
- **Menu latéral** — planning, tickets, documents, visio, paramètres
- **Temps réel** — WebSocket pour tickets et visioconférence
- **Stockage fichiers** — PDF et pièces jointes sur WebDAV (NAS)

L'architecture client suit le pattern **MVC** (modèle / DAO / service / contrôleur JavaFX).

---

## Fonctionnalités

<table>
<tr>
<td width="50%">

**Administration**
- Gestion utilisateurs & rôles
- Permissions par module
- Authentification sécurisée (jBCrypt)

**Commerce & stocks**
- Devis, BC, factures + PDF
- Catalogue produits & alertes stock
- Annuaire clients / fournisseurs

</td>
<td width="50%">

**Ressources humaines**
- Fiches de paie & heures
- Calendrier RH & plannings
- Demandes de congé (validation RH)

**Collaboration**
- Tickets support + messagerie live
- Visioconférence (LiveKit)
- Notifications WebSocket

</td>
</tr>
</table>

---

## Démarrage rapide

```bash
git clone https://github.com/toto49/steevejobs.git
cd steevejobs
cp .env.example .env          # adapter DB, SMTP, WS…
mysql -u root -p steevejobs < sql/steevejobs.sql
./mvnw clean install && ./mvnw javafx:run
```

| Étape | Détail |
|-------|--------|
| JDK | 25+ — [Liberica Full](https://bell-sw.com/pages/downloads/) recommandé |
| BDD | MySQL 8 — script `sql/steevejobs.sql` |
| Config | [`.env.example`](.env.example) → `.env` (ne pas committer) |
| Admin | Exécuter `DatabaseSeeder` une fois au premier lancement |

Guide complet : **[docs/SETUP.md](docs/SETUP.md)**

---

## Documentation

| Document | Pour qui ? |
|----------|------------|
| 📘 [**Guide utilisateur**](docs/GUIDE_UTILISATEUR.md) | Employés, RH, commerciaux — utilisation quotidienne |
| 🛠 [**Installation & config**](docs/SETUP.md) | Développeurs — BDD, `.env`, client JavaFX |
| 🐳 [**Déploiement Docker**](docs/DOCKER.md) | DevOps — Compose, LiveKit, branche [`websocket`](https://github.com/toto49/steevejobs/tree/websocket) |
| 🏗 [**Architecture production**](docs/ARCHITECTURE.md) | DevOps — NAS, reverse proxy, LiveKit, WebDAV |
| 🧪 [**Tests unitaires**](docs/TESTS.md) | Développeurs — JUnit 5, Mockito |
| 📚 [**Index docs**](docs/README.md) | Vue d'ensemble de la documentation |

---

## Stack technique

<p align="center">
<img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/JavaFX-25-4780bc?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX"/>
<img src="https://img.shields.io/badge/MySQL-8+-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Maven-mvnw-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
<img src="https://img.shields.io/badge/Docker-WS-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
<img src="https://img.shields.io/badge/LiveKit-Visio-FF6358?style=for-the-badge&logo=livekit&logoColor=white" alt="LiveKit"/>
<img src="https://img.shields.io/badge/JUnit_5-Tests-25A162?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit"/>
</p>

<details>
<summary><strong>Stack complète & infrastructure</strong></summary>

<br>

**Client :** HikariCP · AtlantaFX · ControlsFX · OpenPDF · Jakarta Mail · Java-WebSocket · Sardine (WebDAV) · Auth0 JWT · dotenv-java

**Production :** Synology NAS · Web Station (reverse proxy) · Let's Encrypt · MailPlus · WebDAV · branche Git `websocket`

→ Détails : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

</details>

---

## Structure du projet

```text
steevejobs/
├── src/main/java/com/eseo/steevejobs/
│   ├── model/       # Entités métier
│   ├── dao/         # Accès MySQL
│   ├── service/     # Logique métier
│   ├── controller/  # Vues JavaFX (FXML)
│   └── config/      # DatabaseSeeder, utilitaires
├── src/main/resources/   # FXML, CSS, images
├── src/test/java/        # Tests unitaires (JUnit + Mockito)
├── sql/                  # Schéma BDD
├── docs/                 # Documentation
├── .env.example          # Modèle de configuration
└── pom.xml
```

---

## Contribuer & licence

| Ressource | Lien |
|-----------|------|
| Contribuer | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Code de conduite | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) |
| Sécurité | [SECURITY.md](SECURITY.md) |
| Licence | [MIT](LICENSE) |

Les contributions sont les bienvenues — merci de lire le guide de contribution avant d'ouvrir une PR.

---

<div align="center">

**SteeveJobs** — Projet ESEO / gestion d'entreprise

</div>
