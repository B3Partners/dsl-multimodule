package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;

/**
 * Rename attributeType
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeName_Rename extends Action {

    private String newAttributeName;
    protected String description = "Met deze Action kan bij een SimpleFeatureType de attribuutNaam worden gewijzigd.";

    public ActionFeatureType_AttributeName_Rename(String attributeName, String newAttributeName) {
        this.attributeName = attributeName;
        this.newAttributeName = newAttributeName;
    }

    public ActionFeatureType_AttributeName_Rename(int attributeID, String newAttributeName) {
        this.attributeID = attributeID;
        this.newAttributeName = newAttributeName;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);

        // Reload original attributeType
        AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.init(feature.getFeatureType().getDescriptor(attributeID));

        // Overwrite attributeType with "attributeType with new name"
        feature.setAttributeDescriptor(attributeID, atb.buildDescriptor(newAttributeName), true);

        return feature;
    }

    @Override
    public String toString() {
        return "Replace name at '" + (attributeID == -1 ? attributeName : attributeID) + "' to \"" + newAttributeName + "\"";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.NEW_ATTRIBUTE_NAME
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.NEW_ATTRIBUTE_NAME
                }));
*/
        return constructors;
    }

    public String getDescription_NL() {
        return description;
    }
}
