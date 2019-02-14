package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Add properties to map for configuration of post collection actions
 * @author Chris van Lith, B3Partners
 */
public class Action_Add_Properties extends Action {
    
    private Map<String,Object> extraProperties = null;

 
    public Action_Add_Properties(Map<String,Object> props) {
        extraProperties = props;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        return feature;
    }

    @Override
    public String toString() {
        return "Add properties to map for configuration: " + extraProperties.toString();
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.PROPERTY_NAME1,
                    ActionFactory.PROPERTY_VALUE1,
                    ActionFactory.PROPERTY_NAME2,
                    ActionFactory.PROPERTY_VALUE2,
                    ActionFactory.PROPERTY_NAME3,
                    ActionFactory.PROPERTY_VALUE3,
                    ActionFactory.PROPERTY_NAME4,
                    ActionFactory.PROPERTY_VALUE4,
                    ActionFactory.PROPERTY_NAME5,
                    ActionFactory.PROPERTY_VALUE5,
                    ActionFactory.PROPERTY_NAME6,
                    ActionFactory.PROPERTY_VALUE6,
                    ActionFactory.PROPERTY_NAME7,
                    ActionFactory.PROPERTY_VALUE7,
                    ActionFactory.PROPERTY_NAME8,
                    ActionFactory.PROPERTY_VALUE8,
                    ActionFactory.PROPERTY_NAME9,
                    ActionFactory.PROPERTY_VALUE9,
                    ActionFactory.PROPERTY_NAME10,
                    ActionFactory.PROPERTY_VALUE10
                }));

        return constructors;
    }
    
    public String getDescription_NL() {
        return "Met deze Action kunnen properties gezet worden voor het starten van post collection actions (zeer geavanceerd).";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
        if (properties!=null && extraProperties!=null) {
            properties.putAll(extraProperties);
        }
    }
}
