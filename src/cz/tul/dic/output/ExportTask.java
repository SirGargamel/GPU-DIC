/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.output;

import cz.tul.dic.data.roi.ROI;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public class ExportTask {

    private static final String SEPARATOR = ";";
    private final ExportMode mode;
    private final ExportTarget target;
    private final Direction direction;
    private final File targetParam;
    private final int[] dataParams;
    private final ROI[] rois;

    public static ExportTask generateExportTask(final String data) {
        final String[] split = data.split(SEPARATOR);
        if (split.length < 4) {
            throw new IllegalArgumentException("Not enough parameters for export task - " + data);
        }

        final int[] dataParams = new int[split.length - 4];
        for (int i = 4; i < split.length; i++) {
            dataParams[i - 4] = Integer.valueOf(split[i]);
        }

        final ExportTask result = new ExportTask(ExportMode.valueOf(split[0]), ExportTarget.valueOf(split[1]), Direction.valueOf(split[2]), new File(split[3]), dataParams);
        return result;
    }

    public ExportTask(final ExportMode mode, final ExportTarget target, final Direction direction, final File targetParam, final int[] dataParams, final ROI... rois) {
        this.mode = mode;
        this.target = target;
        this.direction = direction;
        this.targetParam = targetParam;
        this.dataParams = dataParams;
        this.rois = rois;
    }

    public ExportTask(final ExportMode mode, final ExportTarget target, final Direction direction, final File targetParam, final int[] dataParams) {
        this(mode, target, direction, targetParam, dataParams, (ROI) null);
    }

    public ExportMode getMode() {
        return mode;
    }

    public ExportTarget getTarget() {
        return target;
    }

    public Direction getDirection() {
        return direction;
    }

    public int[] getDataParams() {
        return dataParams;
    }

    public ROI[] getRois() {
        return rois;
    }

    public File getTargetParam() {
        return targetParam;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(mode);
        sb.append(SEPARATOR);
        sb.append(target);
        sb.append(SEPARATOR);
        sb.append(direction);
        sb.append(SEPARATOR);
        sb.append(targetParam);
        sb.append(SEPARATOR);
        if (dataParams != null) {
            for (int i : dataParams) {
                sb.append(Integer.toString(i));
            }            
        }
        sb.append(SEPARATOR);
        sb.setLength(sb.length() - SEPARATOR.length());

        return sb.toString();
    }

}
