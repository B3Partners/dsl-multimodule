package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Replace a value inside a feature where condition is true
 * @author Gertjan Al, B3Partners
 */
public class ActionFeature_Value_Replace extends Action {

    private Object find;
    private Object replace;

    public ActionFeature_Value_Replace(Object find, Object replace) {
        this.find = find;
        this.replace = replace;
    }

    public Feature execute(Feature feature) throws Exception {
        // Replace by value
        for (int i = 0; i < feature.getNumberOfAttributes(); i++) {
            Object obj = feature.getAttribute(i);
            if (find == null) {
                if (obj == null) {
                    feature.setAttribute(i, replace);
                }
            } else {
                if (obj != null) {
                    if (obj.equals(find)) {
                        feature.setAttribute(i, replace);
                    }
                }
            }

        }
        return feature;
    }

    public String toString() {
        return "Replace where value = '" + (find == null ? "null" : find.toString()) + "' to \"" + (replace == null ? "null" : replace.toString()) + "\"";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.OBJECT_FIND,
                        ActionFactory.OBJECT_REPLACE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een feature een attribuutWaarde worden gewijzigd, overal waar de waarde voldoet aan de voorwaarde.";
    }
}
