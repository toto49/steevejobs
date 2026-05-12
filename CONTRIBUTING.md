# Guide de Contribution - SteeveJobs

Avant tout, merci de prendre le temps de contribuer à SteeveJobs ! 🎉
Ce projet est développé en Java / JavaFX et utilise une base de données MySQL. Pour garantir un code de qualité et
faciliter la collaboration, nous vous demandons de suivre les directives ci-dessous.

## Table des matières

1. [Code de Conduite](#code-de-conduite)
2. [Architecture du Projet](#architecture-du-projet)
3. [Configuration de l'environnement](#configuration-de-lenvironnement)
4. [Processus de développement (Git Flow)](#processus-de-développement-git-flow)
5. [Conventions de nommage et de commit](#conventions-de-nommage-et-de-commit)

## Code de Conduite

En participant à ce projet, vous acceptez de respecter notre [Code de Conduite](CODE_OF_CONDUCT.md). Veuillez le lire
avant d'interagir avec la communauté.

## Architecture du Projet

Ce projet respecte strictement un motif architectural en couches :

- **Model (`com.eseo.steevejobs.model`)** : Contient les objets métiers (Produit, Livre, Adherent, etc.).
- **DAO (`com.eseo.steevejobs.dao`)** : Gère l'accès aux données (requêtes SQL).
- **Service (`com.eseo.steevejobs.service`)** : Contient toute la logique métier. **Les contrôleurs ne doivent jamais
  appeler les DAO directement.** Ils doivent passer par les services.
- **Controller (`com.eseo.steevejobs.controller`)** : Gère l'interface graphique JavaFX. La logique métier y est
  strictement interdite.

## Configuration de l'environnement

Pour compiler et exécuter le projet localement :

1. Assurez-vous d'avoir installé **Java JDK 25+** (Liberica JDK recommandé pour JavaFX).
2. Clonez le dépôt : `git clone https://github.com/toto49/steevejobs.git`
3. Installez un serveur **MySQL** (version 8.x) et créez une base de données nommée `steevejobs`.
4. Importez le fichier SQL de structure fourni dans le dossier `/sql`.
5. Ouvrez le projet dans IntelliJ IDEA en tant que projet Maven/Gradle.

## Processus de développement (Git Flow)

Ne travaillez **jamais** directement sur la branche `main`.

1. Récupérez les dernières modifications : `git pull origin main`
2. Créez une branche explicite à partir de `main` :
    - Pour une fonctionnalité : `git checkout -b feat/nom-de-la-feature`
    - Pour un bug : `git checkout -b fix/nom-du-bug`
    - Pour de la documentation : `git checkout -b docs/mise-a-jour-readme`
3. Poussez votre branche : `git push origin votre-branche`
4. Ouvrez une Pull Request sur GitHub.

## Conventions de nommage et de commit

Nous utilisons les **Conventional Commits** pour garder un historique propre :

- `feat: ajout de la barre de recherche dans la liste des adhérents`
- `fix: correction du crash lors de l'ajout d'un livre sans auteur`
- `refactor: déplacement de la méthode de calcul EAN13 dans le service`
- `style: mise à jour du fichier CSS des boutons`

Tout code non formaté ou ne respectant pas l'architecture MVC sera refusé lors de la revue de code.
