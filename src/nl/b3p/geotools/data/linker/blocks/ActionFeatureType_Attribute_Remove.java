package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Remove a attribute with a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Attribute_Remove extends Action {

    public ActionFeatureType_Attribute_Remove(int attributeID) {
        this.attributeID = attributeID;
    }

    public ActionFeatureType_Attribute_Remove(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        if (hasLegalAttributeID(feature)) {
            FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
            ftb.importType(feature.getFeatureType());
            ftb.removeType(attributeID);

            feature = ftb.getFeatureType().create(removeAttribute(feature, attributeID), feature.getID());
        }
        return feature;
    }

    /**
     * Remove attribute by index
     * @param feature Feature with original attributes
     * @param removeID Position of attribute to remove
     * @return Attribute array
     */
    protected Object[] removeAttribute(Feature feature, int removeID) {
        Object[] newAttributes = new Object[feature.getAttributes(null).length - 1];

        int newPos = 0;
        for (int i = 0; i < newAttributes.length + 1; i++) {
            if (i != removeID) {
                newAttributes[newPos] = feature.getAttribute(i);
                newPos++;
            }
        }

        return newAttributes;
    }

    @Override
    public String toString() {
        return "Remove attribute '" + (attributeName.equals("") ? attributeID : attributeName) + "'";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_ID
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType een attribuut worden verwijderd.";
    }
}
