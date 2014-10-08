package nl.b3p.geotools.data.linker.blocks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.FeatureException;
import nl.b3p.geotools.data.linker.Status;
import static nl.b3p.geotools.data.linker.blocks.Action.log;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 *
 * @author meine
 */
public class ActionFeatureType_Parse_Date extends Action {

    private String format = null;

    public ActionFeatureType_Parse_Date(String attributeName, String format) {
        this.attributeName = attributeName;
        this.format = format;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        try {
            fixAttributeID(feature);
        } catch (FeatureException fex) {
            log.debug("Attribuut " + attributeName + " niet gevonden voor feature.");

            return feature;
        }

        String value = null;

        if (feature.getAttribute(attributeID) != null) {
            Object valueObj = feature.getAttribute(attributeID);
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            if(valueObj instanceof Date){
                Date d = (Date)valueObj;
                value = sdf.format(d);
            }else{
                throw new IllegalArgumentException("Attribute " + attributeName + " is not of type date.");
            }
        }
        feature.setAttributeDescriptor(attributeID, attributeName, String.class, false);

        if (value == null || value.equals("")) {
            feature.setAttribute(attributeID, null);
        } else {
            feature.setAttribute(attributeID, value);
        }
        return feature;
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }

    @Override
    public String toString() {
        return "Verander kolom.";
    }

    @Override
    public String getDescription_NL() {
        return "Parse een datum gegeven een format en converteer naar een string.";
    }

    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.ATTRIBUTE_NAME,
            ActionFactory.DATE_FORMAT
        }));

        return constructors;
    }

}
