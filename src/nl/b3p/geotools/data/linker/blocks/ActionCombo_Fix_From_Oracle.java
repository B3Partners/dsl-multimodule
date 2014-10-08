/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.Map;
import nl.b3p.geotools.data.linker.Status;

/**
 * Transform SimpleFeature for use outside Oracle
 * @author Gertjan
 */
public class ActionCombo_Fix_From_Oracle extends ActionCombo {

    public ActionCombo_Fix_From_Oracle() {
        // Shorts to Integers 
        // EvdP: bovenstaand commentaar klopt niet met de code. Waarom Short -> Double ipv Integer?
        // EvdP: Short -> Double lijkt ongelooflijk onnodig; Weer Integer van gemaakt
        ActionFeatureType_Replace_Class_All actionReplaceClasses = 
                new ActionFeatureType_Replace_Class_All(Short.class, Integer.class, true);
        actionList.add(actionReplaceClasses);
    }

    public String getDescription_NL() {
        return "In deze ActionCombo wordt de typename zo aangepast dat deze gebruikt kan worden als Oracle tabelnaam. Dit betekent dat de typenaam omgezet wordt naar hoofdletters en dat de lengte word aangepast aan de maximale lengte 26 (neem laatste deel van de typenaam)";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }

    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
