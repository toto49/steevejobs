package controller;

import com.eseo.steevejobs.controller.MenuController;
import controller.support.ControllerFieldInjector;
import controller.support.JavaFxTestSupport;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du contrôleur de menu et des badges de notification.
 * <p>
 * Cycle de vie : toolkit JavaFX démarré en {@code @BeforeAll}.
 * Fixtures : boutons avec icône SVG injectés sur le contrôleur.
 * </p>
 */
class MenuControllerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.ensureInitialized();
    }

    @Test
    void mettreAJourBadge_afficheEtMasqueLeCompteur() throws Exception {
        Method mettreAJourBadge = MenuController.class.getDeclaredMethod("mettreAJourBadge", String.class, int.class);
        mettreAJourBadge.setAccessible(true);

        JavaFxTestSupport.runOnFxThread(() -> {
            MenuController controller = new MenuController();
            Button btnTicket = creerBoutonAvecIcone();
            ControllerFieldInjector.inject(controller, "btnAccueil", creerBoutonAvecIcone());
            ControllerFieldInjector.inject(controller, "btnTicket", btnTicket);

            try {
                mettreAJourBadge.invoke(controller, "AUTEUR", 2);
                mettreAJourBadge.invoke(controller, "AUTEUR", 0);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            StackPane graphic = (StackPane) btnTicket.getGraphic();
            Label badge = graphic.getChildren().stream()
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertFalse(badge.isVisible());
        });
    }

    private static Button creerBoutonAvecIcone() {
        Button button = new Button();
        SVGPath icon = new SVGPath();
        icon.setContent("M0,0 L10,0 L10,10 Z");
        button.setGraphic(icon);
        return button;
    }
}
