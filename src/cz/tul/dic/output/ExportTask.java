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
    private final Object param;

    public ExportTask(ExportMode mode, ExportTarget target, Object param) {
        this.mode = mode;
        this.target = target;
        this.param = param;
    }

    public ExportMode getMode() {
        return mode;
    }

    public ExportTarget getTarget() {
        return target;
    }

    public Object getParam() {
        return param;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(target);
        sb.append("-");
        sb.append(mode);
        sb.append("-");
        sb.append(param);
        
        return sb.toString();
    }

}
