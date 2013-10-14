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
public class ActionFeatureType_Typename_Replace extends Action {

    private String regEx;
    private String replacement;

    /**
     * Change typename by appending a extension or rename it
     * @param newTypeName New typename of extension
     * @param append Boolean if newTypename should be added to current typename
     */
    public ActionFeatureType_Typename_Replace(String regEx, String replace) {
        this.regEx = regEx;
        this.replacement = replace;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        feature.setTypeName(feature.getTypeName().replaceAll(regEx, replacement));
        return feature;
    }

    @Override
    public String toString() {
        return "regEx replacement: " + regEx + " for " + replacement;
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.REGEX,
                    ActionFactory.REPLACEMENT
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de typenaam worden aangepast. De naam kan worden vervangen of kan worden verlengd.";
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
