/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

/**
 * @author Gertjan Al, B3Partners
 */
public class ActionCombo_Recommended extends ActionCombo {

    public ActionCombo_Recommended() {
        // Trim values to 255 characters
        Action action1 = new ActionFeature_Value_Substring_All(0, 255);
        actionList.add(action1);

        // Set typename to a safe value
        Action action2 = new ActionFeatureType_Typename_Replace("([ ])", "_");
        actionList.add(action2);

        Action action3 = new ActionFeatureType_Typename_Replace("([^a-zA-Z0-9_])", "");
        actionList.add(action3);

        Action action4 = new ActionFeatureType_Typename_Case(false);
        actionList.add(action4);

        // Set EPSG:28992
        Action action5 = new ActionFeatureType_Set_CRS("EPSG:28992");
        actionList.add(action5);
    }


    public String getDescription_NL() {
        return "(geen beschrijving opgegeven)";
    }
}
