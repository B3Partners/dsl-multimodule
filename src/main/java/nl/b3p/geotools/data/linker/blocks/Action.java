/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;


import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
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
    protected static final Log log = LogFactory.getLog(Action.class);
    public static final String THE_GEOM = "the_geom";

    abstract public EasyFeature execute(EasyFeature feature) throws Exception;
    
    abstract public void flush(Status status, Map properties) throws Exception;

    @Override
    abstract public String toString();

    abstract public String getDescription_NL();

    public static List<List<String>> getConstructors() {
        return null;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Constructor has filled attributeID or attibuteName. With this funtion attributeID will be fixed (set / filled), using attributeName
     *
     * @param feature the current feature
     * @throws Exception generic exception
     */
    protected void fixAttributeID(EasyFeature feature) throws Exception {
        if (attributeID == -1) {
            attributeID = feature.getAttributeDescriptorIDbyName(attributeName);
        }else{
            attributeName = feature.getAttributeDescriptorNameByID(attributeID);
        }
    }
    
    protected Integer[] getAttributeIds(EasyFeature feature, String[] columnNames) throws Exception {
        Integer[] ids = null;
        if (columnNames != null && columnNames.length > 0) {
            ids = new Integer[columnNames.length];
            
            for (int i=0; i < columnNames.length; i++) {
                ids[i] = feature.getAttributeDescriptorIDbyName(columnNames[i]);
            }
        }
        
        return ids;
    }
    
    protected Integer getAttributeId(EasyFeature feature, String columnName) throws Exception {
        Integer id = null;
        if (columnName != null && !columnName.equals("")) {
            id = feature.getAttributeDescriptorIDbyName(columnName);            
        }
        
        return id;
    }


    /**
     * Fix a string and filter characters not allowed
     *
     * @param in the input String
     * @return the clean String
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

    abstract public void processPostCollectionActions(Status status, Map properties) throws Exception;
}
