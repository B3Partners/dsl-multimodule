/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

/**
 * Change typename by using a substring
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_Substring extends Action {

    private int beginIndex = -1;
    private int endIndex = -1;
    private boolean hasStartAndEnd;
    private boolean reverse = false;

    public ActionFeatureType_Typename_Substring(int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        hasStartAndEnd = true;
    }

    public ActionFeatureType_Typename_Substring(int beginIndex) {
        this.beginIndex = beginIndex;
        hasStartAndEnd = false;
    }

    // Calculate substring from end (e.g. last 5 characters)
    public ActionFeatureType_Typename_Substring(int length, boolean reverse) {
        // Re-use of the var
        this.beginIndex = length;
        this.reverse = reverse;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {
        String typename = feature.getFeatureType().getTypeName();

        if (beginIndex >= 0 && endIndex <= typename.length()) {
            if (reverse) {
                typename = typename.substring(typename.length() - beginIndex);
            } else {
                if (hasStartAndEnd) {
                    typename = typename.substring(beginIndex, endIndex);
                } else {
                    typename = typename.substring(beginIndex);
                }
            }
        } else {
            String error;
            if (!reverse) {
                error = "Typename substring out of range; " + typename + ".subString(" + (hasStartAndEnd ? Integer.toString(beginIndex) + ", " + Integer.toString(endIndex) : Integer.toString(beginIndex)) + ")";
            } else {
                error = "Reverse typename substring failed; get last piece from " + typename + " with length " + beginIndex;
            }

            throw new Exception(error);
        }

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(typename);
        ftb.importType(feature.getFeatureType());
        ftb.setName(typename);

        feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());

        return feature;
    }

    @Override
    public String toString() {
        if (!reverse) {
            return "Typename subString(" + (hasStartAndEnd ? Integer.toString(beginIndex) + ", " + Integer.toString(endIndex) : Integer.toString(beginIndex)) + ")";
        } else {
            return "Reverse typename substring; get last piece from typename with length " + beginIndex;
        }
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.BEGIN_INDEX,
                        ActionFactory.END_INDEX
                    }, new String[]{
                        ActionFactory.BEGIN_INDEX
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType de typenaam worden ingekort door middel van een substring";
    }
}
