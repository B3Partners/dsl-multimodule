/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Check a value in a SimpleFeature on a given attribute
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_Feature_Value extends ActionCondition {

    private ActionCondition.CompareType compareType;
    private Object right;

    /**
     * Compare value inside the SimpleFeature with attributeName to object right
     * @param attributeName Name of attribute in SimpleFeatureType
     * @param compareType Compare type; equal, not_equal, smaller, greater, smaller_equal, greater_equal
     * @param right Object to compare value to
     */
    public ActionCondition_Feature_Value(String attributeName, ActionCondition.CompareType compareType, Object right) {
        this.attributeName = attributeName;
        this.compareType = compareType;
        this.right = right;
    }

    /**
     * Compare value inside the SimpleFeature at position attributeID to object right
     * @param attributeID Position in attribute[]
     * @param compareType Compare type; equal, not_equal, smaller, greater, smaller_equal, greater_equal
     * @param right Object to compare value to
     */
    public ActionCondition_Feature_Value(int attributeID, ActionCondition.CompareType compareType, Object right) {
        this.attributeID = attributeID;
        this.compareType = compareType;
        this.right = right;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);
        Object left = feature.getAttribute(attributeID);

        return compare(feature, left, compareType, right);
    }

    public String toString() {
        return "if (" + (attributeName == null ? "Column " + attributeID : attributeName.toString()) + " " + compareType.toString() + " " + right.toString() + listsToString();
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de waarde van een attribuut gelijk is aan de opgegeven waarde";
    }
}
