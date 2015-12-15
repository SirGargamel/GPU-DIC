/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.gui.Context;
import cz.tul.dic.gui.MainWindow;
import cz.tul.dic.gui.lang.Lang;
import cz.tul.dic.data.task.loaders.InputLoader;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.policies.StartupPolicy;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.RollingFileWriter;

/**
 *
 * @author Petr Jecmen
 */
public class DicMain extends Application {

    private static final boolean DEBUG_COMPUTE_PREPROCESSING = false;
    private static final String DEBUG_SMALL = "-d";
    private static final String DEBUG_COMPUTE = "-debug";
    private static final String LICENSE_FILE = "license.dat";
    private static final File LICENSE = new File(LICENSE_FILE);
    private static final String[] FILES_TO_DEBUG = new String[]{
        //        "d:\\temp\\.test FS vs Quality\\6107544m.avi.config",
        //        "d:\\temp\\.test FS vs Quality\\6113599m.avi.config",
        //        "d:\\temp\\.test FS vs Quality\\6203652m.avi.config",
        //        "d:\\temp\\.test FS vs Quality\\7202845m.avi.config",
        //        "d:\\temp\\.test FS vs Quality\\9112502m.avi.config",
        //        "d:\\temp\\.test FS vs Quality\\9905121m.avi.config",
        //        "d:\\temp\\.test spacing\\6107544m\\6107544m.avi.config",
        //        "d:\\temp\\.test spacing\\6113599m\\6113599m.avi.config",
        //        "d:\\temp\\.test spacing\\6203652m\\6203652m.avi.config",
        //        "d:\\temp\\.test spacing\\7202845m\\7202845m.avi.config",
        //        "d:\\temp\\.test spacing\\9112502m\\9112502m.avi.config",
        //        "d:\\temp\\.test spacing\\9905121m\\9905121m.avi.config",                
        //////////////////////////////
        "d:\\temp\\.smallSolverCompare\\6107544m.avi.config",
        "d:\\temp\\.smallSolverCompare\\6113599m.avi.config",
        "d:\\temp\\.smallSolverCompare\\6203652m.avi.config",
        "d:\\temp\\.smallSolverCompare\\7202845m.avi.config",
        "d:\\temp\\.smallSolverCompare\\9112502m.avi.config",
        "d:\\temp\\.smallSolverCompare\\9905121m.avi.config", 
    ////////////////////////////
            "d:\\temp\\.solverCompare\\6107544m.avi.config",
            "d:\\temp\\.solverCompare\\6113599m.avi.config",
            "d:\\temp\\.solverCompare\\6203652m.avi.config",
            "d:\\temp\\.solverCompare\\7202845m.avi.config",
            "d:\\temp\\.solverCompare\\9112502m.avi.config",
            "d:\\temp\\.solverCompare\\9905121m.avi.config",
    ///////////////////////////////
    //            "z:\\TUL\\DIC\\testData\\.custom\\ShiftX.config",
    //            "z:\\TUL\\DIC\\testData\\.custom\\ShiftXY.config",
    //            "z:\\TUL\\DIC\\testData\\.custom\\StretchX.config",
    };

    @Override
    public void start(Stage stage) throws Exception {
        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();

        if (parameters.contains(DEBUG_COMPUTE) || parameters.contains(DEBUG_SMALL)) {
            configureTinyLog(true);
            DebugControl.startDebugMode();
        } else {
            configureTinyLog(false);
        }

        Stats.getInstance();

        if (parameters.contains(DEBUG_COMPUTE)) {
            if (DEBUG_COMPUTE_PREPROCESSING) {
                performPreprocessingTest();
            } else {
//                performComputationTest();
                performEngineTest();
            }
        }

        boolean validLicense = Utils.checkLicense(LICENSE);
        if (!validLicense) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Lang.getString("LicenseMissingTitle"));
            alert.setHeaderText(null);
            alert.setContentText(Lang.getString("LicenseMissingText"));
            alert.showAndWait();

            final FileChooser chooser = new FileChooser();
            chooser.setTitle(Lang.getString("LicenseMissingSelectTitle"));
            chooser.setInitialFileName(LICENSE_FILE);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("License file [license.dat]", LICENSE_FILE));
            final File license = chooser.showOpenDialog(null);
            if (license != null) {
                validLicense = Utils.checkLicense(license);
                if (validLicense) {
                    Files.copy(license.toPath(), LICENSE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(Lang.getString("LicenseMissingTitle"));
                alert.setHeaderText(null);
                alert.setContentText(Lang.getString("LicenseMissingText2"));
                alert.showAndWait();
            }
        }

        if (validLicense) {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            final ResourceBundle rb = Lang.getBundle();
            fxmlLoader.setResources(rb);

            final Parent root = FXMLLoader.load(getClass().getResource("/cz/tul/dic/gui/MainWindow.fxml"), rb);

            final Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("logo.png")));
            stage.setTitle(rb.getString("Title"));
            stage.setResizable(false);
            stage.show();
        }
    }

    private static void configureTinyLog(final boolean debug) throws IOException {
        Configurator c = Configurator.defaultConfig();
        c.writingThread(true);
        try {
            c.writer(new MultiWriter(new ConsoleWriter(), new RollingFileWriter("log.txt", 5, new StartupPolicy())));
        } catch (Exception ex) {
            c.writer(new ConsoleWriter());
            Logger.error(ex);
        }
        if (debug) {
            c.level(Level.TRACE);
        } else {
            c.level(Level.INFO);
        }
        c.activate();

        if (debug) {
            Logger.info("----- Starting APP ----- DEBUG -----");
        } else {
            Logger.info("----- Starting APP -----");
        }
    }

    private static void performComputationTest() {
        final int fs1 = 5; //10
        final int fs2 = 30; //30        
        TaskContainer task;
        for (int size = fs1; size <= fs2; size += 5) {
            for (String s : FILES_TO_DEBUG) {
                try {
                    Context.getInstance().setTc(TaskContainer.initTaskContainer(new File(s)));
                    task = Context.getInstance().getTc();
                    if ((int) task.getParameter(TaskParameter.SUBSET_SIZE) < size) {
                        System.out.println("STOPPING --- " + task.getParameter(TaskParameter.SUBSET_SIZE) + " --- " + size + " --- " + s);
                        break;
                    }
                    task.setParameter(TaskParameter.IN, new File(s));
                    task.setParameter(TaskParameter.SUBSET_SIZE, size);
                    task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, size / 2);
//                    tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, (double) size);
//                    tc.setParameter(TaskParameter.SOLVER, Solver.CoarseFine);                                        

//                    commenceComputation(task);                    
                    commenceComputationDynamic(task);
//                    commenceComputationDynamicSpacingSweep(tc, 1, fs2 / 2);

                    exportTask(task);
                } catch (IOException | ComputationException ex) {
                    Logger.error(ex);
                } catch (Exception t) {
                    Logger.error(t);
                    System.out.println(Context.getInstance().getTc());
                }
            }
        }
    }

    private static void exportTask(final TaskContainer task) {
        for (int r : TaskContainerUtils.getRounds(task).values()) {
//            // displacements
//            task.addExport(ExportTask.generateMapExport(Direction.D_DX, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.D_DX)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.D_DY, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.D_DY)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.D_DABS, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.D_DABS)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.DX, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.DX)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.DY, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.DY)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.DABS, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.DABS)), r, null));
//            // strains
//            task.addExport(ExportTask.generateMapExport(Direction.EXX, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.EXX)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.EYY, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.EYY)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.EXY, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.EXY)), r, null));
//            task.addExport(ExportTask.generateMapExport(Direction.EABS, ExportTarget.FILE, new File(NameGenerator.generateMap(task, r, Direction.EABS)), r, null));
        }
    }

    private static void commenceComputation(TaskContainer task) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(task);

        long time = System.nanoTime();
        Engine.getInstance().computeTask(task);
        time = System.nanoTime() - time;
        printInfo("task", task, time);
    }

    private static void printInfo(final String descr, final TaskContainer task, final long time) {
        Logger.info("Finished " + descr + ": " + task.getParameter(TaskParameter.SUBSET_SIZE) + "/" + task.getParameter(TaskParameter.SUBSET_GENERATOR_PARAM) + "/" + task.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
    }

    private static void commenceComputationDynamic(TaskContainer task) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(task);

        long time = System.nanoTime();
        final ComplexTaskSolver cts = new ComplexTaskSolver(task);
        cts.solveComplexTask();
        time = System.nanoTime() - time;
        printInfo("dynamic task", task, time);
    }

    private static void commenceComputationDynamicStrainParamSweep(final TaskContainer task, final double strainParamMin, final double strainParamMax) throws ComputationException, IOException {
        commenceComputationDynamic(task);

        // strain sweep and export               
        for (double strainParam = strainParamMin; strainParam <= strainParamMax; strainParam++) {
            task.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, strainParam);
            StrainEstimator.computeStrain(task);

//            tc.getExports().clear();
//            for (int r : TaskContainerUtils.getRounds(tc).values()) {
//                tc.addExport(ExportTask.generateMapExport(Direction.EXX, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.EXX)), r, null));
//                tc.addExport(ExportTask.generateMapExport(Direction.EYY, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.EYY)), r, null));
//                tc.addExport(ExportTask.generateMapExport(Direction.EXY, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.EXY)), r, null));
//                tc.addExport(ExportTask.generateMapExport(Direction.EABS, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.EABS)), r, null));                
//            }
//            Exporter.export(tc);
            TaskContainerUtils.serializeTaskToBinary(task, new File(NameGenerator.generateBinary(task)));
        }
    }

    private static void commenceComputationDynamicSpacingSweep(final TaskContainer task, final int spacingMin, final int spacingMax) throws ComputationException, IOException {
        for (int param = spacingMax; param >= spacingMin; param--) {
            task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, param);
            commenceComputationDynamic(task);
        }
    }

    private static void performEngineTest() {
        final int fs1 = 5; //10
        final int fs2 = 30; //30        
        TaskContainer task;
        for (int size = fs1; size <= fs2; size += 5) {
            for (String s : FILES_TO_DEBUG) {
                for (Solver slvr : Solver.values()) {
                    if (slvr == Solver.BRUTE_FORCE) {
                        continue;
                    }

                    try {
                        Context.getInstance().setTc(TaskContainer.initTaskContainer(new File(s)));
                        task = Context.getInstance().getTc();
                        if ((int) task.getParameter(TaskParameter.SUBSET_SIZE) < size) {
                            System.out.println("STOPPING --- " + task.getParameter(TaskParameter.SUBSET_SIZE) + " --- " + size + " --- " + s);
                            break;
                        }
                        task.setParameter(TaskParameter.IN, new File(s));
                        task.setParameter(TaskParameter.SUBSET_SIZE, size);
                        task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, size / 2);
                        task.setParameter(TaskParameter.SOLVER, slvr);

                        commenceComputationDynamic(task);

                        exportTask(task);
                    } catch (IOException | ComputationException ex) {
                        Logger.error(ex);
                    } catch (Exception t) {
                        Logger.error(t);
                        System.out.println(Context.getInstance().getTc());
                    }
                }
            }
        }
    }

    private static void performPreprocessingTest() throws ComputationException, IOException {
        final String pathBase = "d:\\Tul\\DIC_Preprocess\\";

        final List<String> configs = new ArrayList<>(20);
//        configs.add("6107544m.avi00015.bmp.config");
//        configs.add("6203652m.avi00014.bmp.config");
//        configs.add("7202845m.avi00004.bmp.config");
//        configs.add("9112502m.avi00016.bmp.config");
//        configs.add("Sample3 Reference.bmp.config");
//        configs.add("Sample4 Reference.bmp.config");
        configs.add("Sample7-Reference Image.bmp.config");
//        configs.add("trs2_b8_00.bmp.config");
//        configs.add("trxy_s2_00.bmp.config");

        final List<String> filters = new ArrayList<>(7);
        filters.add("orig");
        filters.add("histogram");
        filters.add("median");
        filters.add("bilateral");
        filters.add("clahe");
        filters.add("lucyRichardson");
        filters.add("wiener");
        filters.add("gaussian");
        filters.add("binomial");

        final List<Solver> solvers = new ArrayList<>();
        solvers.add(Solver.COARSE_FINE);
        solvers.add(Solver.NEWTON_RHAPSON_CENTRAL);
//        solvers.add(Solver.NEWTON_RHAPSON_CENTRAL_HE);

        TaskContainer task;
        for (String in : configs) {
            task = TaskContainer.initTaskContainer(new File(pathBase.concat(in)));

            for (String filter : filters) {
                for (int size = 5; size <= 15; size += 5) {
                    task.setParameter(TaskParameter.SUBSET_SIZE, size);

                    for (Solver solver : solvers) {
                        task.setParameter(TaskParameter.SOLVER, solver);

                        final double roiWidth;
                        if (task.getRois(0) != null) {
                            roiWidth = task.getRois(0).iterator().next().getWidth();
                        } else {
                            roiWidth = task.getImage(0).getWidth();
                        }
                        task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, Math.max((int) roiWidth / 5, 2 * size));
                        findAllConfigurationsAndCompute(task, filter);
                    }
                }
            }
        }
    }

    private static void findAllConfigurationsAndCompute(final TaskContainer task, final String filter) throws ComputationException, IOException {
        final List<File> input = task.getInput();

        final File imageA = input.get(0);
        final File filterDir = new File(imageA.getParent().concat(File.separator).concat(filter));
        final String imageAname = imageA.getName();
        final int indexA = imageAname.lastIndexOf('.');
        final String imageAtitle = imageAname.substring(0, indexA);
        final String imageAext = imageAname.substring(indexA);
        final File[] imagesA = filterDir.listFiles((File dir, String name) -> name.startsWith(imageAtitle) && name.endsWith(imageAext));

        final File imageB = input.get(1);
        final String imageBname = imageB.getName();
        final int indexB = imageBname.lastIndexOf('.');
        final String imageBtitle = imageBname.substring(0, indexB);
        final String imageBext = imageBname.substring(indexB);
        final File[] imagesB = filterDir.listFiles((File dir, String name) -> name.startsWith(imageBtitle) && name.endsWith(imageBext));

        TaskContainer newTask = null;
        List<File> inputs;
        for (int i = 0; i < imagesA.length; i++) {
            try {
                newTask = new TaskContainer(task);
                inputs = newTask.getInput();
                inputs.clear();
                inputs.add(imagesA[i]);
                inputs.add(imagesB[i]);

                newTask.setParameter(TaskParameter.IN, imagesA[i]);
                InputLoader.loadInput(newTask, newTask);

                Engine.getInstance().computeTask(newTask);
                Logger.info("Computed {}", imagesA[i].getAbsolutePath());
            } catch (Exception ex) {
                Logger.error(ex, "{}", newTask);
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
