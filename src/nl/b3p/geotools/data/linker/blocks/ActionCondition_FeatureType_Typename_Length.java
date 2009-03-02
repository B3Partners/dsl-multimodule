/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Check length of typename
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_FeatureType_Typename_Length extends ActionCondition {

    private ActionCondition.CompareType compare;
    private int length;

    public ActionCondition_FeatureType_Typename_Length(ActionCondition.CompareType compare, int length) {
        this.compare = compare;
        this.length = length;
    }

    public Feature execute(Feature feature) throws Exception {
        int left = feature.getFeatureType().getTypeName().length();

        return compare(feature, left, compare, length);
    }

    public String toString() {
        return "if (Lenght of typename " + compare.toString() + " " + length + listsToString();
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.COMPARE_TYPE,
                        ActionFactory.LENGTH
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de lenghte van de typename voldoet aan de eisen";
    }
}
