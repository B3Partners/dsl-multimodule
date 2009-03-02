package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Append a attribute to the typename
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_AppendAttribute extends Action {

    private int maxLength;

    public ActionFeatureType_Typename_AppendAttribute(String attributeName, int maxLength) {
        this.attributeName = attributeName;
        this.maxLength = maxLength;
    }

    public ActionFeatureType_Typename_AppendAttribute(int attributeID, int maxLength) {
        this.attributeID = attributeID;
        this.maxLength = maxLength;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        String attrValue = fixTypename(feature.getAttribute(attributeID).toString());

        if (attrValue.length() > maxLength) {
            attrValue = attrValue.substring(0, maxLength);
        }

        String newTypename = feature.getFeatureType().getTypeName() + attrValue;

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());
        ftb.setName(newTypename);

        feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());

        return feature;
    }



    @Override
    public String toString() {
        return "Append attribute '" + (attributeName.equals("") ? attributeID : attributeName) + "' to typename";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.LENGTH
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.LENGTH
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan de waarde van een attribuut worden toegevoegd aan de typename";
    }
}

