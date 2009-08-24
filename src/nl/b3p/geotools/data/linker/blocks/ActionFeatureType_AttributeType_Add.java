package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Add a attribute at a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeType_Add extends Action {

    private Class attributeClass;

    public ActionFeatureType_AttributeType_Add(String attributeName, Class attributeClass) {
        this.attributeName = attributeName;
        this.attributeClass = attributeClass;
    }

    public ActionFeatureType_AttributeType_Add(int attributeID, String attributeName, Class attributeClass) {
        this.attributeName = attributeName;
        this.attributeClass = attributeClass;
        this.attributeID = attributeID;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (attributeID == -1) {
            feature.addAttributeDescriptor(attributeName, attributeClass);
        } else {
            feature.insertAttributeDescriptor(attributeID, attributeName, attributeClass);
        }
        return feature;
    }

    public String toString() {
        if (attributeID == -1) {
            return "Add attribute '" + attributeName + "' as \"" + attributeClass.getSimpleName() + "\"";
        } else {
            return "Insert attribute at '" + attributeID + "' as \"" + attributeClass.getSimpleName() + "\"";
        }
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.ATTRIBUTE_CLASS
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.ATTRIBUTE_CLASS,
                    ActionFactory.ATTRIBUTE_ID
                }));
*/
        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType een attribuut worden toegevoegd. Er kan een index worden opgegeven.";
    }
}
