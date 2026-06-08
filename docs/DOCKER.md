# Déploiement Docker (branche `websocket`)

Guide de déploiement du **serveur relais WebSocket** et du **serveur LiveKit** via Docker Compose.

> **Branche source :** [github.com/toto49/steevejobs/tree/websocket](https://github.com/toto49/steevejobs/tree/websocket)  
> Les fichiers Docker officiels du projet se trouvent à la racine de cette branche. Des copies de référence (sans secrets) sont aussi disponibles dans [`docs/docker/`](docker/).

---

## Sommaire

1. [Vue d'ensemble](#1-vue-densemble)
2. [Fichiers de configuration](#2-fichiers-de-configuration)
3. [Prérequis](#3-prérequis)
4. [Installation pas à pas](#4-installation-pas-à-pas)
5. [Ports et réseau](#5-ports-et-réseau)
6. [Reverse proxy (production)](#6-reverse-proxy-production)
7. [Dépannage](#7-dépannage)

---

## 1. Vue d'ensemble

Le `compose.yaml` lance **deux services** :

```text
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
├──────────────────────────┬──────────────────────────────┤
│  ws-server               │  livekit-server               │
│  (build Dockerfile)      │  (image livekit/livekit-server)│
│  Port 8887               │  network_mode: host           │
│  serveur.jar + .env      │  livekit.yaml                 │
└──────────────────────────┴──────────────────────────────┘
         │                              │
         ▼                              ▼
   Client JavaFX                  Navigateur visio
   (WSS notifications,            (WebRTC / caméra / micro)
    tokens LiveKit)
```

| Service | Rôle |
|---------|------|
| **ws-server** | Relais WebSocket : tickets, visio, JWT LiveKit, sync BDD |
| **livekit-server** | Média WebRTC (audio/vidéo, partage d'écran, TURN) |

---

## 2. Fichiers de configuration

| Fichier                               | Emplacement branche `websocket` | Copie doc (référence)                                             |
|---------------------------------------|---------------------------------|-------------------------------------------------------------------|
| [`compose.yaml`](docker/compose.yaml) | Racine                          | [`docs/docker/compose.yaml`](docker/compose.yaml)                 |
| [`Dockerfile`](docker/Dockerfile)     | Racine                          | [`docs/docker/Dockerfile`](docker/Dockerfile)                     |
| `livekit.yaml`                        | Racine (local, **gitignoré**)   | [`docs/docker/livekit.yaml.example`](docker/livekit.yaml.example) |
| `.env`                                | Racine (local, **gitignoré**)   | [`docs/docker/.env.example`](docker/.env.example)                 |
| Front visio                           | —                               | [`docs/livekit/`](../livekit/) (page statique, hors Docker)       |

### `Dockerfile`

Image **Eclipse Temurin 17 JRE Alpine** ; exécute `serveur.jar` compilé par Maven :

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY serveur.jar /app/serveur.jar
EXPOSE 8887
CMD ["java", "-jar", "/app/serveur.jar"]
```

### `compose.yaml`

- **ws-server** : build local, monte `./.env`, expose **8887**
- **livekit-server** : image officielle, mode **`network_mode: host`**, config via `livekit.yaml`

> Le mode `host` pour LiveKit est requis pour le RTC (plage UDP 50000–60000). Sur Synology, vérifiez les règles pare-feu DSM en conséquence.

### `livekit.yaml`

Paramètres principaux :

| Clé | Description |
|-----|-------------|
| `port: 7880` | Port HTTP/gRPC LiveKit |
| `rtc.tcp_port: 7881` | TCP RTC |
| `rtc.port_range_*` | Plage UDP média |
| `rtc.use_external_ip` | Annonce IP publique (NAT) |
| `turn.*` | Serveur TURN intégré (TLS 5349, UDP 3478) |
| `keys` | Paire `API_KEY` / secret — **doit correspondre** à `API_KEY_VISIO` / `API_SECRET_VISIO` du `.env` serveur |

Utilisez le modèle [`livekit.yaml.example`](docker/livekit.yaml.example) et remplacez `votre-domaine.fr` / les clés par vos valeurs.

---

## 3. Prérequis

- Git + accès à la branche [`websocket`](https://github.com/toto49/steevejobs/tree/websocket)
- **JDK 17+** et **Maven** (compilation du JAR)
- **Docker** + **Docker Compose** v2
- MySQL accessible depuis le conteneur `ws-server`
- (Production) Reverse proxy + certificats SSL

---

## 4. Installation pas à pas

### Étape A — Récupérer la branche

```bash
git clone https://github.com/toto49/steevejobs.git
cd steevejobs
git checkout websocket
```

Ou consultez directement le dépôt :  
**https://github.com/toto49/steevejobs/tree/websocket**

### Étape B — Compiler le serveur relais

```bash
mvn clean package
cp target/websocket-server-with-dependencies.jar serveur.jar
# ou : mv target/...jar serveur.jar  selon le nom généré par Maven
```

### Étape C — Configurer l'environnement

```bash
cp docs/docker/.env.example .env    # depuis main, ou modèle local sur websocket
cp docs/docker/livekit.yaml.example livekit.yaml
# Éditez .env et livekit.yaml avec vos valeurs (sans les committer)
```

Assurez-vous que **`JWT_SECRET`** est **identique** entre :

- le `.env` du serveur Docker ;
- le `.env` du client JavaFX (`steevejobs` branche principale).

Et que les clés LiveKit correspondent entre `livekit.yaml` (`keys`) et `.env` (`API_KEY_VISIO` / `API_SECRET_VISIO`).

### Étape D — Lancer la stack

```bash
docker compose up -d --build
```

Vérification :

```bash
docker compose ps
docker logs steevejobs-websocket
docker logs steevejobs-livekit
```

Arrêt :

```bash
docker compose down
```

### Déploiement Synology NAS

1. Copiez le dossier `websocket-steevejobs` sur le NAS (partage ou SSH).
2. Placez `serveur.jar`, `.env`, `livekit.yaml`, `Dockerfile`, `compose.yaml` au même niveau.
3. Dans **Container Manager**, importez le projet Compose ou lancez `docker compose up -d` en SSH.
4. Configurez le reverse proxy DSM (voir ci-dessous).

---

## 5. Ports et réseau

| Service | Port(s) | Protocole | Usage |
|---------|---------|-----------|--------|
| ws-server | **8887** | TCP / WS | Notifications, visio, tickets |
| livekit-server | **7880** | TCP | API LiveKit |
| livekit-server | **7881** | TCP | RTC |
| livekit-server | **50000–60000** | UDP | Média WebRTC |
| TURN (livekit) | **5349** | TLS | Traversée NAT |
| TURN (livekit) | **3478** | UDP | Traversée NAT |

Ouvrez ces ports sur le pare-feu si LiveKit est exposé directement. En production, seuls **443/WSS** passent souvent par le reverse proxy ; la plage UDP reste ouverte côté serveur LiveKit.

---

## 6. Reverse proxy (production)

Exemple de routage (domaines anonymisés) :

| URL publique                     | Cible interne                                   |
|----------------------------------|-------------------------------------------------|
| `wss://notif.votre-domaine.fr`   | `http://127.0.0.1:8887` (ws-server)             |
| `wss://livekit.votre-domaine.fr` | LiveKit `:7880` (+ UDP RTC)                     |
| `https://visio.votre-domaine.fr` | Dossier statique [`docs/livekit/`](../livekit/) |

Headers WebSocket requis sur le proxy :

```text
Upgrade: websocket
Connection: Upgrade
```

### Déployer le front visio

1. Copiez `docs/livekit/` sur le NAS (Web Station ou partage servi en HTTPS).
2. Éditez `js/config.js` :

```javascript
window.VISIO_CONFIG = {
  brandName: "SteeveJobs",
  livekitUrl: "wss://livekit.votre-domaine.fr",
  kickApiUrl: "https://visio.votre-domaine.fr/api/visio/kick",
  endRoomApiUrl: "https://visio.votre-domaine.fr/api/visio/end-room",
  connectTimeoutMs: 25000,
  loadingMessage: "Connexion à la salle...",
  backLink: "/",
};
```

3. Vérifiez que `URL_FRONT_VISIO` côté client pointe vers `https://visio.votre-domaine.fr/index.html`.

Voir aussi [Architecture production](ARCHITECTURE.md).

---

## 7. Dépannage

| Symptôme | Piste |
|----------|-------|
| `ws-server` ne démarre pas | `.env` manquant ou `serveur.jar` absent |
| Client : WebSocket déconnecté | Port 8887, `WS_SERVER_*` client, proxy WSS |
| Token visio refusé | `JWT_SECRET` client = serveur ; logs `steevejobs-websocket` |
| Pas de vidéo / audio | Pare-feu UDP 50000–60000 ; `use_external_ip: true` |
| Erreur clé LiveKit | Aligner `keys` dans `livekit.yaml` et `.env` serveur |
| Duplicate entry salon visio | Salons instantanés supprimés à la fermeture (comportement attendu) |

---

## Liens utiles

- [Branche `websocket` sur GitHub](https://github.com/toto49/steevejobs/tree/websocket)
- [Guide d'installation client](SETUP.md)
- [Architecture production](ARCHITECTURE.md)
- [LiveKit — documentation officielle](https://docs.livekit.io/)

---

[← Index documentation](README.md)
