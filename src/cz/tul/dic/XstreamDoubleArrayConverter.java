/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Arrays;

/**
 *
 * @author user
 */


public class XstreamDoubleArrayConverter implements Converter {
    
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final double[] data = (double[]) source;
        writer.addAttribute("data", Arrays.toString(data));  
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canConvert(Class type) {
        return double[].class.isAssignableFrom(type);
    }
    
}
