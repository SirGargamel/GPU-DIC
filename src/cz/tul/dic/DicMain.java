package cz.tul.dic;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Petr Jecmen
 */
public class DicMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        ResourceBundle rb = ResourceBundle.getBundle("cz.tul.dic.gui.lang.Lang", Locale.getDefault());
        fxmlLoader.setResources(rb);

        Parent root = FXMLLoader.load(getClass().getResource("/cz/tul/dic/gui/MainWindow.fxml"), rb);

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle(rb.getString("Title"));
        stage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
