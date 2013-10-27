/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.Status;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
/**
 * Condition by SimpleFeatureType class on a given attibuteType
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_FeatureType_Class extends ActionCondition {

    private Class right;

    /**
     * Check if Class at given attributeID is equal to given columnClass
     * @param attributeID Position in attribute[]
     * @param columnClass Class to compare to
     */
    public ActionCondition_FeatureType_Class(int attributeID, Class columnClass) {
        this.attributeID = attributeID;
        this.right = columnClass;
    }

    /**
     * Check if Class at given attributeColumn is equal to given columnClass
     * @param attributeName Name of attributeColumn in attribute[]
     * @param columnClass Class to compare to
     */
    public ActionCondition_FeatureType_Class(String attributeName, Class columnClass) {
        this.attributeName = attributeName;
        this.right = columnClass;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);
        Class left = feature.getAttributeType(attributeID).getBinding();

        return compare(feature, left, ActionCondition.CompareType.EQUAL, right);
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.ATTRIBUTE_CLASS
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.ATTRIBUTE_CLASS
                }));


        return constructors;
    }

    public String toString() {
        return "if (Column " + attributeID + " equals " + right.toString() + listsToString();
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of de class van een attribuut in een SimpleFeatureType gelijk is aan de opgegeven class";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
