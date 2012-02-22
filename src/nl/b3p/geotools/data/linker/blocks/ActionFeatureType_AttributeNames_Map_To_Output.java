package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;

/* Map output columns to inputcolumns. User sees list of all output columns and can
 choose input columns per output column from a dropdown. */
public class ActionFeatureType_AttributeNames_Map_To_Output extends Action {

    private Integer[] attributeIds;
    private String[] currentAttributeNames;
    private String[] newAttributeNames;
    
    private List<String> allOutputColumns;
    private List<String> removeColumns = null;
    
    protected String description = "Kies per uitvoerkolom een invoerkolom.";

    public ActionFeatureType_AttributeNames_Map_To_Output(String[] currentAttributeNames, String[] newAttributeNames, List<String> outputColumns) {
        this.currentAttributeNames = currentAttributeNames;
        this.newAttributeNames = newAttributeNames;
        
        if (outputColumns != null && outputColumns.size() > 0) {
            this.allOutputColumns = outputColumns;
        }
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        /* TODO: Mogelijk x en y kolommen niet verwijderen. Dit gaat anders mis bij inlezen
         * csv in combinatie met het Maak Point uit waarden blok. Dit mapping blok moet dan
         * als laatste in de actielijst staan */
        
        /* Alle niet gemapte velden van (feature) invoer wissen */
        if (removeColumns == null && allOutputColumns != null &&
                allOutputColumns.size() > 0) {
            
            removeColumns = new ArrayList<String>();
            String geomColumn = feature.getFeatureType().getGeometryDescriptor().getName().getLocalPart();

            for (int i = 0; i < feature.getAttributeCount(); i++) {

                String columnName = feature.getAttributeDescriptorNameByID(i);

                /* Do not remove the geom column */
                if (!columnName.equals(geomColumn)) {
                    removeColumns.add(columnName);
                }
            }

            removeColumns.removeAll(Arrays.asList(newAttributeNames));            
        }
        
        /* Remove columns that are not mapped by user. */
        for (String columnName : removeColumns) {
            int attributeId = feature.getAttributeDescriptorIDbyName(columnName);
            feature.removeAttributeDescriptor(attributeId);
        }
        
        /* TODO: Van alle niet gemapte uitvoervelden die niet ook als invoer voorkomen
         * een kolom toevoegen met lege waarde */
        
        /* Voor alle gemapte velden feature kolomnaam omzetten naar bijbehorende
         * uitvoer kolom */
        if (newAttributeNames != null && newAttributeNames.length > 0) {            
            attributeIds = getAttributeIds(feature, newAttributeNames);
            
            for (int i=0; i < newAttributeNames.length; i++) {
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                atb.init(feature.getFeatureType().getDescriptor(attributeIds[i]));
                
                feature.setAttributeDescriptor(attributeIds[i], atb.buildDescriptor(currentAttributeNames[i]), true);              
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