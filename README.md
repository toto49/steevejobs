# 💼 SteeveJobs

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/fr/java/technologies/downloads/#java25)
[![JavaFX](https://img.shields.io/badge/JavaFX-GUI-4780bc?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Liberica](https://img.shields.io/badge/Liberica%20JDK-25-0A7BBB?style=for-the-badge&logo=java&logoColor=white)](https://bell-sw.com/pages/downloads/#jdk-25-lts)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/fr/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

**SteeveJobs** est un logiciel de bureau moderne conçu pour la gestion complète d’une entreprise de type PME

Développé en **Java 25** avec **JavaFX**, l’application propose une interface fluide, intuitive et pensée pour faciliter
le travail quotidien des employés de l'entreprise.

---

## ✨ Fonctionnalités principales

### 🛡️ Souveraineté & Sécurité (SecNumCloud)

- **Conformité anticipée :** Prêt pour les réglementations françaises, incluant l'obligation de réception des factures
  électroniques (réforme 2026).
- **Sécurité des accès :** Authentification robuste (JBCrypt) et gestion fine des permissions par module.

### 👥 Ressources Humaines (RH)

- **Gestion du personnel :** Accès, création et suivi des fiches de paie.
- **Plannings :** Organisation et gestion des emplois du temps des équipes.

### 📦 Gestion Commerciale & Logistique

- **Catalogue :** Gestion complète des produits.
- **Réseau (Tiers) :** Annuaire centralisé pour le suivi des clients et fournisseurs.
- **Gestion Documentaire :** Suivi des statuts et types de documents de l'entreprise.

### 💬 Support & Communication

- **Ticketing :** Système interne de création et de suivi des tickets de support.

### 🛠️ Administration

- **Gestion des utilisateurs :** Création, modification, suppréssion des utilisateurs.
- **Gestion des permissions :** Gestion des permissions d'accès aux modules interne de l'application.

---

## 🛠️ Stack technique & architecture

Le projet repose sur une architecture en couches de type **MVC (Modèle-Vue-Contrôleur)** afin de garantir une base de
code claire, sécurisée et maintenable.

- **Langage :** Java 25
- **Interface graphique :** JavaFX (avec Scene Builder FXML & AtlantaFX)
- **Base de données :** MySQL (JDBC direct)
- **Synology MailPlus Server**
- **Outils externes :** JavaFX, ControlsFX, Atlantafx, JUnit, Kotlin Stdlib, Kotlin Test, jBCrypt, Dotenv Java, MySQL
  Connector/J, Jakarta Mail (API + Impl), JavaFX Maven Plugin, Kotlin Maven Plugin, Maven Compiler Plugin

```text
/model       → Entités métiers (Livre, DVD, Adherent, Emprunt...)
/dao         → Accès aux données sécurisé (PreparedStatement)
/service     → Logique métier (règles d’emprunt, orchestration)
/controller  → Interface JavaFX et gestion des événements utilisateur
/config      → Contient tout ce qui concerne la configuration de base de l’application
```

---

## 🚀 Installation & démarrage

### Prérequis

- **Java JDK 25+** installé (Nous recommandons fortement Liberica JDK 25 Full pour inclure JavaFX nativement)
- **MySQL 8.0+** fonctionnel
- **Maven** (inclus via le wrapper `mvnw` du projet, ou installable manuellement)
- **Serveur STMP** installé et fonctionnel pour pouvoir envoyer des mails

### 1. Configuration de l'environnement (JDK & Maven)

#### ☕ Installer Liberica JDK Full 25

Le projet utilisant JavaFX, l'utilisation de la version "Full" de Liberica JDK est recommandée car elle intègre les
modules JavaFX directement.

1. Rendez-vous sur la [page de téléchargement de BellSoft (Liberica JDK)](https://bell-sw.com/pages/downloads/).
2. Sélectionnez **Java 25** et votre système d'exploitation.
3. **Important :** Veillez à bien télécharger le package **"Full JDK"** (et non "Standard JDK").
4. Installez-le. Si vous utilisez IntelliJ IDEA ou Eclipse, allez dans les paramètres du projet et sélectionnez ce
   nouveau JDK comme SDK par défaut.

#### 🏗️ Installer Maven (si vous ne souhaitez pas utiliser le wrapper)

Ce projet inclut déjà un wrapper Maven (`mvnw` / `mvnw.cmd`) qui permet d'exécuter Maven sans l'installer. Vous pouvez
simplement utiliser `./mvnw` à la place de `mvn` dans les commandes ci-dessous.
Cependant, pour l'installer globalement sur votre machine :

* **Windows :** Téléchargez l'archive sur le site d'Apache Maven, extrayez-la, puis ajoutez le chemin du dossier `bin` à
  votre variable d'environnement `PATH`. (Alternative via Winget : `winget install Microsoft.Maven`).
* **macOS (via Homebrew) :** `brew install maven`
* **Linux (Debian/Ubuntu) :** `sudo apt install maven`

### 2. Préparation de la base de données

Connectez-vous à votre serveur MySQL et créez la base de données :

```sql
CREATE
DATABASE steevejobs;
```

Ensuite, importez la structure des tables en exécutant le script fourni : `sql/steevejobs.sql`.

### 3. Configuration sécurisée

Pour que l'application puisse se connecter à la base de données, vous devez créer un fichier nommé exactement `.env` et
y ajouter vos identifiants MySQL. Son emplacement dépend de votre utilisation :

- **Si vous lancez le projet depuis les sources :** Placez le fichier `.env` à la racine du projet (au même niveau que
  le fichier `pom.xml`).
- **Si vous avez téléchargé l'application compilée (.exe, .jar, etc.) :** Placez le fichier `.env` dans le même dossier
  que l'exécutable.

Contenu à ajouter dans le fichier `.env` :

```env
DB_URL=jdbc:mysql://localhost:3306/steevejobs
DB_USER=root
DB_PASSWORD=votre_mot_de_passe_ici
SMTP_URL = lien de votre serveur mail
EXPEDITEUR = adresse mail de votre expediteur
```

### 🌱 Initialisation de la base de données (premier lancement)

Lors du tout premier lancement, exécutez la classe utilitaire `DatabaseSeeder.java`
(package `com.eseo.steevejobs.config`) directement depuis votre IDE pour :

- générer un compte administrateur par défaut pour la première connexion

> ⚠️ Cette étape est fortement recommandée : elle vous fournit un environnement
> fonctionnel "clé en main", sans avoir à créer les données initiales manuellement.

### 4. Compilation et lancement

Ouvrez un terminal à la racine du projet et exécutez les commandes suivantes :

```bash
# Compiler le projet et télécharger les dépendances
mvn clean install

# Exécuter les tests unitaires
mvn test

# Lancer l'application JavaFX
mvn javafx:run
```

---

## 📖 Documentation technique (Javadoc)

Conformément aux bonnes pratiques de versionnement avec Git, les fichiers HTML générés automatiquement (comme la
Javadoc) ne sont pas inclus dans ce dépôt. Ce choix permet de :

- garder un historique Git plus propre,
- éviter les conflits de fusion sur des fichiers générés,
- ne pas alourdir inutilement le dépôt.

### Méthode 1 — Génération via Maven en ligne de commande

Depuis la racine du projet, exécutez :

```bash
mvn javadoc:javadoc
```

Les fichiers générés seront disponibles dans :

```text
target/site/apidocs/
```

### Méthode 2 — Génération via IntelliJ IDEA avec Maven

Si vous utilisez **IntelliJ IDEA**, vous pouvez générer la Javadoc directement depuis l’interface :

1. Ouvrez l’onglet **Maven** situé dans la barre latérale droite d’IntelliJ IDEA.
2. Déroulez votre projet **MediaStock**.
3. Ouvrez la section **Plugins**.
4. Déroulez **javadoc**.
5. Double-cliquez sur **javadoc:javadoc**.

Vous pouvez également retrouver la documentation générée dans le même dossier :

```text
target/site/apidocs/
```

> 💡 **Astuce :** si l’onglet Maven n’apparaît pas dans IntelliJ IDEA, vérifiez que le projet a bien été importé comme
> projet **Maven** à partir du fichier `pom.xml`.

---

## 🤝 Contribuer & documentation

Les contributions sont les bienvenues ! Merci de consulter les fichiers suivants avant toute Pull Request :

- [`CONTRIBUTING.md`](CONTRIBUTING.md) : règles de contribution
- [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) : code de conduite
- [`SECURITY.md`](SECURITY.md) : signalement de failles

Retrouvez la documentation complète dans le dossier [`/docs`](docs/README-DOCS.md).

---

## 📄 Licence

Ce projet est distribué sous licence **MIT**. Voir le fichier [`LICENSE`](LICENSE) pour plus de détails.
