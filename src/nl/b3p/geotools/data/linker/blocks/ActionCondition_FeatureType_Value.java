/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Check a name of a given attributeType
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_FeatureType_Value extends ActionCondition {

    private Object right;

    /**
     * Check if attribute of featureType at position attributeID is equal to columnText
     * @param attributeID Position in attribute[]
     * @param attributeName AttributeName to check for in attribute[]
     */
    public ActionCondition_FeatureType_Value(int attributeID, String attributeName) {
        this.attributeID = attributeID;
        this.right = attributeName;
    }

    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);
        Object left = feature.getFeatureType().getAttributeType(attributeID).getName();

        return compare(feature, left, ActionCondition.CompareType.EQUAL, right);
    }

    public String toString() {
        return "if (Column " + attributeID + " equals " + right.toString() + listsToString();
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.ATTRIBUTE_NAME
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de attribuutNaam gelijk is aan de opgegeven tekst";
    }
}
