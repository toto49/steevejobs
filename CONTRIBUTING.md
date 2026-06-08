# Guide de Contribution - SteeveJobs

Avant tout, merci de prendre le temps de contribuer à SteeveJobs ! 🎉
Ce projet est développé en Java / JavaFX et utilise une base de données MySQL. Pour garantir un code de qualité et
faciliter la collaboration, nous vous demandons de suivre les directives ci-dessous.

## Table des matières

1. [Code de Conduite](#code-de-conduite)
2. [Architecture du Projet](#architecture-du-projet)
3. [Configuration de l'environnement](#configuration-de-lenvironnement)
4. [Tests et qualité](#tests-et-qualité)
5. [Processus de développement (Git Flow)](#processus-de-développement-git-flow)
6. [Conventions de nommage et de commit](#conventions-de-nommage-et-de-commit)

## Code de Conduite

En participant à ce projet, vous acceptez de respecter notre [Code de Conduite](CODE_OF_CONDUCT.md). Veuillez le lire
avant d'interagir avec la communauté.

## Architecture du Projet

Ce projet respecte strictement un motif architectural en couches :

- **Model (`com.eseo.steevejobs.model`)** : Contient les entités métier (`User`, `Produit`, `DemandeConge`, `Ticket`, etc.).
- **DAO (`com.eseo.steevejobs.dao`)** : Gère l'accès aux données (requêtes SQL).
- **Service (`com.eseo.steevejobs.service`)** : Contient toute la logique métier. **Les contrôleurs ne doivent jamais
  appeler les DAO directement.** Ils doivent passer par les services.
- **Controller (`com.eseo.steevejobs.controller`)** : Gère l'interface graphique JavaFX (19 contrôleurs FXML). La
  logique métier y est
  strictement interdite.

## Configuration de l'environnement

Pour compiler et exécuter le projet localement :

1. Assurez-vous d'avoir installé **Java JDK 25+** (Liberica JDK Full recommandé pour JavaFX).
2. Clonez le dépôt : `git clone https://github.com/toto49/steevejobs.git`
3. Installez un serveur **MySQL** (version 8.x) et créez une base de données nommée `steevejobs`.
4. Importez le fichier SQL de structure fourni dans le dossier `/sql`.
5. Copiez `.env.example` vers `.env` et adaptez les variables.
6. Ouvrez le projet dans IntelliJ IDEA en tant que projet **Maven** (`pom.xml` ou `mvnw`).

Guide détaillé : [docs/SETUP.md](docs/SETUP.md).

## Tests et qualité

Avant toute Pull Request :

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

- **~150 scénarios** JUnit 5 (services mockés, DAO H2, controllers JavaFX) — voir [docs/TESTS.md](docs/TESTS.md).
- Les tests activent automatiquement le mode silencieux (`TestRuntime` / `-Dsteevejobs.test=true`) : pas de WebSocket ni
  de connexion BDD réelle dans les controllers.
- La CI valide le build sur **Windows, Linux et macOS**.

Pour empaqueter l'application : `mvn package -DskipTests` → `target/dist/SteeveJobs/`.

## Processus de développement (Git Flow)

Ne travaillez **jamais** directement sur `master` (branche stable).

1. Récupérez les dernières modifications : `git pull origin develop`
2. Créez une branche explicite à partir de `develop` :
    - Fonctionnalité : `git checkout -b features/nom-de-la-feature`
    - Correctif urgent : `git checkout -b hotfix/nom-du-correctif`
    - Documentation : `git checkout -b docs/mise-a-jour-readme`
3. Poussez votre branche : `git push origin votre-branche`
4. Ouvrez une Pull Request vers `develop` sur GitHub.

## Conventions de nommage et de commit

Nous utilisons les **Conventional Commits** pour garder un historique propre :

- `feat(conge): validation RH des demandes avec solde insuffisant`
- `fix(auth): correction du verrouillage après tentatives échouées`
- `refactor(dao): factorisation des requêtes PreparedStatement dans UserDAO`
- `docs(tests): mise à jour des effectifs dans TESTS.md`

Tout code non formaté ou ne respectant pas l'architecture MVC sera refusé lors de la revue de code.
