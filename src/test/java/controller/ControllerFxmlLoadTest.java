package controller;

import com.eseo.steevejobs.service.SessionService;
import controller.support.JavaFxTestSupport;
import dao.support.DaoIntegrationExtension;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.support.TestDataFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests paramétrés de chargement FXML des vues principales de l'application.
 * <p>
 * Cycle de vie : JavaFX initialisé une fois ; {@code @BeforeEach} pose un utilisateur connecté
 * via {@link com.eseo.steevejobs.service.SessionService} et {@link service.support.TestDataFactory}.
 * Base H2 en mémoire via {@link DaoIntegrationExtension} pour les vues dont {@code initialize()} interroge la BDD.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class ControllerFxmlLoadTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.ensureInitialized();
    }

    @BeforeEach
    void utilisateurConnecte() {
        SessionService.setUtilisateurConnecte(TestDataFactory.utilisateurActif(1, "fxml@test.local"));
    }

    @ParameterizedTest(name = "charge {0}")
    @ValueSource(strings = {
            "/com/eseo/steevejobs/view/bienvenue-view.fxml",
            "/com/eseo/steevejobs/view/menu-view.fxml",
            "/com/eseo/steevejobs/view/home-view.fxml",
            "/com/eseo/steevejobs/view/ticket-view.fxml",
            "/com/eseo/steevejobs/view/ticketsList-view.fxml",
            "/com/eseo/steevejobs/view/parametres-view.fxml",
            "/com/eseo/steevejobs/view/stock-view.fxml",
            "/com/eseo/steevejobs/view/calendrier-view.fxml",
            "/com/eseo/steevejobs/view/visio-view.fxml",
            "/com/eseo/steevejobs/view/document-view.fxml",
            "/com/eseo/steevejobs/view/clients-view.fxml",
            "/com/eseo/steevejobs/view/adminuser-view.fxml",
            "/com/eseo/steevejobs/view/adminpermission-view.fxml",
            "/com/eseo/steevejobs/view/fiche-paye-view.fxml",
            "/com/eseo/steevejobs/view/documentUser-view.fxml",
            "/com/eseo/steevejobs/view/modifier-document-view.fxml",
            "/com/eseo/steevejobs/view/nouveau-document-view.fxml",
            "/com/eseo/steevejobs/view/demandes-conge-popup.fxml",
            "/com/eseo/steevejobs/view/calendrier-rh-view.fxml"
    })
    void fxml_doitChargerSansErreur(String fxmlPath) throws Exception {
        JavaFxTestSupport.runOnFxThread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.load();
                assertNotNull(loader.getController());
            } catch (Exception e) {
                throw new RuntimeException(fxmlPath, e);
            }
        });
    }
}
