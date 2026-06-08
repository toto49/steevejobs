# Stratégie de tests unitaires — SteeveJobs

## Objectif

Couvrir la logique métier des **services Java** (validation, sécurité, règles métier, gestion d'erreurs) **sans dépendre de MySQL**, conformément aux exigences du projet (tests unitaires variés et utiles).

Les tests mockent les DAO avec Mockito : aucune base de données n'est requise pour les exécuter.

**Principe retenu :** couvrir la logique métier des services et **chaque DAO métier** via des tests d'intégration H2 (`mvn test` → **~150 scénarios** exécutés).

## Organisation du code de test

```
src/test/java/
├── DatabaseConnexion.java          # utilitaire manuel (vérif connexion BDD), pas un test JUnit
├── controller/
│   ├── BienvenueControllerTest.java
│   ├── ControllerFxmlLoadTest.java
│   ├── MenuControllerTest.java
│   ├── TicketControllerTest.java
│   ├── TicketsListControllerTest.java
│   └── support/
├── dao/
│   ├── ComposerDAOTest.java
│   ├── DemandeCongeDAOTest.java
│   ├── DocumentDAOTest.java
│   ├── FichePayeDAOTest.java
│   ├── HeuresTravailDAOTest.java
│   ├── MessageDAOTest.java
│   ├── PermissionDAOTest.java
│   ├── PlanningDAOTest.java
│   ├── ProduitDAOTest.java
│   ├── TicketDAOTest.java
│   ├── TiersDAOTest.java
│   ├── UserDAOTest.java
│   ├── VisioDAOTest.java
│   └── support/
│       ├── DaoIntegrationExtension.java
│       ├── DaoTestCleanup.java
│       ├── DaoTestFixtures.java
│       └── SqlCleanup.java
└── service/
    ├── ConnexionServiceTest.java
    ├── CongeUtilTest.java
    ├── JwtServiceTest.java
    ├── UserServiceTest.java, DemandeCongeServiceTest.java, … (16 classes)
    └── support/
        ├── MockitoJava25Support.java
        └── TestDataFactory.java
```

**34 classes de test JUnit**, **~150 scénarios** exécutés par Surefire (`mvn test`), dont **19 chargements FXML** via un test paramétré.

| Couche | Classes | Méthodes `@Test` | Scénarios exécutés |
|--------|---------|------------------|-------------------|
| Services (mockés) | 16 | 87 | 87 |
| Controllers (JavaFX) | 5 | 7 | 25 |
| DAO (H2 intégration) | 13 | 38 | 38 |
| **Total** | **34** | **132** | **~150** |

## Technologies

| Outil | Rôle |
|-------|------|
| JUnit 5 | Framework de test |
| JUnit Params | Chargement FXML paramétré (`@ParameterizedTest`) |
| Mockito 5.20 | Mocks des DAO et services PDF |
| H2 (test) | Base en mémoire pour les tests d'intégration DAO |
| JavaFX Platform | Tests controllers (thread FX, chargement FXML) |
| Maven Surefire | Exécution via `mvn test` |

## Services et DAO

Les tests de **services** mockent les DAO avec Mockito. Ils valident :

- la logique métier (validation, sécurité, règles métier) ;
- les **contrats d'appel** vers le DAO (méthodes invoquées, paramètres, gestion d'erreur).

Ils n'exécutent **pas** le SQL réel. Un passage complet des tests services garantit que la couche métier et les controllers qui s'appuient sur ces services se comportent correctement **dans le modèle MVC testé**.

La couche **DAO** est validée par des **tests d'intégration** dédiés (voir ci-dessous). Une vérification manuelle sur MySQL reste possible via `DatabaseConnexion.java`.

## Tests DAO (intégration SQL)

Les tests DAO (`src/test/java/dao/`) exécutent du **vrai SQL** contre une base **H2 en mémoire** (MODE MySQL), sans toucher à la base de production :

- schéma minimal dans `src/test/resources/dao/schema-h2.sql` ;
- initialisation via `DatabaseConnection.reconfigureForTests(...)` ;
- **2 à 3 scénarios par classe DAO** (CRUD, requêtes métier, upsert) — sauf `UserDAOTest` (2) ;
- chaque insertion enregistre une **suppression** dans `DaoTestCleanup` ;
- `@AfterEach` (extension `DaoIntegrationExtension`) exécute les suppressions **dans l'ordre inverse**.

Emails de test préfixés `dao-test-…@test.local` pour éviter les collisions.

| DAO | Fichier de test | Scénarios |
|-----|-----------------|-----------|
| `UserDAO` | `UserDAOTest` | Création/lecture email, désactivation |
| `ProduitDAO` | `ProduitDAOTest` | CRUD, `updateStock`, `getByNom` |
| `TiersDAO` | `TiersDAOTest` | CRUD, `emailExists`, `updateTiers` |
| `TicketDAO` | `TicketDAOTest` | CRUD, `updateStatut`, `findAll` |
| `MessageDAO` | `MessageDAOTest` | CRUD, `findByTicketId`, `deleteMessage` |
| `PlanningDAO` | `PlanningDAOTest` | CRUD, `findByUserId`, `deletePlanning` |
| `HeuresTravailDAO` | `HeuresTravailDAOTest` | Sauvegarde/lecture, date sans enregistrement, upsert (`ON DUPLICATE KEY`) |
| `PermissionDAO` | `PermissionDAOTest` | Rôle/permission, `getPermissionIdsByRole`, révocation |
| `DemandeCongeDAO` | `DemandeCongeDAOTest` | CRUD, `findByStatut`, `findByUserId` |
| `FichePayeDAO` | `FichePayeDAOTest` | CRUD, `findByEmployeId`, `countByEmployeId` |
| `VisioDAO` | `VisioDAOTest` | Salon instantané, planification, `isCreateur` |
| `DocumentDAO` | `DocumentDAOTest` | CRUD, `updateStatut`, `findByTiersId` |
| `ComposerDAO` | `ComposerDAOTest` | Lignes produit, suppression, multi-produits |

**Couverture DAO complète** : les 13 DAO métier sont testés (38 scénarios).

## Controllers

Les tests controllers (`src/test/java/controller/`) couvrent :

- le **câblage FXML** de **19 vues** via un test paramétré unique (`ControllerFxmlLoadTest`) ;
- la **logique UI testable sans BDD** : titres tickets, session chat, badges menu, validation login vide ;
- l'exécution sur le **thread JavaFX** via `JavaFxTestSupport`.

Ils ne remplacent pas un test d'intégration bout-en-bout avec MySQL.

### Vues FXML testées (19)

`bienvenue`, `menu`, `home`, `ticket`, `ticketsList`, `parametres`, `stock`, `calendrier`, `visio`, `document`, `clients`, `adminuser`, `adminpermission`, `fiche-paye`, `documentUser`, `modifier-document`, `nouveau-document`, `demandes-conge-popup`, `calendrier-rh`.

## Services couverts

| Service | Fichier de test | Scénarios testés |
|---------|----------------|------------------|
| `UserService` | `UserServiceTest` | Création, email dupliqué, authentification (succès, échec, compte désactivé, verrouillage), hash BCrypt, activation/désactivation, mots de passe |
| `WebSocketUiBridge` | `WebSocketUiBridgeTest` | Callbacks tickets (refresh chat, liste, notification) |
| `PermissionService` | `PermissionServiceTest` | Lecture permissions, assignation/révocation, protection SuperAdmin, validation des paramètres |
| `ProduitService` | `ProduitServiceTest` | Ajout/modification, validation nom/prix, mise à jour du stock, produit introuvable |
| `TiersService` | `TiersServiceTest` | Validation email/SIRET/nom, CRUD, erreurs BDD simulées |
| `PlanningService` | `PlanningServiceTest` | Cohérence des dates, utilisateur obligatoire, ajout/modification/suppression, erreurs BDD |
| `DocumentService` | `DocumentServiceTest` | Validation document, export PDF, suppression, changement de statut |
| `FichePayeService` | `FichePayeServiceTest` | Génération paie, doublon mensuel, salaire/taux invalides, SMIC minimum |
| `HeuresTravailService` | `HeuresTravailServiceTest` | Sauvegarde et lecture des heures, échec DAO |
| `TicketServiceImpl` | `TicketServiceTest` | Création ticket, messages, changement de statut, formatage des dates |
| `MessageServiceImpl` | `MessageServiceTest` | Lecture par ID/auteur, suppression, erreurs SQL |
| `ConnexionService` | `ConnexionServiceTest` | Génération de mots de passe aléatoires (longueur, charset) |
| `JwtService` | `JwtServiceTest` | Génération JWT (null sans `.env`, structure `header.payload.signature` si configuré) |
| `DemandeCongeService` | `DemandeCongeServiceTest` | Création, validation RH, solde congés, refus, modification, suppression |
| `CongeUtil` | `CongeUtilTest` | Comptage jours, type congé, limites annuelles |
| `VisioService` | `VisioServiceTest` | Validation salon, connexion, planification, clôture instant/planifié, accès invités |

## Controllers couverts

| Controller | Fichier de test | Scénarios testés |
|------------|----------------|------------------|
| `TicketController` | `TicketControllerTest` | Session chat (`initData`, `fermerChat`), message vide ignoré |
| `TicketsListController` | `TicketsListControllerTest` | Titres « MES TICKETS » / filtre service |
| `BienvenueController` | `BienvenueControllerTest` | Validation champs login vides |
| `MenuController` | `MenuControllerTest` | Affichage / masquage badges notifications |
| (19 vues FXML) | `ControllerFxmlLoadTest` | Chargement sans erreur + controller non null |

## Types de scénarios

Pour chaque service, les tests couvrent :

1. **Happy path** — comportement nominal attendu
2. **Cas limites** — IDs invalides, valeurs nulles ou vides, bornes métier
3. **Exceptions** — erreurs métier (`IllegalArgumentException`, `SecurityException`…) et erreurs BDD simulées (`RuntimeException`)
4. **Règles métier** — stock, permissions, statuts tickets, montants paie, validations légales (SIRET, SMIC…)

## Exécution

Les tests activent automatiquement le mode silencieux (`-Dsteevejobs.test=true`) : pas de WebSocket, pas de chargements BDD async dans les controllers JavaFX, logs Hikari réduits. Sous IntelliJ, l'extension `ProjectTestExtension` est auto-détectée ; sinon ajouter `-Dsteevejobs.test=true` dans la configuration JUnit.

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

La CI GitHub Actions exécute `mvn -B test` sur **Windows, Linux et macOS** (Linux utilise `xvfb-run` pour le thread
JavaFX). Chaque job produit aussi une image jpackage dans `target/dist/` (artefacts téléchargeables).

### Compatibilité JDK 25+

Mockito s'appuie sur Byte Buddy. Le projet active le mode expérimental via :

- `.mvn/jvm.config` → `-Dnet.bytebuddy.experimental=true`
- `pom.xml` (plugin Surefire) → même flag dans `argLine`

Chaque classe de test service appelle aussi `MockitoJava25Support.enable()` dans un bloc `static`.

## Ajouter un test

1. Créer une classe `MonServiceTest.java` dans `src/test/java/service/` ou `MonControllerTest.java` dans `src/test/java/controller/`.
2. Annoter avec `@ExtendWith(MockitoExtension.class)` pour les services.
3. Mocker les DAO injectés dans le constructeur du service (pattern déjà utilisé dans les autres tests).
4. Pour un controller JavaFX : initialiser le toolkit via `JavaFxTestSupport.ensureInitialized()` et exécuter les assertions dans `JavaFxTestSupport.runOnFxThread(...)`.
5. Nommer les méthodes de façon explicite : `action_contexte_resultatAttendu`.

Exemple minimal (service) :

```java
@ExtendWith(MockitoExtension.class)
class MonServiceTest {

    static { MockitoJava25Support.enable(); }

    @Mock
    private MonDAO monDAO;

    private MonService service;

    @BeforeEach
    void setUp() {
        service = new MonService(monDAO);
    }

    @Test
    void maMethode_parametreInvalide_doitLeverException() {
        assertThrows(IllegalArgumentException.class, () -> service.maMethode(null));
    }
}
```
