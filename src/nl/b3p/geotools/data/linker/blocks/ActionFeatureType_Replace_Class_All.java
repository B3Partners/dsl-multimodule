/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.Feature;

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

    public Feature execute(Feature feature) throws Exception {
        int attributeCount = feature.getFeatureType().getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            if(feature.getFeatureType().getAttributeType(i).getType().equals(find)){
              ActionFeatureType_Replace_Class actionReplace = new ActionFeatureType_Replace_Class(i, replace, tryCast);
              feature = actionReplace.execute(feature);
            }
        }

        return feature;
    }

    public String getDescription_NL() {
        return "";
    }

    public String toString() {
        return "Verander alle attribuutklassen waar voldaan wordt aan de voorwaarde; bijvoorbeeld alle Shorts naar Doubles";
    }
}
