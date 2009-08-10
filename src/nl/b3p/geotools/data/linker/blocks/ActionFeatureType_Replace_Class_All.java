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

    private Class find;
    private Class replace;
    private boolean tryCast;

    public ActionFeatureType_Replace_Class_All(Class find, Class replace, boolean tryCast) {
        this.find = find;
        this.replace = replace;
        this.tryCast = tryCast;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        int attributeCount = feature.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            if (feature.getAttributeType(i).getBinding().equals(find)) {
                ActionFeatureType_Replace_Class actionReplace = new ActionFeatureType_Replace_Class(i, replace, tryCast);
                feature = actionReplace.execute(feature);
            }
        }

        return feature;
    }

    public String getDescription_NL() {
        return "Verander alle attribuutklassen waar voldaan wordt aan de voorwaarde; bijvoorbeeld alle Shorts naar Doubles";
    }

    public String toString() {
        return "Change " + find.toString() + " to " + replace.toString() + " and " + (tryCast ? "" : "do not ") + "try to cast the value";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.OBJECT_FIND,
                    ActionFactory.OBJECT_REPLACE,
                    ActionFactory.TRYCAST
                }));

        return constructors;
    }
}
