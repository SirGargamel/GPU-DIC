package cz.tul.dic.engine.cluster;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public abstract class ClusterAnalyzer<T> {
    
    private static final double DEFAULT_PRECISION = 0.1;
    protected Set<T> values;
    protected double precision;
    
    public ClusterAnalyzer(){
        values = new HashSet<>();
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
