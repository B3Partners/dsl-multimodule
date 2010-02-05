/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Change all classes e.g. all Shorts to Doubles
 * @author Gertjan
 */
public class ActionFeatureType_Replace_Class_All extends Action {

    private Class attribute_class;
    private Class new_attribute_class;
    private boolean tryCast;

    public ActionFeatureType_Replace_Class_All(Class attribute_class, Class new_attribute_class, boolean tryCast) {
        this.attribute_class = attribute_class;
        this.new_attribute_class = new_attribute_class;
        this.tryCast = tryCast;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        int attributeCount = feature.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            if (feature.getAttributeType(i).getBinding().equals(attribute_class)) {
                ActionFeatureType_Replace_Class actionReplace = new ActionFeatureType_Replace_Class(i, new_attribute_class, tryCast);
                feature = actionReplace.execute(feature);
            }
        }

        return feature;
    }

    public String getDescription_NL() {
        return "Verander alle attribuutklassen waar voldaan wordt aan de voorwaarde; bijvoorbeeld alle Shorts naar Doubles";
    }

    public String toString() {
        return "Change " + attribute_class.toString() + " to " + new_attribute_class.toString() + " and " + (tryCast ? "" : "do not ") + "try to cast the value";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_CLASS,
                    ActionFactory.NEW_ATTRIBUTE_CLASS,
                    ActionFactory.TRYCAST
                }));

        return constructors;
    }
}
