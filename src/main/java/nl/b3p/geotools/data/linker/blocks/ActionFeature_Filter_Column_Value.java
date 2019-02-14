package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.FeatureException;
import static nl.b3p.geotools.data.linker.blocks.Action.log;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.filter.Filter;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 *
 * @author Boy de Wit
 */
public class ActionFeature_Filter_Column_Value extends Action {

    protected static final Log logger = LogFactory.getLog(ActionFeature_Filter_Column_Value.class);
    private String columnName = null;
    private String operator = null;
    private String filterValue = null;

    public ActionFeature_Filter_Column_Value (
            String columnName,
            String operator,
            String filterValue) {

        this.columnName = columnName;
        this.operator = operator;
        this.filterValue = filterValue;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        long start = new Date().getTime();
        
        int id = feature.getAttributeDescriptorIDbyName(columnName);
        if (id<0) {
            String err = "Kolom " + columnName + " in filter blok bestaat niet.";
            
            throw new Exception(err);
        }
        
        //Een filter wordt opgebouwd via kolomnaam + operator + waarde
        //
        //Operator wordt zondermeer toegevoegd, dit kan als volgt gebruikt worden:
        //* is null of is not null
        //* kolomnaam = 12345 (testen op getal, werkt alleen als db-type getal is)
        //* kolomnaam = '' (testen op lege string)
        //
        //Het is in de DSL niet mogelijk om quotjes te plaatsen om een waarde.
        //Deze worden door de GUI er weer afgestript. Om dit toch zo logisch
        //mogelijk te laten werken, worden standaard quotjes toegevoegd om de waarde.
        //Dit werkt goed voor het veel gebruikte varchar db-type.
        //Als waarde leeg is, dan worden geen quotes toegevoegd; daarmee kan via 
        //de operator een geldig statement worden gemaakt.
        //Als je "kolomnaam = ''" wil doen, dan kun je geen '' in waarde plaatsen
        //(wordt weggehaald), maar je kunt wel waarde leeglaten en bij
        //operator = '' neerzetten.
        
        String filterString = columnName + " " + operator;
        
        if (filterValue!=null && !filterValue.trim().isEmpty()) {
            filterString += " '";
            filterString += filterValue;
            filterString += "'";
        }
        
        Filter filter = CQL.toFilter(filterString);
        boolean result = filter.evaluate(feature.getFeature());

        if (!result) {
            feature.setSkipped(true);
        }
        
        long end = new Date().getTime() - start;        
        
        log.debug("FILTER BLOCK: " + end);

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

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
