-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : mar. 02 juin 2026 à 10:18
-- Version du serveur : 10.11.11-MariaDB
-- Version de PHP : 8.2.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `steevejobs`
--

-- --------------------------------------------------------

--
-- Structure de la table `COMPOSER`
--

CREATE TABLE `COMPOSER` (
  `id_documents` int(11) NOT NULL,
  `id_produits` int(11) NOT NULL,
  `quantite` decimal(10,2) NOT NULL,
  `prix_vente` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `DEMANDE_CONGE`
--

CREATE TABLE `DEMANDE_CONGE` (
  `id_demande_conge` int(11) NOT NULL,
  `jour_debut` datetime NOT NULL,
  `jour_fin` datetime NOT NULL,
  `statut` enum('EN_ATTENTE','VALIDEE','REFUSEE') NOT NULL DEFAULT 'EN_ATTENTE',
  `commentaire_employe` text DEFAULT NULL,
  `commentaire_rh` text DEFAULT NULL,
  `date_demande` datetime NOT NULL,
  `id_user` int(11) NOT NULL,
  `id_planning` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `DOCUMENTS`
--

CREATE TABLE `DOCUMENTS` (
  `id_documents` int(11) NOT NULL,
  `type` enum('bon commande','devis','facture') NOT NULL,
  `date` date NOT NULL,
  `total_ht` decimal(10,2) NOT NULL,
  `total_ttc` decimal(10,2) NOT NULL,
  `statut` enum('à payer','en attente','payé','refusé') NOT NULL,
  `url` varchar(255) NOT NULL,
  `id_tiers` int(11) NOT NULL,
  `id_vendeur` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `FICHE_PAYE`
--

CREATE TABLE `FICHE_PAYE` (
  `id_paye` int(11) NOT NULL,
  `date` date NOT NULL,
  `url` varchar(255) NOT NULL,
  `id_user` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `HEURES_TRAVAIL`
--

CREATE TABLE `HEURES_TRAVAIL` (
  `id_heures` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `date_jour` date NOT NULL,
  `heure_debut_matin` time DEFAULT NULL,
  `heure_fin_matin` time DEFAULT NULL,
  `heure_debut_aprem` time DEFAULT NULL,
  `heure_fin_aprem` time DEFAULT NULL,
  `heures_total` time DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `MESSAGES`
--

CREATE TABLE `MESSAGES` (
  `id_messages` int(11) NOT NULL,
  `contenu` text NOT NULL,
  `piece_jointe` varchar(255) DEFAULT NULL,
  `date_envoi` datetime NOT NULL,
  `id_auteur` int(11) NOT NULL,
  `id_ticket` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `PERMISSION`
--

CREATE TABLE `PERMISSION` (
  `id_permission` int(11) NOT NULL,
  `code_action` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `PLANNING`
--

CREATE TABLE `PLANNING` (
  `id_planning` int(11) NOT NULL,
  `jour_debut` datetime NOT NULL,
  `jour_fin` datetime NOT NULL,
  `type` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `id_user` int(11) NOT NULL,
  `couleur` varchar(20) DEFAULT '#7298E0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `PRODUITS`
--

CREATE TABLE `PRODUITS` (
  `id_produits` int(11) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `prix_unitaire` decimal(10,2) DEFAULT NULL,
  `taux_tva` decimal(5,2) DEFAULT 20.00,
  `quantite` int(11) DEFAULT NULL,
  `poids` decimal(10,2) DEFAULT NULL,
  `actif` tinyint(1) DEFAULT 1,
  `seuil_alerte` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `ROLE_PERMISSION`
--

CREATE TABLE `ROLE_PERMISSION` (
  `nom_role` varchar(50) NOT NULL,
  `id_permission` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `TICKETS`
--

CREATE TABLE `TICKETS` (
  `id_tickets` int(11) NOT NULL,
  `sujet` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `service` varchar(255) NOT NULL,
  `statut` enum('EN_COURS','EN_ATTENTE','FERME') NOT NULL DEFAULT 'EN_ATTENTE',
  `non_lu_admin` tinyint(1) NOT NULL DEFAULT 0,
  `non_lu_auteur` tinyint(1) NOT NULL DEFAULT 0,
  `date_ouverture` datetime NOT NULL,
  `id_auteur` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `TIERS`
--

CREATE TABLE `TIERS` (
  `id_tiers` int(11) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `prenom` varchar(255) DEFAULT NULL,
  `type` enum('client','fournisseur') NOT NULL,
  `email` varchar(255) NOT NULL,
  `adresse` varchar(255) NOT NULL,
  `tel` varchar(255) NOT NULL,
  `siret` varchar(20) DEFAULT NULL,
  `num_tva` varchar(50) DEFAULT NULL,
  `actif` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `USER`
--

CREATE TABLE `USER` (
  `id_user` int(11) NOT NULL,
  `nom` varchar(255) DEFAULT NULL,
  `prenom` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `mdp` varchar(255) NOT NULL,
  `adresse` varchar(255) DEFAULT NULL,
  `tel` varchar(255) DEFAULT NULL,
  `role` varchar(50) NOT NULL,
  `poste` varchar(255) NOT NULL,
  `actif` tinyint(1) DEFAULT 1,
  `tentatives_echouees` int(11) DEFAULT 0,
  `bloque_jusqua` datetime DEFAULT NULL,
  `date_dernier_echec` datetime DEFAULT NULL,
  `taux` int(11) DEFAULT NULL,
  `taux_patronal` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `VISIO`
--

CREATE TABLE `VISIO` (
  `id` int(11) NOT NULL,
  `room_name` varchar(100) NOT NULL,
  `createur_id` int(11) NOT NULL,
  `statut` enum('PROGRAMMEE','EN_COURS','TERMINE') DEFAULT 'PROGRAMMEE',
  `type_reunion` enum('INSTANTANEE','PLANIFIEE') NOT NULL DEFAULT 'PLANIFIEE',
  `heure_programmee` datetime DEFAULT NULL,
  `heure_debut` timestamp NULL DEFAULT NULL,
  `heure_fin` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `VISIO_INVITATIONS`
--

CREATE TABLE `VISIO_INVITATIONS` (
  `id` int(11) NOT NULL,
  `visio_id` int(11) NOT NULL,
  `employe_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `COMPOSER`
--
ALTER TABLE `COMPOSER`
  ADD PRIMARY KEY (`id_documents`,`id_produits`),
  ADD KEY `id_produits` (`id_produits`);

--
-- Index pour la table `DEMANDE_CONGE`
--
ALTER TABLE `DEMANDE_CONGE`
  ADD PRIMARY KEY (`id_demande_conge`),
  ADD KEY `idx_demande_conge_user` (`id_user`),
  ADD KEY `idx_demande_conge_statut` (`statut`),
  ADD KEY `idx_demande_conge_planning` (`id_planning`);

--
-- Index pour la table `DOCUMENTS`
--
ALTER TABLE `DOCUMENTS`
  ADD PRIMARY KEY (`id_documents`),
  ADD UNIQUE KEY `id_documents` (`id_documents`),
  ADD KEY `id_tiers` (`id_tiers`),
  ADD KEY `id_vendeur` (`id_vendeur`),
  ADD KEY `idx_docs_statut` (`statut`),
  ADD KEY `idx_docs_type` (`type`),
  ADD KEY `idx_docs_date` (`date`),
  ADD KEY `idx_docs_recherche_rapide` (`type`,`statut`);

--
-- Index pour la table `FICHE_PAYE`
--
ALTER TABLE `FICHE_PAYE`
  ADD PRIMARY KEY (`id_paye`),
  ADD UNIQUE KEY `id_paye` (`id_paye`),
  ADD KEY `id_user` (`id_user`),
  ADD KEY `idx_paye_mois` (`date`);

--
-- Index pour la table `HEURES_TRAVAIL`
--
ALTER TABLE `HEURES_TRAVAIL`
  ADD PRIMARY KEY (`id_heures`),
  ADD UNIQUE KEY `unique_user_date` (`id_user`,`date_jour`);

--
-- Index pour la table `MESSAGES`
--
ALTER TABLE `MESSAGES`
  ADD PRIMARY KEY (`id_messages`),
  ADD UNIQUE KEY `id_messages` (`id_messages`),
  ADD KEY `id_auteur` (`id_auteur`),
  ADD KEY `id_ticket` (`id_ticket`),
  ADD KEY `idx_messages_date` (`date_envoi`);

--
-- Index pour la table `PERMISSION`
--
ALTER TABLE `PERMISSION`
  ADD PRIMARY KEY (`id_permission`),
  ADD UNIQUE KEY `id_permission` (`id_permission`),
  ADD UNIQUE KEY `code_action` (`code_action`);

--
-- Index pour la table `PLANNING`
--
ALTER TABLE `PLANNING`
  ADD PRIMARY KEY (`id_planning`),
  ADD UNIQUE KEY `id_planning` (`id_planning`),
  ADD KEY `id_user` (`id_user`),
  ADD KEY `idx_planning_dates` (`jour_debut`,`jour_fin`);

--
-- Index pour la table `PRODUITS`
--
ALTER TABLE `PRODUITS`
  ADD PRIMARY KEY (`id_produits`),
  ADD UNIQUE KEY `id_produits` (`id_produits`),
  ADD KEY `idx_produits_nom` (`nom`),
  ADD KEY `idx_produits_actif` (`actif`);

--
-- Index pour la table `ROLE_PERMISSION`
--
ALTER TABLE `ROLE_PERMISSION`
  ADD PRIMARY KEY (`nom_role`,`id_permission`),
  ADD KEY `id_permission` (`id_permission`);

--
-- Index pour la table `TICKETS`
--
ALTER TABLE `TICKETS`
  ADD PRIMARY KEY (`id_tickets`),
  ADD UNIQUE KEY `id_tickets` (`id_tickets`),
  ADD KEY `id_auteur` (`id_auteur`),
  ADD KEY `idx_tickets_statut` (`statut`),
  ADD KEY `idx_tickets_date` (`date_ouverture`);

--
-- Index pour la table `TIERS`
--
ALTER TABLE `TIERS`
  ADD PRIMARY KEY (`id_tiers`),
  ADD UNIQUE KEY `id_tiers` (`id_tiers`),
  ADD KEY `idx_tiers_nom` (`nom`),
  ADD KEY `idx_tiers_siret` (`siret`),
  ADD KEY `idx_tiers_type_actif` (`type`,`actif`);

--
-- Index pour la table `USER`
--
ALTER TABLE `USER`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `id_user` (`id_user`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_user_role` (`role`),
  ADD KEY `idx_user_actif` (`actif`),
  ADD KEY `idx_user_nom_prenom` (`nom`,`prenom`);

--
-- Index pour la table `VISIO`
--
ALTER TABLE `VISIO`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `room_name` (`room_name`),
  ADD KEY `fk_visio_createur` (`createur_id`);

--
-- Index pour la table `VISIO_INVITATIONS`
--
ALTER TABLE `VISIO_INVITATIONS`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_invitation` (`visio_id`,`employe_id`),
  ADD KEY `fk_invitation_user` (`employe_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `DEMANDE_CONGE`
--
ALTER TABLE `DEMANDE_CONGE`
  MODIFY `id_demande_conge` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `DOCUMENTS`
--
ALTER TABLE `DOCUMENTS`
  MODIFY `id_documents` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `FICHE_PAYE`
--
ALTER TABLE `FICHE_PAYE`
  MODIFY `id_paye` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `HEURES_TRAVAIL`
--
ALTER TABLE `HEURES_TRAVAIL`
  MODIFY `id_heures` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `MESSAGES`
--
ALTER TABLE `MESSAGES`
  MODIFY `id_messages` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `PERMISSION`
--
ALTER TABLE `PERMISSION`
  MODIFY `id_permission` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `PLANNING`
--
ALTER TABLE `PLANNING`
  MODIFY `id_planning` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `PRODUITS`
--
ALTER TABLE `PRODUITS`
  MODIFY `id_produits` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `TICKETS`
--
ALTER TABLE `TICKETS`
  MODIFY `id_tickets` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `TIERS`
--
ALTER TABLE `TIERS`
  MODIFY `id_tiers` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `USER`
--
ALTER TABLE `USER`
  MODIFY `id_user` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `VISIO`
--
ALTER TABLE `VISIO`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `VISIO_INVITATIONS`
--
ALTER TABLE `VISIO_INVITATIONS`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `COMPOSER`
--
ALTER TABLE `COMPOSER`
  ADD CONSTRAINT `COMPOSER_ibfk_1` FOREIGN KEY (`id_documents`) REFERENCES `DOCUMENTS` (`id_documents`) ON DELETE CASCADE,
  ADD CONSTRAINT `COMPOSER_ibfk_2` FOREIGN KEY (`id_produits`) REFERENCES `PRODUITS` (`id_produits`) ON DELETE CASCADE;

--
-- Contraintes pour la table `DEMANDE_CONGE`
--
ALTER TABLE `DEMANDE_CONGE`
  ADD CONSTRAINT `DEMANDE_CONGE_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `DOCUMENTS`
--
ALTER TABLE `DOCUMENTS`
  ADD CONSTRAINT `DOCUMENTS_ibfk_1` FOREIGN KEY (`id_tiers`) REFERENCES `TIERS` (`id_tiers`),
  ADD CONSTRAINT `DOCUMENTS_ibfk_2` FOREIGN KEY (`id_vendeur`) REFERENCES `USER` (`id_user`);

--
-- Contraintes pour la table `FICHE_PAYE`
--
ALTER TABLE `FICHE_PAYE`
  ADD CONSTRAINT `FICHE_PAYE_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `HEURES_TRAVAIL`
--
ALTER TABLE `HEURES_TRAVAIL`
  ADD CONSTRAINT `fk_heures_user` FOREIGN KEY (`id_user`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `MESSAGES`
--
ALTER TABLE `MESSAGES`
  ADD CONSTRAINT `MESSAGES_ibfk_1` FOREIGN KEY (`id_auteur`) REFERENCES `USER` (`id_user`),
  ADD CONSTRAINT `MESSAGES_ibfk_2` FOREIGN KEY (`id_ticket`) REFERENCES `TICKETS` (`id_tickets`) ON DELETE CASCADE;

--
-- Contraintes pour la table `PLANNING`
--
ALTER TABLE `PLANNING`
  ADD CONSTRAINT `PLANNING_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `ROLE_PERMISSION`
--
ALTER TABLE `ROLE_PERMISSION`
  ADD CONSTRAINT `ROLE_PERMISSION_ibfk_1` FOREIGN KEY (`id_permission`) REFERENCES `PERMISSION` (`id_permission`) ON DELETE CASCADE;

--
-- Contraintes pour la table `TICKETS`
--
ALTER TABLE `TICKETS`
  ADD CONSTRAINT `TICKETS_ibfk_1` FOREIGN KEY (`id_auteur`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `VISIO`
--
ALTER TABLE `VISIO`
  ADD CONSTRAINT `fk_visio_createur` FOREIGN KEY (`createur_id`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE;

--
-- Contraintes pour la table `VISIO_INVITATIONS`
--
ALTER TABLE `VISIO_INVITATIONS`
  ADD CONSTRAINT `fk_invitation_user` FOREIGN KEY (`employe_id`) REFERENCES `USER` (`id_user`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_invitation_visio` FOREIGN KEY (`visio_id`) REFERENCES `VISIO` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
