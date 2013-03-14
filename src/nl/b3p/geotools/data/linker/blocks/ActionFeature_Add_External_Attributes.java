package nl.b3p.geotools.data.linker.blocks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.poi.ExcelReader;

/**
 *
 * @author Boy de Wit
 */
public class ActionFeature_Add_External_Attributes extends Action {

    private int attributeIDDXFHandle;
    private int attributeIDOtherFileHandle;
    private int attributeIDOtherFileName;
    private String attributeNameDXFHandle;
    private String attributeNameOtherFileHandle;
    private String attributeNameOtherFileName;
    private boolean useID = true;

    public ActionFeature_Add_External_Attributes(int attributeIDDXFHandle,
            int attributeIDOtherFileHandle, int attributeIDOtherFileName) {

        this.attributeIDDXFHandle = attributeIDDXFHandle;
        this.attributeIDOtherFileHandle = attributeIDOtherFileHandle;
        this.attributeIDOtherFileName = attributeIDOtherFileName;
    }

    public ActionFeature_Add_External_Attributes(String attributeNameDXFHandle,
            String attributeNameOtherFileHandle, String attributeNameOtherFileName) {

        this.attributeNameDXFHandle = attributeNameDXFHandle;
        this.attributeNameOtherFileHandle = attributeNameOtherFileHandle;
        this.attributeNameOtherFileName = attributeNameOtherFileName;

        this.useID = false;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

        attributeIDDXFHandle = -1;
        attributeIDOtherFileHandle = -1;
        attributeIDOtherFileName = -1;

        String dxfHandle = null;
        EasyFeature f = null;

        if (!useID) {
            /* Get value of handle dxf record */
            if (attributeNameDXFHandle != null && !attributeNameDXFHandle.equals("")) {
                attributeIDDXFHandle = feature.getAttributeDescriptorIDbyName(attributeNameDXFHandle);
            }

            if (attributeIDDXFHandle > 0) {
                if (feature.getAttribute(attributeIDDXFHandle) != null) {
                    dxfHandle = feature.getAttribute(attributeIDDXFHandle).toString();
                }
            }
        }

        /* Check if handle exists in external file */
        if (attributeNameOtherFileName != null &&
                dxfHandle != null && !dxfHandle.equals("")) {
            
            return addExternalData(feature, dxfHandle);
        }

        return feature;
    }

    private EasyFeature addExternalData(EasyFeature feature, String dxfHandle) {
        ExcelReader reader = new ExcelReader();

        try {
            List<String> record = null;
            List<String> columns = reader.getColumns(attributeNameOtherFileName);

            /* Kijken of dxfHandle waarde in de Excel kolom 
             * attributeNameOtherFileHandle voorkomt */
            Integer index = columns.indexOf(attributeNameOtherFileHandle.toLowerCase());
            
            if (index != null && index > -1) {
                record = reader.getRecord(attributeNameOtherFileName, index, dxfHandle);
            }

            /* Record is kolomnamen eerste regel van Excel + waardes */
            if (record != null && columns != null && record.size() > columns.size()) {
                for (int i = 0; i < columns.size(); i++) {
                    /* Koppel kolom zelf niet toevoegen */
                    if (i == index) {
                        continue;
                    }
                    
                    feature.addAttributeDescriptor(record.get(i), String.class);
                    feature.setAttribute(record.get(i), record.get(i + columns.size()));
                }
            }

            /* Kolommen in nabewerking ook toevoegen aan bijbehorend vlak */

        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }

        return feature;
    }

    public String toString() {
        return "";
    }

    public String getDescription_NL() {
        return "Verrijken met attributen uit extern bestand.";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[] {
                    ActionFactory.ATTRIBUTE_NAME_OTHER_FILE_NAME,
                    ActionFactory.ATTRIBUTE_NAME_DXF_HANDLE,
                    ActionFactory.ATTRIBUTE_NAME_OTHER_FILE_HANDLE                    
                }));

        return constructors;
    }
}
