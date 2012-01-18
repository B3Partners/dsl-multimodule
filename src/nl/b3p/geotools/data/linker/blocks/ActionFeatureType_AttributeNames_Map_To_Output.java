package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;

/* Map output columns to inputcolumns. User sees list of all output columns and can
 choose input columns per output column from a dropdown. */
public class ActionFeatureType_AttributeNames_Map_To_Output extends Action {

    private Integer[] attributeIds;
    private String[] currentAttributeNames;
    private String[] newAttributeNames;
    
    protected String description = "Kies per uitvoerkolom een invoerkolom.";

    public ActionFeatureType_AttributeNames_Map_To_Output(String[] currentAttributeNames, String[] newAttributeNames) {
        this.currentAttributeNames = currentAttributeNames;
        this.newAttributeNames = newAttributeNames;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (newAttributeNames != null && newAttributeNames.length > 0) {
            attributeIds = getAttributeIds(feature, newAttributeNames);
            
            for (int i=0; i < newAttributeNames.length; i++) {
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                atb.init(feature.getFeatureType().getDescriptor(attributeIds[i]));
                
                feature.setAttributeDescriptor(attributeIds[i], atb.buildDescriptor(currentAttributeNames[i]), true);              
            }
            
            //feature getkollommen
                    
            //loop alle kolommen en stel lijstje van kolommen die niet in newAttributeNames staan
                    
            //maak leeg behalve indien geo
            //SimpleFeature f = feature.getFeature();
            //f.setAttribute(attribute, s.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replacement) ));          
        }               

        return feature;
    }

    @Override
    public String toString() {
        return "Kies per uitvoerkolom een invoerkolom.";
    }

    public static List<List<String>> getConstructors(String[] invoer) {
        List<List<String>> constructors = new ArrayList<List<String>>();

        if (invoer != null) {
            constructors.add(Arrays.asList(invoer));
        }  
        
        return constructors;
    }

    public String getDescription_NL() {
        return description;
    }
}