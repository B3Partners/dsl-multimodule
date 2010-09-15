/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

/**
 * Transform SimpleFeature for use inside Oracle
 * @author Gertjan
 */
public class ActionCombo_Fix_To_Oracle extends ActionCombo {

    protected final static int ORACLE_MAX_TABLE_NAME_LENGTH_VANILLA = 30;
    protected final static int ORACLE_SPATIAL_MAX_LENGTH_SUFFIX = "_MV_THE_GEOM_IDX".length();

    protected final static int ORACLE_MAX_TABLE_NAME_LENGTH =
            ORACLE_MAX_TABLE_NAME_LENGTH_VANILLA - ORACLE_SPATIAL_MAX_LENGTH_SUFFIX;


    public ActionCombo_Fix_To_Oracle() {
        // Typename to uppercase
        ActionFeatureType_Typename_Case actionTypenameCase = new ActionFeatureType_Typename_Case(true);
        actionList.add(actionTypenameCase);

        // If typename is longer than allowed 26 characters
        ActionCondition_FeatureType_Typename_Length actionTypenameLength = new ActionCondition_FeatureType_Typename_Length(ActionCondition.CompareType.GREATER, ORACLE_MAX_TABLE_NAME_LENGTH);
        actionList.add(actionTypenameLength);

        // Then trim typename
        ActionFeatureType_Typename_Substring actionTypenameSubstring = new ActionFeatureType_Typename_Substring(ORACLE_MAX_TABLE_NAME_LENGTH, true);
        actionTypenameLength.addActionToList(true, actionTypenameSubstring);
    }

    public String getDescription_NL() {
        return "In deze ActionCombo wordt de typename zo aangepast dat deze gebruikt kan worden als Oracle tabelnaam. Dit betekend dat de typenaam omgezet wordt naar hoofdlettes en dat de lengte word aangepast aan de maximale lengte " + ORACLE_MAX_TABLE_NAME_LENGTH + " (neem laatste deel van de typenaam)";
    }
}
