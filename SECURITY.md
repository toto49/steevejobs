# Politique de Sécurité de SteeveJobs

Nous prenons la sécurité très au sérieux. Ce document décrit la procédure à suivre si vous découvrez une vulnérabilité
de sécurité dans le code source de MediaStock.

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

**Ne signalez jamais une faille de sécurité critique en ouvrant une Issue publique sur GitHub.** Si vous pensez avoir
trouvé une vulnérabilité (par exemple, une Injection SQL, un accès non autorisé aux données des adhérents, etc.),
veuillez envoyer un e-mail directement à l'équipe de développement principale :
👉 **[tom.boudaud@reseau.eseo.fr]**

**Dans votre e-mail, veuillez inclure :**

1. Une description détaillée de la vulnérabilité.
2. Les étapes exactes pour reproduire le problème (Preuve de Concept / PoC).
3. L'impact potentiel si la faille est exploitée.

**Notre engagement :**

- Nous accuserons réception de votre e-mail dans un délai de 48 heures.
- Nous vous tiendrons au courant de l'avancement de la résolution du problème.
- Une fois le problème résolu en privé, nous publierons un correctif officiel.
