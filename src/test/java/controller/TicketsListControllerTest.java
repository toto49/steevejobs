package controller;

import com.eseo.steevejobs.controller.TicketsListController;
import controller.support.ControllerFieldInjector;
import controller.support.JavaFxTestSupport;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires du contrôleur de liste de tickets (titre de page selon le filtre).
 * <p>
 * Cycle de vie : initialisation JavaFX en {@code @BeforeAll} ; exécution des scénarios sur le thread FX.
 * </p>
 */
class TicketsListControllerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.ensureInitialized();
    }

    @Test
    void initData_doitMettreAJourLeTitre() throws Exception {
        JavaFxTestSupport.runOnFxThread(() -> {
            TicketsListController controller = new TicketsListController();
            Label title = new Label();
            ControllerFieldInjector.inject(controller, "titlepageticket", title);

            controller.initData("rh");

            assertEquals("TICKETS RH", title.getText());
        });
    }
}
