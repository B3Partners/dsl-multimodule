package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

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
    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        String newAttributeName = feature.getFeatureType().getAttributeType(attributeID).getName();

        if (toUpper) {
            newAttributeName = newAttributeName.toUpperCase();
        } else {
            newAttributeName = newAttributeName.toLowerCase();
        }

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());

        AttributeType type = AttributeTypeFactory.newAttributeType(newAttributeName, feature.getFeatureType().getAttributeType(attributeID).getType());

        ftb.removeType(attributeID);
        ftb.addType(attributeID, type);

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
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.UPPERCASE
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.UPPERCASE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan de naam van een attribuut in upper- of lowercase gezet worden";
    }
}
