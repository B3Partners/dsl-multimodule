/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.HashMap;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;
import org.geotools.feature.type.*;

/**
 * Set a value inside a feature at a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeature_Value_Set extends Action {

    private Object objectReplace;
    private static final HashMap attributeMapping = new HashMap();
    private boolean append;


    static {
        attributeMapping.put(String.class, TextualAttributeType.class);
        attributeMapping.put(Integer.class, NumericAttributeType.class);
        attributeMapping.put(Double.class, NumericAttributeType.class);
    }

    public ActionFeature_Value_Set(String attributeName, Object objectReplace, boolean append) {
        this.attributeName = attributeName;
        this.objectReplace = objectReplace;
        this.append = append;
    }

    public ActionFeature_Value_Set(int attributeID, Object objectReplace, boolean append) {
        this.attributeID = attributeID;
        this.objectReplace = objectReplace;
        this.append = append;
    }

    public ActionFeature_Value_Set(String attributeName, Object objectReplace) {
        this.attributeName = attributeName;
        this.objectReplace = objectReplace;
        this.append = false;
    }

    public ActionFeature_Value_Set(int attributeID, Object objectReplace) {
        this.attributeID = attributeID;
        this.objectReplace = objectReplace;
        this.append = false;
    }

    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        if (attributeID != -1) {
            if (feature.getFeatureType().getAttributeType(attributeID).getClass().equals(attributeMapping.get(objectReplace.getClass()))) {
                Object replaceValue = objectReplace;
                if (append && objectReplace instanceof String) {
                    replaceValue = ((String) feature.getAttribute(attributeID) + (String) replaceValue);
                }

                feature.setAttribute(attributeID, replaceValue);

            } else {
                throw new Exception("Unable to set value in feature to " + objectReplace.toString() + "; " + feature.getFeatureType().getAttributeType(attributeID).getClass().getSimpleName() + " expected");
            }
        } else {
            throw new Exception("Attribute " + attributeID + " not found");
        }

        return feature;
    }

    public String toString() {
        return "Set feature attribute '" + (attributeID == -1 ? attributeName : attributeID) + "' to \"" + objectReplace.toString() + "\"";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.OBJECT_REPLACE,
                        ActionFactory.APPEND
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.OBJECT_REPLACE,
                        ActionFactory.APPEND
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.OBJECT_REPLACE
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.OBJECT_REPLACE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een feature een attribuutWaarde in een bepaalde kolom worden gewijzigd.";
    }
}
