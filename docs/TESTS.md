# Stratégie de tests unitaires — SteeveJobs

## Objectif

Couvrir la logique métier des **services Java** (validation, sécurité, règles métier, gestion d'erreurs) **sans dépendre de MySQL**, conformément aux exigences du projet (tests unitaires variés et utiles).

Les tests mockent les DAO avec Mockito : aucune base de données n'est requise pour les exécuter.

## Organisation du code de test

Toute la suite de tests se trouve sous `src/test/java/service/` :

```
src/test/java/
├── DatabaseConnexion.java          # utilitaire manuel (vérif connexion BDD), pas un test JUnit
└── service/
    ├── ConnexionServiceTest.java
    ├── DocumentServiceTest.java
    ├── FichePayeServiceTest.java
    ├── HeuresTravailServiceTest.java
    ├── MessageServiceTest.java
    ├── PermissionServiceTest.java
    ├── PlanningServiceTest.java
    ├── ProduitServiceTest.java
    ├── TicketServiceTest.java
    ├── TiersServiceTest.java
    ├── UserServiceTest.java
    └── support/
        ├── MockitoJava25Support.java   # compatibilité Mockito / JDK 25+
        └── TestDataFactory.java        # jeux de données réutilisables
```

**11 classes de test**, **78 scénarios** au total.

## Technologies

| Outil | Rôle |
|-------|------|
| JUnit 5 | Framework de test |
| Mockito 5.20 | Mocks des DAO et services PDF |
| Maven Surefire | Exécution via `mvn test` |

## Services couverts

| Service | Fichier de test | Scénarios testés |
|---------|----------------|------------------|
| `UserService` | `UserServiceTest` | Création, email dupliqué, authentification (succès, échec, compte désactivé), hash SHA-256, activation/désactivation, mots de passe |
| `PermissionService` | `PermissionServiceTest` | Lecture permissions, assignation/révocation, protection SuperAdmin, validation des paramètres |
| `ProduitService` | `ProduitServiceTest` | Ajout/modification, validation nom/prix, mise à jour du stock, produit introuvable |
| `TiersService` | `TiersServiceTest` | Validation email/SIRET/nom, CRUD, erreurs BDD simulées |
| `PlanningService` | `PlanningServiceTest` | Cohérence des dates, utilisateur obligatoire, ajout/modification/suppression, erreurs BDD |
| `DocumentService` | `DocumentServiceTest` | Validation document, export PDF, suppression, changement de statut |
| `FichePayeService` | `FichePayeServiceTest` | Génération paie, doublon mensuel, salaire/taux invalides, SMIC minimum |
| `HeuresTravailService` | `HeuresTravailServiceTest` | Sauvegarde et lecture des heures, échec DAO |
| `TicketServiceImpl` | `TicketServiceTest` | Création ticket, messages, changement de statut, formatage des dates |
| `MessageServiceImpl` | `MessageServiceTest` | Lecture par ID/auteur, suppression, erreurs SQL |
| `ConnexionService` | `ConnexionServiceTest` | Génération de mots de passe aléatoires |

## Types de scénarios

Pour chaque service, les tests couvrent :

1. **Happy path** — comportement nominal attendu
2. **Cas limites** — IDs invalides, valeurs nulles ou vides, bornes métier
3. **Exceptions** — erreurs métier (`IllegalArgumentException`, `SecurityException`…) et erreurs BDD simulées (`RuntimeException`)
4. **Règles métier** — stock, permissions, statuts tickets, montants paie, validations légales (SIRET, SMIC…)

## Exécution

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

La CI GitHub Actions exécute la même commande sur chaque push/PR (`mvn -B test`).

### Compatibilité JDK 25+

Mockito s'appuie sur Byte Buddy. Le projet active le mode expérimental via :

- `.mvn/jvm.config` → `-Dnet.bytebuddy.experimental=true`
- `pom.xml` (plugin Surefire) → même flag dans `argLine`

Chaque classe de test appelle aussi `MockitoJava25Support.enable()` dans un bloc `static`.

## Ajouter un test

1. Créer une classe `MonServiceTest.java` dans `src/test/java/service/`.
2. Annoter avec `@ExtendWith(MockitoExtension.class)`.
3. Mocker les DAO injectés dans le constructeur du service (pattern déjà utilisé dans les autres tests).
4. Nommer les méthodes de façon explicite : `action_contexte_resultatAttendu`.

Exemple minimal :

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

## Transparence IA

Une partie de cette suite de tests a été générée/assistée par IA, puis revue pour coller aux services réels du projet.
