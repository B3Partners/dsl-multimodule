package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

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
    public Feature execute(Feature feature) throws Exception {
        String typename = feature.getFeatureType().getTypeName();
        if (toUpper) {
            typename = typename.toUpperCase();
        } else {
            typename = typename.toLowerCase();
        }

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(typename);
        ftb.importType(feature.getFeatureType());
        ftb.setName(typename);

        feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());

        return feature;
    }

    @Override
    public String toString() {
        return "Set typename to " + (toUpper ? "upper" : "lower") + "case";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.UPPERCASE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType de typenaam worden omgezet naar hoofdletters (uppercase) of kleine letters (lowercase).";
    }
}
