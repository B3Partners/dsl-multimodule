/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import nl.b3p.datastorelinker.entity.Database;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.oracle.OracleDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Convert batch to actionList and execute it
 * @author Gertjan Al, B3Partners
 */
public class DataStoreLinker implements Runnable {

    private static final Log log = LogFactory.getLog(DataStoreLinker.class);
    
    public static final String ACTIONLIST_PREFIX = "actionlist";
    public static final String ACTION_PREFIX = "action";
    public static final String TYPE_PREFIX = "type";
    public static final String SETTINGS_PREFIX = "settings";
    public static final String DATASTORE2READ_PREFIX = "read.datastore.";
    public static final String READ_TYPENAME = "read.typename";
    public static final Map<String, String> errorMapping = new HashMap();

    static {
        // Map combination of hasError (boolean / int) and errorDescription (String)
        errorMapping.put("parseError", "error");
    }

    protected Status status;
    protected TypeNameStatus typeNameStatus;

    protected DataStore dataStore2Read;
    protected ActionList actionList;
    protected Properties batch;

    protected nl.b3p.datastorelinker.entity.Process process;
    protected boolean disposed = false;


    public synchronized Status getStatus() {
        return status;
    }

    public synchronized TypeNameStatus getTypeNameStatus() {
        return typeNameStatus;
    }

    public DataStoreLinker(nl.b3p.datastorelinker.entity.Process process) throws Exception {
        this.process = process;
        dataStore2Read = openDataStore();
        actionList = createActionList();
        postInit();
    }

    public DataStoreLinker(Properties batch) throws Exception {
        init(batch);
        // Build actionList from propertyfile
        dataStore2Read = createDataStore2Read(batch);
        actionList = createActionList(batch);
        postInit();
    }

    public DataStoreLinker(Map<String, Object> input, Map<String, Object> actions, Properties batch) throws Exception {
        init(batch);
        dataStore2Read = openDataStore(input);
        actionList = createActionList(actions);
        postInit();
    }

    private void init(Properties batch) throws ConfigurationException, IOException {
        this.batch = batch;
        reset();
    }

    protected synchronized void reset() throws ConfigurationException, IOException {
        if (process != null)
            status = new Status(process);
        else if (batch != null)
            status = new Status(batch);
        else
            throw new ConfigurationException("Provide either an xml configuration file or a properties configuration file.");
        typeNameStatus = new TypeNameStatus();
        calculateSizes();
    }

    /**
     * Calculating the size can take a very long time depending on the implementation
     * of the chosen datastore. Some implementations walk through all features
     * to calculate the size.
     * 
     * @throws IOException
     */
    private void calculateSizes() throws IOException {
        int totalFeatureSize = 0;
        for (String typeName2Read : getTypeNames()) {
            FeatureCollection fc = dataStore2Read.getFeatureSource(typeName2Read).getFeatures();
            totalFeatureSize += fc.size();
        }
        status.setTotalFeatureSize(totalFeatureSize);
    }

    private void postInit() {
        log.info("dsl init complete.");
    }

    public void run() {
        try {
            process();
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public synchronized void dispose() throws Exception {
        if (!disposed) {
            dataStore2Read.dispose();
            actionList.close();
            disposed = true;
        }
    }

    /**
     * Process all features in dataStore2Read
     */
    public void process() throws Exception {
        reset();
        try {
            for (String typeName2Read : getTypeNames()) {
                processTypeName(typeName2Read);
            }
        } catch (IOException ex) {
            throw new Exception("Linking DataStores failed;", ex);
        } finally {
            dispose();
        }

        if (!status.getErrorReport().equals("")) {
            if (batch != null) {
                status.setErrorReport("Er zijn " + status.getErrorCount() + " fouten gevonden in DataStore " + getSaveProp(batch, "read.datastore.url", "-undefined-") + ".\n" + "Deze features zijn niet verwerkt door de DataStoreLinker.\n\nFoutmeldingen:\n" + (status.getErrorReport().length() > 500 ? status.getErrorReport().substring(0, 500) + "... (see log)" : status.getErrorReport()));
                DataStoreLinkerMail.mail(batch, status.getErrorReport());
            } else if (process != null) {
                status.setErrorReport("Er zijn " + status.getErrorCount() + " fouten gevonden in DataStore.\n" + "Deze features zijn niet verwerkt door de DataStoreLinker.\n\nFoutmeldingen:\n" + (status.getErrorReport().length() > 500 ? status.getErrorReport().substring(0, 500) + "... (see log)" : status.getErrorReport()));
                DataStoreLinkerMail.mail(process, status.getErrorReport());
            }
            
        }
    }

    private void processTypeName(String typeName2Read) throws Exception, IOException {
        SimpleFeature feature = null;

        FeatureCollection fc = dataStore2Read.getFeatureSource(typeName2Read).getFeatures();
        FeatureIterator iterator = fc.features();

        typeNameStatus.reset();

        try {
            while (iterator.hasNext()) {
                try {
                    feature = (SimpleFeature) iterator.next();
                    status.setTotalFeatureCount(status.getTotalFeatureCount() + 1);
                } catch (Exception ex) {
                    throw new Exception("Could not add Features, problem with provided reader", ex);
                }
                boolean mustBreak = processFeature(typeName2Read, feature);
                if (mustBreak)
                    break;
            }
            log.info("Total of: " + status.getTotalFeatureCount() + " features processed (" + typeName2Read + ")");
            log.info("Try to do the Post actions");
            actionList.processPostCollectionActions();
        } finally {
            fc.close(iterator);
        }
    }

    private boolean processFeature(String typeName2Read, SimpleFeature feature) throws Exception {
        boolean mustBreak = false;

        if (status.getTotalFeatureCount() % 10000 == 0) {
            log.info("" + status.getTotalFeatureCount() + " features processed (" + typeName2Read + ")");
        }

        doRunOnce(feature);
        
        if (status.getTotalFeatureCount() >= status.getFeatureStart() && (
                    (status.getTotalFeatureCount() <= status.getFeatureEnd() && status.getFeatureEnd() != -1) ||
                    (status.getFeatureEnd() == -1)
                )
            ) {
            // Does SimpleFeatureType support error handling?
            if (typeNameStatus.isDsReportsError()) {
                if (Boolean.parseBoolean(feature.getAttribute(typeNameStatus.getDsHasErrorAttribute()).toString()) || feature.getAttribute(typeNameStatus.getDsHasErrorAttribute()).toString().equals("1")) {
                    // feature contains error
                    status.setErrorReport(status.getErrorReport() + feature.getAttribute(typeNameStatus.getDsErrorAttribute()).toString() + "\n");
                    status.setErrorCount(status.getErrorCount() + 1);
                } else {
                    // feature is healty
                    actionList.process(new EasyFeature(feature));
                }
            } else {
                //
                if (feature.getDefaultGeometry() == null) {
                    // error, skip
                    status.setErrorCount(status.getErrorCount() + 1);
                    log.warn("Feature " + status.getTotalFeatureCount() + " has no geometry (null); skipping feature");
                } else {
                    if (feature.getDefaultGeometry() instanceof Geometry) {
                        if (((Geometry) feature.getDefaultGeometry()).isValid()) {
                            actionList.process(new EasyFeature(feature));
                        } else {
                            status.setErrorCount(status.getErrorCount() + 1);
                            log.warn("Feature " + status.getTotalFeatureCount() + " has no valid geometry (geometry.isValid() == false");
                        }
                    } else {
                        status.setErrorCount(status.getErrorCount() + 1);
                        log.warn("Feature " + status.getTotalFeatureCount() + " doesn't contain a allowed geometry (geometry instanceof com.vividsolutions.jts.geom.Geometry == false)");
                    }
                }
            }
            status.setProcessedFeatures(status.getProcessedFeatures() + 1);
        } else {
            mustBreak = true;
        }

        // TODO: rollback? option to rollback or not?
        if (status.isInterrupted())
            throw new InterruptedException("User canceled the process.");
        
        return mustBreak;
    }

    /**
     * Create dataStore2Read from properties
     */
    private DataStore createDataStore2Read(Properties batch) throws Exception {
        return openDataStore(propertiesToMaps(batch, DATASTORE2READ_PREFIX));
    }

    private ActionList createActionList() throws Exception {
        ActionList newActionList = new ActionList();

        Element actionsElement = new DOMBuilder().build(process.getActions());
        for (Object actionObject : actionsElement.getChildren("action")) {
            Element actionElement = (Element)actionObject;

            Map<String, Object> parameters = new HashMap<String, Object>();
            Element parametersElement = actionElement.getChild("parameters");
            if (parametersElement != null) {
                for (Object parameterObject : parametersElement.getChildren("parameter")) {
                    Element parameterElement = (Element)parameterObject;

                    String key = parameterElement.getChildTextTrim("paramId");
                    Object value = parameterElement.getChildTextTrim("value");
                    
                    String type = parameterElement.getChildTextTrim("type");
                    if (type.equalsIgnoreCase("boolean")) {
                        value = Boolean.valueOf(value.toString());
                    } else if (key.equalsIgnoreCase("url")) {
                        File file = new File(value.toString());
                        if (file.exists())
                            value = file.toURI().toURL();
                    }

                    parameters.put(key, value);
                }
            }

            newActionList.add(ActionFactory.createAction(
                    actionElement.getChildTextTrim("type"),
                    parameters));
        }

        // Finally: add output database to action list:
        newActionList.add(ActionFactory.createAction(
                "ActionCombo_GeometrySplitter_Writer",
                process.toOutputMap()));

        return newActionList;
    }

    /**
     * Create actionList from batch
     */
    private ActionList createActionList(Properties batch) throws Exception {
        Map<String, Object> actionlistParams = propertiesToMaps(batch, ACTIONLIST_PREFIX + "." + ACTION_PREFIX);
        return createActionList(actionlistParams);
    }

    public static ActionList createActionList(Map<String, Object> actionlistParams) throws Exception {
        ActionList actionList = new ActionList();

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

    private DataStore openDataStore() throws ConfigurationException, Exception {
        Database database = process.getInput().getDatabase();
        nl.b3p.datastorelinker.entity.File file = process.getInput().getFile();

        Map params = null;
        if (database != null) {
            return openDataStore(database);
        } else if (file != null) { // TODO: this should be a file now; change xsd to reflect this choice
            return openDataStore(file);
        } else { // safeguard:
            throw new ConfigurationException("Xml configuration exception: No input database or file specified.");
        }
    }

    public static DataStore openDataStore(Database database) throws ConfigurationException, Exception {
        return openDataStore(database.toMap());
    }

    public static DataStore openDataStore(nl.b3p.datastorelinker.entity.File file) throws ConfigurationException, Exception {
        return openDataStore(file.toMap());
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
                    stepIn.put(keypart, false);

                } else if (value.toLowerCase().equals("true")) {
                    stepIn.put(keypart, true);

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

    public String getFinishedMessage() {
        if (status.getProcessedFeatures() == 0 && status.getErrorCount() == 0) {
            return "No features processed, was this intended?";
        } else if (status.getProcessedFeatures() == status.getTotalFeatureCount() && status.getErrorCount() == 0) {
            return "All " + status.getProcessedFeatures() + " features processed";
        } else if (status.getProcessedFeatures() == status.getTotalFeatureCount()) {
            return status.getProcessedFeatures() + " features processed, but " + status.getErrorCount() + " errors (see log)";
        } else {
            return status.getProcessedFeatures() + " of " + status.getTotalFeatureCount() + " features processed.\n" + "Using parameters:\n" + "Start:  " + status.getFeatureStart() + "\n" + "End:    " + status.getFeatureEnd() + "\n" + "Errors: " + status.getErrorCount();
        }
    }

    private void doRunOnce(SimpleFeature feature) {
        if (typeNameStatus.isRunOnce()) {
            for (String hasErrorKey : errorMapping.keySet()) {
                if (feature.getFeatureType().getType(hasErrorKey) != null) {
                    String errorDesc = errorMapping.get(hasErrorKey);
                    if (feature.getFeatureType().getType(errorDesc) != null) {
                        // SimpleFeatureType contains errorReport
                        typeNameStatus.setDsHasErrorAttribute(hasErrorKey);
                        typeNameStatus.setDsErrorAttribute(errorDesc);
                        typeNameStatus.setDsReportsError(true);
                        break;
                    }
                }
            }
            typeNameStatus.setRunOnce(false);
        }
    }

    private String[] getTypeNames() throws IOException {
        String[] typenames = null;
        if (batch != null && batch.containsKey(READ_TYPENAME)) {
            // One table / schema
            typenames = new String[] {
                batch.getProperty(READ_TYPENAME)
            };
        } else if (process != null && process.getInput().getTableName() != null) {
            // One table / schema
            typenames = new String[] {
                process.getInput().getTableName()
            };
        }

        if (typenames == null) {
            // All tables / schema's
            typenames = dataStore2Read.getTypeNames();
        }
        return typenames;
    }

}
