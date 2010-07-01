/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

/**
 * Transform SimpleFeature for use outside Oracle
 * @author Gertjan
 */
public class ActionCombo_Fix_From_Oracle extends ActionCombo {

    public ActionCombo_Fix_From_Oracle() {
        // Shorts to Integers
        ActionFeatureType_Replace_Class_All actionReplaceClasses = new ActionFeatureType_Replace_Class_All(Short.class, Double.class, true);
        actionList.add(actionReplaceClasses);
    }

    public String getDescription_NL() {
        return "In deze ActionCombo wordt de typename zo aangepast dat deze gebruikt kan worden als Oracle tabelnaam. Dit betekend dat de typenaam omgezet wordt naar hoofdletters en dat de lengte word aangepast aan de maximale lengte 26 (neem laatste deel van de typenaam)";
    }
}
