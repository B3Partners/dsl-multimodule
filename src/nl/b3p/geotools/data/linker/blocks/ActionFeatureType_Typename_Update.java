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

/**
 * Change typename by overwriting it or append a String
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Typename_Update extends Action {

    private boolean append;
    private String newTypeName;

    /**
     * Change typename by appending a extension or rename it
     * @param newTypeName New typename of extension
     * @param append Boolean if newTypename should be added to current typename
     */
    public ActionFeatureType_Typename_Update(String newTypeName, boolean append) {
        this.newTypeName = newTypeName;
        this.append = append;
    }

    public ActionFeatureType_Typename_Update(String newTypeName) {
        this.newTypeName = newTypeName;
        this.append = false;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (append) {
            feature.setTypeName(feature.getTypeName() + newTypeName);
        } else {
            feature.setTypeName(newTypeName);
        }
        return feature;
    }

    @Override
    public String toString() {
        if (append) {
            return "Add \"" + newTypeName + "\" to typename";
        } else {
            return "Change typename to \"" + newTypeName + "\"";
        }
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.NEW_TYPENAME
                }));
*/
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.NEW_TYPENAME,
                    ActionFactory.APPEND
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de typenaam worden aangepast. De naam kan worden vervangen of kan worden verlengd.";
    }
}
