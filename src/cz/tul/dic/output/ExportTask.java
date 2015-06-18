/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.target.ExportTarget;
import java.io.File;
import java.io.Serializable;

/**
 *
 * @author Petr Jecmen
 */
public final class ExportTask implements Serializable {

    private static final String SEPARATOR = ";";
    private final ExportMode mode;
    private final ExportTarget target;
    private final Direction direction;
    private final Object targetParam;
    private final int[] dataParams;
    private final double[] limits;

    private ExportTask(final Direction direction, final ExportMode mode, final ExportTarget target, final Object targetParam, final int[] dataParams, final double... limits) {
        this.mode = mode;
        this.target = target;
        this.direction = direction;
        this.targetParam = targetParam;
        this.dataParams = dataParams;

        if (limits != null && limits.length > 1) {
            this.limits = limits;
        } else {
            this.limits = new double[]{Double.NaN, Double.NaN};
        }
    }
    
    public static ExportTask generateExportTask(final String data) {
        final String[] split = data.split(SEPARATOR);
        if (split.length < 4) {
            throw new IllegalArgumentException("Not enough parameters for export task - " + data);
        }

        final int[] dataParams = new int[split.length - 4];
        for (int i = 4; i < split.length; i++) {
            dataParams[i - 4] = Integer.parseInt(split[i]);
        }

        Direction dir;
        if (split[2].equals("null")) {
            dir = Direction.D_DY;
        } else {
            dir = Direction.valueOf(split[2]);
        }
        
        return new ExportTask(dir, ExportMode.valueOf(split[0]), ExportTarget.valueOf(split[1]), new File(split[3]), dataParams);
    }

    public static ExportTask generateMapExport(final Direction dir, final ExportTarget target, final Object targetArg, final int round, final double[] limits) {
        return new ExportTask(dir, ExportMode.MAP, target, targetArg, new int[]{round}, limits);
    }

    public static ExportTask generatePointExport(final ExportTarget target, final Object targetArg, final int x, final int y) {
        return new ExportTask(Direction.D_DY, ExportMode.POINT, target, targetArg, new int[]{x, y});
    }

    public static ExportTask generateDoublePointExport(final ExportTarget target, final Object targetArg, final int x1, final int y1, final int x2, final int y2) {
        return new ExportTask(null, ExportMode.DOUBLE_POINT, target, targetArg, new int[]{x1, y1, x2, y2});
    }

    public static ExportTask generateSequenceExport(final Direction dir, final ExportTarget target, final Object targetArg, final double[] limits) {
        return new ExportTask(dir, ExportMode.SEQUENCE, target, targetArg, null, limits);
    }

    public static ExportTask generateVideoExport(final Direction dir, final Object targetArg, final double... limits) {
        return new ExportTask(dir, ExportMode.VIDEO, ExportTarget.FILE, targetArg, null, limits);
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

    public Object getTargetParam() {
        return targetParam;
    }

    public double[] getLimits() {
        return limits;
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
                sb.append(SEPARATOR);
            }
        }
        sb.setLength(sb.length() - SEPARATOR.length());

        return sb.toString();
    }

}
