package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import nl.b3p.geotools.data.linker.FeatureException;
import nl.b3p.geotools.data.linker.Status;
import static nl.b3p.geotools.data.linker.blocks.Action.log;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.oracle.OracleDialect;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.factory.Hints;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCFeatureStore;
import org.geotools.jdbc.PrimaryKey;
import org.geotools.jdbc.PrimaryKeyColumn;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;

/**
 * Write to a datastore (file or JDBC)
 *
 * @author Gertjan Al, B3Partners
 */
public class ActionDataStore_Writer extends Action {

    private boolean initDone = false;//,  runOnce = true;
    private DataStore dataStore2Write = null;
    private Map params;
    private final boolean append;
    private final boolean dropFirst;
    // post collection actions
    private boolean postCollectionActionsInitDone = false;
    private boolean polygonize = false;
    private boolean polygonizeWithAttr = false;
    private boolean polygonizeSufLki = false;
    private boolean postPointWithinPolygon = false;
    private Exception constructorEx;
    private List<String> datastoreTypeNames = new ArrayList();
    private Map<String, FeatureStore> featureStores = new HashMap();
    private Map<String, PrimaryKey> featurePKs = new HashMap();
    private List<String> featureTypeNames = new ArrayList();
    private Map<String, DefaultFeatureCollection> featureCollectionCache = new HashMap();
    private Map<String, Integer> featureBatchSizes = new HashMap();
    private Map<String, Integer> featuresWritten = new HashMap();
    private Map<String, List<List<String>>> featureErrors = new HashMap();
    private Map<String, List<List<String>>> featureNonFatals = new HashMap();
    private static final int MAX_CONNECTIONS_NR = 50;
    private static final String MAX_CONNECTIONS = "max connections";
    private static final int BATCHSIZE = 50;
    private static final int MAX_BATCHSIZE = 5000;
    private static final int INCREASEFACTOR = 2;
    private static final int DECREASEFACTOR = 10;
    private ArrayList<CollectionAction> collectionActions = new ArrayList();
    private Random generator = new Random((new Date()).getTime());

    public ActionDataStore_Writer(Map params, Map properties) {// Boolean append, Boolean dropFirst, Boolean polygonize, String polygonizeClassificationAttribute){
        this.params = params;
        log.debug(params);

        if (ActionFactory.propertyCheck(properties, ActionFactory.APPEND)) {
            append = (Boolean) properties.get(ActionFactory.APPEND);
        } else {
            append = true;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.DROPFIRST)) {
            dropFirst = (Boolean) properties.get(ActionFactory.DROPFIRST);
        } else {
            dropFirst = false;
        }
        if (!params.containsKey(MAX_CONNECTIONS)) {
            params.put(MAX_CONNECTIONS, MAX_CONNECTIONS_NR);
        }

        try {
            dataStore2Write = DataStoreLinker.openDataStore(params);
            if (dataStore2Write != null) {
                datastoreTypeNames = Arrays.asList(dataStore2Write.getTypeNames());
                initDone = true;
            }
        } catch (Exception ex) {
            constructorEx = ex;
        }

        initPostCollectionActions(properties);
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (!initDone) {
            throw new Exception("\nOpening dataStore failed; datastore could not be found, missing library or no access to file: " 
                    + toString() + "\n\n" + constructorEx.getLocalizedMessage());
        }

        long start = new Date().getTime();

        feature = fixFeatureTypeName(feature);
        String typename = feature.getFeatureType().getTypeName();

        PrimaryKey pks = null;
        DefaultFeatureCollection fc = null;
        FeatureStore store = null;
        List<List<String>> errors = null;
        List<List<String>> nonFatals = null;
        int batchsize = BATCHSIZE;
        int numWritten = 0;

        //TODO aanpassen batchsize mogelijk maken
        //hiermee kan dan ook (via batch size is -1) in een transactie oude records
        //worden verwijderd en nieuwe records worden toegevoegd, zodat een volledige
        //rollback gedaan kan worden

        if (featureTypeNames.contains(typename)) {
            pks = featurePKs.get(typename);
            fc = featureCollectionCache.get(typename);
            store = featureStores.get(typename);
            batchsize = featureBatchSizes.get(typename);
            numWritten = featuresWritten.get(typename);
            errors = featureErrors.get(typename);
            nonFatals = featureNonFatals.get(typename);
        } else {

            //TODO uitzoeken of drop ook in de transactie kan
            boolean delayRemoveUntilFirstCommit = false;
            if (batchsize == -1) {
                delayRemoveUntilFirstCommit = true;
            }

            // uitzoeken of tabel al is aangemaakt
            // mogelijk wordt de naam van de feature type hierbij nog aangepast
            String oldTypeName = typename;
            boolean typeExists = datastoreTypeNames.contains(oldTypeName);
            typename = checkSchema(feature.getFeatureType(), delayRemoveUntilFirstCommit, typeExists);
            if (!typename.equals(oldTypeName)) {
                feature.setTypeName(typename);
            }

            fc = new DefaultFeatureCollection(typename, feature.getFeatureType());
            featureCollectionCache.put(typename, fc);
            if (dataStore2Write != null) {
                if (dataStore2Write instanceof JDBCDataStore) {
                    FeatureSource fs = ((JDBCDataStore) dataStore2Write).getFeatureSource(typename, Transaction.AUTO_COMMIT);
                    if (fs instanceof JDBCFeatureStore) {
                        store = (JDBCFeatureStore) fs;
                        pks = ((JDBCFeatureStore) fs).getPrimaryKey();
                    } else {
                        throw new IllegalStateException("Table cannot be written: no primary key? FeatureSource was not of type JDBCFeatureStore");
                    }
                } else {
                    store = (FeatureStore) dataStore2Write.getFeatureSource(typename);
                }
            }

            featureStores.put(typename, store);
            featurePKs.put(typename, pks);
            featureBatchSizes.put(typename, batchsize);
            featuresWritten.put(typename, numWritten);
            featureErrors.put(typename, new ArrayList<List<String>>());
            featureNonFatals.put(typename, new ArrayList<List<String>>());
            // remember that typename is processed
            featureTypeNames.add(typename);
        }

        feature.convertGeomTo2D();

        prepareWrite(fc, pks, feature);

        // start writing when number of features is larger than batch size,
        // except when batch size is -1, then always wait for last feature
        Integer collectionSize = fc.size();

        if ((collectionSize >= batchsize && batchsize != -1)) {

            try {
                batchsize = writeCollection(fc, store, batchsize);
                featureBatchSizes.put(typename, batchsize);
            } catch (Exception ex) {
                throw new FeatureException("Error writing feature collection. Features not written.", ex);
            } finally {
                fc.retainAll(new ArrayList());
            }
        }

        long end = new Date().getTime() - start;

        log.debug("WRITER BLOCK: " + end);

        return feature;
    }

    private void prepareWrite(DefaultFeatureCollection fc, PrimaryKey pk, EasyFeature feature) throws IOException {
        // Bepaal de primary key(s) van record in de doeltabel
        PrimaryKey usePk = pk;
        // TODO: overnemen van pk uit source en instellen voor target
        // kan niet omdat dit niet (gemakkelijk) ondersteund wordt.
        // Nu wordt de automatisch gegenereerde primary key gebruikt
        // indien er geen doeltabel wordt gebruikt.
//          if (dropFirst) {
//              if (feature.getUserData().containsKey("sourcePks")) {
//                  usePk = (PrimaryKey) feature.getUserData().get("sourcePks");
//              }
//          }

        SimpleFeature newFeature = feature.getFeature();

        // bouw pk uit gemapte waarden uit bron
        StringBuilder oldfid = new StringBuilder();
        if (usePk != null) {
            List<PrimaryKeyColumn> pkcs = usePk.getColumns();
            for (PrimaryKeyColumn pkc : pkcs) {
                String cn = pkc.getName();
                Object o = feature.getAttribute(cn);
                if (o == null) {
                    // primary is blijkbaar niet gemapt, dan vertrouwen op autonumber
                    oldfid = null;
                    break;
                }
                oldfid.append(o).append(".");
            }
            if (oldfid != null) {
                oldfid.setLength(oldfid.length() - 1);
            }
        }
        if (oldfid != null && oldfid.length() > 0) {
            // voeg pk toe aan feature en zeg deze te gebruiken
            newFeature = feature.copy(oldfid.toString()).getFeature();
            newFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
        }
        
        // Check of feature geskipped moet worden
        SimpleFeatureType type = (SimpleFeatureType) fc.getSchema();
        if (feature.isSkipped()) {
             List<String> message = new ArrayList(
                Arrays.asList("Feature uitgefilterd", feature.getID()));
            featureNonFatals.get(type.getTypeName()).add(message);
        } else {
            fc.add(newFeature);
        }
    }

    private int writeCollection(DefaultFeatureCollection fc, FeatureStore store, int batchsize) throws FeatureException, IOException {
        // maak nieuwe subcollecties
        SimpleFeatureType type = (SimpleFeatureType) fc.getSchema();
        int orgbatchsize = batchsize;
        int stamp = generator.nextInt(10000);
        int orgfcsize = fc.size();

        log.info("Starting write out for typename: " + type.getTypeName()
                + " with batch size: " + orgbatchsize
                + " and stamp: " + stamp
                + " and size: " + orgfcsize);

        DefaultFeatureCollection currentFc = null;
        if (batchsize == -1) {
            // alles in een keer
            currentFc = fc;
        } else {
            // splits de collectie zodat in delen geprobeerd kan worden te schrijven
            currentFc = new DefaultFeatureCollection(type.getTypeName(), type);
            List removeList = new ArrayList();
            FeatureIterator fi = fc.features();
            int count = 0;
            while (fi.hasNext() && count < batchsize) {
                SimpleFeature newFeature = (SimpleFeature) fi.next();
                currentFc.add(newFeature);
                removeList.add(newFeature);
                count++;
            }
            fc.removeAll(removeList);

            if (!fc.isEmpty()) {
                batchsize = writeCollection(fc, store, batchsize);
                featureBatchSizes.put(type.getTypeName(), batchsize);
            }
        }

        Transaction t = new DefaultTransaction("add");
        store.setTransaction(t);
        try {
            if (batchsize == -1) {
                // als alles in een keer, dan ook pas oude feature weggooien
                // als nieuwe insert ok zijn (dus in zelfde transactie)
                log.info("Removing all features from: " + type.getTypeName()
                        + " within insert transaction.");
                store.removeFeatures(Filter.INCLUDE);
            }
            // schrijf batch aan features
            store.addFeatures(currentFc);
            t.commit();
            
            int numWritten = featuresWritten.get(type.getTypeName());
            int numProcessed = currentFc.size();
            featuresWritten.put(type.getTypeName(), numWritten + numProcessed);

            // indien succesvol dan volgende keer grotere batch
            batchsize *= INCREASEFACTOR;

            if (batchsize > MAX_BATCHSIZE) {
                batchsize = MAX_BATCHSIZE;
            }

            currentFc.retainAll(new ArrayList());
        } catch (Exception ex) {

            try {
                t.rollback();
            } catch (IOException ioex) {
                log.error("Error rolling back feature type: " + type.getTypeName() 
                        + ", retrying after error: " + ioex.getLocalizedMessage());
            }

            if (batchsize == -1) {
                // als batch size is -1 dan is de gehele collectie in een keer
                // geschreven en moet niet geprobeerd worden in kleinere delen
                // te committen, meteen foutmelding sturen
                List<String> message = new ArrayList(
                        Arrays.asList(ExceptionUtils.getRootCauseMessage(ex), "*"));
                featureErrors.get(type.getTypeName()).add(message);

                return batchsize;
            }

            if (batchsize == 1) {
                // als slechts een enkele feature niet geprocessed kan worden
                // dan opgeven
                SimpleFeature f = (SimpleFeature) (currentFc.toArray())[0];
                List<String> message = new ArrayList(
                        Arrays.asList(ExceptionUtils.getRootCauseMessage(ex), f.getID()));
                featureErrors.get(type.getTypeName()).add(message);
                return batchsize;
            }

            // probeer opnieuw met aangepast batch size
            batchsize /= DECREASEFACTOR;
            if (batchsize < 2) {
                batchsize = 1;
            }
            log.info("Rollback for feature type: " + type.getTypeName()
                    + ", retry with new batch size: " + batchsize);
            batchsize = writeCollection(currentFc, store, batchsize);
            featureBatchSizes.put(type.getTypeName(), batchsize);



        } finally {
            t.close();
        }

        log.info("finishing write out for typename: " + type.getTypeName()
                + " with batch size: " + orgbatchsize
                + " and stamp: " + stamp
                + " and size: " + orgfcsize);

        return batchsize;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing ActionDataStore Writer");
        if (dataStore2Write != null) {
            dataStore2Write.dispose();
        }
    }

    private void initPostCollectionActions(Map properties) {
        //slecht een keer init toestaan, ofwel in constructor ofwel in block
        //anders wordt het wel erg complex.
        if (postCollectionActionsInitDone) {
            return;
        }

        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE)) {
            polygonize = new Boolean(properties.get(ActionFactory.POLYGONIZE).toString());
            postCollectionActionsInitDone = true;
        } else {
            polygonize = false;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR)) {
            polygonizeWithAttr = new Boolean(properties.get(ActionFactory.POLYGONIZEWITHATTR).toString());
            postCollectionActionsInitDone = true;
        } else {
            polygonizeWithAttr = false;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZESUFLKI)) {
            polygonizeSufLki = new Boolean(properties.get(ActionFactory.POLYGONIZESUFLKI).toString());
            postCollectionActionsInitDone = true;
        } else {
            polygonizeSufLki = false;
        }

        if (ActionFactory.propertyCheck(properties, ActionFactory.POSTPOINTWITHINPOLYGON)) {
            postPointWithinPolygon = new Boolean(properties.get(ActionFactory.POSTPOINTWITHINPOLYGON).toString());
            postCollectionActionsInitDone = true;
        } else {
            postPointWithinPolygon = false;
        }
        if (this.polygonize) {
            log.info("Polygonize is configured as post action");
            collectionActions.add(new CollectionAction_Polygonize(new HashMap(properties)));
        }

        if (this.postPointWithinPolygon) {
            log.info("Point_Within_Polygon with attribute is configured as post action");
            try {
                collectionActions.add(new CollectionAction_Point_Within_Polygon(dataStore2Write, new HashMap(properties)));
            } catch (Exception e) {
                log.error("Cannot create Point_Within_Polygon post action", e);
            }
        }

        if (this.polygonizeWithAttr) {
            log.info("Polygonize with attribute is configured as post action");
            try {
                collectionActions.add(new CollectionAction_PolygonizeWithAttr(dataStore2Write, new HashMap(properties)));
            } catch (Exception e) {
                log.error("Can not create PolygonizeWithAttr post action", e);
            }
        } else if (this.polygonizeSufLki) {
            log.info("Polygonize with attribute is configured as post action");
            try {
                collectionActions.add(new CollectionAction_PolygonizeSufLki(dataStore2Write, new HashMap(properties)));
            } catch (Exception e) {
                log.error("Can not create PolygonizeWithAttr post action", e);
            }
        }

        if (this.postPointWithinPolygon) {
            log.info("Find polygon with point is configured as post action");
            try {
                collectionActions.add(new CollectionAction_Intersects_XY_Add_Attrib(dataStore2Write, new HashMap(properties)));
            } catch (Exception e) {
                log.error("Can not create Find polygon with point post action", e);
            }
        }

    }

    @Override
    public void processPostCollectionActions(Status status, Map properties) {
        //deze post actions kunnen ook door een block gezet zijn,
        initPostCollectionActions(properties);

        log.info("Collect errors from ActionDataStore_Writer");
        if (dataStore2Write != null) {
            try {
                String typeNames[] = dataStore2Write.getTypeNames();
                List<List<String>> nonFatals = null;
                for (int i = 0; i < typeNames.length; i++) {
                    nonFatals = featureNonFatals.get(typeNames[i]);
                    if (nonFatals != null && !nonFatals.isEmpty()) {
                        for (int j = 0; j < nonFatals.size(); j++) {
                            List<String> message = nonFatals.get(j);
                            status.addNonFatalError(message.get(0), message.get(1));
                        }
                    }
                }
                List<List<String>> errors = null;
                for (int i = 0; i < typeNames.length; i++) {
                    errors = featureErrors.get(typeNames[i]);
                    if (errors != null && !errors.isEmpty()) {
                        for (int j = 0; j < errors.size(); j++) {
                            List<String> message = errors.get(j);
                            status.addWriteError(message.get(0), message.get(1));
                        }
                    }
                }
                for (int i = 0; i < typeNames.length; i++) {
                    Integer numWritten = featuresWritten.get(typeNames[i]);
                    if (numWritten!=null) {
                        int numProcessed = status.getProcessedFeatures();
                        status.setProcessedFeatures(numProcessed + numWritten.intValue());
                    }
                }
            } catch (IOException e) {
                status.addWriteError("Error collecting errors/messages from ActionDataStore_Writer", null);
                log.error("Error collecting errors/messages from ActionDataStore_Writer.", e);
            }
        }

        for (int i = 0; i < collectionActions.size(); i++) {
            log.info("Process Post actions ActionDataStore_Writer");
            CollectionAction ca = collectionActions.get(i);
            //do the polygonize function
            if (ca instanceof CollectionAction_Polygonize) {
                for (int s = 0; s < featureTypeNames.size(); s++) {
                    try {
                        GeometryDescriptor gd = dataStore2Write.getSchema(featureTypeNames.get(s)).getGeometryDescriptor();
                        if (gd == null) {
                            continue;
                        }
                        Class geometryTypeBinding = dataStore2Write.getSchema(featureTypeNames.get(s)).getGeometryDescriptor().getType().getBinding();
                        if (LineString.class == geometryTypeBinding || MultiLineString.class == geometryTypeBinding) {
                            FeatureSource fs = dataStore2Write.getFeatureSource(featureTypeNames.get(s));
                            FeatureCollection fc = fs.getFeatures();
                            ca.execute(fc, this);
                        }
                    } catch (Exception e) {
                        status.addWriteError("Error while polygonizing the lines", "");
                        log.error("Error while Polygonizing the lines.", e);
                    }
                }
            }
            if (ca instanceof CollectionAction_Point_Within_Polygon) {
                DataStore ds = null;
                try {
                    CollectionAction_Point_Within_Polygon cap = (CollectionAction_Point_Within_Polygon) ca;

                    FeatureSource fs = dataStore2Write.getFeatureSource(cap.getPointsTable());
                    DefaultFeatureCollection fc = (DefaultFeatureCollection)fs.getFeatures();

                    FeatureSource fs2 = dataStore2Write.getFeatureSource(cap.getPolygonTable());
                    DefaultFeatureCollection fc2 = (DefaultFeatureCollection)fs2.getFeatures();

                    ds = DataStoreLinker.openDataStore(this.params);
                    cap.setDataStore2Write(ds);

                    cap.execute(fc, fc2, this);
                } catch (Exception e) {
                    status.addWriteError("Error while points within polygon with attributes", "");
                    log.error("Error while Points within Polygon with attributes.", e);
                } finally {
                    if (ds != null) {
                        ds.dispose();
                    }
                }
            }

            if (ca instanceof CollectionAction_PolygonizeWithAttr) {
                DataStore ds = null;
                try {
                    CollectionAction_PolygonizeWithAttr cap = (CollectionAction_PolygonizeWithAttr) ca;
                    FeatureSource fs = dataStore2Write.getFeatureSource(cap.getAttributeFeatureName());
                    FeatureCollection fc = fs.getFeatures();
                    ds = DataStoreLinker.openDataStore(this.params);
                    cap.setDataStore2Write(ds);
                    cap.execute(fc, this);
                } catch (Exception e) {
                    status.addWriteError("Error while polygonizing the lines with attributes", "");
                    log.error("Error while polygonizing the lines with attributes.", e);
                } finally {
                    if (ds != null) {
                        ds.dispose();
                    }
                }
            }
        }
    }
    /**
     * Check the schema and return the name.
     */
    private String checkSchema(SimpleFeatureType featureType, boolean delayRemoveUntilFirstCommit, boolean typeExists) throws Exception {

        String typename2Write = featureType.getTypeName();

        if (dropFirst && typeExists) {
            // Check if DataStore is a Database
            if (dataStore2Write instanceof JDBCDataStore) {
                log.info("Verwijderen van tabel: " + featureType.getTypeName());
                try {
                    dataStore2Write.removeSchema(featureType.getTypeName());
                } catch (IOException io) {
                    log.warn("Verwijderen van " + featureType.getTypeName() + " is niet gelukt, melding: " + io.getLocalizedMessage());
                }
            }
            typeExists = false;
        }

        // If table does not exist, create new
        if (!typeExists) {
            log.info("Creating new table with name: " + featureType.getTypeName());
            dataStore2Write.createSchema(featureType);

        } else if (!append && !delayRemoveUntilFirstCommit) {
            log.info("Removing all features from: " + typename2Write);
            boolean deleteSuccess = false;
            // Check if DataStore is a Database
            if (dataStore2Write instanceof JDBCDataStore) {
                // Empty table
                JDBCDataStore database = (JDBCDataStore) dataStore2Write;
                Connection con = null;
                try {
                    con = database.getConnection(Transaction.AUTO_COMMIT);
                    // try truncate: fast
                    removeAllFeaturesWithTruncate(database, con, typename2Write);
                    deleteSuccess = true;
                } catch (Exception e) {
                    log.debug("Removing using truncate failed: ", e);
                    Connection con1 = null;
                    try {
                        con1 = database.getConnection(Transaction.AUTO_COMMIT);
                        // try delete from table: mot so fast
                        removeAllFeaturesWithDelete(database, con, typename2Write);
                        deleteSuccess = true;
                    } catch (Exception e2) {
                        log.debug("Removing using delete from table failed: ", e2);
                    } finally {
                        if (con1 != null) {
                            con1.close();
                        }
                    }
                } finally {
                    if (con != null) {
                        con.close();
                    }
                }
            }
            if (!deleteSuccess) {
                // try using geotools: slowest
                removeAllFeatures(dataStore2Write, typename2Write);
                log.info("Removing using geotools");
            }
        }

        // lijst van tabellen opnieuw ophalen want er zijn tabellen geschreven
        // of verwijderd.
        datastoreTypeNames = Arrays.asList(dataStore2Write.getTypeNames());

        return typename2Write;
    }

    /**
     * De feature wordt ontdaan van ongeldige waarden en spaties worden
     * vervangen door underscrores. Verder wordt gekeken of er al een tabel
     * bestaat in de doeldatabase, waarbij hoofdletters worden genegeerd. Als
     * dat zo is dat wordt de tabelnaam (featuretype name) goed gezet waarbij
     * wel de hoofdletters goed gezet worden. Hierdoor kan later direct in de
     * lijst van tabellen worden gecheckt.
     *
     * @param feature
     * @return feature dat gefixt is
     * @throws Exception
     */
    private EasyFeature fixFeatureTypeName(EasyFeature feature) throws Exception {
        String oldTypeName = feature.getTypeName();

        String typename = fixTypename(oldTypeName.replaceAll(" ", "_"));
        for (String tnn : datastoreTypeNames) {
            // hoofdletters gebruik is niet relevant
            if (tnn.equalsIgnoreCase(typename)) {
                typename = tnn;
                break;
            }
        }
        // hoofdletter gebruik is wel relevant
        if (!typename.equals(oldTypeName)) {
            feature.setTypeName(typename);
        }
        return feature;
    }

    public Map getParams() {
        return params;
    }

    public String toString() {
        if (params==null) {
            return "No datastore params";
        }
        Map cleanedParams = new HashMap();
        for (Object param : params.keySet()) {
            if (!param.toString().contains("passw")) {
                cleanedParams.put(param, params.get(param));
            }
        }
        return "Datastore params: " + cleanedParams.toString();
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.PARAMS,
            ActionFactory.APPEND,
            ActionFactory.DROPFIRST
        }));

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.PARAMS
        }));

        return constructors;

    }

    public String getDescription_NL() {
        return "Schrijf de SimpleFeature weg naar een datastore. Als de datastore een database is, kan de SimpleFeature worden toegevoegd of kan de tabel worden geleegd voor het toevoegen";
    }

    private void removeAllFeaturesWithTruncate(JDBCDataStore database, Connection con, String typeName) throws SQLException {
        PreparedStatement ps = null;
        if (database.getSQLDialect() instanceof PostGISDialect) {
            ps = con.prepareStatement("TRUNCATE TABLE \"" + typeName + "\" CASCADE");
        } else { //if (database.getSQLDialect() instanceof OracleDialect) {
            ps = con.prepareStatement("TRUNCATE TABLE \"" + typeName + "\"");
        }
        ps.execute();
        log.info("Removing using truncate");
    }

    private void removeAllFeaturesWithDelete(JDBCDataStore database, Connection con, String typeName) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM \"" + typeName + "\"");
        ps.execute();
        log.info("Removing using delete from table");
    }

    private void removeAllFeatures(DataStore datastore, String typeName) throws IOException, Exception {
        DefaultTransaction transaction = new DefaultTransaction("removeTransaction");
        FeatureStore<SimpleFeatureType, SimpleFeature> store = (FeatureStore<SimpleFeatureType, SimpleFeature>) datastore.getFeatureSource(typeName);

        try {
            store.removeFeatures(Filter.INCLUDE);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
        for (Map.Entry pairs : featureCollectionCache.entrySet()) {
            String key = (String) pairs.getKey();
            FeatureStore store = featureStores.get(key);
            int batchsize = featureBatchSizes.get(key);
            DefaultFeatureCollection fc = (DefaultFeatureCollection) pairs.getValue();
            int fcsize = fc.size();
            if (fc != null && fcsize > 0) {
                batchsize = writeCollection(fc, store, batchsize);
            }
            log.info("finished flushing cache for typename: " + key
                    + " with batch size: " + batchsize
                    + " and size: " + fcsize);
        }
    }
}
