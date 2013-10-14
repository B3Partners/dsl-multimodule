/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Check a name of a given attributeType
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_FeatureType_Name extends ActionCondition {

    private Object right;

    /**
     * Check if attribute of SimpleFeatureType at position attributeID is equal to columnText
     * @param attributeID Position in attribute[]
     * @param attributeName AttributeName to check for in attribute[]
     */
    public ActionCondition_FeatureType_Name(int attributeID, String attributeName) {
        this.attributeID = attributeID;
        this.right = attributeName;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);
        Object left = feature.getFeatureType().getType(attributeID).getName().getLocalPart();

        return compare(feature, left, ActionCondition.CompareType.EQUAL, right);
    }

    public String toString() {
        return "if (Column " + attributeID + " equals " + right.toString() + listsToString();
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.ATTRIBUTE_NAME
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de attribuutNaam gelijk is aan de opgegeven tekst";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
