/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gertjan Al, B3Partners
 */
public class ActionCombo_Recommended extends ActionCombo {

    public ActionCombo_Recommended() {
        // Trim values to 255 characters
        actionList.add(new ActionFeature_Value_Substring_All(0, 255));

        // Set typename to a safe value
        actionList.add(new ActionFeatureType_Typename_Replace("([ ])", "_"));
        actionList.add(new ActionFeatureType_Typename_Replace("([^a-zA-Z0-9_])", ""));
        actionList.add(new ActionFeatureType_Typename_Replace("^[0-9]", "_$0"));
        actionList.add(new ActionFeatureType_Typename_Case(false));

        // Set EPSG:28992
        actionList.add(new ActionFeatureType_Set_CRS("EPSG:28992"));
    }

    public static List<List<String>> getConstructors() {
        return new ArrayList<List<String>>();
    }

    public String getDescription_NL() {
        return "Creëert veilige waardes voor uw DataStoreLinker proces: Alle waardes in een attribuut / kolom van klasse String krijgen een maximum waarde van 255 ter preventie van de PostGIS 256 bug. De typenaam / tabelnaam krijgt een veilige waarde. De EPSG wordt op 28992 gezet (Amersfoort, Nieuw) ter preventie van de Shape bug.";
    }
}
