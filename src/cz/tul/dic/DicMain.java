package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.CorrelationCalculator;
import cz.tul.dic.gui.Context;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.labellers.TimestampLabeller;
import org.pmw.tinylog.policies.DailyPolicy;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.RollingFileWriter;

/**
 *
 * @author Petr Jecmen
 */
public class DicMain extends Application {

    private static final String DEBUG = "debug";

    @Override
    public void start(Stage stage) throws Exception {
        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();

        if (parameters.contains(DEBUG)) {
            configureTinyLog(true);

            NameGenerator.enableDebugMode();
            Utils.enableDebugMode();
            ExportUtils.enableDebugMode();

            performComputationTest();
        } else {
            configureTinyLog(false);
        }

        final FXMLLoader fxmlLoader = new FXMLLoader();
        final ResourceBundle rb = Lang.getBundle();
        fxmlLoader.setResources(rb);

        final Parent root = FXMLLoader.load(getClass().getResource("/cz/tul/dic/gui/MainWindow.fxml"), rb);

        final Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle(rb.getString("Title"));
        stage.setResizable(false);
        stage.show();
    }

    private void configureTinyLog(final boolean debug) throws IOException {
        Configurator c = Configurator.defaultConfig();
        c.writingThread(true);
        if (debug) {
            c.writer(new ConsoleWriter())
                    .level(LoggingLevel.TRACE);
        } else {
            final DateFormat df = new SimpleDateFormat("");

            final StringBuilder sb = new StringBuilder();
            sb.append("log__");
            sb.append(df.format(Calendar.getInstance().getTime()));
            sb.append(".txt");
//            c.writer(new FileWriter(sb.toString()))
//                    .level(LoggingLevel.INFO);
            c.writer(new RollingFileWriter("log.txt", 10, new TimestampLabeller("yyyy-MM-dd"), new DailyPolicy()))
                    .level(LoggingLevel.INFO);
        }
        c.activate();

        Logger.info("----- Starting APP -----");
    }

    private void performComputationTest() {
        final int fs1 = 15;
        final int fs2 = 15;
        final double ps1 = 10;
        final double ps2 = 50;
        TaskContainer tc;
        for (int size = fs1; size <= fs2; size++) {
            try {
//            Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\7202845m.avi.config")));
//            TaskContainer tc = Context.getInstance().getTc();
//            InputLoader.loadInput(tc);
//            tc.setParameter(TaskParameter.FACET_SIZE, 20);
//            tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
//            Computation.commenceComputationDynamic(tc);
//
//            TaskContainerUtils.serializeTaskToBinary(tc, new File("D:\\temp\\7202845m.avi.test.task"));

                // 7202845m
                // 9905121m
                // 9820088m
//                for (FacetGeneratorMode fgm : FacetGeneratorMode.values()) {
//                for (int windowSize = 0; windowSize < 1; windowSize++) {
//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("d:\\temp\\simulace\\Pldi_Deska_s_otvorem_20snTexturaCoarse.avi.config")));
//                tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.setParameter(TaskParameter.FACET_SIZE, size);
//                Computation.commenceComputation(tc);
//                }
                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("d:\\temp\\7202845m\\7202845m.avi-oneRound-classic.config")));
                tc = Context.getInstance().getTc();
                InputLoader.loadInput(tc);
                tc.setParameter(TaskParameter.FACET_SIZE, size);
                Computation.commenceComputationDynamic(tc);
//                
//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\7202845m\\7202845m.avi.config")));
//                tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.setParameter(TaskParameter.FACET_SIZE, size);
//                Computation.commenceComputationDynamic(tc);
//                
//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\7202845m.avi-classic.config")));
//                tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.setParameter(TaskParameter.FACET_SIZE, size);
//                Computation.commenceComputationDynamicStrainParamSweep(tc, ps1, ps2);
//                
//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\9820088m.avi-classic.config")));
//                tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.setParameter(TaskParameter.FACET_SIZE, size);
//                Computation.commenceComputationDynamic(tc);
//                CorrelationCalculator.dumpCounterStats();

//                Context.getInstance().setTc(TaskContainerUtils.deserializeTaskFromConfig(new File("D:\\temp\\9905121m\\9905121m.avi.config")));
//                tc = Context.getInstance().getTc();
//                InputLoader.loadInput(tc);
//                tc.setParameter(TaskParameter.FACET_SIZE, size);
//                Computation.commenceComputationDynamic(tc);
//                }

            } catch (IOException | ComputationException ex) {
                Logger.error(ex);
            }
        }
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
