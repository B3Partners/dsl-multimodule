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
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Set a value inside a SimpleFeature at a given position
 * @author Gertjan Al, B3Partners
 */
public class ActionFeature_Value_Set extends Action {

    private Object objectReplace;
    private boolean append;

    public ActionFeature_Value_Set(String attributeName, Object objectReplace, boolean append) {
        this.attributeName = attributeName;
        this.objectReplace = objectReplace;
        this.append = append;
    }

    public ActionFeature_Value_Set(int attributeID, Object objectReplace, boolean append) {
        this.attributeID = attributeID;
        this.objectReplace = objectReplace;
        this.append = append;
    }

    public ActionFeature_Value_Set(String attributeName, Object objectReplace) {
        this.attributeName = attributeName;
        this.objectReplace = objectReplace;
        this.append = false;
    }

    public ActionFeature_Value_Set(int attributeID, Object objectReplace) {
        this.attributeID = attributeID;
        this.objectReplace = objectReplace;
        this.append = false;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        fixAttributeID(feature);

        if (attributeID != -1) {
            if (feature.getAttributeType(attributeID).getBinding().equals(objectReplace.getClass())) {
                Object replaceValue = objectReplace;
                if (append && objectReplace instanceof String) {
                    replaceValue = ((String) feature.getAttribute(attributeID) + (String) replaceValue);
                }
                feature.setAttribute(attributeID, replaceValue);

            } else {
                throw new Exception("Unable to set value in SimpleFeature to " + objectReplace.toString() + "; " + feature.getAttributeType(attributeID).getClass().getSimpleName() + " expected");
            }

        } else {
            throw new Exception("Attribute " + attributeID + " not found");
        }

        return feature;
    }

    public String toString() {
        return "Set SimpleFeature attribute '" + (attributeID == -1 ? attributeName : attributeID) + "' to \"" + objectReplace.toString() + "\"";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.OBJECT_REPLACE,
                    ActionFactory.APPEND,
                    ActionFactory.POSTGRESBOOLEAN
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.OBJECT_REPLACE,
                    ActionFactory.APPEND
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.OBJECT_REPLACE
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.OBJECT_REPLACE
                }));
*/

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeature een attribuutWaarde in een bepaalde kolom worden gewijzigd.";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
