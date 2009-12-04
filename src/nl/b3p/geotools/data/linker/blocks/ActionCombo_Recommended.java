/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Transform SimpleFeature into recommended settings
 * @author Gertjan
 */
public class ActionCombo_Recommended extends Action {
    // This action is called Combo, but extends a normal action, due to the need of user params (newTypename, append)

    private String newTypeName;
    private boolean append = false;

    public ActionCombo_Recommended(String newTypeName, boolean append) {
        this.newTypeName = newTypeName;
        this.append = append;
    }

    public ActionCombo_Recommended(String newTypeName) {
        this.newTypeName = newTypeName;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        // Trim values to 255 characters
        Action action = new ActionFeature_Value_Substring_All(0, 255);
        feature = action.execute(feature);

        // Set typename
        action = new ActionFeatureType_Typename_Update(newTypeName, append);
        feature = action.execute(feature);

        // Set EPSG:28992
        action = new ActionFeatureType_Set_CRS("EPSG:28992");

        return action.execute(feature);
    }

    @Override
    public String toString() {
        return "Change typename to \"" + newTypeName + "\", set EPSG to 28992 and trim values to 255 characters";
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
        return "In deze ActionCombo worden de attribuutwaarden afgekapt, ter preventie van de Postgis 256 bug, en wordt de EPSG gezet ter preventie van de Shape bug";
    }
}
