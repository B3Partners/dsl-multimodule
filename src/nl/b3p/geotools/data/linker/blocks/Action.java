/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.*;

import nl.b3p.geotools.data.linker.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Every action extends Action
 * Every Comboblock extends actionCombo
 * Every Condition extends Condition
 * @author Gertjan Al, B3Partners
 */
public abstract class Action {

    protected String attributeName = "";
    protected int attributeID = -1;
    protected static final Log log = LogFactory.getLog(DataStoreLinker.class);
    protected static final String THE_GEOM = "the_geom";

    abstract public Feature execute(Feature feature) throws Exception;

    @Override
    abstract public String toString();

    abstract public String getDescription_NL();

    public static String[][] getConstructors() {
        return null;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Constructor has filled attributeID or attibuteName. With this funtion attributeID will be fixed (set / filled), using attributeName
     */
    protected void fixAttributeID(Feature feature) throws Exception {
        if (attributeID == -1) {
            // Get attributeID by attributeName
            for (int i = 0; i < feature.getNumberOfAttributes(); i++) {
                if (feature.getFeatureType().getAttributeType(i).getName().equals(attributeName)) {
                    attributeID = i;
                }
            }

            if (attributeID == -1) {
                String attributes = "";
                for (AttributeType attributeType : feature.getFeatureType().getAttributeTypes()) {
                    attributes += attributeType.getName() + ", ";
                }
                throw new Exception("Unable to locate index of '" + attributeName + "' in feature{" + attributes + "}");
            }
        }
    }

    /**
     * Check if attributeID falls between bounds of number of attributes in featureType
     */
    protected boolean hasLegalAttributeID(Feature feature) throws Exception {
        boolean isLegal = (attributeID < feature.getFeatureType().getAttributeCount() && attributeID != -1);

        if (!isLegal) {
            throw new Exception("Illegal attributeID set for " + getClass().getSimpleName() + " (" + attributeID + "," + attributeName + ")");
        }

        return isLegal;
    }

    /**
     * Fix a string and filter characters not allowed
     */
    public static String fixTypename(String in) {
        String allowed = "qwertyuiopasdfghjklzxcvbnm1234567890_";
        String out = "";

        for (int i = 0; i < in.length(); i++) {
            for (int j = 0; j < allowed.length(); j++) {
                if (in.substring(i, i + 1).toLowerCase().equals(allowed.substring(j, j + 1))) {
                    out += in.substring(i, i + 1);
                }
            }
        }

        return out;
    }

    public void close() throws Exception {
        // Override this if necessary
        // Used for closing iterator or reader / writer
    }
}
