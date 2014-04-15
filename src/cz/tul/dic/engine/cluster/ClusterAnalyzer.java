package cz.tul.dic.engine.cluster;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public abstract class ClusterAnalyzer<T> {
    
    private static final double DEFAULT_PRECISION = 0.1;
    protected List<T> values;
    protected double precision;
    
    public ClusterAnalyzer(){
        values = new LinkedList<>();
        precision = DEFAULT_PRECISION;
    }
    
    public void addValue(final T val) {
        values.add(val);
    }
    
    public void setPrecision(final double precision) {
        this.precision = precision;
    }

    public abstract T findMajorValue();
    
}
