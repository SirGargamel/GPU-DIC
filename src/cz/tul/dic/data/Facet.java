package cz.tul.dic.data;

/**
 *
 * @author Petr Jecmen
 */
public class Facet {

    private final int[] data;
    private final float[] center;
    private final int size;

    public Facet(int[] data, float[] center, int size) {
        this.data = data;
        this.center = center;
        this.size = size;
    }

    public int[] getData() {
        return data;
    }

    public float[] getCenter() {
        return center;
    }

    public int getSize() {
        return size;
    }
    
}
