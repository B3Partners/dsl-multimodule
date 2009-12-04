package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Replace a value inside a SimpleFeature where condition is true
 * @author Gertjan Al, B3Partners
 */
public class ActionFeature_Value_Substring_All extends Action {

    private int beginIndex = -1;
    private int endIndex = -1;

    public ActionFeature_Value_Substring_All(int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public ActionFeature_Value_Substring_All(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        int attributeCount = feature.getAttributeCount();

        for (int i = 0; i < attributeCount; i++) {
            if (feature.getAttributeType(i).getBinding().equals(String.class)) {
                ActionFeature_Value_Substring action = null;

                if (endIndex == -1) {
                    action = new ActionFeature_Value_Substring(i, beginIndex);
                } else {
                    action = new ActionFeature_Value_Substring(i, beginIndex, endIndex);
                }
                
                feature = action.execute(feature);
            }
        }
        return feature;
    }

    public String toString() {
        return "Make substring of all string values = substring(" + beginIndex + (endIndex == -1 ? "" : ", " + endIndex) + ")";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.BEGIN_INDEX,
                    ActionFactory.END_INDEX
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.BEGIN_INDEX
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeature alle attribuutWaardes worden gewijzigd in een subString van deze waarde";
    }
}
