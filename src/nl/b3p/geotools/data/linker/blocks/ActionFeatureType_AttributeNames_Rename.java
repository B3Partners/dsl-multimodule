package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.feature.AttributeTypeBuilder;

/* 
    Map input columns to outputcolumns. User sees list of all input columns and
    can fill in a new name in text fields. When user does not fill in a field the
    column will be removed and not appear in the output table. The geom column
    will not be removed.
*/
public class ActionFeatureType_AttributeNames_Rename extends Action {

    private Integer[] attributeIds;
    private String[] currentAttributeNames;
    private String[] newAttributeNames;
    private List<String> removeColumns = null;
    
    protected String description = "Kies per invoerkolom een eigen uitvoerkolomnaam.";

    public ActionFeatureType_AttributeNames_Rename(String[] currentAttributeNames, String[] newAttributeNames) {
        this.currentAttributeNames = currentAttributeNames;
        this.newAttributeNames = newAttributeNames;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        
        /* Create list of all columns and remove columns that are not mapped
         * by user. */
        if (removeColumns == null && currentAttributeNames != null &&
                currentAttributeNames.length > 0) {

            removeColumns = new ArrayList<String>();
            
            String geomColumn = feature.getFeatureType().getGeometryDescriptor().getName().getLocalPart();

            for (int i = 0; i < feature.getAttributeCount(); i++) {
                String columnName = feature.getAttributeDescriptorNameByID(i);
                
                /* Do not remove the geom column */
                if (!columnName.equals(geomColumn)) {
                    removeColumns.add(columnName);
                }
            }

            removeColumns.removeAll(Arrays.asList(currentAttributeNames));
        }

        /* Rename columns that are mapped by user */
        if (currentAttributeNames != null && currentAttributeNames.length > 0) {
            attributeIds = getAttributeIds(feature, currentAttributeNames);

            for (int i = 0; i < currentAttributeNames.length; i++) {
                AttributeTypeBuilder atb = new AttributeTypeBuilder();
                atb.init(feature.getFeatureType().getDescriptor(attributeIds[i]));

                feature.setAttributeDescriptor(attributeIds[i], atb.buildDescriptor(newAttributeNames[i]), true);
            }

            /* Remove columns that are not mapped by user. */
            for (String columnName : removeColumns) {
                int attributeId = feature.getAttributeDescriptorIDbyName(columnName);
                feature.removeAttributeDescriptor(attributeId);
            }
        }

        return feature;
    }

    @Override
    public String toString() {
        return "Kies per invoerkolom een eigen uitvoerkolomnaam.";
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
