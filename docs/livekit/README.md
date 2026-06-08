# Front visio LiveKit

Page web statique ouverte par le client JavaFX lors d'une visioconférence (`URL_FRONT_VISIO`).

## Fichiers

| Fichier         | Rôle                                                           |
|-----------------|----------------------------------------------------------------|
| `index.html`    | Interface (caméra, micro, chat, partage d'écran)               |
| `js/config.js`  | URLs LiveKit et APIs serveur — **à adapter avant déploiement** |
| `js/app.js`     | Connexion LiveKit, gestion des participants                    |
| `css/style.css` | Styles                                                         |

## Déploiement

1. Copiez ce dossier sur votre serveur web (NAS Synology, reverse proxy).
2. Éditez `js/config.js` avec vos domaines (`votre-domaine.fr`).
3. Configurez le client JavaFX : `URL_FRONT_VISIO=https://visio.votre-domaine.fr/index.html`.
4. Alignez `JWT_SECRET` et les clés LiveKit entre client, serveur WebSocket et `livekit.yaml`.

Guides complets : [DOCKER.md](../DOCKER.md) · [ARCHITECTURE.md](../ARCHITECTURE.md)
