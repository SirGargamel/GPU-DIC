/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset.generator;

/**
 *
 * @author Petr Jecmen
 */
public enum SubsetGenerator {

    EQUAL("EqualSpacingSubsetGenerator"),
    DYNAMIC("DynamicSubsetGenerator"),
    RANDOM("RandomSubsetGenerator");
    
    private final String className;

    private SubsetGenerator(final String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
    
}
