/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.debug;

import cz.tul.dic.ComputationException;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.engine.cluster.Analyzer2D;
import cz.tul.dic.engine.displacement.MaxAndWeightedAverage;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Stats implements IGPUResultsReceiver {

    private static final Stats INSTANCE;
    private final EnumMap<Types, Boolean> data;
    private TaskContainer tc;
    private int gpuBatch;

    static {
        INSTANCE = new Stats();
        try {
            INSTANCE.loadConfig(Stats.class.getResourceAsStream("stats.properties"));
        } catch (IOException ex) {
            Logger.error("Error loading internal stats properites.", ex);
        }
        try {
            INSTANCE.loadConfig(new FileInputStream("stats.properties"));
            Logger.info("Loading external stats.properties.");
        } catch (IOException ex) {
            // do nothing, external stats not found
        }
        if (INSTANCE.get(Types.GPU_RESULTS)) {
            Kernel.registerListener(INSTANCE);
        }
    }

    private Stats() {
        data = new EnumMap<>(Types.class);
    }

    public static Stats getInstance() {
        return INSTANCE;
    }

    public void loadConfig(final InputStream in) throws IOException {
        final Properties prop = new Properties();
        prop.load(in);

        Types type;
        Boolean val;
        String valS;
        for (String s : prop.stringPropertyNames()) {
            try {
                type = Types.valueOf(s);
                valS = prop.getProperty(s);
                if (valS.equalsIgnoreCase("true")) {
                    val = Boolean.TRUE;
                } else if (valS.equalsIgnoreCase("false")) {
                    val = Boolean.FALSE;
                } else {
                    throw new IllegalArgumentException("Cannot parse into Boolean: \"" + valS + "\" for property " + type);
                }
                data.put(type, val);
            } catch (IllegalArgumentException ex) {
                Logger.warn("Illegal item in stats properties file - " + ex.getLocalizedMessage());
            }
        }
    }

    private boolean get(Types type) {
        return data.containsKey(type) && data.get(type);
    }

    public void setTaskContainer(final TaskContainer tc) {
        this.tc = tc;
        gpuBatch = 0;
    }

    public void dumpDeformationsStatisticsUsage(final int round) throws IOException {
        if (get(Types.DEF_USAGE)) {
            final ValueCounter counterGood = ValueCounter.createCounter();
            final ValueCounter counterNotGood = ValueCounter.createCounter();
            final ValueCounter quality = ValueCounter.createCounter();
            final Map<ROI, List<CorrelationResult>> results = tc.getResult(round, round + 1).getCorrelations();
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
    }

    public void dumpDeformationsStatisticsUsage() throws IOException {
        if (get(Types.DEF_USAGE)) {
            final ValueCounter counterGood = ValueCounter.createCounter();
            final ValueCounter counterNotGood = ValueCounter.createCounter();
            final ValueCounter quality = ValueCounter.createCounter();
            final Set<Integer> rounds = TaskContainerUtils.getRounds(tc).keySet();
            final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

            int val;
            Map<ROI, List<CorrelationResult>> results;
            for (Integer round : rounds) {
                results = tc.getResult(round, round + 1).getCorrelations();
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
    }

    public void dumpDeformationsStatisticsPerQuality(final int round) throws IOException {
        if (get(Types.DEF_QUALITY)) {
            final Map<Integer, ValueCounter> counters = new HashMap<>();
            final Map<ROI, List<CorrelationResult>> results = tc.getResult(round, round + 1).getCorrelations();

            for (int i = -10; i < 11; i++) {
                counters.put(i, ValueCounter.createCounter());
            }

            int val;
            ValueCounter counter;
            for (ROI roi : results.keySet()) {
                for (CorrelationResult cr : results.get(roi)) {
                    if (cr != null) {
                        val = (int) (cr.getValue() * 10);
                        counter = counters.get(val);
                        if (counter != null) {
                            counter.inc(cr.getDeformation());
                        } else {
                            Logger.warn("Illegal correlation value - {0}", cr.getValue());
                        }
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
    }

    public void dumpDeformationsStatisticsPerQuality() throws IOException {
        if (get(Types.DEF_QUALITY)) {
            final Map<Integer, ValueCounter> counters = new HashMap<>();
            final Set<Integer> rounds = TaskContainerUtils.getRounds(tc).keySet();

            for (int i = 0; i < 11; i++) {
                counters.put(i, ValueCounter.createCounter());
            }

            int val;
            ValueCounter counter;
            Map<ROI, List<CorrelationResult>> results;
            for (Integer round : rounds) {
                results = tc.getResult(round, round + 1).getCorrelations();
                if (results != null) {
                    for (ROI roi : results.keySet()) {
                        for (CorrelationResult cr : results.get(roi)) {
                            if (cr != null) {
                                val = (int) (cr.getValue() * 10);
                                counter = counters.get(val);
                                if (counter != null) {
                                    counter.inc(cr.getDeformation());
                                }
                            }
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
    }

    @Override
    public void dumpGpuResults(final float[] resultData, final List<Facet> facets, final List<double[]> deformationLimits) {
        if (get(Types.GPU_RESULTS)) {
            final File outFile = new File(NameGenerator.generateGpuResultsDump(tc, gpuBatch++));
            outFile.getParentFile().mkdirs();
            try (BufferedWriter out = new BufferedWriter(new FileWriter(outFile))) {
                out.newLine();

                final int defCountPerFacet = resultData.length / facets.size();
                int facetCounter = 0;
                for (int i = 0; i < facets.size(); i++) {
                    out.write(facets.get(i).toString());
                    out.write(Arrays.toString(deformationLimits.get(i)));
                    out.newLine();
                    for (int r = facetCounter * defCountPerFacet; r < (facetCounter + 1) * defCountPerFacet; r++) {
                        out.write(Float.toString(resultData[r]));
                        out.newLine();
                    }
                    facetCounter++;
                }
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Stats.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    private static void saveDump(final String fileName, final String textDump) throws IOException {
        final File outFile = new File(fileName);
        outFile.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(outFile)) {
            out.write(textDump);
        }
    }

    public void drawFacetQualityStatistics(final Map<ROI, List<Facet>> allFacets, final int roundFrom, final int roundTo) throws IOException, ComputationException {
        if (get(Types.FACET_QUALITY)) {
            final File out = new File(NameGenerator.generateQualityMapFacet(tc, roundTo));
            out.getParentFile().mkdirs();

            final Map<ROI, List<CorrelationResult>> allResults = tc.getResult(roundFrom, roundTo).getCorrelations();
            final Image img = tc.getImage(roundTo);
            final double[][] resultData = Utils.generateNaNarray(img.getWidth(), img.getHeight());
            List<CorrelationResult> results;
            List<Facet> facets;
            double[] center;
            CorrelationResult result;
            for (ROI roi : allResults.keySet()) {
                results = allResults.get(roi);
                facets = allFacets.get(roi);
                for (int i = 0; i < results.size(); i++) {
                    center = facets.get(i).getCenter();
                    result = results.get(i);
                    if (result != null) {
                        resultData[(int) Math.round(center[0])][(int) Math.round(center[1])] = result.getValue();
                    }
                }
            }
            ImageIO.write(ExportUtils.overlayImage(img, ExportUtils.createImageFromMap(resultData, Direction.Dabs)), "BMP", out);
        }
    }

    public void drawPointResultStatistics(final int roundFrom, final int roundTo) throws IOException, ComputationException {
        if (get(Types.POINT_QUALITY)) {
            final File out = new File(NameGenerator.generateQualityMapPoint(tc, roundTo));
            out.getParentFile().mkdirs();

            ImageIO.write(ExportUtils.overlayImage(tc.getImage(roundTo), ExportUtils.createImageFromMap(tc.getResult(roundFrom, roundTo).getDisplacementResult().getQuality(), Direction.Dabs)), "BMP", out);
        }
    }

    public void exportPointSubResultsStatistics(final Analyzer2D counter, final String name) {
        if (get(Types.POINT_STATS)) {
            final File out = new File(name);
            out.getParentFile().mkdirs();

            final List<double[]> vals = counter.listValues();
            final String[][] values = new String[vals.size()][2];
            final double precision = counter.getPrecision();
            for (int i = 0; i < vals.size(); i++) {
                values[i][0] = Utils.format(precision * (int) Math.round(vals.get(i)[0] / precision));
                values[i][1] = Utils.format(precision * (int) Math.round(vals.get(i)[1] / precision));
            }
            try {
                CsvWriter.writeDataToCsv(new File(name), values);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(MaxAndWeightedAverage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void drawRegressionQualities(final Image img, final double[][][] resultQuality, final String nameA, final String nameB) throws ComputationException {
        if (get(Types.REGRESSION_QUALITY)) {
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

    public boolean isGpuDebugEnabled() {
        return DebugControl.isDebugMode() && Stats.getInstance().get(Types.GPU_RESULTS);
    }

    public static enum Types {

        GPU_RESULTS,
        DEF_USAGE,
        DEF_QUALITY,
        FACET_QUALITY,
        POINT_QUALITY,
        POINT_STATS,
        REGRESSION_QUALITY

    }

}
