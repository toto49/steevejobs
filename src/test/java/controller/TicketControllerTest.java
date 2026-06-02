package controller;

import com.eseo.steevejobs.controller.TicketController;
import com.eseo.steevejobs.model.Ticket;
import com.eseo.steevejobs.service.SessionService;
import controller.support.ControllerFieldInjector;
import controller.support.JavaFxTestSupport;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.support.TestDataFactory;

import static org.junit.jupiter.api.Assertions.*;

class TicketControllerTest {

    private TicketController controller;

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.ensureInitialized();
    }

    @BeforeEach
    void setUp() throws Exception {
        TicketController.fermerChat();
        SessionService.setUtilisateurConnecte(TestDataFactory.utilisateurActif(1, "user@test.fr"));

        JavaFxTestSupport.runOnFxThread(() -> {
            controller = new TicketController();
            ControllerFieldInjector.inject(controller, "chatMessagesContainer", new VBox());
            ControllerFieldInjector.inject(controller, "messageInput", new TextField());
            ControllerFieldInjector.inject(controller, "messageScrollPane", new ScrollPane());
            ControllerFieldInjector.inject(controller, "ticketTitleLabel", new Label());
            ControllerFieldInjector.inject(controller, "ticketObjectLabel", new Label());
            ControllerFieldInjector.inject(controller, "serviceLabel", new Label());
            ControllerFieldInjector.inject(controller, "statusLabel", new Label());
            ControllerFieldInjector.inject(controller, "dateLabel", new Label());
            ControllerFieldInjector.inject(controller, "descriptionLabel", new Label());
            ControllerFieldInjector.inject(controller, "actionButton", new javafx.scene.control.Button());
            controller.initialize();
        });
    }

    @AfterEach
    void tearDown() {
        TicketController.fermerChat();
        SessionService.setUtilisateurConnecte(null);
    }

    @Test
    void initData_doitEnregistrerInstanceEtTicketId() throws Exception {
        JavaFxTestSupport.runOnFxThread(() -> {
            controller.initData(42);
            assertSame(controller, TicketController.getActiveInstance());
            assertEquals(42, controller.getCurrentTicketId());
        });
    }

    @Test
    void fermerChat_doitReinitialiserInstanceActive() throws Exception {
        JavaFxTestSupport.runOnFxThread(() -> {
            controller.initData(7);
            TicketController.fermerChat();
            assertNull(TicketController.getActiveInstance());
        });
    }

    @Test
    void handleSendMessage_messageVide_neAjoutePasDeBulle() throws Exception {
        VBox container = new VBox();
        TextField input = new TextField("   ");

        JavaFxTestSupport.runOnFxThread(() -> {
            ControllerFieldInjector.inject(controller, "chatMessagesContainer", container);
            ControllerFieldInjector.inject(controller, "messageInput", input);

            Ticket ticket = new Ticket();
            ticket.setId(1);
            ControllerFieldInjector.inject(controller, "currentTicket", ticket);

            controller.handleSendMessage();

            assertTrue(container.getChildren().isEmpty());
            assertEquals("   ", input.getText());
        });
    }
}
