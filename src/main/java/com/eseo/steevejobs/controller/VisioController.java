package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.VisioService;
import com.eseo.steevejobs.service.WebSocketService;
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

public class VisioController {

    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ITEMS_PER_PAGE = 8;
    private static final int MAX_SUGGESTIONS = 8;
    private static final double HAUTEUR_LIGNE_TABLE = 46;
    private static final double LARGEUR_COL_SUPPRESSION = 52;

    private static final String TYPE_DELETE_VISIO = "DELETE_VISIO";
    private static final String TYPE_REQUEST_VISIO_TOKEN = "REQUEST_VISIO_TOKEN";
    private static final String TYPE_PLANIFY_VISIO = "PLANIFY_VISIO";
    private static final String TYPE_GET_MY_VISIOS = "GET_MY_VISIOS";

    private static final String CSS_MENU_SUGGESTION = "visio-suggestion-menu";
    private static final String CSS_ITEM_SUGGESTION = "visio-suggestion-item";

    private static VisioController activeInstance;
    private static Dotenv dotenv;

    private final UserDAO userDAO = new UserDAO();
    private final VisioService visioService = new VisioService();
    private final ContextMenu menuSuggestions = new ContextMenu();

    private final List<Integer> listeIdInvitesSelectionnes = new ArrayList<>();
    private final ObservableList<Visio> listeToutesReunions = FXCollections.observableArrayList();
    private final ObservableList<Visio> listeReunionsActives = FXCollections.observableArrayList();
    private final ObservableList<Visio> listeReunionsArchives = FXCollections.observableArrayList();
    private final ObservableList<User> tousLesEmployes = FXCollections.observableArrayList();

    private boolean attenteTokenVisio = false;
    private String roomNameEnAttente = "";

    @FXML private TextField txtRoomName;
    @FXML private Button btnRejoindre;

    @FXML private TextField txtPlanifRoomName;
    @FXML private DatePicker datePlanif;
    @FXML private Spinner<Integer> spinnerHeure;
    @FXML private Spinner<Integer> spinnerMinute;
    @FXML private Button btnPlanifier;

    @FXML private TableColumn<Visio, String> colRoomName;
    @FXML private TableColumn<Visio, String> colStatut;
    @FXML private TableColumn<Visio, String> colDate;

    @FXML private Button btnRejoindreSelection;
    @FXML
    private TabPane tabPaneReunions;
    @FXML
    private TableView<Visio> tableReunionsActives;
    @FXML
    private TableView<Visio> tableReunionsArchives;
    @FXML
    private TableColumn<Visio, String> colRoomNameArchive;
    @FXML
    private TableColumn<Visio, String> colStatutArchive;
    @FXML
    private TableColumn<Visio, String> colDateArchive;
    @FXML
    private Pagination paginationReunions;
    @FXML
    private Pagination paginationReunionsArchives;

    @FXML
    private TextField txtRechercheInvite;
    @FXML
    private FlowPane flowPaneInvitesBadges;

    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    private static Dotenv getDotenv() {
        if (dotenv == null) {
            dotenv = Dotenv.load();
        }
        return dotenv;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
        attenteTokenVisio = false;

        initialiserMenuSuggestions();

        initialiserTablesEtPagination();
        initialiserDatePicker();
        initialiserDateEtHeure();
        chargerEmployes();
        initialiserRechercheInvites();

        rafraichirListeReunions();
    }

    public void couperController() {
        attenteTokenVisio = false;
        roomNameEnAttente = "";
        activeInstance = null;
        menuSuggestions.hide();
    }

    private void initialiserMenuSuggestions() {
        menuSuggestions.getStyleClass().add(CSS_MENU_SUGGESTION);
        menuSuggestions.setAutoHide(true);
        menuSuggestions.setHideOnEscape(true);
    }

    private void chargerEmployes() {
        try {
            tousLesEmployes.setAll(userDAO.findActiveUsers());
            trierEmployes();
        } catch (SQLException e) {
            afficherStatut("Impossible de charger la liste des employés.", true);
        }
    }

    private void trierEmployes() {
        FXCollections.sort(
                tousLesEmployes,
                Comparator.comparing(
                        user -> construireNomComplet(user).toLowerCase()
                )
        );
    }

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

    private void initialiserTablesEtPagination() {
        initialiserTableActives();
        initialiserTableArchives();
        initialiserPaginationArchives();
    }

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

    private void initialiserPaginationArchives() {
        if (paginationReunionsArchives != null) {
            paginationReunionsArchives.setPageFactory(this::creerPageReunionsArchives);
        }
    }

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

    private void configurerTableVisio(TableView<Visio> table) {
        table.setFixedCellSize(HAUTEUR_LIGNE_TABLE);
        table.setPlaceholder(new Label("Aucune réunion à afficher."));
    }

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

    private Button creerBoutonSuppression() {
        return new Button("🗑");
    }

    private boolean estCreateurSalon(Visio visio) {
        User utilisateur = SessionService.getUtilisateurConnecte();
        return utilisateur != null
                && visio != null
                && visio.getCreateur_id() == utilisateur.getId();
    }

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

    private void envoyerSuppressionSalon(String roomName) {
        JSONObject requete = new JSONObject();
        requete.put("type", TYPE_DELETE_VISIO);
        requete.put("roomName", roomName);

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Suppression du salon en cours...", false);
    }

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

    private String formaterDate(LocalDateTime heure) {
        return heure != null ? heure.format(DATE_HEURE_FMT) : "—";
    }

    private Node creerPageReunions(int pageIndex) {
        afficherPageReunions(pageIndex);
        return new VBox();
    }

    private Node creerPageReunionsArchives(int pageIndex) {
        afficherPageReunionsArchives(pageIndex);
        return new VBox();
    }

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

    private void appliquerPageTable(TableView<Visio> table, List<Visio> source, int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        if (fromIndex >= source.size()) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }

        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, source.size());
        table.setItems(FXCollections.observableArrayList(source.subList(fromIndex, toIndex)));
    }

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

    private String normaliserRecherche(String texte) {
        return texte == null ? "" : texte.trim().toLowerCase();
    }

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

    private void afficherOuRafraichirMenuSuggestions() {
        if (txtRechercheInvite == null || txtRechercheInvite.getScene() == null) {
            return;
        }

        if (menuSuggestions.isShowing()) {
            menuSuggestions.hide();
        }

        menuSuggestions.show(txtRechercheInvite, Side.BOTTOM, 0, 4);
    }

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

    private String construireNomComplet(User user) {
        return user.getPrenom() + " " + user.getNom();
    }

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

    private static String construireIdentityLiveKit(User user) {
        return String.valueOf(user.getId());
    }

    private static String construireNomAffichageLiveKit(User user) {
        return user.getPrenom() + " " + user.getNom();
    }

    private void definirBoutonsConnexionDesactives(boolean desactiver) {
        if (btnRejoindre != null) {
            btnRejoindre.setDisable(desactiver);
        }
        if (btnRejoindreSelection != null) {
            btnRejoindreSelection.setDisable(desactiver);
        }
    }

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

    public void rafraichirListeReunions() {
        JSONObject msg = new JSONObject();
        msg.put("type", TYPE_GET_MY_VISIOS);
        WebSocketService.getInstance().envoyerMessageBrut(msg.toString());
    }

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

    private void formaterSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);

        spinner.getValueFactory().setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }

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