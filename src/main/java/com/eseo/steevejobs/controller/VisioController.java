package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.UserService;
import com.eseo.steevejobs.service.VisioService;
import com.eseo.steevejobs.util.TestRuntime;
import com.eseo.steevejobs.service.WebSocketService;
import com.eseo.steevejobs.service.WebSocketUiBridge;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * Contrôleur FXML de la visioconférence (connexion, planification, listes de réunions).
 * Liaisons FXML : champs salon, planification, tableaux actifs/archives, recherche d'invités.
 * Communique avec le NAS via {@link WebSocketService} (tokens LiveKit, planification, suppression).
 */
public class VisioController {

    /** Format d'affichage des dates-heures de réunion dans les tableaux. */
    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Nombre de réunions affichées par page de pagination. */
    private static final int ITEMS_PER_PAGE = 8;
    /** Nombre maximal de suggestions d'employés dans le menu d'invitation. */
    private static final int MAX_SUGGESTIONS = 8;
    /** Hauteur fixe d'une ligne de tableau de réunions (pixels). */
    private static final double HAUTEUR_LIGNE_TABLE = 46;
    /** Largeur de la colonne bouton suppression (pixels). */
    private static final double LARGEUR_COL_SUPPRESSION = 52;

    /** Type de message WebSocket pour supprimer un salon. */
    private static final String TYPE_DELETE_VISIO = "DELETE_VISIO";
    /** Type de message WebSocket pour demander un token LiveKit. */
    private static final String TYPE_REQUEST_VISIO_TOKEN = "REQUEST_VISIO_TOKEN";
    /** Type de message WebSocket pour planifier une réunion. */
    private static final String TYPE_PLANIFY_VISIO = "PLANIFY_VISIO";
    /** Type de message WebSocket pour lister les réunions de l'utilisateur. */
    private static final String TYPE_GET_MY_VISIOS = "GET_MY_VISIOS";

    /** Classe CSS du menu contextuel de suggestions d'invités. */
    private static final String CSS_MENU_SUGGESTION = "visio-suggestion-menu";
    /** Classe CSS d'une entrée de suggestion d'invité. */
    private static final String CSS_ITEM_SUGGESTION = "visio-suggestion-item";

    /** Instance du contrôleur actuellement affichée (callbacks WebSocket). */
    private static VisioController activeInstance;
    /** Variables d'environnement (.env) pour l'URL LiveKit et la configuration NAS. */
    private static Dotenv dotenv;

    /** Service de gestion des utilisateurs. */
    private final UserService userService = new UserService();
    /** Service de gestion des visioconférences. */
    private final VisioService visioService = new VisioService();
    /** Menu contextuel des suggestions d'invités. */
    private final ContextMenu menuSuggestions = new ContextMenu();

    /** Identifiants des invités sélectionnés pour la planification. */
    private final List<Integer> listeIdInvitesSelectionnes = new ArrayList<>();
    /** Liste complète des réunions reçues du serveur. */
    private final ObservableList<Visio> listeToutesReunions = FXCollections.observableArrayList();
    /** Réunions actives affichées dans le tableau principal. */
    private final ObservableList<Visio> listeReunionsActives = FXCollections.observableArrayList();
    /** Réunions archivées affichées dans le tableau secondaire. */
    private final ObservableList<Visio> listeReunionsArchives = FXCollections.observableArrayList();
    /** Liste des employés disponibles pour invitation. */
    private final ObservableList<User> tousLesEmployes = FXCollections.observableArrayList();

    /** Indique si une requête de token visio est en cours. */
    private boolean attenteTokenVisio = false;
    /** Nom du salon en attente de connexion après réponse WebSocket. */
    private String roomNameEnAttente = "";

    /** Champ de saisie du nom de salon à rejoindre. */
    @FXML private TextField txtRoomName;
    /** Bouton de connexion à un salon libre. */
    @FXML private Button btnRejoindre;

    /** Champ de saisie du nom de salon à planifier. */
    @FXML private TextField txtPlanifRoomName;
    /** Sélecteur de date de la réunion planifiée. */
    @FXML private DatePicker datePlanif;
    /** Spinner de sélection de l'heure de la réunion. */
    @FXML private Spinner<Integer> spinnerHeure;
    /** Spinner de sélection des minutes de la réunion. */
    @FXML private Spinner<Integer> spinnerMinute;
    /** Bouton de planification d'une réunion. */
    @FXML private Button btnPlanifier;

    /** Colonne nom du salon dans le tableau des réunions actives. */
    @FXML private TableColumn<Visio, String> colRoomName;
    /** Colonne statut dans le tableau des réunions actives. */
    @FXML private TableColumn<Visio, String> colStatut;
    /** Colonne date dans le tableau des réunions actives. */
    @FXML private TableColumn<Visio, String> colDate;

    /** Bouton de connexion à la réunion sélectionnée. */
    @FXML private Button btnRejoindreSelection;
    /** Onglets séparant réunions actives et archives. */
    @FXML
    private TabPane tabPaneReunions;
    /** Tableau des réunions actives. */
    @FXML
    private TableView<Visio> tableReunionsActives;
    /** Tableau des réunions archivées. */
    @FXML
    private TableView<Visio> tableReunionsArchives;
    /** Colonne nom du salon dans le tableau des archives. */
    @FXML
    private TableColumn<Visio, String> colRoomNameArchive;
    /** Colonne statut dans le tableau des archives. */
    @FXML
    private TableColumn<Visio, String> colStatutArchive;
    /** Colonne date dans le tableau des archives. */
    @FXML
    private TableColumn<Visio, String> colDateArchive;
    /** Pagination du tableau des réunions actives. */
    @FXML
    private Pagination paginationReunions;
    /** Pagination du tableau des réunions archivées. */
    @FXML
    private Pagination paginationReunionsArchives;

    /** Champ de recherche d'un invité à ajouter. */
    @FXML
    private TextField txtRechercheInvite;
    /** Conteneur des badges des invités sélectionnés. */
    @FXML
    private FlowPane flowPaneInvitesBadges;

    /**
     * Retourne l'instance active du contrôleur visio.
     *
     * @return contrôleur courant ou {@code null}
     */
    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    /**
     * Charge la configuration Dotenv de manière paresseuse.
     *
     * @return instance Dotenv
     */
    private static Dotenv getDotenv() {
        if (dotenv == null) {
            dotenv = Dotenv.load();
        }
        return dotenv;
    }

    /**
     * Initialise composants, callbacks WebSocket et charge la liste des réunions.
     * Ne charge pas les données en mode test.
     */
    @FXML
    public void initialize() {
        activeInstance = this;
        attenteTokenVisio = false;
        if (TestRuntime.isEnabled()) {
            return;
        }
        enregistrerCallbacksWebSocket();

        initialiserMenuSuggestions();

        initialiserTablesEtPagination();
        initialiserDatePicker();
        initialiserDateEtHeure();
        chargerEmployes();
        initialiserRechercheInvites();

        rafraichirListeReunions();
    }

    /**
     * Libère les callbacks WebSocket et l'état de connexion lors d'un changement de page.
     */
    public void couperController() {
        attenteTokenVisio = false;
        roomNameEnAttente = "";
        WebSocketUiBridge.getInstance().clearVisioCallbacks();
        activeInstance = null;
        menuSuggestions.hide();
    }

    /**
     * Enregistre les callbacks WebSocket pour la visioconférence.
     */
    private void enregistrerCallbacksWebSocket() {
        WebSocketUiBridge.getInstance().setVisioCallbacks(new WebSocketUiBridge.VisioCallbacks() {
            /**
             * Reçoit un token LiveKit et lance la connexion au salon.
             *
             * @param token jeton LiveKit
             * @param roomName nom de la salle
             */
            @Override
            public void onTokenSuccess(String token, String roomName) {
                recevoirTokenEtLancer(token, roomName);
            }

            /**
             * Affiche un message de retour serveur (succès ou erreur).
             *
             * @param message message serveur
             */
            @Override
            public void onVisioMessage(String message) {
                recevoirErreurVisio(message);
            }

            /**
             * Met à jour les tableaux avec la liste des réunions reçue.
             *
             * @param reunions liste JSON des réunions
             */
            @Override
            public void onReunionsList(JSONArray reunions) {
                if (reunions != null) {
                    recevoirListeReunions(reunions);
                }
            }

            /**
             * Traite le retour serveur après une suppression de salon.
             *
             * @param message retour de suppression
             */
            @Override
            public void onSalonDeleted(String message) {
                recevoirSuppressionSalon(message);
            }

            /** Demande un rechargement de la liste des réunions. */
            @Override
            public void onRefreshReunionsRequested() {
                rafraichirListeReunions();
            }
        });
    }

    /**
     * Configure le menu contextuel des suggestions d'invités.
     */
    private void initialiserMenuSuggestions() {
        menuSuggestions.getStyleClass().add(CSS_MENU_SUGGESTION);
        menuSuggestions.setAutoHide(true);
        menuSuggestions.setHideOnEscape(true);
    }

    /**
     * Charge la liste des employés actifs depuis la base.
     */
    private void chargerEmployes() {
        try {
            tousLesEmployes.setAll(userService.getActiveUsers());
            trierEmployes();
        } catch (SQLException e) {
            afficherStatut("Impossible de charger la liste des employés.", true);
        }
    }

    /**
     * Trie la liste des employés par nom complet.
     */
    private void trierEmployes() {
        FXCollections.sort(
                tousLesEmployes,
                Comparator.comparing(
                        user -> construireNomComplet(user).toLowerCase()
                )
        );
    }

    /**
     * Configure le champ de recherche et le menu de suggestions d'invités.
     */
    private void initialiserRechercheInvites() {
        if (txtRechercheInvite == null) {
            return;
        }

        txtRechercheInvite.textProperty().addListener((obs, oldText, newText) ->
                gererMenuSuggestionsEnTempsReel(newText)
        );

        txtRechercheInvite.setOnAction(event -> ajouterInviteManuel());

        txtRechercheInvite.focusedProperty().addListener((obs, ancien, focusActif) -> {
            if (!focusActif) {
                menuSuggestions.hide();
            }
        });
    }

    /**
     * Initialise les tableaux de réunions actives et archives ainsi que leur pagination.
     */
    private void initialiserTablesEtPagination() {
        initialiserTableActives();
        initialiserTableArchives();
        initialiserPaginationArchives();
    }

    /**
     * Configure le tableau des réunions actives et sa pagination.
     */
    private void initialiserTableActives() {
        if (tableReunionsActives == null) {
            return;
        }

        colRoomName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoom_name())
        );

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getStatut() != null
                                ? data.getValue().getStatut().getValeur()
                                : ""
                )
        );

        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(formaterDate(data.getValue().getHeure_programmee()))
        );

        configurerTableVisio(tableReunionsActives);
        ajouterColonneSuppressionSiAbsente(tableReunionsActives);

        if (paginationReunions != null) {
            paginationReunions.setPageFactory(this::creerPageReunions);
        }
    }

    /**
     * Configure la pagination du tableau des réunions archivées.
     */
    private void initialiserPaginationArchives() {
        if (paginationReunionsArchives != null) {
            paginationReunionsArchives.setPageFactory(this::creerPageReunionsArchives);
        }
    }

    /**
     * Configure le tableau des réunions archivées.
     */
    private void initialiserTableArchives() {
        if (tableReunionsArchives == null) {
            return;
        }

        colRoomNameArchive.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoom_name())
        );

        colStatutArchive.setCellValueFactory(data ->
                new SimpleStringProperty("Terminée")
        );

        colDateArchive.setCellValueFactory(data ->
                new SimpleStringProperty(formaterDate(data.getValue().getHeure_programmee()))
        );

        configurerTableVisio(tableReunionsArchives);
        ajouterColonneSuppressionSiAbsente(tableReunionsArchives);
    }

    /**
     * Applique les paramètres d'affichage communs à un tableau de réunions.
     *
     * @param table tableau à configurer
     */
    private void configurerTableVisio(TableView<Visio> table) {
        table.setFixedCellSize(HAUTEUR_LIGNE_TABLE);
        table.setPlaceholder(new Label("Aucune réunion à afficher."));
    }

    /**
     * Ajoute une colonne de suppression si elle n'est pas déjà présente.
     *
     * @param table tableau cible
     */
    private void ajouterColonneSuppressionSiAbsente(TableView<Visio> table) {
        boolean colonneDejaPresente = table.getColumns().stream()
                .anyMatch(col -> "colSuppressionVisio".equals(col.getId()));

        if (colonneDejaPresente) {
            return;
        }

        TableColumn<Visio, Void> colSupprimer = new TableColumn<>("");
        colSupprimer.setId("colSuppressionVisio");
        colSupprimer.setPrefWidth(LARGEUR_COL_SUPPRESSION);
        colSupprimer.setMinWidth(LARGEUR_COL_SUPPRESSION);
        colSupprimer.setMaxWidth(LARGEUR_COL_SUPPRESSION);
        colSupprimer.setSortable(false);
        colSupprimer.setResizable(false);

        colSupprimer.setCellFactory(col -> new TableCell<>() {
            private final Button btnSupprimer = creerBoutonSuppression();

            {
                btnSupprimer.setOnAction(event -> {
                    Visio visio = getVisioCourante();
                    if (visio != null) {
                        confirmerEtSupprimerSalon(visio);
                    }
                });
            }

            /**
             * Affiche le bouton de suppression si l'utilisateur est créateur du salon.
             *
             * @param item non utilisé
             * @param empty {@code true} si la ligne est hors plage
             */
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                Visio visio = empty ? null : getVisioCourante();
                if (visio == null) {
                    setGraphic(null);
                    return;
                }

                btnSupprimer.getStyleClass().removeAll(
                        "visio-btn-delete",
                        "visio-btn-delete-disabled"
                );

                boolean createur = estCreateurSalon(visio);
                btnSupprimer.setDisable(!createur);
                btnSupprimer.getStyleClass().add(
                        createur ? "visio-btn-delete" : "visio-btn-delete-disabled"
                );

                setGraphic(btnSupprimer);
                setAlignment(Pos.CENTER);
            }

            /**
             * Retourne la réunion associée à la ligne courante du tableau.
             *
             * @return visio de la ligne ou {@code null} si index invalide
             */
            private Visio getVisioCourante() {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) {
                    return null;
                }
                return getTableView().getItems().get(index);
            }
        });

        table.getColumns().add(colSupprimer);
    }

    /**
     * Crée le bouton graphique de suppression d'une réunion.
     *
     * @return bouton poubelle
     */
    private Button creerBoutonSuppression() {
        return new Button("🗑");
    }

    /**
     * Indique si l'utilisateur connecté est le créateur du salon.
     *
     * @param visio réunion évaluée
     * @return {@code true} si l'utilisateur est créateur
     */
    private boolean estCreateurSalon(Visio visio) {
        User utilisateur = SessionService.getUtilisateurConnecte();
        return utilisateur != null
                && visio != null
                && visio.getCreateur_id() == utilisateur.getId();
    }

    /**
     * Demande confirmation puis envoie la suppression d'un salon au serveur.
     *
     * @param visio réunion à supprimer
     */
    private void confirmerEtSupprimerSalon(Visio visio) {
        if (!estCreateurSalon(visio)) {
            afficherStatut("Seul le créateur du salon peut le supprimer.", true);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer le salon");
        confirmation.setHeaderText("Supprimer « " + visio.getRoom_name() + " » ?");
        confirmation.setContentText("Cette action retirera le salon pour tous les participants.");

        Optional<ButtonType> choix = confirmation.showAndWait();
        if (choix.isEmpty() || choix.get() != ButtonType.OK) {
            return;
        }

        envoyerSuppressionSalon(visio.getRoom_name());
    }

    /**
     * Envoie une requête WebSocket de suppression de salon.
     *
     * @param roomName nom du salon à supprimer
     */
    private void envoyerSuppressionSalon(String roomName) {
        JSONObject requete = new JSONObject();
        requete.put("type", TYPE_DELETE_VISIO);
        requete.put("roomName", roomName);

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Suppression du salon en cours...", false);
    }

    /**
     * Affiche le retour serveur après une demande de suppression de salon.
     *
     * @param message message de succès ou d'erreur renvoyé par le serveur
     */
    public void recevoirSuppressionSalon(String message) {
        Platform.runLater(() -> {
            boolean succes = message != null && message.startsWith("✅");
            afficherStatut(message, !succes);

            if (succes) {
                rafraichirListeReunions();
                return;
            }

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Suppression impossible");
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message.replace("❌ ", "") : "Erreur inconnue.");
            alert.showAndWait();
        });
    }

    /**
     * Formate une date de réunion pour l'affichage dans les tableaux.
     *
     * @param heure date-heure source
     * @return chaîne formatée ou tiret si absente
     */
    private String formaterDate(LocalDateTime heure) {
        return heure != null ? heure.format(DATE_HEURE_FMT) : "—";
    }

    /**
     * Fabrique une page de pagination pour les réunions actives.
     *
     * @param pageIndex index de la page demandée
     * @return nœud vide (le contenu est appliqué au tableau)
     */
    private Node creerPageReunions(int pageIndex) {
        afficherPageReunions(pageIndex);
        return new VBox();
    }

    /**
     * Fabrique une page de pagination pour les réunions archivées.
     *
     * @param pageIndex index de la page demandée
     * @return nœud vide (le contenu est appliqué au tableau)
     */
    private Node creerPageReunionsArchives(int pageIndex) {
        afficherPageReunionsArchives(pageIndex);
        return new VBox();
    }

    /**
     * Affiche une page du tableau des réunions actives.
     *
     * @param pageIndex index de la page
     */
    private void afficherPageReunions(int pageIndex) {
        if (tableReunionsActives == null) {
            return;
        }

        appliquerPageTable(
                tableReunionsActives,
                listeReunionsActives,
                pageIndex
        );
    }

    /**
     * Affiche une page du tableau des réunions archivées.
     *
     * @param pageIndex index de la page
     */
    private void afficherPageReunionsArchives(int pageIndex) {
        if (tableReunionsArchives == null) {
            return;
        }

        appliquerPageTable(
                tableReunionsArchives,
                listeReunionsArchives,
                pageIndex
        );
    }

    /**
     * Applique une tranche paginée à un tableau de réunions.
     *
     * @param table tableau cible
     * @param source liste complète des réunions
     * @param pageIndex index de la page
     */
    private void appliquerPageTable(TableView<Visio> table, List<Visio> source, int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        if (fromIndex >= source.size()) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }

        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, source.size());
        table.setItems(FXCollections.observableArrayList(source.subList(fromIndex, toIndex)));
    }

    /**
     * Met à jour les deux composants de pagination (actives et archives).
     */
    private void mettreAJourPagination() {
        mettreAJourPaginationListe(
                paginationReunions,
                listeReunionsActives,
                this::afficherPageReunions
        );
        mettreAJourPaginationListe(
                paginationReunionsArchives,
                listeReunionsArchives,
                this::afficherPageReunionsArchives
        );
    }

    /**
     * Recalcule le nombre de pages et affiche la page courante.
     *
     * @param pagination composant de pagination
     * @param source liste source des réunions
     * @param afficherPage callback d'affichage de page
     */
    private void mettreAJourPaginationListe(
            Pagination pagination,
            List<Visio> source,
            IntConsumer afficherPage
    ) {
        if (pagination == null) {
            return;
        }

        int taille = source.size();
        boolean pageSuivanteDisponible = taille > ITEMS_PER_PAGE;
        int pageCount = pageSuivanteDisponible
                ? (int) Math.ceil((double) taille / ITEMS_PER_PAGE)
                : 1;

        pagination.setVisible(pageSuivanteDisponible);
        pagination.setManaged(pageSuivanteDisponible);
        pagination.setPageCount(pageCount);

        int pageCourante = pagination.getCurrentPageIndex();
        if (pageCourante >= pageCount) {
            pageCourante = 0;
            pagination.setCurrentPageIndex(0);
        }

        afficherPage.accept(pageCourante);
    }

    /**
     * Met à jour le menu de suggestions selon la saisie en cours.
     *
     * @param saisie texte saisi dans le champ de recherche
     */
    private void gererMenuSuggestionsEnTempsReel(String saisie) {
        if (txtRechercheInvite == null) {
            return;
        }

        if (saisie == null || saisie.trim().isEmpty()) {
            menuSuggestions.hide();
            return;
        }

        String recherche = normaliserRecherche(saisie);
        if (recherche.isEmpty()) {
            menuSuggestions.hide();
            return;
        }

        menuSuggestions.getItems().clear();

        List<User> resultats = trouverEmployesCorrespondants(recherche);
        int compteur = 0;

        for (User user : resultats) {
            if (compteur >= MAX_SUGGESTIONS) {
                break;
            }

            MenuItem item = creerItemSuggestion(user);
            menuSuggestions.getItems().add(item);
            compteur++;
        }

        if (menuSuggestions.getItems().isEmpty()) {
            menuSuggestions.hide();
            return;
        }

        afficherOuRafraichirMenuSuggestions();
    }

    /**
     * Normalise une chaîne de recherche (trim et minuscules).
     *
     * @param texte texte saisi
     * @return texte normalisé ou chaîne vide
     */
    private String normaliserRecherche(String texte) {
        return texte == null ? "" : texte.trim().toLowerCase();
    }

    /**
     * Recherche les employés correspondant à une requête (nom ou poste).
     *
     * @param recherche texte de recherche normalisé
     * @return liste triée (correspondance en début de nom en priorité)
     */
    private List<User> trouverEmployesCorrespondants(String recherche) {
        List<User> commencePar = new ArrayList<>();
        List<User> contient = new ArrayList<>();

        for (User user : tousLesEmployes) {
            String nomComplet = construireNomComplet(user).toLowerCase();
            String poste = user.getPoste() != null ? user.getPoste().toLowerCase() : "";

            boolean matchNom = nomComplet.contains(recherche);
            boolean matchPoste = poste.contains(recherche);

            if (!matchNom && !matchPoste) {
                continue;
            }

            if (nomComplet.startsWith(recherche)) {
                commencePar.add(user);
            } else {
                contient.add(user);
            }
        }

        List<User> resultat = new ArrayList<>(commencePar);
        resultat.addAll(contient);
        return resultat;
    }

    /**
     * Crée une entrée de menu pour ajouter un invité depuis les suggestions.
     *
     * @param user employé proposé
     * @return item de menu cliquable
     */
    private MenuItem creerItemSuggestion(User user) {
        String poste = user.getPoste() != null && !user.getPoste().isBlank()
                ? user.getPoste()
                : "Collaborateur";

        MenuItem item = new MenuItem(construireNomComplet(user) + " — " + poste);
        item.getStyleClass().add(CSS_ITEM_SUGGESTION);

        item.setOnAction(e -> {
            creerBadgeInvite(user);
            txtRechercheInvite.clear();
            menuSuggestions.hide();
        });

        return item;
    }

    /**
     * Affiche ou rafraîchit le menu contextuel sous le champ de recherche.
     */
    private void afficherOuRafraichirMenuSuggestions() {
        if (txtRechercheInvite == null || txtRechercheInvite.getScene() == null) {
            return;
        }

        if (menuSuggestions.isShowing()) {
            menuSuggestions.hide();
        }

        menuSuggestions.show(txtRechercheInvite, Side.BOTTOM, 0, 4);
    }

    /**
     * Ajoute un badge visuel pour un invité sélectionné à la planification.
     *
     * @param user employé invité
     */
    private void creerBadgeInvite(User user) {
        if (user == null || flowPaneInvitesBadges == null) {
            return;
        }

        final int idEmploye = user.getId();
        if (listeIdInvitesSelectionnes.contains(idEmploye)) {
            return;
        }

        listeIdInvitesSelectionnes.add(idEmploye);

        final String nomAffichage = construireNomComplet(user);

        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        badge.setSpacing(6);
        badge.getStyleClass().add("user-badge");

        Label lblNom = new Label(nomAffichage);
        lblNom.getStyleClass().add("user-badge-text");

        Label lblSupprimer = new Label("❌");
        lblSupprimer.getStyleClass().add("user-badge-delete");
        lblSupprimer.setOnMouseClicked(event -> {
            flowPaneInvitesBadges.getChildren().remove(badge);
            listeIdInvitesSelectionnes.remove(Integer.valueOf(idEmploye));
        });

        badge.getChildren().addAll(lblNom, lblSupprimer);
        flowPaneInvitesBadges.getChildren().add(badge);
    }

    /**
     * Ajoute un invité saisi manuellement depuis le champ de recherche.
     * Liaison FXML : action Entrée sur {@code txtRechercheInvite}.
     */
    @FXML
    private void ajouterInviteManuel() {
        if (txtRechercheInvite == null) {
            return;
        }

        String recherche = txtRechercheInvite.getText();
        if (recherche == null || recherche.trim().isEmpty()) {
            return;
        }

        User employeTrouve = trouverEmployeParRecherche(recherche.trim());
        if (employeTrouve != null) {
            creerBadgeInvite(employeTrouve);
            txtRechercheInvite.clear();
            menuSuggestions.hide();
            return;
        }

        afficherStatut("⚠️ Aucun collaborateur trouvé pour : " + recherche.trim(), true);
    }

    /**
     * Recherche un employé par correspondance exacte ou partielle sur le nom.
     *
     * @param recherche texte saisi
     * @return employé trouvé ou {@code null}
     */
    private User trouverEmployeParRecherche(String recherche) {
        String rechercheMinuscule = recherche.toLowerCase();

        User meilleurMatch = null;

        for (User user : tousLesEmployes) {
            String nomComplet = construireNomComplet(user).toLowerCase();

            if (nomComplet.equals(rechercheMinuscule)) {
                return user;
            }

            if (meilleurMatch == null && nomComplet.contains(rechercheMinuscule)) {
                meilleurMatch = user;
            }
        }

        return meilleurMatch;
    }

    /**
     * Construit le nom complet affiché d'un employé.
     *
     * @param user employé source
     * @return prénom et nom concaténés
     */
    private String construireNomComplet(User user) {
        return user.getPrenom() + " " + user.getNom();
    }

    /**
     * Demande la connexion au salon saisi dans le champ libre.
     * Liaison FXML : {@code btnRejoindre}.
     */
    @FXML
    private void demanderConnexion() {
        if (txtRoomName == null) {
            afficherStatut("Champ salon introuvable.", true);
            return;
        }

        String room = txtRoomName.getText().trim();
        if (room.isEmpty()) {
            afficherStatut("Veuillez renseigner un identifiant de salon.", true);
            return;
        }

        envoyerRequeteConnexion(room);
    }

    /**
     * Rejoint la réunion sélectionnée dans le tableau des réunions actives.
     * Liaison FXML : {@code btnRejoindreSelection}.
     */
    @FXML
    private void rejoindreSalonSelectionne() {
        if (tableReunionsActives == null) {
            afficherStatut("Table des réunions indisponible.", true);
            return;
        }

        Visio visioSelectionnee = tableReunionsActives.getSelectionModel().getSelectedItem();
        if (visioSelectionnee == null) {
            afficherStatut("Sélectionnez d'abord une réunion dans le tableau.", true);
            return;
        }

        envoyerRequeteConnexion(visioSelectionnee.getRoom_name());
    }

    /**
     * Envoie une requête WebSocket pour obtenir un token de connexion LiveKit.
     *
     * @param roomName nom du salon à rejoindre
     */
    private void envoyerRequeteConnexion(String roomName) {
        if (attenteTokenVisio) {
            afficherStatut("Connexion déjà en cours, patientez...", false);
            return;
        }

        User currentUser = SessionService.getUtilisateurConnecte();
        if (currentUser == null) {
            afficherStatut("Utilisateur non connecté.", true);
            return;
        }

        attenteTokenVisio = true;
        roomNameEnAttente = roomName;
        definirBoutonsConnexionDesactives(true);

        JSONObject requete = new JSONObject();
        requete.put("type", TYPE_REQUEST_VISIO_TOKEN);
        requete.put("roomName", roomName);
        requete.put("identity", construireIdentityLiveKit(currentUser));
        requete.put("displayName", construireNomAffichageLiveKit(currentUser));

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Connexion au serveur de visio en cours...", false);
    }

    /**
     * Construit l'identifiant LiveKit à partir de l'identifiant utilisateur.
     *
     * @param user utilisateur connecté
     * @return identifiant sous forme de chaîne
     */
    private static String construireIdentityLiveKit(User user) {
        return String.valueOf(user.getId());
    }

    /**
     * Construit le nom affiché dans la visio pour un utilisateur.
     *
     * @param user utilisateur connecté
     * @return prénom et nom concaténés
     */
    private static String construireNomAffichageLiveKit(User user) {
        return user.getPrenom() + " " + user.getNom();
    }

    /**
     * Active ou désactive les boutons de connexion pendant une requête token.
     *
     * @param desactiver {@code true} pour désactiver les boutons
     */
    private void definirBoutonsConnexionDesactives(boolean desactiver) {
        if (btnRejoindre != null) {
            btnRejoindre.setDisable(desactiver);
        }
        if (btnRejoindreSelection != null) {
            btnRejoindreSelection.setDisable(desactiver);
        }
    }

    /**
     * Valide et envoie une demande de planification de réunion au serveur.
     * Liaison FXML : {@code btnPlanifier}.
     */
    @FXML
    private void planifierReunion() {
        if (txtPlanifRoomName == null || datePlanif == null) {
            afficherStatut("Formulaire de planification incomplet.", true);
            return;
        }

        String room = txtPlanifRoomName.getText().trim();
        if (datePlanif.getValue() == null) {
            afficherStatut("Date obligatoire.", true);
            return;
        }

        Integer heure = lireValeurSpinner(spinnerHeure);
        Integer minute = lireValeurSpinner(spinnerMinute);
        if (heure == null || minute == null) {
            afficherStatut("Heure et minutes invalides.", true);
            return;
        }

        LocalDateTime heurePro = LocalDateTime.of(datePlanif.getValue(), LocalTime.of(heure, minute));

        Optional<String> erreurPlanif = visioService.validerPlanification(room, heurePro);
        if (erreurPlanif.isPresent()) {
            afficherStatut(erreurPlanif.get(), true);
            return;
        }

        JSONArray invitesArray = new JSONArray();
        for (int idInvite : listeIdInvitesSelectionnes) {
            invitesArray.put(idInvite);
        }

        JSONObject planifMsg = new JSONObject();
        planifMsg.put("type", TYPE_PLANIFY_VISIO);
        planifMsg.put("roomName", room);
        planifMsg.put("heureProgrammee", heurePro.toString());
        planifMsg.put("invites", invitesArray);

        WebSocketService.getInstance().envoyerMessageBrut(planifMsg.toString());
        afficherStatut("Enregistrement de la planification...", false);

        viderFormulairePlanification();
    }

    /**
     * Réinitialise le formulaire de planification et la liste des invités.
     */
    private void viderFormulairePlanification() {
        if (txtPlanifRoomName != null) {
            txtPlanifRoomName.clear();
        }

        if (txtRechercheInvite != null) {
            txtRechercheInvite.clear();
        }

        if (flowPaneInvitesBadges != null) {
            flowPaneInvitesBadges.getChildren().clear();
        }

        listeIdInvitesSelectionnes.clear();
        menuSuggestions.hide();
    }

    /**
     * Demande au serveur la liste des réunions de l'utilisateur connecté.
     */
    public void rafraichirListeReunions() {
        JSONObject msg = new JSONObject();
        msg.put("type", TYPE_GET_MY_VISIOS);
        WebSocketService.getInstance().envoyerMessageBrut(msg.toString());
    }

    /**
     * Reçoit un token JWT LiveKit et ouvre l'URL de visio dans le navigateur système.
     *
     * @param tokenJWT jeton d'accès à la salle
     * @param roomNameServeur nom de salle renvoyé par le serveur (secours si attente locale vide)
     */
    public void recevoirTokenEtLancer(String tokenJWT, String roomNameServeur) {
        if (!attenteTokenVisio) {
            return;
        }

        attenteTokenVisio = false;

        String roomEffective = (roomNameEnAttente != null && !roomNameEnAttente.isBlank())
                ? roomNameEnAttente
                : roomNameServeur;

        if (roomEffective == null || roomEffective.isBlank()) {
            Platform.runLater(() -> {
                definirBoutonsConnexionDesactives(false);
                afficherStatut("Nom de salle introuvable.", true);
            });
            return;
        }

        Platform.runLater(() -> {
            definirBoutonsConnexionDesactives(false);

            try {
                String urlFrontNas = getDotenv().get("URL_FRONT_VISIO");
                String urlComplete = String.format(
                        "%s?token=%s&room=%s",
                        urlFrontNas,
                        URLEncoder.encode(tokenJWT, StandardCharsets.UTF_8),
                        URLEncoder.encode(roomEffective, StandardCharsets.UTF_8)
                );

                System.out.println("🚀 Redirection navigateur : " + urlComplete);
                afficherStatut("🚀 Visio ouverte dans votre navigateur !", false);

                ouvrirNavigateur(urlComplete);

            } catch (Exception e) {
                afficherStatut("❌ Erreur d'ouverture : " + e.getMessage(), true);
                e.printStackTrace();
            }
        });
    }

    /**
     * Ouvre une URL dans le navigateur système par défaut.
     *
     * @param url adresse à ouvrir
     * @throws Exception si le lancement du processus échoue
     */
    private void ouvrirNavigateur(String url) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win")) {
            pb = new ProcessBuilder("cmd", "/c", "start", "", url);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("open", url);
        } else {
            pb = new ProcessBuilder("xdg-open", url);
        }

        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.environment().putAll(System.getenv());
        pb.start();
    }

    /**
     * Traite les messages de retour visio (succès planification ou erreur connexion).
     *
     * @param messageErreur message serveur ; préfixe {@code ✅} pour succès
     */
    public void recevoirErreurVisio(String messageErreur) {
        attenteTokenVisio = false;

        Platform.runLater(() -> {
            definirBoutonsConnexionDesactives(false);

            if (messageErreur != null && messageErreur.startsWith("✅")) {
                afficherStatut(messageErreur, false);
                rafraichirListeReunions();
            } else {
                afficherStatut(messageErreur != null ? messageErreur : "Erreur inconnue.", true);
            }
        });
    }

    /**
     * Met à jour les tableaux actifs et archives à partir de la réponse WebSocket.
     *
     * @param reunionsJson tableau JSON des réunions
     */
    public void recevoirListeReunions(JSONArray reunionsJson) {
        Platform.runLater(() -> {
            listeToutesReunions.clear();
            listeReunionsActives.clear();
            listeReunionsArchives.clear();

            for (int i = 0; i < reunionsJson.length(); i++) {
                JSONObject obj = reunionsJson.getJSONObject(i);
                Visio visio = convertirJsonEnVisio(obj);

                listeToutesReunions.add(visio);
                if (visio.getStatut() == VisioStatut.TERMINE) {
                    listeReunionsArchives.add(visio);
                } else {
                    listeReunionsActives.add(visio);
                }
            }

            trierListesReunions();
            mettreAJourPagination();
        });
    }

    /**
     * Trie les listes de réunions actives (croissant) et archives (décroissant).
     */
    private void trierListesReunions() {
        Comparator<Visio> comparatorActives = Comparator.comparing(
                Visio::getHeure_programmee,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Visio> comparatorArchives = Comparator.comparing(
                Visio::getHeure_programmee,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        FXCollections.sort(listeReunionsActives, comparatorActives);
        FXCollections.sort(listeReunionsArchives, comparatorArchives);
    }

    /**
     * Convertit un objet JSON serveur en modèle {@link Visio}.
     *
     * @param obj objet JSON source
     * @return réunion convertie
     */
    private Visio convertirJsonEnVisio(JSONObject obj) {
        Visio visio = new Visio();
        visio.setId(obj.getInt("id"));
        visio.setRoom_name(obj.getString("roomName"));
        visio.setCreateur_id(obj.optInt("createurId", -1));
        visio.setStatut(VisioStatut.valueOf(obj.getString("statut")));

        String dateStr = obj.optString("heureProgrammee");
        if (!dateStr.isEmpty()) {
            visio.setHeure_programmee(LocalDateTime.parse(dateStr));
        }

        return visio;
    }

    /**
     * Applique le style personnalisé au sélecteur de date de planification.
     */
    private void initialiserDatePicker() {
        if (datePlanif == null) {
            return;
        }

        if (!datePlanif.getStyleClass().contains("date-picker-custom")) {
            datePlanif.getStyleClass().add("date-picker-custom");
        }

        URL popupCss = getClass().getResource("/style/popup.css");
        if (popupCss == null) {
            return;
        }

        String popupCssUrl = popupCss.toExternalForm();
        if (!datePlanif.getStylesheets().contains(popupCssUrl)) {
            datePlanif.getStylesheets().add(popupCssUrl);
        }
    }

    /**
     * Initialise la date et les spinners d'heure avec l'heure courante.
     */
    private void initialiserDateEtHeure() {
        LocalDateTime maintenant = LocalDateTime.now();

        if (datePlanif != null) {
            datePlanif.setValue(maintenant.toLocalDate());
        }

        if (spinnerHeure != null) {
            spinnerHeure.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, maintenant.getHour())
            );
            formaterSpinner(spinnerHeure);
        }

        if (spinnerMinute != null) {
            spinnerMinute.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, maintenant.getMinute(), 1)
            );
            formaterSpinner(spinnerMinute);
        }
    }

    /**
     * Rend un spinner éditable avec un formatage sur deux chiffres.
     *
     * @param spinner spinner heure ou minute à configurer
     */
    private void formaterSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);

        spinner.getValueFactory().setConverter(new StringConverter<>() {
            /**
             * Formate la valeur du spinner sur deux chiffres.
             *
             * @param value valeur du spinner
             * @return représentation sur deux chiffres
             */
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }

            /**
             * Extrait un entier depuis la saisie utilisateur.
             *
             * @param string saisie utilisateur
             * @return entier extrait ou {@code null}
             */
            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }

                String chiffres = string.replaceAll("\\D", "");
                return chiffres.isEmpty() ? null : Integer.parseInt(chiffres);
            }
        });
    }

    /**
     * Lit et valide la valeur courante d'un spinner entier.
     *
     * @param spinner spinner source
     * @return valeur validée ou {@code null} si invalide
     */
    private Integer lireValeurSpinner(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return null;
        }

        try {
            spinner.commitValue();
        } catch (Exception ignored) {
        }

        Integer value = spinner.getValue();
        if (value == null) {
            return null;
        }

        if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
            if (value < intFactory.getMin() || value > intFactory.getMax()) {
                return null;
            }
        }

        return value;
    }

    /**
     * Affiche une alerte d'avertissement en cas d'erreur utilisateur.
     *
     * @param message texte à afficher
     * @param erreur {@code true} pour afficher une alerte, {@code false} pour ignorer
     */
    private void afficherStatut(String message, boolean erreur) {
        if (!erreur || message == null || message.isBlank()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Visioconférence");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}