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
    //private static final String LIST_PREFIX = "actionlist.action";
    private static final String ACTIONLIST_PREFIX = "actionlist";
    private static final String ACTION_PREFIX = "action";
    private static final String TYPE_PREFIX = "type";
    private static final String SETTINGS_PREFIX = "settings";
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
     * Create dataStore2Read from properties
     */
    public static DataStore createDataStore2Read(Properties batch) throws Exception {
        return openDataStore(propertiesToMaps(batch, DATASTORE2READ_PREFIX));
    }

    /**
     * Create actionList from batch
     */
    public static ActionList createActionList(Properties batch) throws Exception {
        ActionList actionList = new ActionList();
        Map<String, Object> actionlistParams = propertiesToMaps(batch, ACTIONLIST_PREFIX + "." + ACTION_PREFIX);

        int count = 1;
        while (true) {
            if (actionlistParams.containsKey(Integer.toString(count))) {
                Map<String, Object> params = (Map) actionlistParams.get(Integer.toString(count));

                if (params.containsKey(TYPE_PREFIX) && params.containsKey(SETTINGS_PREFIX)) {
                    if (params.get(TYPE_PREFIX) instanceof String && params.get(SETTINGS_PREFIX) instanceof Map) {
                        actionList.add(ActionFactory.createAction((String) params.get(TYPE_PREFIX), (Map) params.get(SETTINGS_PREFIX)));
                    } else {
                        throw new Exception("Expected " + ACTION_PREFIX + Integer.toString(count) + "." + TYPE_PREFIX + " to be String and " + ACTION_PREFIX + Integer.toString(count) + "." + SETTINGS_PREFIX + " to be a Map");
                    }
                } else {
                    throw new Exception("No type or settings found for " + Integer.toString(count));
                }
            } else {
                break;
            }
            count++;
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

    /**
     * Make HashMap structure from propertylist
     * @param batch     Original properties list
     * @param filter    Specified to filter start of properties, like 'actionlist.action'
     * @return
     * @throws java.io.IOException
     */
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
