package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Set all attributeNames to upper- or lowercase
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeNames_Case extends Action {

    private boolean toUpper;

    public ActionFeatureType_AttributeNames_Case(boolean toUpper) {
        this.toUpper = toUpper;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        for (int i = 0; i < feature.getAttributeCount(); i++) {
            Action action = new ActionFeatureType_AttributeName_Case(i, toUpper);
            action.execute(feature);
        }

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
        return "Met deze Action kan de naam van een attribuut in upper- of lowercase gezet worden";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
