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
 * Check a class on a given attribute
 * @author Gertjan Al, B3Partners
 */
public class ActionCondition_Feature_Class extends ActionCondition {

    private Class right;
    private boolean useGeometry = false;

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

    public ActionCondition_Feature_Class(Class columnClass) {
        this.right = columnClass;
        this.useGeometry = true;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        // Aanpassing om tabel zonder geometrie te kunnen vullen:
        // Point,LineString,Polygon,MultiPoint,MultiLineString,MultiPolygon 
        // compare met Object is altijd false
        Class left = Object.class;

        if (useGeometry && feature.getFeatureType().getGeometryDescriptor() == null) {
            attributeName = null;
        } else if (useGeometry) {
            attributeName = feature.getFeatureType().getGeometryDescriptor().getLocalName();
        }

        if (attributeName != null) {
            fixAttributeID(feature);
            Object o = feature.getAttribute(attributeID);
            if (o != null) {
                left = o.getClass();
            }
        }

        return compare(feature, left, ActionCondition.CompareType.EQUAL, right);
    }

    public String toString() {
        return "if (Column '" + (attributeName.equals("") ? attributeID : attributeName) + "' equals " + right.toString() + listsToString();
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

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_CLASS
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze ActionCondition kan gecontroleerd worden of een attribuut een bepaalde class bevat";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
