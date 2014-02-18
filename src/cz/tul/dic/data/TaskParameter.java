package cz.tul.dic.data;

import cz.tul.dic.generators.FacetGeneratorMode;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public enum TaskParameter {
    
    DIR(File.class),
    FACET_GENERATOR_MODE(FacetGeneratorMode.class),
    FACET_GENERATOR_SPACING(int.class),
    ;
    
    private final Class type;
    
    TaskParameter(final Class cls) {
        this.type = cls;
    }
            
    public Class getType() {
        return type;
    }
}
