package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Remove a attribute with a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeType_Remove extends Action {

    public ActionFeatureType_AttributeType_Remove(int attributeID) {
        this.attributeID = attributeID;
    }

    public ActionFeatureType_AttributeType_Remove(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);
        feature.removeAttributeDescriptor(attributeID);

        return feature;
    }

    @Override
    public String toString() {
        return "Remove attribute '" + (attributeName.equals("") ? attributeID : attributeName) + "'";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID
                }));
*/
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType een attribuut worden verwijderd.";
    }
}
