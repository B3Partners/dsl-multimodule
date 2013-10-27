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
 * Change typename by using a substring
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_Substring extends Action {

    private int beginIndex = -1;
    private int endIndex = -1;
    private boolean reverse = false;

    public ActionFeatureType_Typename_Substring(int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public ActionFeatureType_Typename_Substring(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    // Calculate substring from end (e.g. last 5 characters)
    public ActionFeatureType_Typename_Substring(int length, boolean reverse) {
        // Re-use of the var
        this.endIndex = length;
        this.reverse = reverse;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        String typename = feature.getFeatureType().getTypeName();

        if (reverse) {
            if (typename.length() - endIndex > 0) {
                typename = typename.substring(typename.length() - endIndex);
            }
            // if <= 0 then typename is already ok (e.g. short enough)
        } else if (beginIndex >= 0 && endIndex <= typename.length()) {
            if (endIndex >= 0) {
                typename = typename.substring(beginIndex, endIndex);
            } else {
                typename = typename.substring(beginIndex);
            }
        } else {
            String error;
            if (!reverse) {
                error = "Typename substring out of range; " + typename + ".subString(" + (endIndex >= 0 ? Integer.toString(beginIndex) + ", " + Integer.toString(endIndex) : Integer.toString(beginIndex)) + ")";
            } else {
                error = "Reverse typename substring failed; get last piece from " + typename + " with length " + endIndex;
            }

            throw new Exception(error);
        }

        feature.setTypeName(typename);

        return feature;
    }

    @Override
    public String toString() {
        if (!reverse) {
            return "Typename subString(" + (endIndex >= 0 ? Integer.toString(beginIndex) + ", " + Integer.toString(endIndex) : Integer.toString(beginIndex)) + ")";
        } else {
            return "Reverse typename substring; get last piece from typename with length " + endIndex;
        }
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.BEGIN_INDEX,
                    ActionFactory.END_INDEX
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.BEGIN_INDEX
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de typenaam worden ingekort door middel van een substring";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
