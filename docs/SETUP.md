# Guide d'installation et de configuration

Ce guide s'adresse aux **développeurs** et **administrateurs** qui installent SteeveJobs en local ou préparent un déploiement.

> Pour utiliser l'application au quotidien (sans installer le code), consultez le [Guide utilisateur](GUIDE_UTILISATEUR.md).

---

## Prérequis

| Composant | Version minimale | Notes |
|-----------|------------------|-------|
| JDK | 25+ | [Liberica JDK Full](https://bell-sw.com/pages/downloads/) recommandé (JavaFX inclus) |
| MySQL | 8.0+ | Base relationnelle métier |
| Maven | 3.9+ | Ou utilisez `./mvnw` / `mvnw.cmd` |
| Docker | récent | Serveur WebSocket (branche `websocket`) |
| SMTP | — | Dev local ou MailPlus en production |

---

## 1. Cloner le dépôt

```bash
git clone https://github.com/toto49/steevejobs.git
cd steevejobs
```

Configurez le **JDK 25 Full** comme SDK dans IntelliJ IDEA ou Eclipse.

---

## 2. Base de données

```sql
CREATE DATABASE steevejobs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Import du schéma :

```bash
mysql -u root -p steevejobs < sql/steevejobs.sql
```

---

## 3. Fichier `.env`

```bash
cp .env.example .env
```

**Emplacement :**

- Projet lancé depuis les sources → racine du repo (à côté de `pom.xml`)
- Application compilée → même dossier que le `.jar` / `.exe`

**Variables utilisées par le client :**

| Variable | Rôle |
|----------|------|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion MySQL |
| `SMTP_URL`, `EXPEDITEUR` | Envoi d'e-mails |
| `WS_SERVER_IP`, `WS_SERVER_PORT` | Serveur WebSocket relais |
| `JWT_SECRET` | Tokens JWT client ↔ serveur |
| `WEBDAV_BASE_URL`, `WEBDAV_USERNAME`, `WEBDAV_PASSWORD` | Stockage fichiers (GED) |
| `URL_FRONT_VISIO` | Page web LiveKit (navigateur) |

Ne commitez **jamais** le fichier `.env` rempli. Utilisez des domaines et secrets de **développement** ou des placeholders (`votre-domaine.fr`).

---

## 4. Premier lancement — données initiales

Exécutez la classe `com.eseo.steevejobs.config.DatabaseSeeder` depuis votre IDE pour :

- créer les permissions de base ;
- générer un **compte administrateur** initial.

---

## 5. Compiler, tester et lancer

```bash
# Windows
.\mvnw.cmd clean install
.\mvnw.cmd test
.\mvnw.cmd javafx:run

# Linux / macOS
./mvnw clean install
./mvnw test
./mvnw javafx:run
```

Sur Java 25, les tests Mockito peuvent nécessiter :

```bash
./mvnw test -Dnet.bytebuddy.experimental=true
```

---

## 6. Serveur WebSocket & LiveKit (Docker)

Notifications temps réel, visio et synchronisation tickets : déployés via **Docker Compose** sur la branche **[`websocket`](https://github.com/toto49/steevejobs/tree/websocket)**.

Résumé :

```bash
git checkout websocket
mvn clean package && cp target/*-with-dependencies.jar serveur.jar
cp livekit.yaml.example livekit.yaml   # adapter clés et domaine
cp .env.example .env                   # JWT, BDD, LiveKit…
docker compose up -d --build
```

**Guide complet (fichiers, ports, NAS, dépannage) :** **[docs/DOCKER.md](DOCKER.md)**

Fichiers de référence (sans secrets) : [`docs/docker/`](docker/)

En production : reverse proxy `wss://notif.votre-domaine.fr` → port **8887**. Voir [Architecture production](ARCHITECTURE.md).

---

## 7. Javadoc

```bash
mvn javadoc:javadoc
# → target/site/apidocs/
```

IntelliJ : **Maven → Plugins → javadoc → javadoc:javadoc**.

---

## Dépannage rapide

| Symptôme | Piste |
|----------|-------|
| Erreur connexion BDD | Vérifiez `DB_*` dans `.env` et que MySQL écoute |
| WebSocket déconnecté | `WS_SERVER_IP` / `WS_SERVER_PORT`, serveur Docker actif |
| Visio : token refusé | `JWT_SECRET` identique client + serveur ; voir logs NAS |
| E-mail non envoyé | `SMTP_URL` et pare-feu port 587 |
| PDF / pièces jointes | Droits WebDAV et `WEBDAV_*` |

---

[← Index documentation](README.md) · [Guide utilisateur →](GUIDE_UTILISATEUR.md)
