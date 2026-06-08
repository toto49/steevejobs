# Documentation SteeveJobs

Bienvenue dans la documentation du projet. Choisissez le guide adapté à votre profil :

| Document                                       | Public                      | Contenu                                                                                      |
|------------------------------------------------|-----------------------------|----------------------------------------------------------------------------------------------|
| [**Guide utilisateur**](GUIDE_UTILISATEUR.md)  | Employés, RH, admins métier | Connexion, navigation, modules, calendrier, visio, tickets                                   |
| [**Guide d'installation**](SETUP.md)           | Développeurs, DevOps        | Prérequis, BDD, `.env`, lancement client, jpackage                                           |
| [**Déploiement Docker**](DOCKER.md)            | DevOps                      | Compose, LiveKit, branche [`websocket`](https://github.com/toto49/steevejobs/tree/websocket) |
| [**Architecture production**](ARCHITECTURE.md) | DevOps                      | NAS Synology, reverse proxy, DNS, WebDAV                                                     |
| [**Front visio LiveKit**](livekit/README.md)   | DevOps                      | Page web statique hébergée sur `URL_FRONT_VISIO`                                             |
| [**Tests unitaires**](TESTS.md)                | Développeurs                | Stratégie JUnit / Mockito / H2, exécution des tests                                          |
| [**Transparence IA**](TRANSPARENCE_IA.md)      | Évaluation / équipe         | Usage de l'IA sur le projet (obligatoire)                                                    |

## Ressources complémentaires

- [README principal](../README.md) — présentation du dépôt
- [CONTRIBUTING.md](../CONTRIBUTING.md) — contribution au code
- [SECURITY.md](../SECURITY.md) — signalement de vulnérabilités
- [.env.example](../.env.example) — configuration client (sans secrets)
- [docs/docker/.env.example](docker/.env.example) — configuration serveur WebSocket / LiveKit

## Parcours recommandés

| Profil                        | Parcours                                                                                 |
|-------------------------------|------------------------------------------------------------------------------------------|
| **Employé / RH / commercial** | [Guide utilisateur](GUIDE_UTILISATEUR.md)                                                |
| **Développeur local**         | [Installation](SETUP.md) → [Tests](TESTS.md)                                             |
| **DevOps / production**       | [Architecture](ARCHITECTURE.md) → [Docker](DOCKER.md) → [Front visio](livekit/README.md) |
