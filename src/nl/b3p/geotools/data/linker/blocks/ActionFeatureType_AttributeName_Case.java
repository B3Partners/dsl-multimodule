package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.opengis.feature.type.AttributeType;

/**
 * Set typename to upper- or lowercase
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeName_Case extends Action {

    private boolean toUpper;

    public ActionFeatureType_AttributeName_Case(int attributeID, boolean toUpper) {
        this.toUpper = toUpper;
        this.attributeID = attributeID;
    }

    public ActionFeatureType_AttributeName_Case(String attributeName, boolean toUpper) {
        this.toUpper = toUpper;
        this.attributeName = attributeName;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);

        AttributeType attributeType = feature.getAttributeType(attributeID);
        String newAttributeName = attributeType.getName().getLocalPart();

        if (toUpper) {
            newAttributeName = newAttributeName.toUpperCase();
        } else {
            newAttributeName = newAttributeName.toLowerCase();
        }

        Action rename = new ActionFeatureType_AttributeName_Rename(attributeID, newAttributeName);
        rename.execute(feature);

        return feature;
    }

    @Override
    public String toString() {
        return "Set typename to " + (toUpper ? "upper" : "lower") + "case";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.UPPERCASE
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.UPPERCASE
                }));
 */

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan de naam van een attribuut in upper- of lowercase gezet worden";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
