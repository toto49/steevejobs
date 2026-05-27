package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.WebSocketService;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class VisioController {

    private static VisioController activeInstance;
    private static Dotenv dotenv;
    private boolean attenteTokenVisio = false;
    @FXML
    private TextField txtRoomName;
    @FXML
    private Button btnRejoindre;
    @FXML
    private Label lblStatut;
    private final ObservableList<Visio> listeReunionsData = FXCollections.observableArrayList();
    private final ObservableList<User> listeEmployesData = FXCollections.observableArrayList();
    @FXML
    private TextField txtPlanifRoomName;
    @FXML
    private DatePicker datePlanif;
    @FXML
    private ComboBox<String> comboHeure;
    @FXML
    private ComboBox<String> comboMinute;
    @FXML
    private ListView<User> listEmployesDisponibles;
    @FXML
    private Button btnPlanifier;
    @FXML
    private TableView<Visio> tableReunions;
    @FXML
    private TableColumn<Visio, String> colRoomName;
    @FXML
    private TableColumn<Visio, String> colStatut;
    @FXML
    private TableColumn<Visio, String> colDate;
    @FXML
    private Button btnRejoindreSelection;

    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    private static Dotenv getDotenv() {
        if (dotenv == null) dotenv = Dotenv.load();
        return dotenv;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
        this.attenteTokenVisio = false;

        if (lblStatut != null) lblStatut.setText("");
        if (tableReunions != null) {
            colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));
            colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("heureProgrammee"));
            tableReunions.setItems(listeReunionsData);

            rafraichirListeReunions();
        }
        if (comboHeure != null && comboMinute != null) {
            for (int i = 0; i < 24; i++) comboHeure.getItems().add(String.format("%02dd", i));
            for (int i = 0; i < 60; i += 5) comboMinute.getItems().add(String.format("%02dd", i));
            comboHeure.setValue("14");
            comboMinute.setValue("00");
        }

        if (listEmployesDisponibles != null) {
            listEmployesDisponibles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listEmployesDisponibles.setItems(listeEmployesData);
            chargerListeEmployes();
        }
    }

    public void couperController() {
        this.attenteTokenVisio = false;
        activeInstance = null;
    }


    @FXML
    private void demanderConnexion() {
        String room = txtRoomName.getText().trim();
        if (room.isEmpty()) {
            lblStatut.setStyle("-fx-text-fill: #e74c3c;");
            lblStatut.setText("⚠️ Veuillez renseigner un identifiant de salon.");
            return;
        }
        envoyerRequeteConnexion(room);
    }

    @FXML
    private void rejoindreSalonSelectionne() {
        Visio visioSelectionnee = tableReunions.getSelectionModel().getSelectedItem();
        if (visioSelectionnee == null) {
            lblStatut.setStyle("-fx-text-fill: #e74c3c;");
            lblStatut.setText("⚠️ Sélectionnez d'abord une réunion dans le tableau.");
            return;
        }
        envoyerRequeteConnexion(visioSelectionnee.getRoom_name());
    }

    private void envoyerRequeteConnexion(String roomName) {
        this.attenteTokenVisio = true;
        User currentUser = SessionService.getUtilisateurConnecte();
        String nomUtilisateur = (currentUser != null) ? currentUser.getNom() : "Tom_Boudaud";

        JSONObject requete = new JSONObject();
        requete.put("type", "REQUEST_VISIO_TOKEN");
        requete.put("roomName", roomName);
        requete.put("identity", nomUtilisateur);

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());

        if (lblStatut != null) {
            lblStatut.setStyle("-fx-text-fill: #3498db;");
            lblStatut.setText("⏳ Négociation du protocole de sécurité avec le NAS...");
        }
    }

    @FXML
    private void planifierReunion() {
        String room = txtPlanifRoomName.getText().trim();
        if (room.isEmpty() || datePlanif.getValue() == null) {
            lblStatut.setStyle("-fx-text-fill: #e74c3c;");
            lblStatut.setText("⚠️ Nom de salle et date obligatoires.");
            return;
        }


        int heure = Integer.parseInt(comboHeure.getValue().replace("d", ""));
        int minute = Integer.parseInt(comboMinute.getValue().replace("d", ""));
        LocalDateTime heurePro = LocalDateTime.of(datePlanif.getValue(), LocalTime.of(heure, minute));

        JSONArray invitesArray = new JSONArray();
        for (User u : listEmployesDisponibles.getSelectionModel().getSelectedItems()) {
            invitesArray.put(u.getId());
        }

        JSONObject planifMsg = new JSONObject();
        planifMsg.put("type", "PLANIFY_VISIO");
        planifMsg.put("roomName", room);
        planifMsg.put("heureProgrammee", heurePro.toString());
        planifMsg.put("invites", invitesArray);

        WebSocketService.getInstance().envoyerMessageBrut(planifMsg.toString());
        lblStatut.setStyle("-fx-text-fill: #3498db;");
        lblStatut.setText("⏳ Enregistrement de la planification sur le NAS...");
    }

    public void rafraichirListeReunions() {
        JSONObject msg = new JSONObject();
        msg.put("type", "GET_MY_VISIOS");
        WebSocketService.getInstance().envoyerMessageBrut(msg.toString());
    }

    private void chargerListeEmployes() {
    }


    public void recevoirTokenEtLancer(String tokenJWT) {
        if (!attenteTokenVisio) return;
        this.attenteTokenVisio = false;

        Platform.runLater(() -> {
            try {
                String urlFrontNas = getDotenv().get("URL_FRONT_VISIO");
                String urlLiveKit = getDotenv().get("LIVEKIT_SERVER_URL");

                String urlComplete = String.format("%s?url=%s&token=%s", urlFrontNas, urlLiveKit, tokenJWT);
                System.out.println("🚀 Lancement de la visio hébergée sur le NAS : " + urlComplete);

                if (lblStatut != null) {
                    lblStatut.setStyle("-fx-text-fill: #2ecc71;");
                    lblStatut.setText("✅ Redirection vers le serveur de visio...");
                }

                String arch = System.getProperty("os.arch").toLowerCase();
                if (arch.contains("arm") || arch.contains("aarch64")) {
                    Runtime.getRuntime().exec("cmd /c start msedge --app=\"" + urlComplete + "\"");
                } else {
                    Desktop.getDesktop().browse(new URI(urlComplete));
                }

            } catch (Exception e) {
                if (lblStatut != null) {
                    lblStatut.setStyle("-fx-text-fill: #e74c3c;");
                    lblStatut.setText("❌ Erreur : " + e.getMessage());
                }
            }
        });
    }


    public void recevoirErreurVisio(String messageErreur) {
        this.attenteTokenVisio = false;
        Platform.runLater(() -> {
            if (lblStatut != null) {
                lblStatut.setStyle("-fx-text-fill: #e74c3c;");
                lblStatut.setText(messageErreur);
            }
        });
    }

    public void recevoirListeReunions(JSONArray reunionsJson) {
        Platform.runLater(() -> {
            listeReunionsData.clear();
            for (int i = 0; i < reunionsJson.length(); i++) {
                JSONObject obj = reunionsJson.getJSONObject(i);

                Visio v = new Visio();
                v.setId(obj.getInt("id"));
                v.setRoom_name(obj.getString("roomName"));
                v.setStatut(VisioStatut.valueOf(obj.getString("statut")));

                String dateStr = obj.optString("heureProgrammee");
                if (!dateStr.isEmpty()) {
                    v.setHeure_programmee(LocalDateTime.parse(dateStr));
                }

                listeReunionsData.add(v);
            }
        });
    }
}