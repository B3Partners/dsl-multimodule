/**
 * $Id$
 */

package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.opengis.feature.simple.SimpleFeature;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;



/* Vervang tekst uit een attribuut waarde door andere tekst */
public class ActionFeature_Value_ReplaceText extends Action {

    private String attribute;
    private String search;
    private String replacement;

    // <editor-fold defaultstate="collapsed" desc="getters en setters">
    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
    // </editor-fold>

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.OBJECT_FIND,
                    ActionFactory.OBJECT_REPLACE
                }));

        return constructors;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {

        if(search == null) {
            return feature;
        }

        if(replacement == null) {
            replacement = "";
        }
        
        SimpleFeature f = feature.getFeature();

        if(f.getAttribute(attribute) instanceof String) {
            String s = (String)f.getAttribute(attribute);
            f.setAttribute(attribute, s.replaceAll(Pattern.quote(search), Matcher.quoteReplacement(replacement) ));
        }
        return feature;
    }

    @Override
    public String toString() {
        return "replace \"" + search + "\" with \"" + replacement + "\" for attribute\"" + attribute + "\"";
    }

    @Override
    public String getDescription_NL() {
        return "Vervang tekst van attribuut";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
