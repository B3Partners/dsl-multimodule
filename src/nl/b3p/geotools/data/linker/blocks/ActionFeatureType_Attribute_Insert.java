package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Add a attribute at a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Attribute_Insert extends Action {

    private Class attributeClass;

    public ActionFeatureType_Attribute_Insert(String attributeName, Class attributeClass) {
        this.attributeName = attributeName;
        this.attributeClass = attributeClass;
    }

    public ActionFeatureType_Attribute_Insert(String attributeName, Class attributeClass, int attributeID) {
        this.attributeName = attributeName;
        this.attributeClass = attributeClass;
        this.attributeID = attributeID;
    }

    public Feature execute(Feature feature) throws Exception {
        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());

        if (attributeID >= 0 && attributeID < feature.getNumberOfAttributes()) {
            // Insert
            AttributeType type = AttributeTypeFactory.newAttributeType(attributeName, attributeClass);
            ftb.addType(attributeID, type);
            feature = ftb.getFeatureType().create(expandAttributes(feature, attributeID), feature.getID());

        } else if (attributeID == -1) {
            // Add
            AttributeType type = AttributeTypeFactory.newAttributeType(attributeName, attributeClass);
            ftb.addType(type);
            feature = ftb.getFeatureType().create(expandAttributes(feature, feature.getNumberOfAttributes()), feature.getID());

        } else {
            throw new Exception("Attribute Insert: invalid column '" + attributeID + "'; maximum columnIndex: " + Integer.toString(feature.getAttributes(null).length - 1));
        }

        return feature;
    }

    private Object[] expandAttributes(Feature feature, int expandID) {
        Object[] newAttributes = new Object[feature.getAttributes(null).length + 1];

        int oldPos = 0;
        for (int i = 0; i < newAttributes.length; i++) {
            if (i != expandID) {
                newAttributes[i] = feature.getAttribute(oldPos);
                oldPos++;
            } else {
                newAttributes[i] = null;
            }
        }

        return newAttributes;
    }

    public String toString() {
        if (attributeID == -1) {
            return "Add attribute '" + attributeName + "' as \"" + attributeClass.getSimpleName() + "\"";
        } else {
            return "Insert attribute at '" + attributeID + "' as \"" + attributeClass.getSimpleName() + "\"";
        }
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.ATTRIBUTE_CLASS
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.ATTRIBUTE_CLASS,
                        ActionFactory.ATTRIBUTE_ID
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType een attribuut worden toegevoegd. Er kan een index worden opgegeven.";
    }
}
