/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.output;

/**
 *
 * @author Petr Jecmen
 */
public class ExportTask {

    private final ExportMode mode;
    private final ExportTarget target;
    private final Direction direction;
    private final Object targetParam;
    private final int[] dataParams;

    public ExportTask(ExportMode mode, ExportTarget target, final Direction direction, final Object targetParam, final int... dataParams) {
        this.mode = mode;
        this.target = target;
        this.direction = direction;
        this.dataParams = dataParams;
        this.targetParam = targetParam;
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

    public Object getTargetParam(){
        return targetParam;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(target);
        sb.append("-");
        sb.append(mode);
        sb.append("-");
        sb.append(direction);
        sb.append("-");
        sb.append(dataParams);
        sb.append("-");
        sb.append(targetParam);

        return sb.toString();
    }

}
