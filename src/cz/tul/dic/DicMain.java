package cz.tul.dic;

import cz.tul.dic.gui.lang.Lang;
import java.io.IOException;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.FileWriter;

/**
 *
 * @author Petr Jecmen
 */
public class DicMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        configureTinyLog();

        final FXMLLoader fxmlLoader = new FXMLLoader();
        final ResourceBundle rb = Lang.getBundle();
        fxmlLoader.setResources(rb);

        final Parent root = FXMLLoader.load(getClass().getResource("/cz/tul/dic/gui/MainWindow.fxml"), rb);

        final Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle(rb.getString("Title"));
        stage.show();
    }

    private void configureTinyLog() throws IOException {

        Configurator.defaultConfig()
                //                .writer(new FileWriter("log.txt"))
                .writer(new ConsoleWriter())
                .level(LoggingLevel.TRACE)
                .formatPattern("[{level}: {class}.{method}()]\\n  {message}")                
                .activate();
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
