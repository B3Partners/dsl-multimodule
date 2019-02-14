package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Append a attribute to the typename
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_AppendAttribute extends Action {

    private int maxLength;

    public ActionFeatureType_Typename_AppendAttribute(String attributeName, int maxLength) {
        this.attributeName = attributeName;
        this.maxLength = maxLength;
    }

    public ActionFeatureType_Typename_AppendAttribute(int attributeID, int maxLength) {
        this.attributeID = attributeID;
        this.maxLength = maxLength;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);
        String attributeValue = feature.getAttribute(attributeID).toString();

        if (attributeValue.length() > maxLength) {
            attributeValue = attributeValue.substring(0, maxLength);
            attributeValue = attributeValue.replaceAll("([\\s])", "_");
        }

        feature.setTypeName(feature.getTypeName() + attributeValue);

        return feature;
    }

    @Override
    public String toString() {
        return "Append attribute '" + (attributeName.equals("") ? attributeID : attributeName) + "' to typename";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.LENGTH
                }));
*/
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.LENGTH
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan de waarde van een attribuut worden toegevoegd aan de typename";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}

