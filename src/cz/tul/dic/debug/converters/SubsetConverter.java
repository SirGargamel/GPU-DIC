/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.debug.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import cz.tul.dic.data.subset.AbstractSubset;
import java.util.Arrays;

/**
 *
 * @author Petr Ječmen
 */
public class SubsetConverter implements Converter {

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final AbstractSubset subset = (AbstractSubset) source;
        writer.startNode("center");
        final String center = Arrays.toString(subset.getCenter());
        writer.setValue(center.substring(1, center.length() - 1));
        writer.endNode();
        writer.startNode("size");
        writer.setValue(Integer.toString(subset.getSize()));
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canConvert(Class type) {
        return AbstractSubset.class.isAssignableFrom(type);
    }

}
