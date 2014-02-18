package cz.tul.dic.data;

/**
 *
 * @author Petr Jecmen
 */
public class Result {

    private final double[][][] data;

    public Result(double[][][] data) {
        this.data = data;
    }

    public double[][][] getData() {
        return data;
    }

}
