package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.model.Enum.AppModule;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.util.TestRuntime;
import com.eseo.steevejobs.service.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contrôleur FXML du tableau de bord (accueil) affichant les modules applicatifs.
 * Liaison FXML : {@code appsGrid}.
 * Filtre les cartes selon les permissions utilisateur ({@link AppModule#getCodeAction()}).
 */
public class HomeController {

    /** Cache des images de modules par chemin de ressource. */
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    /** Utilisateur connecté pour le filtrage des modules affichés. */
    private User currentUser;
    /** Cache des codes de permission du dernier utilisateur chargé. */
    private static List<String> cachedPermissions = null;
    /** Durée de validité du cache des permissions (millisecondes). */
    private static final long CACHE_TTL_MS = 30_000;
    /** Horodatage du dernier remplissage du cache des permissions. */
    private static long cacheTimestamp = -1;

    /** Grille affichant les cartes des modules applicatifs. */
    @FXML
    private FlowPane appsGrid;

    /** Compteur global de tickets non lus pour la vue technicien (staff). */
    public static int notificationsTech = 0;
    /** Compteur global de tickets non lus pour la vue auteur (demandeur). */
    public static int notificationsAuteur = 0;

    /** Instance du contrôleur d'accueil actuellement affichée. */
    private static HomeController activeInstance;

    /** Badge de notification sur la carte tickets (vue staff). */
    private Label badgeCarteTech;
    /** Badge de notification sur la carte tickets (vue demandeur). */
    private Label badgeCarteAuteur;
    /** Identifiant utilisateur associé au cache des permissions. */
    private static int cachedUserId = -1;
    /** Service de chargement des permissions utilisateur. */
    private final PermissionService permissionService = new PermissionService();
    /** Liste des codes d'action autorisés pour l'utilisateur courant. */
    private List<String> currentUserPermissions;
    /** Planificateur de rafraîchissement périodique des permissions. */
    private ScheduledExecutorService permissionScheduler;

    /**
     * Retourne l'instance active du contrôleur d'accueil.
     *
     * @return instance courante ou {@code null} si non initialisée
     */
    public static HomeController getActiveInstance() {
        return activeInstance;
    }

    /**
     * Constructeur par défaut requis par le chargeur FXML.
     */
    public HomeController() {
    }

    /**
     * Met à jour les compteurs de notifications tickets et synchronise les badges menu/accueil.
     *
     * @param typeCible {@code TECH} (staff) ou {@code AUTEUR} (demandeur)
     */
    public static void ajouterNotification(String typeCible) {
        if (TestRuntime.isEnabled()) {
            return;
        }
        User user = SessionService.getUtilisateurConnecte();
        if (user == null) return;

        CompletableFuture.supplyAsync(() -> {
            TicketService ts = new TicketServiceImpl();
            if ("TECH".equals(typeCible)) {
                return ts.getNombreTicketsNonLusAdmin(user.getRole(), user.getId());
            } else if ("AUTEUR".equals(typeCible)) {
                return ts.getNombreTicketsNonLusAuteur(user.getId());
            }
            return 0;
        }).thenAcceptAsync(nouveauCompte -> {
            if ("TECH".equals(typeCible)) {
                notificationsTech = nouveauCompte;
                if (activeInstance != null && activeInstance.badgeCarteTech != null) {
                    activeInstance.badgeCarteTech.setText(String.valueOf(notificationsTech));
                    activeInstance.badgeCarteTech.setVisible(notificationsTech > 0);
                }
            } else if ("AUTEUR".equals(typeCible)) {
                notificationsAuteur = nouveauCompte;
                if (activeInstance != null && activeInstance.badgeCarteAuteur != null) {
                    activeInstance.badgeCarteAuteur.setText(String.valueOf(notificationsAuteur));
                    activeInstance.badgeCarteAuteur.setVisible(notificationsAuteur > 0);
                }
            }

            if (MenuController.getInstance() != null) {
                MenuController.getInstance().allumerBadge(typeCible, nouveauCompte);
            }
        }, Platform::runLater).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Invalide le cache des permissions et relance le rendu du centre applicatif.
     */
    public static void invaliderCachePermissions() {
        cachedPermissions = null;
        cachedUserId = -1;
        cacheTimestamp = -1;
        if (activeInstance != null) {
            Platform.runLater(() -> {
                User user = SessionService.getUtilisateurConnecte();
                if (user != null) {
                    activeInstance.onUserLogin(user.getId());
                }
            });
        }
    }

    /**
     * Charge permissions et compteurs de tickets après connexion, puis affiche les modules autorisés.
     *
     * @param idUserConnecte identifiant de l'utilisateur connecté
     */
    public void onUserLogin(int idUserConnecte) {
        if (TestRuntime.isEnabled()) {
            this.currentUserPermissions = List.of();
            renderAppCenter();
            return;
        }
        CompletableFuture.supplyAsync(() -> {
            TicketService ticketService = new TicketServiceImpl();
            int nTech = 0;
            if (currentUser != null && ("ADMIN".equals(currentUser.getRole()) || "RH".equals(currentUser.getRole()))) {
                nTech = ticketService.getNombreTicketsNonLusAdmin(currentUser.getRole(), currentUser.getId());
            }
            int nAuteur = currentUser != null ? ticketService.getNombreTicketsNonLusAuteur(currentUser.getId()) : 0;
            return new int[]{nTech, nAuteur};
        }).thenAcceptAsync(counts -> {
            notificationsTech = counts[0];
            notificationsAuteur = counts[1];

            if (badgeCarteTech != null) {
                badgeCarteTech.setText(String.valueOf(notificationsTech));
                badgeCarteTech.setVisible(notificationsTech > 0);
            }
            if (badgeCarteAuteur != null) {
                badgeCarteAuteur.setText(String.valueOf(notificationsAuteur));
                badgeCarteAuteur.setVisible(notificationsAuteur > 0);
            }

            if (MenuController.getInstance() != null && currentUser != null) {
                if ("ADMIN".equals(currentUser.getRole()) || "RH".equals(currentUser.getRole())) {
                    MenuController.getInstance().allumerBadge("TECH", notificationsTech);
                }
                MenuController.getInstance().allumerBadge("AUTEUR", notificationsAuteur);
            }
        }, Platform::runLater).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        boolean cacheValide = cachedPermissions != null
                && cachedUserId == idUserConnecte
                && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS;

        if (cacheValide) {
            this.currentUserPermissions = cachedPermissions;
            renderAppCenter();
        } else {
            CompletableFuture.supplyAsync(() -> permissionService.getUserPermissions(idUserConnecte))
                    .thenAcceptAsync(perms -> {
                        cachedPermissions = perms;
                        cachedUserId = idUserConnecte;
                        cacheTimestamp = System.currentTimeMillis();
                        this.currentUserPermissions = perms;

                        renderAppCenter();
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        }
    }

    /**
     * Point d'entrée FXML : enregistre l'instance, charge l'accueil ou redirige vers la connexion.
     *
     * @throws IOException si le chargement de {@code bienvenue-view.fxml} échoue
     */
    @FXML
    public void initialize() throws IOException {
        activeInstance = this;
        this.currentUser = SessionService.getUtilisateurConnecte();

        if (this.currentUser != null) {
            onUserLogin(currentUser.getId());
            demarrerSchedulerPermissions();
        } else {
            SessionService.setUtilisateurConnecte(null);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/bienvenue-view.fxml"));
            Parent loginRoot = loader.load();
            HelloApplication.changerPageGlobale(loginRoot, "Connexion");
        }
    }

    /**
     * Démarre une tâche planifiée qui vérifie les permissions toutes les 30 secondes.
     */
    private void demarrerSchedulerPermissions() {
        if (permissionScheduler != null && !permissionScheduler.isShutdown()) {
            permissionScheduler.shutdown();
        }

        permissionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "permission-checker");
            t.setDaemon(true);
            return t;
        });

        permissionScheduler.scheduleAtFixedRate(() -> {
            if (currentUser == null) return;

            CompletableFuture.supplyAsync(() -> permissionService.getUserPermissions(currentUser.getId()))
                    .thenAccept(perms -> {
                        if (!perms.equals(cachedPermissions)) {
                            Platform.runLater(() -> {
                                cachedPermissions = perms;
                                cacheTimestamp = System.currentTimeMillis();
                                this.currentUserPermissions = perms;
                                renderAppCenter();
                            });
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Reconstruit la grille des cartes de modules selon les permissions de l'utilisateur.
     */
    private void renderAppCenter() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::renderAppCenter);
            return;
        }

        if (appsGrid == null) {
            System.err.println("⚠️ [SteeveJobs] Impossible d'afficher le tableau de bord : appsGrid est null.");
            return;
        }

        appsGrid.getChildren().clear();

        for (AppModule app : AppModule.values()) {
            if (hasPermission(app.getCodeAction())) {

                String parametre = null;
                String codeAction = app.getCodeAction();
                if ("APP_TICKETS_VIEW".equals(codeAction) && currentUser != null) {
                    parametre = currentUser.getRole();
                }

                Label badgeDynamique = new Label();
                badgeDynamique.setVisible(false);

                if ("APP_TICKETS_VIEW".equals(codeAction)) {
                    badgeCarteTech = badgeDynamique;
                    if (notificationsTech > 0) {
                        badgeDynamique.setText(String.valueOf(notificationsTech));
                        badgeDynamique.setVisible(true);
                    }
                } else if ("MES_TICKETS".equals(codeAction)) {
                    badgeCarteAuteur = badgeDynamique;
                    if (notificationsAuteur > 0) {
                        badgeDynamique.setText(String.valueOf(notificationsAuteur));
                        badgeDynamique.setVisible(true);
                    }
                }

                HBox card = createAppCard(
                        app.getTitle(),
                        app.getSubtitle(),
                        badgeDynamique,
                        app.getBgColor(),
                        app.getChemin(),
                        app.getImage(),
                        parametre,
                        codeAction
                );

                card.prefWidthProperty().bind(appsGrid.widthProperty().divide(3).subtract(60));
                card.prefHeightProperty().bind(card.widthProperty().multiply(0.6));

                appsGrid.getChildren().add(card);
            }
        }
    }

    /**
     * Charge une vue FXML paramétrée dans la zone centrale du menu.
     *
     * @param chemin nom de base de la vue (sans suffixe {@code -view.fxml})
     * @param parametre donnée passée au contrôleur paramétrable
     * @param titreCard titre affiché dans l'en-tête du menu
     */
    private void chargerPageAvecParametre(String chemin, String parametre, String titreCard) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/" + chemin + "-view.fxml"));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ParametrizedController) {
                ((ParametrizedController) controller).initData(parametre);
            }
            MenuController.getInstance().setCenterView(view);
            MenuController.getInstance().changerTitre(formaterTitreModule(titreCard));

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("❌ Erreur lors de l'ouverture de la vue paramétrée : " + chemin);
        }
    }

    /**
     * Crée une carte cliquable représentant un module applicatif.
     *
     * @param title titre principal
     * @param subtitle sous-titre
     * @param badge badge de notification optionnel
     * @param bgColor couleur de fond CSS
     * @param chemin chemin de navigation du module
     * @param nomFichierImage nom du fichier image dans {@code /images/}
     * @param parametreFacultatif paramètre transmis au contrôleur cible, ou {@code null}
     * @param codeAction code permission / action du module
     * @return carte horizontale interactive
     */
    private HBox createAppCard(String title, String subtitle, Label badge, String bgColor, String chemin, String nomFichierImage, String parametreFacultatif, String codeAction) {
        HBox card = new HBox();
        card.setMinSize(250, 220);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15;");
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setAlignment(Pos.CENTER_LEFT);

        VBox imagePlaceholder = new VBox();
        imagePlaceholder.setStyle("-fx-alignment: center;");
        imagePlaceholder.prefWidthProperty().bind(card.widthProperty().multiply(0.3));
        Image cachedImage = IMAGE_CACHE.computeIfAbsent(nomFichierImage, key -> {
            return new Image(getClass().getResource("/images/" + key).toExternalForm());
        });
        ImageView imageView = new ImageView(cachedImage);

        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(card.widthProperty().multiply(0.28));
        imageView.fitHeightProperty().bind(imageView.fitWidthProperty());

        imagePlaceholder.getChildren().add(imageView);

        VBox textZone = new VBox(5);
        textZone.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", card.widthProperty().divide(14), "px; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold;")
        );

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", card.widthProperty().divide(22), "px; -fx-text-fill: #333333;")
        );

        textZone.getChildren().addAll(lblTitle, lblSubtitle);

        if (badge != null) {
            badge.styleProperty().bind(
                    Bindings.concat("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: ", card.widthProperty().divide(26), "px;")
            );
            VBox.setMargin(badge, new Insets(10, 0, 0, 0));
            textZone.getChildren().add(badge);
        }

        card.getChildren().addAll(imagePlaceholder, textZone);
        card.setOnMouseEntered(e -> card.setOpacity(0.8));
        card.setOnMouseExited(e -> card.setOpacity(1.0));
        card.setOnMouseClicked(e -> {
            if (MenuController.getInstance() == null) {
                System.err.println("Erreur : MenuController n'est pas initialisé.");
                return;
            }

            if ("APP_VISO_VIEW".equals(codeAction)) {
                try {
                    JSONObject requete = new JSONObject();
                    requete.put("type", "REQUEST_VISIO_TOKEN");
                    requete.put("roomName", "Salle_De_Crise");

                    if (currentUser != null) {
                        requete.put("identity", String.valueOf(currentUser.getId()));
                        requete.put("displayName", currentUser.getPrenom() + " " + currentUser.getNom());
                    } else {
                        requete.put("identity", "0");
                        requete.put("displayName", "Invité");
                    }
                    WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
                    System.out.println("⏳ Requête de token envoyée au NAS Synology...");

                } catch (Exception ex) {
                    System.err.println("❌ Erreur lors de l'action sur la carte Visio : " + ex.getMessage());
                }
            }
            if (parametreFacultatif != null) {
                chargerPageAvecParametre(chemin, parametreFacultatif, title);
            } else {
                MenuController.getInstance().chargerPage(chemin);
                MenuController.getInstance().changerTitre(formaterTitreModule(title));
            }
        });

        return card;
    }

    /**
     * Vérifie si l'utilisateur connecté possède la permission demandée.
     *
     * @param requiredPermission code action requis
     * @return {@code true} si la permission est accordée
     */
    private boolean hasPermission(String requiredPermission) {
        return currentUserPermissions != null && currentUserPermissions.contains(requiredPermission);
    }

    /**
     * Normalise le titre d'un module pour l'affichage dans l'en-tête du menu.
     *
     * @param title titre brut de la carte
     * @return titre sur une seule ligne
     */
    private String formaterTitreModule(String title) {
        if (title == null) {
            return "";
        }
        return title.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }
}