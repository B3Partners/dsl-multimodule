/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import com.vividsolutions.jts.geom.Geometry;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.geotools.data.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.oracle.OracleDataStoreFactory;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Convert batch to actionList and execute it
 * @author Gertjan Al, B3Partners
 */
public class DataStoreLinker {

    private static final Log log = LogFactory.getLog(DataStoreLinker.class);
    static final Logging logging = Logging.ALL;
    public static final String ACTIONLIST_PREFIX = "actionlist";
    public static final String ACTION_PREFIX = "action";
    public static final String TYPE_PREFIX = "type";
    public static final String SETTINGS_PREFIX = "settings";
    public static final String DATASTORE2READ_PREFIX = "read.datastore.";
    public static final String FEATURES_START = "read.features.start";
    public static final String FEATURES_END = "read.features.end";
    public static final String READ_TYPENAME = "read.typename";
    public static final Map<String, String> errorMapping = new HashMap();

    static {
        // Map combination of hasError (boolean / int) and errorDescription (String)
        errorMapping.put("parseError", "error");
    }

    /**
     * Process all features in dataStore2Read
     */
    public static String process(DataStore dataStore2Read, ActionList actionList, Properties batch) throws Exception {
        // Print actionList to log
        //log.info(actionList.getDescription());

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

                SimpleFeature feature = null;
                FeatureIterator iterator = dataStore2Read.getFeatureSource(typeName2Read).getFeatures().features();

                boolean runOnce = true;
                boolean dsReportsError = false;
                String dsHasErrorAttribute = "";
                String dsErrorAttribute = "";


                try {
                    while (iterator.hasNext()) {
                        try {
                            feature = (SimpleFeature) iterator.next();
                            totalFeatureCount++;
                        } catch (Exception ex) {
                            actionList.close();
                            throw new Exception("Could not add Features, problem with provided reader", ex);
                        }

                        // Check if SimpleFeature contains errorReport for all posibilities in errorMapping (HashMap)
                        if (runOnce) {
                            boolean match = false;
                            Iterator iter = errorMapping.keySet().iterator();

                            while (iter.hasNext() && !match) {
                                String hasErrorKey = (String) iter.next();

                                if (feature.getFeatureType().getType(hasErrorKey) != null) {
                                    String errorDesc = errorMapping.get(hasErrorKey);

                                    if (feature.getFeatureType().getType(errorDesc) != null) {
                                        // SimpleFeatureType contains errorReport
                                        dsHasErrorAttribute = hasErrorKey;
                                        dsErrorAttribute = errorDesc;

                                        match = true;
                                        dsReportsError = true;
                                    }
                                }
                            }
                            runOnce = false;
                        }

                        // Check if SimpleFeature is between defined start and end
                        if (totalFeatureCount >= featureStart && ((totalFeatureCount <= featureEnd && featureEnd != -1) || (featureEnd == -1))) {

                            // Does SimpleFeatureType support error handling?
                            if (dsReportsError) {
                                if (Boolean.parseBoolean(feature.getAttribute(dsHasErrorAttribute).toString()) ||
                                        feature.getAttribute(dsHasErrorAttribute).toString().equals("1")) {

                                    // feature contains error
                                    errorReport += feature.getAttribute(dsErrorAttribute).toString() + "\n";
                                    errorCount++;
                                } else {
                                    // feature is healty
                                    actionList.process(new EasyFeature(feature));
                                }
                            } else {
                                //
                                if (feature.getDefaultGeometry() == null) {
                                    // error, skip
                                    errorCount++;
                                    log.warn("Feature " + totalFeatureCount + " has no geometry (null); skipping feature");
                                } else {
                                    if (feature.getDefaultGeometry() instanceof Geometry) {
                                        if (((Geometry) feature.getDefaultGeometry()).isValid()) {
                                            actionList.process(new EasyFeature(feature));
                                        } else {
                                            errorCount++;
                                            log.warn("Feature " + totalFeatureCount + " has no valid geometry (geometry.isValid() == false");
                                        }
                                    } else {
                                        errorCount++;
                                        log.warn("Feature " + totalFeatureCount + " doesn't contain a allowed geometry (geometry instanceof com.vividsolutions.jts.geom.Geometry == false)");
                                    }
                                }
                            }
                            processedFeatures++;
                        }else{
                            break;
                        }
                    }
                } catch (Exception ex) {
                    actionList.close();
                    throw ex;

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
        if (processedFeatures == 0 && errorCount == 0) {
            return "No features processed, was this intended?";

        } else if (processedFeatures == totalFeatureCount && errorCount == 0) {
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

                if (params.containsKey(TYPE_PREFIX)) {

                    if (params.containsKey(SETTINGS_PREFIX)) {
                        if (params.get(TYPE_PREFIX) instanceof String && params.get(SETTINGS_PREFIX) instanceof Map) {
                            actionList.add(ActionFactory.createAction((String) params.get(TYPE_PREFIX), (Map) params.get(SETTINGS_PREFIX)));
                        } else {
                            throw new Exception("Expected " + ACTION_PREFIX + Integer.toString(count) + "." + TYPE_PREFIX + " to be String and " + ACTION_PREFIX + Integer.toString(count) + "." + SETTINGS_PREFIX + " to be a Map");
                        }
                    } else {
                        if (params.get(TYPE_PREFIX) instanceof String) {
                            actionList.add(ActionFactory.createAction((String) params.get(TYPE_PREFIX), new HashMap()));
                        } else {
                            throw new Exception("Expected " + ACTION_PREFIX + Integer.toString(count) + "." + TYPE_PREFIX + " to be String");
                        }
                    }
                    /*
                    if (params.get(TYPE_PREFIX) instanceof String && params.get(SETTINGS_PREFIX) instanceof Map) {
                    actionList.add(ActionFactory.createAction((String) params.get(TYPE_PREFIX), (Map) params.get(SETTINGS_PREFIX)));
                    } else {
                    throw new Exception("Expected " + ACTION_PREFIX + Integer.toString(count) + "." + TYPE_PREFIX + " to be String and " + ACTION_PREFIX + Integer.toString(count) + "." + SETTINGS_PREFIX + " to be a Map");
                    }
                     * */
                } else {
                    throw new Exception("No type found for action" + Integer.toString(count));
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
        String errormsg = "DataStore could not be found using parameters";

        try {
            // Workaround to enable use of Oracle DataStore
            if (params.containsKey("dbtype")) {
                if (params.get("dbtype").equals("oracle")) {
                    // Load Oracle Database
                    dataStore = (new OracleDataStoreFactory()).createDataStore(params);
                } else {
                    // Load regular Database
                    dataStore = DataStoreFinder.getDataStore(params);
                }
            } else {
                // Load regular Datastore
                dataStore = DataStoreFinder.getDataStore(params);
            }
        } catch (NullPointerException nullEx) {
            if (!urlExists(params)) {
                throw new Exception("URL in parameters seems to point to a non-existing file \n\n" + params.toString());
            } else {
                throw new Exception(errormsg + " " + params.toString());
            }
        }

        if (dataStore == null) {
            if (!urlExists(params)) {
                throw new Exception("URL in parameters may point to a non-existing file \n\n" + params.toString());
            } else {
                throw new Exception("DataStoreFinder returned null for \n\n" + params.toString());
            }

        } else {
            return dataStore;
        }
    }

    private static boolean urlExists(Map params) {
        try {
            if (params.containsKey("url")) {
                if (params.get("url") instanceof URL) {
                    URL url = (URL) params.get("url");
                    File file = new File(url.toURI());
                    return file.exists();

                } else if (params.get("url") instanceof String) {
                    String url = params.get("url").toString();
                    File file = new File(url);

                    return file.exists();
                }
            }
        } catch (URISyntaxException uriEx) {
            return false;
        }
        return true;
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
