package cz.tul.dic.debug;

import cz.tul.dic.ComputationException;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.CorrelationResult;
import cz.tul.dic.engine.cluster.Analyzer2D;
import cz.tul.dic.engine.displacement.FindMaxAndAverage;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Stats {

    public static void dumpDeformationsStatisticsUsage(final TaskContainer tc, final int round) throws IOException {
        final ValueCounter counterGood = ValueCounter.createCounter();
        final ValueCounter counterNotGood = ValueCounter.createCounter();
        final ValueCounter quality = ValueCounter.createCounter();
        final Map<ROI, List<CorrelationResult>> results = tc.getResults(round);
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        int val;
        for (ROI roi : results.keySet()) {
            for (CorrelationResult cr : results.get(roi)) {
                if (cr != null) {
                    val = (int) (cr.getValue() * 10);
                    quality.inc(val / (double) 10);
                    if (cr.getValue() < resultQuality) {
                        counterNotGood.inc(cr.getDeformation());
                    } else {
                        counterGood.inc(cr.getDeformation());
                    }
                } else {
                    counterNotGood.inc();
                    quality.inc();
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- ROUND\n");
        sb.append("-- GOOD --");
        sb.append(counterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(counterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(quality.toString());
        saveDump(NameGenerator.generateDeformationQualityUsageDump(tc, round), sb.toString());
    }

    public static void dumpDeformationsStatisticsUsage(final TaskContainer tc) throws IOException {
        final ValueCounter counterGood = ValueCounter.createCounter();
        final ValueCounter counterNotGood = ValueCounter.createCounter();
        final ValueCounter quality = ValueCounter.createCounter();
        final Set<Integer> rounds = TaskContainerUtils.getRounds(tc).keySet();
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        int val;
        Map<ROI, List<CorrelationResult>> results;
        for (Integer round : rounds) {
            results = tc.getResults(round);
            if (results != null) {
                for (ROI roi : results.keySet()) {
                    for (CorrelationResult cr : results.get(roi)) {
                        if (cr != null) {
                            val = (int) (cr.getValue() * 10);
                            quality.inc(val / (double) 10);
                            if (cr.getValue() < resultQuality) {
                                counterNotGood.inc(cr.getDeformation());
                            } else {
                                counterGood.inc(cr.getDeformation());
                            }
                        } else {
                            counterNotGood.inc();
                            quality.inc();
                        }
                    }
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- TASK\n");
        sb.append("-- GOOD --");
        sb.append(counterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(counterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(quality.toString());
        saveDump(NameGenerator.generateDeformationQualityUsageDump(tc, -1), sb.toString());
    }

    public static void dumpDeformationsStatisticsPerQuality(final TaskContainer tc, final int round) throws IOException {
        final Map<Integer, ValueCounter> counters = new HashMap<>();
        final Map<ROI, List<CorrelationResult>> results = tc.getResults(round);

        for (int i = 0; i < 11; i++) {
            counters.put(i, ValueCounter.createCounter());
        }

        int val;
        ValueCounter counter;
        for (ROI roi : results.keySet()) {
            for (CorrelationResult cr : results.get(roi)) {
                if (cr != null) {
                    val = (int) (cr.getValue() * 10);
                    counter = counters.get(val);
                    counter.inc(cr.getDeformation());
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics per quality --- ROUND");
        for (int i = 0; i < 11; i++) {
            sb.append("\n-- Quality above ");
            sb.append(Utils.format(i / 10.0));
            sb.append(" --");
            sb.append(counters.get(i).toString());
        }
        saveDump(NameGenerator.generateDeformationQualityDump(tc, round), sb.toString());
    }

    public static void dumpDeformationsStatisticsPerQuality(final TaskContainer tc) throws IOException {
        final Map<Integer, ValueCounter> counters = new HashMap<>();
        final Set<Integer> rounds = TaskContainerUtils.getRounds(tc).keySet();

        for (int i = 0; i < 11; i++) {
            counters.put(i, ValueCounter.createCounter());
        }

        int val;
        ValueCounter counter;
        Map<ROI, List<CorrelationResult>> results;
        for (Integer round : rounds) {
            results = tc.getResults(round);
            for (ROI roi : results.keySet()) {
                for (CorrelationResult cr : results.get(roi)) {
                    if (cr != null) {
                        val = (int) (cr.getValue() * 10);
                        counter = counters.get(val);
                        counter.inc(cr.getDeformation());
                    }
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics per quality --- TASK");
        for (int i = 0; i < 11; i++) {
            sb.append("\n-- Quality above ");
            sb.append(Utils.format(i / 10.0));
            sb.append(" --");
            sb.append(counters.get(i).toString());
        }        
        saveDump(NameGenerator.generateDeformationQualityDump(tc, -1), sb.toString());        
    }
    
    private static void saveDump(final String fileName, final String textDump) throws IOException {
        final File outFile = new File(fileName);
        outFile.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(outFile)) {
            out.write(textDump);
        }
    }

    public static void drawFacetQualityStatistics(final TaskContainer tc, final Map<ROI, List<Facet>> allFacets, final int roundFrom, final int roundTo) throws IOException, ComputationException {
        final File out = new File(NameGenerator.generateQualityMapFacet(tc, roundTo));
        out.getParentFile().mkdirs();

        final Map<ROI, List<CorrelationResult>> allResults = tc.getResults(roundFrom);
        final Image img = tc.getImage(roundTo);
        final double[][] resultData = Utils.generateNaNarray(img.getWidth(), img.getHeight());
        List<CorrelationResult> results;
        List<Facet> facets;
        double[] center;
        for (ROI roi : allResults.keySet()) {
            results = allResults.get(roi);
            facets = allFacets.get(roi);
            for (int i = 0; i < results.size(); i++) {
                center = facets.get(i).getCenter();
                if (results.get(i) != null) {
                    resultData[(int) Math.round(center[0])][(int) Math.round(center[1])] = results.get(i).getValue();
                }
            }
        }
        ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultData, Direction.Dabs)), "BMP", out);
    }

    public static void drawPointResultStatistics(final TaskContainer tc, final int roundFrom, final int roundTo) throws IOException, ComputationException {
        final File out = new File(NameGenerator.generateQualityMapPoint(tc, roundTo));
        out.getParentFile().mkdirs();

        ImageIO.write(ExportUtils.overlayImage(tc.getImage(roundTo), ExportUtils.createImageFromMap(tc.getDisplacement(roundFrom, roundTo).getQuality(), Direction.Dabs)), "BMP", out);
    }

    public static void exportPointSubResultsStatistics(final Analyzer2D counter, final String name) {
        final File out = new File(name);
        out.getParentFile().mkdirs();

        final List<double[]> vals = counter.listValues();
        final String[][] data = new String[vals.size()][2];
        final double precision = counter.getPrecision();
        for (int i = 0; i < vals.size(); i++) {
            data[i][0] = Utils.format(precision * (int) Math.round(vals.get(i)[0] / precision));
            data[i][1] = Utils.format(precision * (int) Math.round(vals.get(i)[1] / precision));
        }
        try {
            CsvWriter.writeDataToCsv(new File(name), data);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FindMaxAndAverage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void drawRegressionQualities(final Image img, final double[][][] resultQuality, final String nameA, final String nameB) throws ComputationException {
        final File out = new File(nameA);
        out.getParentFile().mkdirs();

        try {
            ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultQuality[0], Direction.Exx)), "BMP", new File(nameA));
            ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultQuality[1], Direction.Eyy)), "BMP", new File(nameB));
        } catch (IOException ex) {
            Logger.warn(ex);
        }
    }

}
