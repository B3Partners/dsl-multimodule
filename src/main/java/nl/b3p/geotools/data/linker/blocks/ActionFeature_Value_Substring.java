package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Replace a value inside a SimpleFeature where condition is true
 * @author Gertjan Al, B3Partners
 */
public class ActionFeature_Value_Substring extends Action {

    private int beginIndex = -1;
    private int endIndex = -1;

    public ActionFeature_Value_Substring(String attributeName, int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.attributeName = attributeName;
    }

    public ActionFeature_Value_Substring(int attributeID, int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.attributeID = attributeID;
    }

    public ActionFeature_Value_Substring(String attributeName, int beginIndex) {
        this.beginIndex = beginIndex;
        this.attributeName = attributeName;
    }

    public ActionFeature_Value_Substring(int attributeID, int beginIndex) {
        this.beginIndex = beginIndex;
        this.attributeID = attributeID;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);

        if (attributeID != -1) {
            if (feature.getAttribute(attributeID) instanceof String) {
                String value = (String) feature.getAttribute(attributeID);

                if (value.length() > beginIndex) {
                    if (endIndex == -1) {
                        value = value.substring(beginIndex);
                    } else {
                        if (value.length() > endIndex) {
                            value = value.substring(beginIndex, endIndex);
                        }
                    }
                }
                feature.setAttribute(attributeID, value);
            }
        } else {
            throw new Exception("Attribute " + attributeID + " not found");
        }

        return feature;
    }

    public String toString() {
        return "Make substring of value = substring(" + beginIndex + (endIndex == -1 ? "" : ", " + endIndex) + ")";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.BEGIN_INDEX,
                    ActionFactory.END_INDEX
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.BEGIN_INDEX
                }));

        /*
        constructors.add(Arrays.asList(new String[]{
        ActionFactory.ATTRIBUTE_ID,
        ActionFactory.BEGIN_INDEX,
        ActionFactory.END_INDEX
        }));

        constructors.add(Arrays.asList(new String[]{
        ActionFactory.ATTRIBUTE_ID,
        ActionFactory.BEGIN_INDEX
        }));
         */

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeature een attribuutWaarde worden gewijzigd in een subString van deze waarde";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
