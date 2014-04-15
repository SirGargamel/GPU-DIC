package cz.tul.dic.engine.cluster;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public abstract class ClusterAnalyzer<T> {

    private static final double DEFAULT_PRECISION = 0.1;
    protected double precision;

    public ClusterAnalyzer() {
        precision = DEFAULT_PRECISION;
    }

    public abstract void addValue(final T val);

    public void setPrecision(final double precision) {
        this.precision = precision;
    }

    public abstract T findMajorValue();

}
