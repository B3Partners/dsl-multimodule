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

    public EasyFeature execute(EasyFeature feature) throws Exception {
        int left = feature.getTypeName().length();
        return compare(feature, left, compare, length);
    }

    public String toString() {
        return "if (Lenght of typename " + compare.toString() + " " + length + listsToString();
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.COMPARE_TYPE,
                    ActionFactory.LENGTH
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de lenghte van de typename voldoet aan de eisen";
    }
}
