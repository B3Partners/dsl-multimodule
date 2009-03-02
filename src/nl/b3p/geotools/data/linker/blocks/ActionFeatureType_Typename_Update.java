/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;

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
    public Feature execute(Feature feature) throws Exception {
        String typename = (append ? feature.getFeatureType().getTypeName() : "") + newTypeName;

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(typename);
        ftb.importType(feature.getFeatureType());
        ftb.setName(typename);

        feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());

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

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.NEW_TYPENAME
                    }, new String[]{
                        ActionFactory.NEW_TYPENAME,
                        ActionFactory.APPEND
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType de typenaam worden aangepast. De naam kan worden vervangen of kan worden verlengd.";
    }
}
