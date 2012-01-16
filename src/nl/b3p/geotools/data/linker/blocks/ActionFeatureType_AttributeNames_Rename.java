package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;

public class ActionFeatureType_AttributeNames_Rename extends Action {

    private Integer[] attributeIds;
    private String[] currentAttributeNames;
    private String[] newAttributeNames;
    
    protected String description = "Map de invoerkolommen naar de uitvoerkolommen.";

    public ActionFeatureType_AttributeNames_Rename(String[] currentAttributeNames, String[] newAttributeNames) {
        this.currentAttributeNames = currentAttributeNames;
        this.newAttributeNames = newAttributeNames;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (currentAttributeNames != null && currentAttributeNames.length > 0) {
            attributeIds = getAttributeIds(feature, currentAttributeNames);
            
            for (int i=0; i < currentAttributeNames.length; i++) {
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                atb.init(feature.getFeatureType().getDescriptor(attributeIds[i]));
                
                feature.setAttributeDescriptor(attributeIds[i], atb.buildDescriptor(newAttributeNames[i]), true);
            }
        }               

        return feature;
    }

    @Override
    public String toString() {
        return "Map de invoerkolommen naar de uitvoerkolommen.";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.NEW_ATTRIBUTE_NAME
                }));
        
        return constructors;
    }

    public String getDescription_NL() {
        return description;
    }
}
