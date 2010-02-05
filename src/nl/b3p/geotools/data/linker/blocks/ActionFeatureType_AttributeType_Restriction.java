package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.LengthFunction;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.Filter;

/**
 * Remove a attribute with a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeType_Restriction extends Action {

    private int length = 256;

    public ActionFeatureType_AttributeType_Restriction(int attributeID, int length) {
        this.attributeID = attributeID;
        this.length = length;
    }

    public ActionFeatureType_AttributeType_Restriction(String attributeName, int length) {
        this.attributeName = attributeName;
        this.length = length;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);

        if (attributeID != -1) {
            AttributeDescriptor oldDescriptor = feature.getFeatureType().getAttributeDescriptors().get(attributeID);
            AttributeType oldType = feature.getAttributeType(attributeID);

            FilterFactoryImpl filterFactory = new FilterFactoryImpl();
            filterFactory.createLiteralExpression(length);

            List<Filter> restrictions = new ArrayList<Filter>();
            restrictions.add(filterFactory.lessOrEqual(new LengthFunction(), filterFactory.createLiteralExpression(length)));

            AttributeType newType = new AttributeTypeImpl(oldType.getName(), oldType.getBinding(), oldType.isIdentified(), oldType.isAbstract(), restrictions, oldType.getSuper(), oldType.getDescription());
            AttributeDescriptor newAttributeDescriptor = new AttributeDescriptorImpl(newType, oldDescriptor.getName(), oldDescriptor.getMinOccurs(), oldDescriptor.getMaxOccurs(), oldDescriptor.isNillable(), oldDescriptor.getDefaultValue());

            feature.removeAttributeDescriptor(attributeID);
            feature.insertAttributeDescriptor(attributeID, newAttributeDescriptor);
        }

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
        ActionFactory.ATTRIBUTE_ID,
        ActionFactory.LENGTH,
        }));
         */
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.LENGTH
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType een attribuut worden verwijderd.";
    }
}
