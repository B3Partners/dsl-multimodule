package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Rename attributeType
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Replace_Name extends Action {

    private String newAttributeName;

    public ActionFeatureType_Replace_Name(String attributeName, String newAttributeName) {
        this.attributeName = attributeName;
        this.newAttributeName = newAttributeName;
    }

    public ActionFeatureType_Replace_Name(int attributeID, String newAttributeName) {
        this.attributeID = attributeID;
        this.newAttributeName = newAttributeName;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        if (hasLegalAttributeID(feature)) {
            String typeName = feature.getFeatureType().getTypeName();
            FeatureType featureType = feature.getFeatureType();

            FeatureTypeBuilder ftBuilder = FeatureTypeBuilder.newInstance(typeName);
            ftBuilder.importType(featureType); // import existing featuretype

            AttributeType newAttributeType = AttributeTypeFactory.newAttributeType(newAttributeName, featureType.getAttributeType(attributeID).getType());
            ftBuilder.setType(attributeID, newAttributeType);

            feature = ftBuilder.getFeatureType().create(feature.getAttributes(null), feature.getID());

        }
        return feature;
    }

    @Override
    public String toString() {
        return "Replace name at '" + (attributeID == -1 ? attributeName : attributeID) + "' to \"" + newAttributeName + "\"";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.NEW_ATTRIBUTE_NAME
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.NEW_ATTRIBUTE_NAME
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType de attribuutNaam worden gewijzigd.";
    }
}
