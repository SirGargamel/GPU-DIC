package cz.tul.dic.engine;

/**
 *
 * @author Petr Jecmen
 */
public class EngineMath {
    
    public static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;

        int result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }

        return result;
    }
    
}
