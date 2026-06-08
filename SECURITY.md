# Politique de Sécurité de SteeveJobs

Nous prenons la sécurité très au sérieux. Ce document décrit les mesures mises en place dans l'application, les bonnes pratiques de déploiement, et la procédure à suivre si vous découvrez une vulnérabilité.

## Mesures de sécurité implémentées

| Domaine | Mesure | Détail |
|---------|--------|--------|
| **Mots de passe** | Hachage BCrypt | Cost factor 12 via `UserService` ; aucun mot de passe en clair en base |
| **Bruteforce** | Verrouillage temporaire | 5 tentatives échouées → blocage 15 minutes + alerte email |
| **Accès** | Rôles et permissions | Modules filtrés par `PermissionService` et enum `AppModule` |
| **SQL** | Requêtes paramétrées | `PreparedStatement` dans les DAO ; pas de concaténation d'entrées utilisateur |
| **Secrets** | Variables d'environnement | Credentials lus depuis `.env` (gitignoré) ; modèle dans `.env.example` |
| **WebSocket** | JWT | Token généré à la connexion (`JwtService`) ; enregistrement serveur via session |
| **Validation** | Couche service | Contrôles métier (`IllegalArgumentException`, `SecurityException`) avant persistance |

### Limites connues

- Application **desktop JavaFX** : pas de protection CSRF web classique (hors périmètre navigateur).
- Compte **seeder** de développement (`DatabaseSeeder`) : mot de passe faible par défaut — **à changer immédiatement** en environnement réel.
- Le serveur WebSocket et LiveKit sont déployés séparément (branche `websocket`) — voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Bonnes pratiques de déploiement

1. Copier `.env.example` vers `.env` et **ne jamais committer** `.env`.
2. Choisir un `JWT_SECRET` long et aléatoire ; renouveler les mots de passe admin après la première installation.
3. Utiliser MySQL avec un compte dédié à l'application (droits minimaux).
4. En production : reverse proxy HTTPS, certificats Let's Encrypt, WebDAV et SMTP configurés côté NAS (voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)).
5. Exécuter `mvn test` avant chaque livraison — les scénarios de sécurité auth sont couverts par `UserServiceTest`.
6. Le front visio (`docs/livekit/js/config.js`) ne doit pas contenir de domaines ou clés de production dans le dépôt
   public.

## Versions prises en charge

Nous appliquons les correctifs de sécurité uniquement sur les versions récentes.

| Version | Supportée          | Remarques                            |
|---------|--------------------|--------------------------------------|
| `1`     | :white_check_mark: | Version stable actuelle.             |
| `< 1.0` | :x:                | Versions de développement obsolètes. |

## Ce qui est hors périmètre

Veuillez ne pas signaler les problèmes suivants :

* Attaques par déni de service (DDoS).
* Falsification de requêtes intersites (CSRF) sur les requêtes non authentifiées.
* Problèmes de configuration de l'environnement local (ex: mot de passe root MySQL faible sur votre propre machine).

## Signaler une vulnérabilité

**Ne signalez jamais une faille de sécurité critique en ouvrant une Issue publique sur GitHub.** Si vous pensez avoir trouvé une vulnérabilité (par exemple, une injection SQL, un accès non autorisé aux données utilisateurs, etc.), veuillez envoyer un e-mail directement à l'équipe de développement principale :

👉 **tom.boudaud@reseau.eseo.fr**

**Dans votre e-mail, veuillez inclure :**

1. Une description détaillée de la vulnérabilité.
2. Les étapes exactes pour reproduire le problème (Preuve de Concept / PoC).
3. L'impact potentiel si la faille est exploitée.

**Notre engagement :**

- Nous accuserons réception de votre e-mail dans un délai de 48 heures.
- Nous vous tiendrons au courant de l'avancement de la résolution du problème.
- Une fois le problème résolu en privé, nous publierons un correctif officiel.
