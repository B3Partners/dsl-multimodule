/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Check a class on a given attribute
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_Feature_Class extends ActionCondition {

    private Class right;

    /**
     * Check if Class at given attributeID is equal to given columnClass
     * @param attributeID Position in attribute[]
     * @param columnClass Class to compare to
     */
    public ActionCondition_Feature_Class(int attributeID, Class columnClass) {
        this.attributeID = attributeID;
        this.right = columnClass;
    }

    /**
     * Check if Class at given attributeColumn is equal to given columnClass
     * @param attributeName Name of attributeColumn in attribute[]
     * @param columnClass Class to compare to
     */
    public ActionCondition_Feature_Class(String attributeName, Class columnClass) {
        this.attributeName = attributeName;
        this.right = columnClass;
    }

    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);
        Class left = feature.getAttribute(attributeID).getClass();

        return compare(feature, left, ActionCondition.CompareType.EQUAL, right);
    }

    public String toString() {
        return "if (Column '" + (attributeName.equals("") ? attributeID : attributeName) + "' equals " + right.toString() + listsToString();
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.ATTRIBUTE_CLASS
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.ATTRIBUTE_CLASS
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of een attribuut een bepaalde class bevat";
    }
}
