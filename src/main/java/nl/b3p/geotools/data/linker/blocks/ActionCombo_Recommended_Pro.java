/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.Status;

/**
 * Transform SimpleFeature into recommended settings
 * @author Gertjan
 */
public class ActionCombo_Recommended_Pro extends ActionCombo {

    public ActionCombo_Recommended_Pro(String newTypeName, boolean append) {

        // Trim values to 255 characters
        Action action1 = new ActionFeature_Value_Substring_All(0, 255);
        actionList.add(action1);

        // Set typename
        Action action2 = new ActionFeatureType_Typename_Update(newTypeName, append);
        actionList.add(action2);

        // Set EPSG:28992
        Action action3 = new ActionFeatureType_Set_CRS("EPSG:28992");
        actionList.add(action3);
        
        // Remove existing fid
        // actionList.add(new ActionFeatureType_AttributeType_Remove("fid"));
    }

    public ActionCombo_Recommended_Pro(String newTypeName) {
        this(newTypeName, false);
    }

    @Override
    public String toString() {
        return "Change typename, set EPSG to 28992 and trim values to 255 characters";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
        /*
        constructors.add(Arrays.asList(new String[]{
        ActionFactory.NEW_TYPENAME
        }));
         */
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.NEW_TYPENAME,
                    ActionFactory.APPEND
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Creëert veilige waardes voor uw DataStoreLinker proces: Alle waardes in een attribuut / kolom van klasse String krijgen een maximum waarde van 255 ter preventie van de PostGIS 256 bug. De typenaam / tabelnaam krijgt een nieuwe veilige waarde die u zelf mag bepalen. De EPSG wordt op 28992 gezet (Amersfoort, Nieuw) ter preventie van de Shape bug.";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
