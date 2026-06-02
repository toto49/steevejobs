# Transparence sur l'usage de l'intelligence artificielle

Conformément aux exigences du projet ESEO, ce document indique **où et comment** des outils d'intelligence artificielle (assistants de code, chatbots, génération de texte) ont été utilisés sur **SteeveJobs**, et dans quelle mesure le travail a été **revu par l'équipe**.

L'IA est un **outil d'aide** : les choix fonctionnels, l'architecture, la relecture, les tests manuels et la soutenance restent la responsabilité du groupe.

En fin de projet, l'équipe a aussi sollicité des **audits assistés par IA** (architecture, documentation, tests, build) pour repérer les derniers écarts et **les corriger au fur et à mesure**, après relecture et validation humaines.

---

## Synthèse

| Domaine | Usage de l'IA | Revue humaine |
|---------|---------------|---------------|
| Idées / cadrage | Ponctuel | Oui — sujet et périmètre validés par l'équipe |
| Rédaction & documentation | Oui, partiel | Oui — relecture, adaptation au projet réel |
| Code applicatif | Oui, partiel | Oui — intégration, tests, corrections manuelles |
| Tests unitaires | Oui, partiel | Oui — alignement sur les services réels, exécution `mvn test` |
| Débogage / visio / WebSocket | Oui, ponctuel | Oui — validation sur environnement NAS / client |

---

## 1. Idées et brainstorming

- Aide pour structurer des modules (RH, commerce, support, visio).
- Suggestions de nommage ou d'organisation de dossiers — **non retenues telles quelles** si incohérentes avec le projet existant.

---

## 2. Aide à la rédaction

- **README**, guides (`SETUP`, `GUIDE_UTILISATEUR`, `ARCHITECTURE`, `DOCKER`, `TESTS`) : rédaction ou reformulation assistée, puis **relue et corrigée** par l'équipe (liens, chemins, stack réelle).
- **Commentaires** dans le code : principalement rédigés par l'IA

---

## 3. Aide au code

Zones où l'IA a le plus souvent **proposé ou accéléré** du code (toujours revu avant merge) :

- Services métier : `VisioService`, refactorings DAO.
- Contrôleurs JavaFX : corrections de flux (visio).

Le groupe **ne commit pas** de code généré sans le comprendre, le tester et l'adapter aux conventions du dépôt.

---

## 4. Aide aux tests unitaires

- Génération ou enrichissement de tests JUnit 5 + Mockito dans `src/test/java/service/` (ex. `VisioServiceTest`, `CongeUtilTest`, `DemandeCongeServiceTest`).
- Chaque scénario a été **vérifié** contre le comportement réel du service correspondant.

Détail de la stratégie de tests : **[TESTS.md](TESTS.md)**.

---

## 5. Aide à la documentation technique

- Structuration des fichiers `docs/*.md` et table des matières du README.
- Ce fichier **`TRANSPARENCE_IA.md`** : rédigé pour répondre explicitement à l'exigence de transparence du sujet.

---

## 6. Ce qui n'a pas été délégué à l'IA

- Décisions d'équipe (sprints, backlog, budget fictif, répartition des rôles).
- Soutenances orales et démonstrations en présentiel.
- Configuration réelle du NAS Synology, DNS, certificats, Docker en production.
- Validation métier finale (RH, paie, permissions) par les membres du groupe.

---

## 7. Limites et bonnes pratiques adoptées

- Pas de copier-coller de code **sans** exécution des tests et relecture.
- Pas de secrets (`.env`, mots de passe, clés LiveKit/JWT) générés ou commités par l'IA.
