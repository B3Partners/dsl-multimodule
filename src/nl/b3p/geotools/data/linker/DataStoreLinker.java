/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import nl.b3p.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.feature.*;
import org.geotools.data.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.oracle.OracleDataStoreFactory;
import org.geotools.util.logging.Logging;

/**
 * Convert batch to actionList and execute it
 * @author Gertjan Al, B3Partners
 */
public class DataStoreLinker {

    private static final Log log = LogFactory.getLog(DataStoreLinker.class);
    static final Logging logging = Logging.ALL;
    private static final String LIST_PREFIX = "actionlist.action";
    private static final String DATASTORE2READ_PREFIX = "read.datastore.";
    private static final String FEATURES_START = "read.features.start";
    private static final String FEATURES_END = "read.features.end";
    private static final String READ_TYPENAME = DATASTORE2READ_PREFIX + "typename";
    private static final Map<String, String> errorMapping = new HashMap();


    static {
        // Map combination of hasError (boolean / int) and errorDescription (String)
        errorMapping.put("parseError", "error");
    }

    /**
     * Process all features in dataStore2Read
     */
    public static String process(DataStore dataStore2Read, ActionList actionList, Properties batch) throws Exception {
        // Print actionList to log
        log.info(actionList.getDescription());

        String errorReport = "";
        int errorCount = 0;

        int totalFeatureCount = 0;
        int processedFeatures = 0;
        int featureStart = 0;
        int featureEnd = -1;

        if (batch.containsKey(FEATURES_START)) {
            featureStart = ActionFactory.toInteger(batch.getProperty(FEATURES_START));
        }
        if (batch.containsKey(FEATURES_END)) {
            featureEnd = ActionFactory.toInteger(batch.getProperty(FEATURES_END));
        }


        try {

            String[] typenames;
            if (batch.containsKey(READ_TYPENAME)) {
                // One table / schema
                typenames = new String[]{batch.getProperty(READ_TYPENAME)};
            } else {
                // All tables / schema's
                typenames = dataStore2Read.getTypeNames();
            }

            for (int p = 0; p < typenames.length; p++) {
                String typeName2Read = typenames[p];

                Feature feature = null;
                FeatureIterator iterator = dataStore2Read.getFeatureSource(typeName2Read).getFeatures().features();

                if (!iterator.hasNext()) {
                    log.info("FeatureIterator starting without next for typename " + typeName2Read);
                }

                boolean runOnce = true;
                boolean dsReportsError = false;
                String dsHasErrorAttribute = "";
                String dsErrorAttribute = "";


                try {
                    while (iterator.hasNext()) {
                        try {
                            feature = (Feature) iterator.next();
                            totalFeatureCount++;
                        } catch (Exception ex) {
                            actionList.close();
                            throw new Exception("Could not add Features, problem with provided reader", ex);
                        }

                        // Check if feature contains errorReport for all posibilities in errorMapping (HashMap)
                        if (runOnce) {
                            boolean match = false;
                            Iterator iter = errorMapping.keySet().iterator();
                            while (iter.hasNext() && !match) {
                                String hasErrorKey = (String) iter.next();
                                if (feature.getFeatureType().getAttributeType(hasErrorKey) != null) {
                                    String errorDesc = errorMapping.get(hasErrorKey);
                                    if (feature.getFeatureType().getAttributeType(errorDesc) != null) {
                                        // FeatureType contains errorReport
                                        dsHasErrorAttribute = hasErrorKey;
                                        dsErrorAttribute = errorDesc;

                                        match = true;
                                        dsReportsError = true;
                                    }
                                }
                            }
                            runOnce = false;
                        }

                        // Check if feature is between defined start and end
                        if (totalFeatureCount >= featureStart && ((totalFeatureCount <= featureEnd && featureEnd != -1) || (featureEnd == -1))) {
                            // Does featureType support error handling?
                            if (dsReportsError) {
                                if (Boolean.parseBoolean(feature.getAttribute(dsHasErrorAttribute).toString()) ||
                                        feature.getAttribute(dsHasErrorAttribute).toString().equals("1")) {
                                    // Feature contains error
                                    errorReport += feature.getAttribute(dsErrorAttribute).toString() + "\n";
                                    errorCount++;
                                } else {
                                    // Feature is healty
                                    actionList.process(feature);
                                }
                            } else {
                                // No error handling, process feature
                                actionList.process(feature);
                            }
                            processedFeatures++;
                        }
                    }
                } catch (Exception ex) {
                    actionList.close();
                    throw new Exception(ex);

                } finally {
                    iterator.close();
                }
            }

            dataStore2Read.dispose();

        } catch (IOException ex) {
            actionList.close();
            throw new Exception("Linking DataStores failed;", ex);
        }

        if (!errorReport.equals("")) {
            errorReport = "Er zijn " + errorCount + " fouten gevonden in DataStore " + getSaveProp(batch, "read.datastore.url", "-undefined-") + ".\n" +
                    "Deze features zijn niet verwerkt door de DataStoreLinker.\n\nFoutmeldingen:\n" +
                    (errorReport.length() > 500 ? errorReport.substring(0, 500) + "... (see log)" : errorReport);
            DataStoreLinkerMail.mail(batch, errorReport);
        }

        actionList.close();
        if (processedFeatures == totalFeatureCount && errorCount == 0) {
            return "All " + processedFeatures + " features processed";

        } else if (processedFeatures == totalFeatureCount) {
            return processedFeatures + " features processed, but " + errorCount + " errors (see log)";

        } else {
            return processedFeatures + " of " + totalFeatureCount + " features processed.\n" +
                    "Using parameters:\n" +
                    "Start:  " + featureStart + "\n" +
                    "End:    " + featureEnd + "\n" +
                    "Errors: " + errorCount;
        }
    }

    /**
     * Process a batch
     */
    public static String process(Properties batch) throws Exception {
        // Build actionList from propertyfile
        DataStore dataStore2Read = createDataStore2Read(batch);
        ActionList actionList = createActionList(batch);
        return process(dataStore2Read, actionList, batch);
    }

    /**
     * Filter properties from batch and sort them
     */
    public static TreeMap<String, String> filterBatch(Properties batch, String filter) {
        // Get actionList items from propertyFile
        Set<String> propertyNames = batch.stringPropertyNames();
        TreeMap<String, String> batchList = new TreeMap();

        Iterator iter = propertyNames.iterator();
        while (iter.hasNext()) {
            String property = (String) iter.next();
            if (property.startsWith(filter)) {
                String value = batch.getProperty(property);
                property = property.replaceFirst(filter, "");

                batchList.put(property, value);
            }
        }

        return batchList;
    }

    /**
     * Create dataStore2Read from properties
     */
    public static DataStore createDataStore2Read(Properties batch) throws Exception {
        // Read datastore2read settings
        TreeMap<String, String> batchList = filterBatch(batch, DATASTORE2READ_PREFIX);

        // If none found, try ogr read settings
        if (batchList.size() == 0) {
            throw new Exception("No " + DATASTORE2READ_PREFIX + "found");
        }
        // Open dataStore by read.datastore params
        Iterator iter = batchList.keySet().iterator();
        Map<String, Object> params = new HashMap();

        params = propertiesToMaps(batch, DATASTORE2READ_PREFIX);

        /*

        // Loop through all sorted actionProperties
        while (iter.hasNext()) {
        String property = (String) iter.next();
        String value = batchList.get(property);

        params.put(property, value);
        }


        if (params.containsKey("url")) {
        File file = new File((String) params.get("url"));

        params.clear();

        if (file.exists()) {
        params.put("url", file.toURL());
        }
        }

        Map tmp_db = new HashMap();

        tmp_db.put("port", 5432);
        tmp_db.put("user", "dev");
        tmp_db.put("passwd", "b3p");
        tmp_db.put("database", "uploadDL");
        tmp_db.put("host", "b3p-demoserver");
        tmp_db.put("schema", "public");
        tmp_db.put("dbtype", "postgis");

        params.put("tmp_db", tmp_db);
        params.put("skip_failures", false);
        params.put("srs", "EPSG:28992");
         */
        /*
        if (params.containsKey("url")) {
        File file = new File((String) params.get("url"));

        if (file.exists()) {
        params.put("url", file.toURL());

        } else {
        String error = "File not found; " + file.toURL().toString();
        log.error(error);
        throw new Exception(error);
        }
        }
         */
        return openDataStore(params);

    }

    /**
     * Create actionList from batch
     */
    public static ActionList createActionList(Properties batch) throws Exception {
        TreeMap<String, String> batchList = filterBatch(batch, LIST_PREFIX);

        // ActionList to fill and return
        ActionList actionList = new ActionList();

        // ActionItem totalFeatureCount
        int actionCount = 1;

        Iterator iter = batchList.keySet().iterator();
        String actionClass = "";
        Map actionProperties = new HashMap();

        // Loop through all sorted actionProperties
        while (iter.hasNext()) {
            String property = (String) iter.next();
            String orgProperty = property;

            String actionID = Integer.toString(actionCount) + ".";

            // New actionStart found, create action from cache
            if (!property.startsWith(actionID)) {
                if (!actionClass.equals("")) {
                    // Flush action
                    actionList.add(ActionFactory.createAction(actionClass, new HashMap<String, Object>(actionProperties)));
                } else {
                    throw new Exception("Action " + actionCount + " has no classType; use property '" + LIST_PREFIX + actionID + "type");
                }

                // Set value defaults
                actionClass = "";
                actionProperties.clear();

                // Update count
                actionCount++;
                actionID = Integer.toString(actionCount) + ".";
            }

            // Don't change this if to }else{, actionID might have changed
            if (property.startsWith(actionID)) {

                // Strip actionID
                property = property.replaceFirst(actionID, "");

                if (property.equals("type")) {
                    // Found className of Action
                    actionClass = batchList.get(orgProperty);

                } else if (property.startsWith("settings.")) {
                    // Save actionSetting
                    property = property.replaceFirst("settings.", "");


                    // Find subsettings and put them in a new HashMap
                    Map subParams = actionProperties;
                    String subProperty = "";
                    while (property.contains(".")) {
                        subProperty = property.substring(0, property.indexOf("."));
                        property = property.substring(property.indexOf(".") + 1);

                        if (!subParams.containsKey(subProperty)) {
                            subParams.put(subProperty, new HashMap());
                        }

                        // Step into next hashMap
                        subParams = (Map) subParams.get(subProperty);
                    }

                    // Add value to the selected HashMap
                    if (batchList.containsKey(orgProperty)) {
                        subParams.put(property, batchList.get(orgProperty));

                    } else {
                        throw new Exception("Value could not be found for property " + orgProperty);
                    }
                }
            }
        }

        // Flush last Action
        if (!actionClass.equals("")) {
            actionList.add(ActionFactory.createAction(actionClass, new HashMap<String, Object>(actionProperties)));
        }

        return actionList;
    }

    /**
     * Open a dataStore (save). Don't use DataStoreFinder.getDataStore(...) by yourself (Oracle workaround)
     */
    public static DataStore openDataStore(Map params) throws Exception {
        DataStore dataStore;

        // Workaround to enable use of Oracle DataStore
        if (params.containsKey("dbtype")) {
            if (params.get("dbtype").equals("oracle")) {
                // Load Oracle Database
                dataStore = (new OracleDataStoreFactory()).createDataStore(params);
            } else {
                // Load Normal Database
                dataStore = DataStoreFinder.getDataStore(params);
            }
        } else {
            // Load regular Datastore
            dataStore = DataStoreFinder.getDataStore(params);

            OGRDataStoreFactory fac = new OGRDataStoreFactory();
            return fac.createDataStore(params);
        }

        if (dataStore == null) {
            throw new Exception("DataStore could not be found: " + params.toString());
        } else {
            return dataStore;
        }
    }

    public static String getSaveProp(Properties batch, String key, String defaultValue) {
        if (batch.containsKey(key)) {
            return batch.getProperty(key);
        } else {
            return defaultValue;
        }
    }

    public static Map<String, Object> propertiesToMaps(Properties batch, String filter) throws IOException {
        Map<String, Object> map = new HashMap();
        Iterator iter = batch.keySet().iterator();

        while (iter.hasNext()) {
            String key = (String) iter.next();

            if (key.startsWith(filter)) {
                String value = batch.getProperty(key);

                String keypart = key.substring(filter.length());
                


                Map<String, Object> stepIn = map;

                while (keypart.contains(".")) {
                    String step = keypart.substring(0, keypart.indexOf("."));
                    keypart = keypart.substring(keypart.indexOf(".") + 1);

                    if (stepIn.containsKey(step)) {
                        if (stepIn.get(step) instanceof Map) {
                            stepIn = (Map) stepIn.get(step);
                        } else {
                            throw new IOException("Tried to save settingsMap for property '" + key + "', but property has already a value?\nValue = '" + stepIn.get(step).toString() + "'");
                        }
                    } else {
                        Map<String, Object> newStep = new HashMap();
                        stepIn.put(step, newStep);
                        stepIn = newStep;
                    }
                }


                if (value.toLowerCase().equals("false")) {
                    stepIn.put(keypart, new Boolean(false));

                } else if (value.toLowerCase().equals("true")) {
                    stepIn.put(keypart, new Boolean(true));

                } else if (keypart.toLowerCase().equals("url")) {
                    File file = new File(value);
                    if (file.exists()) {
                        stepIn.put(keypart, file.toURL());
                    } else {
                        stepIn.put(keypart, value);
                    }

                } else {
                    stepIn.put(keypart, value);
                }
            }
        }

        return map;
    }
}
