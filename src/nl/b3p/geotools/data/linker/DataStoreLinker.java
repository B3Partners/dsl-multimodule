/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import nl.b3p.geotools.data.linker.blocks.*;
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
    private static final String OGR2READ_PREFIX = "read.ogr.";
    private static final String FEATURES_START = "read.features.start";
    private static final String FEATURES_END = "read.features.end";
    private static final String READ_TYPENAME = DATASTORE2READ_PREFIX + "typename";
    private static final Map<String, String> errorMapping = new HashMap();
    private static boolean isOgr = false;


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

            if (isOgr) {
                RunOnceOGR_Loader.close(dataStore2Read);
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
        ActionList actionList = new ActionList();

        if (isOgr) {
            // Due to the ogr2ogr process some changes have to be made to support the
            File file = new File(batch.getProperty(OGR2READ_PREFIX + ActionFactory.URL));
            String name = file.getName();
            if (name.contains(".")) {
                name = name.substring(0, name.indexOf("."));
            }

            //actionList.add(new ActionFeatureType_Replace_Name("wkb_geometry", "the_geom"));
            actionList.add(new ActionFeatureType_Typename_Update(Action.fixTypename(name)));
            batch.setProperty(READ_TYPENAME, RunOnceOGR_Loader.TEMP_TABLE);
        }

        // Add actions from batchfile
        actionList.addAll(createActionList(batch));
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
            batchList = filterBatch(batch, OGR2READ_PREFIX);

            if (batchList.size() == 0) {
                throw new Exception("No " + DATASTORE2READ_PREFIX + " or " + OGR2READ_PREFIX + "found");
            } else {
                isOgr = true;
                // Open dataStore by read.ogr params
                // TODO check if vars exist
                String file_in = batchList.get(ActionFactory.URL);
                String fwtools_dir = getOsSpecificProperties().getProperty("ogr.dir");
                String srs = batchList.get(ActionFactory.SRS);

                boolean skipFailures = false;
                if (batchList.containsKey(ActionFactory.SKIPFAILURES)) {
                    skipFailures = ActionFactory.toBoolean(batchList.get(ActionFactory.SKIPFAILURES));
                }

                Map params = filterBatch(batch, OGR2READ_PREFIX + "tempdb.");
                Map db_tmp = new HashMap();

                // Note that other params than these 4 are not allowed
                if (params.containsKey("database")) {
                    db_tmp.put("dbname", params.get("database"));
                }

                if (params.containsKey("passwd")) {
                    db_tmp.put("password", params.get("passwd"));
                }

                if (params.containsKey("user")) {
                    db_tmp.put("user", params.get("user"));
                }

                if (params.containsKey("host")) {
                    db_tmp.put("host", params.get("host"));
                }

                String name = new File(file_in).getName();
                if (name.contains(".")) {
                    name = name.substring(0, name.indexOf("."));
                }
                params.put("table", Action.fixTypename(name));

                // Transform file into db
                RunOnce runOnce = new RunOnceOGR_Loader(fwtools_dir, file_in, db_tmp, srs, skipFailures);
                runOnce.execute();

                // Open the temp database as source
                return openDataStore(params);
            }
        } else {
            isOgr = false;
            // Open dataStore by read.datastore params
            Iterator iter = batchList.keySet().iterator();
            Map<String, Object> params = new HashMap();

            // Loop through all sorted actionProperties
            while (iter.hasNext()) {
                String property = (String) iter.next();
                String value = batchList.get(property);

                params.put(property, value);
            }

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
            return openDataStore(params);
        }
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
        }

        if (dataStore != null) {
            return dataStore;
        } else {
            throw new Exception("DataStore could not be found: " + params.toString());
        }
    }

    public static String getSaveProp(Properties batch, String key, String defaultValue) {
        if (batch.containsKey(key)) {
            return batch.getProperty(key);
        } else {
            return defaultValue;
        }
    }

    public static void setEnvironment(Map<String, String> environment, String prefix) {
        try {
            Properties p = getOsSpecificProperties();

            for (String prop : p.stringPropertyNames()) {
                if (prop.toLowerCase().startsWith(prefix)) {
                    String key = prop.substring(prefix.length());
                    String value = p.getProperty(prop);
                    environment.put(key, value);
                }
            }
        } catch (Exception ex) {
        }
    }

    public static Properties getOsSpecificProperties() {
        String os = System.getProperty("os.name");
        Properties p = new Properties();

        try {
            if (os.toLowerCase().contains("windows")) {
                os = "windows";
            } else if (os.toLowerCase().contains("linux")) {
                os = "linux";
            }

            Class c = DataStoreLinker.class;
            URL envProperties = c.getResource("pref_" + os + ".properties");
            p.load(envProperties.openStream());

        } catch (Exception ex) {
            log.warn("Unable to load environment settings from pref_" + os + ".properties;" + ex.getLocalizedMessage());
        }
        return p;
    }
}
