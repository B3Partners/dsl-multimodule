package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Set a attributeName to upper- or lowercase
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_Case extends Action {

    private boolean toUpper;

    public ActionFeatureType_Typename_Case(boolean toUpper) {
        this.toUpper = toUpper;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        String typename = feature.getFeatureType().getTypeName();
        if (toUpper) {
            typename = typename.toUpperCase();
        } else {
            typename = typename.toLowerCase();
        }
        feature.setTypeName(typename);

        return feature;
    }

    @Override
    public String toString() {
        return "Set typename to " + (toUpper ? "upper" : "lower") + "case";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.UPPERCASE
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de typenaam worden omgezet naar hoofdletters (uppercase) of kleine letters (lowercase).";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
