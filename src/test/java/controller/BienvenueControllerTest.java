package controller;

import com.eseo.steevejobs.controller.BienvenueController;
import controller.support.ControllerFieldInjector;
import controller.support.JavaFxTestSupport;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class BienvenueControllerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.ensureInitialized();
    }

    @Test
    void onLoginClick_champsVides_doitAfficherMessageErreur() throws Exception {
        Method onLoginClick = BienvenueController.class.getDeclaredMethod("onLoginClick", javafx.event.ActionEvent.class);
        onLoginClick.setAccessible(true);

        JavaFxTestSupport.runOnFxThread(() -> {
            BienvenueController controller = new BienvenueController();
            TextField mail = new TextField("");
            PasswordField mdp = new PasswordField();
            CheckBox save = new CheckBox();
            Text error = new Text();

            ControllerFieldInjector.inject(controller, "mail_connexion", mail);
            ControllerFieldInjector.inject(controller, "mdp_connexion", mdp);
            ControllerFieldInjector.inject(controller, "save_connexion", save);
            ControllerFieldInjector.inject(controller, "errror_connexion", error);

            try {
                onLoginClick.invoke(controller, new Object[]{null});
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            assertEquals("Veuillez remplir tous les champs.", error.getText());
        });
    }
}
