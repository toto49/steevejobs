# Stratégie de tests unitaires — SteeveJobs

## Objectif

Couvrir la logique métier des services Java (validation, sécurité, règles métier, gestion d'erreurs) sans dépendre de MySQL, conformément aux exigences du projet (tests unitaires variés et utiles).

## Technologies

- JUnit 5
- Mockito (mocks des DAO)
- Maven Surefire (`mvn test`)

## Services couverts

| Service | Scénarios testés |
|---------|------------------|
| `UserService` | Création, authentification, hash SHA-256, rôles, activation/désactivation |
| `PermissionService` | Permissions utilisateur, rôles, SuperAdmin protégé |
| `ProduitService` | CRUD métier, stock, seuils, produits vrac |
| `TiersService` | Validation email/SIRET, CRUD |
| `PlanningService` | Cohérence des dates, erreurs BDD |
| `DocumentService` | Validation document, export PDF, statuts |
| `FichePayeService` | Génération paie, doublons, montants |
| `HeuresTravailService` | Sauvegarde et lecture des heures |
| `TicketServiceImpl` | Workflow tickets/messages |
| `MessageServiceImpl` | Lecture/suppression messages |
| `ConnexionService` | Génération de mots de passe aléatoires |

## Types de scénarios

Pour chaque service, les tests couvrent :

1. **Happy path** — comportement nominal attendu
2. **Edge cases** — IDs invalides, valeurs limites, null
3. **Exceptions** — erreurs métier et erreurs BDD simulées
4. **Règles métier** — stock, permissions, statuts tickets, montants paie

## Exécution

```bash
./mvnw.cmd test
```

Sur JDK 25+, le projet active automatiquement le mode Byte Buddy expérimental requis par Mockito.

## Transparence IA

Une partie de cette suite de tests a été générée/assistée par IA, puis revue pour coller aux services réels du projet.
