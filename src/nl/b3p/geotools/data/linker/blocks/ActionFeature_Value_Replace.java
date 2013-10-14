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
public class ActionFeature_Value_Replace extends Action {

    private Object find;
    private Object replace;

    public ActionFeature_Value_Replace(Object find, Object replace) {
        this.find = find;
        this.replace = replace;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        // Replace by value
        for (int i = 0; i < feature.getAttributeCount(); i++) {
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

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.OBJECT_FIND,
                    ActionFactory.OBJECT_REPLACE
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeature een attribuutWaarde worden gewijzigd, overal waar de waarde voldoet aan de voorwaarde.";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
