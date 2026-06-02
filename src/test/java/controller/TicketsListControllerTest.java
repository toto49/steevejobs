package controller;

import com.eseo.steevejobs.controller.TicketsListController;
import controller.support.ControllerFieldInjector;
import controller.support.JavaFxTestSupport;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
