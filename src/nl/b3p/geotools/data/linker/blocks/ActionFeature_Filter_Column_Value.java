package nl.b3p.geotools.data.linker.blocks;

import bsh.Interpreter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opengis.feature.simple.SimpleFeature;

/**
 *
 * @author Boy de Wit
 */
public class ActionFeature_Filter_Column_Value extends Action {

    protected static final Log logger = LogFactory.getLog(ActionFeature_Filter_Column_Value.class);
    private String columnName = null;
    private String operator = null;
    private String filterValue = null;

    public ActionFeature_Filter_Column_Value(
            String columnName,
            String operator,
            String filterValue) {

        this.columnName = columnName;
        this.operator = operator;
        this.filterValue = filterValue;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

        Boolean passes = false;
        
        String sourceValue = null;
        if (columnName != null) {
            SimpleFeature sourceF = feature.getFeature();

            if (sourceF.getAttribute(columnName) instanceof String) {
                sourceValue = (String) sourceF.getAttribute(columnName);
            }
        }

        if (sourceValue != null && operator != null && filterValue != null) {
            Interpreter i = new Interpreter();
            
            i.set("a", sourceValue);
            i.set("b", operator);
            i.set("c", filterValue);            
                    
            String cond = "abc";

            Boolean result = (Boolean) i.eval(cond);
            
            if (result) {
                passes = true;
            }
        }

        return feature;
    }

    public String toString() {
        return "";
    }

    public String getDescription_NL() {
        return "Filter feature op basis van kolomwaarde.";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.ATTRIBUTE_NAME_FILTER_COLUMN,
            ActionFactory.ATTRIBUTE_NAME_FILTER_OPERATOR,
            ActionFactory.ATTRIBUTE_NAME_FILTER_VALUE
        }));

        return constructors;
    }
}
