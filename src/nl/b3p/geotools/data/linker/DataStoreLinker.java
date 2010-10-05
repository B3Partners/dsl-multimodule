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
import nl.b3p.datastorelinker.util.Namespaces;
import nl.b3p.datastorelinker.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
//import org.geotools.data.oracle.OracleDataStoreFactory;
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
    protected boolean disposed;


    public synchronized Status getStatus() {
        return status;
    }

    public synchronized TypeNameStatus getTypeNameStatus() {
        return typeNameStatus;
    }

    public DataStoreLinker(nl.b3p.datastorelinker.entity.Process process) throws Exception {
        init(process);
        dataStore2Read = openDataStore();
        actionList = createActionList();
        postInit();
    }

    public DataStoreLinker(Properties batch) throws Exception {
        init(batch);
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

    private void init(nl.b3p.datastorelinker.entity.Process process) throws ConfigurationException, IOException {
        this.process = process;
        disposed = false;
        status = new Status(process);
        typeNameStatus = new TypeNameStatus();
    }

    private void init(Properties batch) throws ConfigurationException, IOException {
        this.batch = batch;
        disposed = false;
        status = new Status(batch);
        typeNameStatus = new TypeNameStatus();
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
        if (dataStore2Read!=null){
            for (String typeName2Read : getTypeNames()) {
                FeatureCollection fc = dataStore2Read.getFeatureSource(typeName2Read).getFeatures();
                totalFeatureSize += fc.size();
            }
        }
        status.setTotalFeatureSize(totalFeatureSize);
    }

    private void postInit() throws IOException {
        calculateSizes();
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
        if (disposed)
            throw new Exception("Dsl already used. Please create a new instance of this class");
        
        try {
            for (String typeName2Read : getTypeNames()) {
                processTypeName(typeName2Read);
            }
        } catch (IOException ex) {
            throw new Exception("Linking DataStores failed", ex);
        } finally {
            //log.info("Error report: " + status.getErrorReport());
            log.info("Error report: " + status.getNonFatalErrorReport("\n", 3));
            // TODO: zet -> van DataStore -> naar DataStore in report.
            //if (status.getErrorCount() > 0) {
                if (batch != null) {
                    //status.setErrorReport("Er zijn " + status.getErrorCount() + " fouten gevonden in DataStore " + getSaveProp(batch, "read.datastore.url", "-undefined-") + ".\n" + "Deze features zijn niet verwerkt door de DataStoreLinker.\n\nFoutmeldingen:\n" + status.getTruncatedErrorReport());
                    DataStoreLinkerMail.mail(batch, status.getNonFatalErrorReport("\n", 3));
                } else if (process != null) {
                    //status.setErrorReport("Er zijn " + status.getErrorCount() + " fouten gevonden in DataStore " + ".\n" + "Deze features zijn niet verwerkt door de DataStoreLinker.\n\nFoutmeldingen:\n" + status.getTruncatedErrorReport());
                    DataStoreLinkerMail.mail(process, status.getNonFatalErrorReport("\n", 3));
                }
            //}
            dispose();
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
                    status.incrementTotalFeatureCount();
                } catch (Exception ex) {
                    throw new Exception("Could not add Features, problem with provided reader", ex);
                }
                boolean mustBreak = processFeature(typeName2Read, feature);
                if (mustBreak)
                    break;

                // TODO: rollback? option to rollback or not?
                if (status.isInterrupted())
                    throw new InterruptedException("User canceled the process.");
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
            log.info("" + status.getTotalFeatureCount() +"/"+status.getTotalFeatureSize() + " features processed (" + typeName2Read + ")");
        }

        doRunOnce(feature);
        
        if (mustProcessMoreFeatures()) {
            String error = testFeature(feature, typeNameStatus);
            if (error == null) {
                actionList.process(new EasyFeature(feature));
            } else {
                status.addNonFatalError(error, status.getTotalFeatureCount());
            }
            status.incrementProcessedFeatures();
        } else {
            mustBreak = true;
        }

        return mustBreak;
    }
    
    private boolean mustProcessMoreFeatures() {
        return status.getTotalFeatureCount() >= status.getFeatureStart() && (
                    (status.getTotalFeatureCount() <= status.getFeatureEnd() && status.getFeatureEnd() != -1) ||
                    (status.getFeatureEnd() == -1)
                );
    }

    /**
     * Returns error string or null if no error occurred during testing.
     * @param feature
     * @return
     */
    public static String testFeature(SimpleFeature feature) {
        return testFeature(feature, null);
    }
    
    /**
     * Returns error string or null if no error occurred during testing.
     * @param feature
     * @return
     */
    private static String testFeature(SimpleFeature feature, TypeNameStatus typeNameStatus) {
        // Does SimpleFeatureType support error handling?
        if (typeNameStatus != null && typeNameStatus.isDsReportsError()) {
            if (Boolean.parseBoolean(feature.getAttribute(typeNameStatus.getDsHasErrorAttribute()).toString()) || feature.getAttribute(typeNameStatus.getDsHasErrorAttribute()).toString().equals("1")) {
                // feature contains error
                //status.setErrorReport(status.getErrorReport() + feature.getAttribute(typeNameStatus.getDsErrorAttribute()).toString() + "\n");
                return feature.getAttribute(typeNameStatus.getDsErrorAttribute()).toString();
            }
        } else {
            if (feature.getDefaultGeometry() == null) {
                return "Feature heeft geen geometrie. Feature wordt overgeslagen.";
            } else if (!(feature.getDefaultGeometry() instanceof Geometry)) {
                return "Feature bevat geen toegestane geometrie.";
            } else if (!(((Geometry)feature.getDefaultGeometry()).isValid())) {
                return "Feature bevat geen valide geometrie";
            }
        }
        return null;
    }

    /**
     * Create dataStore2Read from properties
     */
    private DataStore createDataStore2Read(Properties batch) throws Exception {
        return openDataStore(propertiesToMaps(batch, DATASTORE2READ_PREFIX));
    }

    private ActionList createActionList() throws Exception {
        ActionList newActionList = new ActionList();

        org.w3c.dom.Element actions = process.getActions();
        if (actions != null) {
            Element actionsElement = new DOMBuilder().build(actions);
            for (Object actionObject : actionsElement.getChildren("action", Namespaces.DSL_NAMESPACE)) {
                Element actionElement = (Element)actionObject;

                Map<String, Object> parameters = new HashMap<String, Object>();
                Element parametersElement = actionElement.getChild("parameters", Namespaces.DSL_NAMESPACE);
                if (parametersElement != null) {
                    for (Object parameterObject : parametersElement.getChildren("parameter", Namespaces.DSL_NAMESPACE)) {
                        Element parameterElement = (Element)parameterObject;

                        String key = parameterElement.getChildTextTrim("paramId", Namespaces.DSL_NAMESPACE);
                        Object value = parameterElement.getChildTextTrim("value", Namespaces.DSL_NAMESPACE);

                        try {
                            value = Integer.valueOf(value.toString()).intValue();
                        } catch(NumberFormatException nfe) {
                            try {
                                value = parseBoolean(value.toString()).booleanValue();
                            } catch(Exception pex) {
                            }
                        }

                        /*String type = parameterElement.getChildTextTrim("type", Namespaces.DSL_NAMESPACE);

                        if (type.equalsIgnoreCase("boolean")) {
                            value = Boolean.valueOf(value.toString());
                        } else if (type.equalsIgnoreCase("number")) {
                            value = Integer.valueOf(value.toString());
                        } else if (key.equalsIgnoreCase("url")) {
                            File file = new File(value.toString());
                            if (file.exists())
                                value = file.toURI().toURL();
                        }*/

                        parameters.put(key, value);
                    }
                }

                newActionList.add(ActionFactory.createAction(
                        actionElement.getChildTextTrim("type", Namespaces.DSL_NAMESPACE),
                        parameters));
            }
        }

        // Finally: add output database to action list:
        newActionList.add(ActionFactory.createAction(process.getWriterType(),
                //"ActionCombo_GeometrySplitter_Writer",
                process.toOutputMap()));

        return newActionList;
    }

    private Boolean parseBoolean(String bool) throws Exception {
        if (bool.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        } else if (bool.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        } else {
            throw new Exception("The following value should be a boolean value (true or false): " + bool);
        }
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
        String file = process.getInput().getFile();

        if (database != null) {
            return openDataStore(database);
        } else if (file != null) { // TODO: this should be a file now; change xsd to reflect this "xsd:choice"
            return openDataStore(file);
        } else { // safeguard:
            throw new ConfigurationException("Xml configuration exception: No input database or file specified.");
        }
    }

    public static DataStore openDataStore(Database database) throws ConfigurationException, Exception {
        return openDataStore(database.toMap());
    }

    public static DataStore openDataStore(String file) throws ConfigurationException, Exception {
        return openDataStore(Util.fileToMap(new File(file)));
    }

    /**
     * Open a dataStore (save). Don't use DataStoreFinder.getDataStore(...) by yourself (Oracle workaround)
     */
    public static DataStore openDataStore(Map params) throws Exception {
        log.debug("openDataStore with: " + params);
        log.debug("available datastores: ");
        Iterator<DataStoreFactorySpi> iter = DataStoreFinder.getAvailableDataStores();
        while (iter.hasNext()) {
            DataStoreFactorySpi dsfSpi = iter.next();
            log.debug(dsfSpi + " :: " + dsfSpi.getDescription() + " :: " + dsfSpi.getDisplayName());
        }

        DataStore dataStore;
        String errormsg = "DataStore could not be found using parameters";

        try {
            dataStore = DataStoreFinder.getDataStore(params);
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
                    log.debug("Checking url exists: " + url);
                    File file = new File(url.toURI());

                    log.debug("Checking file url exists: " + file.getAbsolutePath());
                    return file.exists();

                } else if (params.get("url") instanceof String) {
                    String url = params.get("url").toString();
                    log.debug("Checking url exists: " + url);
                    File file = new File(url);

                    log.debug("Checking file url exists: " + file.getAbsolutePath());
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
