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
    private final Object targetParam;
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

        final ExportTask result = new ExportTask(Direction.valueOf(split[2]), ExportMode.valueOf(split[0]), ExportTarget.valueOf(split[1]), new File(split[3]), dataParams);
        return result;
    }
    
    public static ExportTask generateMapExport(final Direction dir, final ExportTarget target, final Object targetArg, final int round, final ROI... rois) {
        return new ExportTask(dir, ExportMode.MAP, target, targetArg, new int[] {round}, rois);
    }
    
    public static ExportTask generateLineExport(final Direction dir, final ExportTarget target, final Object targetArg, final int x, final int y, final ROI... rois) {
        return new ExportTask(dir, ExportMode.LINE, target, targetArg, new int[]{x, y}, rois);
    }
    
    public static ExportTask generateSequenceExport(final Direction dir, final ExportTarget target, final Object targetArg, final ROI... rois) {
        return new ExportTask(dir, ExportMode.SEQUENCE, target, targetArg, null, rois);
    }

    private ExportTask(final Direction direction, final ExportMode mode, final ExportTarget target, final Object targetParam, final int[] dataParams, final ROI... rois) {
        this.mode = mode;
        this.target = target;
        this.direction = direction;
        this.targetParam = targetParam;
        this.dataParams = dataParams;
        if (rois == null || rois.length == 0) {
            this.rois = null;
        } else {
            this.rois = rois;
        }
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

    public Object getTargetParam() {
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
