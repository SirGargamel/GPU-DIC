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
import cz.tul.dic.data.Image;

/**
 *
 * @author Petr Ječmen
 */
public class ImageConverter implements Converter {

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        final Image img = (Image) source;
        if (img != null && img.getImageName() != null) {
            writer.setValue(img.getImageName());
        } else {
            writer.setValue("none");
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(Image.class);
    }

}
