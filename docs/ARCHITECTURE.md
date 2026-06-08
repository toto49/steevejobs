# Architecture production

Infrastructure hybride autour d'un **NAS Synology**, avec séparation des flux par **DNS** et **reverse proxy** (Web Station).

> Les exemples utilisent `votre-domaine.fr`. Ne publiez jamais vos vrais domaines ou IP dans un dépôt public.

---

## Vue d'ensemble

```text
                    ┌─────────────────────────────────────┐
                    │         Client JavaFX SteeveJobs     │
                    └───────────┬─────────────────────────┘
                                │ HTTPS / WSS / JDBC
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
   Reverse Proxy          Reverse Proxy           MySQL
   (Web Station)          (Web Station)          (NAS / Docker)
          │                     │
    livekit.*              notif.*  ──► Docker WS :8887
    visio.*                stockage.* ──► WebDAV
    mail.*                 (MailPlus SMTP)
```

---

## Stack infrastructure

[![Synology](https://img.shields.io/badge/Synology-NAS-0082C9?style=for-the-badge&logo=synology&logoColor=white)](https://www.synology.com/)
[![Docker](https://img.shields.io/badge/Docker-WebSocket-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Nginx](https://img.shields.io/badge/Reverse_Proxy-Web_Station-009639?style=for-the-badge&logo=nginx&logoColor=white)](https://nginx.org/)
[![Let's Encrypt](https://img.shields.io/badge/SSL-TLS-003A70?style=for-the-badge&logo=letsencrypt&logoColor=white)](https://letsencrypt.org/)

[![LiveKit](https://img.shields.io/badge/LiveKit-WebRTC-FF6358?style=for-the-badge&logo=livekit&logoColor=white)](https://livekit.io/)
[![WebSocket](https://img.shields.io/badge/WSS-8887-010101?style=for-the-badge&logo=socketdotio&logoColor=white)](https://developer.mozilla.org/fr/docs/Web/API/WebSockets_API)
[![MySQL](https://img.shields.io/badge/MySQL-8+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![WebDAV](https://img.shields.io/badge/WebDAV-GED-0078D4?style=for-the-badge&logo=icloud&logoColor=white)](https://datatracker.ietf.org/doc/html/rfc4918)

[![MailPlus](https://img.shields.io/badge/MailPlus-SMTP-006FCF?style=for-the-badge&logo=minutemailer&logoColor=white)](https://www.synology.com/fr-fr/dsm/feature/mailplus)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)

---

## Résolution DNS et routage

| Service          | Endpoint (exemple)                  | Destination                                                                 |
|------------------|-------------------------------------|-----------------------------------------------------------------------------|
| LiveKit / WebRTC | `https://livekit.votre-domaine.fr`  | Serveur visio                                                               |
| Temps réel       | `wss://notif.votre-domaine.fr`      | Docker WebSocket → MySQL                                                    |
| Front visio      | `https://visio.votre-domaine.fr`    | Page LiveKit (`URL_FRONT_VISIO`) — sources dans [`docs/livekit/`](livekit/) |
| Fichiers / GED   | `https://stockage.votre-domaine.fr` | Synology WebDAV                                                             |
| Mailing          | `mail.votre-domaine.fr`             | Synology MailPlus                                                           |

---

## Composants

### Reverse proxy et SSL

- Terminaison TLS centralisée (certificats Let's Encrypt ou DSM).
- Protocoles client : **HTTPS** et **WSS** uniquement.
- En-têtes `Upgrade` / `Connection` pour maintenir les WebSockets.

### WebDAV (GED)

Les PDF et pièces jointes ne sont **pas** stockés en BLOB MySQL :

- performances BDD préservées ;
- ACLs gérées côté NAS (Btrfs).

### MailPlus

Envoi transactionnel (factures, notifications) avec enregistrements DNS **SPF** et **DKIM** pour la délivrabilité.

### Serveur relais WebSocket + LiveKit (Docker)

- **Branche Git :** [`websocket`](https://github.com/toto49/steevejobs/tree/websocket)
- **Stack :** `compose.yaml` — services `ws-server` (port **8887**) + `livekit-server` (WebRTC, mode host)
- **Documentation :** [DOCKER.md](DOCKER.md) · fichiers dans [`docs/docker/`](docker/)

### Front visio (page web statique)

Le client JavaFX ouvre le navigateur avec un token JWT ; la page web se connecte ensuite à LiveKit.

| Fichier                                             | Rôle                                                                     |
|-----------------------------------------------------|--------------------------------------------------------------------------|
| [`docs/livekit/index.html`](livekit/index.html)     | Interface utilisateur (caméra, micro, chat, partage d'écran)             |
| [`docs/livekit/js/config.js`](livekit/js/config.js) | URL LiveKit (`livekitUrl`), APIs serveur (`kickApiUrl`, `endRoomApiUrl`) |
| [`docs/livekit/js/app.js`](livekit/js/app.js)       | Logique client LiveKit                                                   |

Déployer le dossier `docs/livekit/` derrière le reverse proxy `visio.*`. Adapter `config.js` avec vos domaines — **ne
pas publier de secrets ou d'URL de production** dans le dépôt public.

---

## Variables d'environnement (rappel)

Modèle complet : [`.env.example`](../.env.example).

Le serveur Docker (branche `websocket`) utilise en plus des clés **LiveKit** (`API_KEY_VISIO`, `API_SECRET_VISIO`, `LIVEKIT_SERVER_URL`) côté serveur — non lues par le client JavaFX standard.

---

[← Index documentation](README.md) · [Installation →](SETUP.md)
