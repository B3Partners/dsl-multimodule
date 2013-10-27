package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;

/* Map output columns to inputcolumns. User sees list of all output columns and can
 choose input columns per output column from a dropdown. */
public class ActionFeatureType_AttributeNames_Map_To_Output extends Action {

    private String[] outputAttributeNames;
    private String[] inputAttributeNames;
    
    private List<String> allOutputColumns;
    
    protected String description = "Kies per uitvoerkolom een invoerkolom.";

    public ActionFeatureType_AttributeNames_Map_To_Output(String[] outputAttributeNames, String[] inputAttributeNames, List<String> outputColumns) {
        this.outputAttributeNames = outputAttributeNames;
        this.inputAttributeNames = inputAttributeNames;
        
        if (outputColumns != null && outputColumns.size() > 0) {
            this.allOutputColumns = outputColumns;
        }
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }

     /**
     * simpele container voor attribuut
     */
    protected class AttributeSummary  {
        Class binding;
        Object value;
        String name;
 
        AttributeSummary(String name, Object value, Class binding) {
            this.binding = binding;
            this.value = value;
            this.name = name;
        }
    }
    
    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
 
        // geen mapping ingesteld
        if (allOutputColumns == null || allOutputColumns.size() == 0) {
            return feature;
        }

        // Alle attributen bewaren
        Map<String, AttributeSummary> inputAttributes = new HashMap<String, AttributeSummary>();
        for (int i = 0; i < inputAttributeNames.length; i++) {
            // haal id van column op
            Integer inputColumnId = getAttributeId(feature, inputAttributeNames[i]);
            // haal descriptor van input op
            AttributeDescriptor inputAd = feature.getFeatureType().getDescriptor(inputColumnId);
            // haal type van input op
            AttributeType at = inputAd.getType();
            inputAttributes.put(inputAttributeNames[i],
                    new AttributeSummary(inputAttributeNames[i],
                    feature.getAttribute(inputColumnId), at.getBinding()));
        }

        // verwijder dan alle niet-geom kolommen
        feature.removeAllAttributeDescriptors(true); // keepGeom = true

        // haal geom kolom op na verwijderen andere attributen
        String geometryName = null;
        GeometryAttribute ga = feature.getFeature().getDefaultGeometryProperty();
        if (ga != null) {
            geometryName = ga.getDescriptor().getLocalName();
        }
 
        for (String outputColumnName : allOutputColumns) {
            // haal naam van de input op obv index
            String inputColumnName = null;
            for (int i = 0; i < outputAttributeNames.length; i++) {
                if (outputAttributeNames[i].equals(outputColumnName)) {
                    inputColumnName = inputAttributeNames[i];
                    break;
                }
            }

            // default binding voor niet gemapte velden is String
            Class binding = String.class;
            Object value = null;
            String name = outputColumnName;
            if (inputColumnName != null) {
                AttributeSummary as = inputAttributes.get(inputColumnName);
                binding = as.binding;
                value = as.value;
            }
       
            // check of we de geom kolom willen hernoemen
            // als dit 2 keer gebeurt, dan wordt alleen de laatste gevuld
            if (inputColumnName!=null && inputColumnName.equalsIgnoreCase(geometryName)) {
                // hernoem geom kolom
                int geometryID = -1;
               List<AttributeDescriptor> attributeDescriptors = feature.getFeatureType().getAttributeDescriptors();
                for (int i = 0; i < attributeDescriptors.size(); i++) {
                    if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(geometryName)) {
                        geometryID = i;
                        break;
                    }
                }
                
                
                // Reload original attributeType
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                atb.init(feature.getFeatureType().getDescriptor(geometryID));

                // Overwrite attributeType with "attributeType with new name"
                feature.setAttributeDescriptor(geometryID, atb.buildDescriptor(name), true);

            } else {
                // voeg nieuwe kolom toe
                feature.addAttributeDescriptor(name, binding);
                // haal nieuwe id van column op
                Integer newColumnId = getAttributeId(feature, name);
                // zet waarde in nieuwe kolom
                feature.setAttribute(newColumnId, value);
            }
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