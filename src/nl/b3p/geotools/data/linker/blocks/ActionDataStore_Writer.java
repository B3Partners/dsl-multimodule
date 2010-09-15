package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.geotools.data.*;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.data.oracle.OracleDialect;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * Write to a datastore (file or JDBC)
 * @author Gertjan Al, B3Partners
 */
public class ActionDataStore_Writer extends Action {

    private boolean initDone = false;//,  runOnce = true;
    private DataStore dataStore2Write = null;
    private Map params;
    private final boolean append;
    private final boolean dropFirst;
    private final boolean polygonize;
    private final boolean polygonizeWithAttr;
    private final boolean polygonizeSufLki;
    private Exception constructorEx;
    private HashMap<String, FeatureWriter> featureWriters = new HashMap();
    private HashMap<String, String> checked = new HashMap();
    private ArrayList<String> featureTypeNames = new ArrayList();
    private static final int MAX_CONNECTIONS_NR = 50;
    private static final String MAX_CONNECTIONS = "max connections";
    private static int processedTypes = 0;
    private ArrayList<CollectionAction> collectionActions = new ArrayList();

    public ActionDataStore_Writer(Map params, Map properties) {// Boolean append, Boolean dropFirst, Boolean polygonize, String polygonizeClassificationAttribute){
        this.params = params;

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
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE)) {
            polygonize = (Boolean) properties.get(ActionFactory.POLYGONIZE);
        } else {
            polygonize = false;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR)) {
            polygonizeWithAttr = (Boolean) properties.get(ActionFactory.POLYGONIZEWITHATTR);
        } else {
            polygonizeWithAttr = false;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZESUFLKI)) {
            polygonizeSufLki = (Boolean) properties.get(ActionFactory.POLYGONIZESUFLKI);
        } else {
            polygonizeSufLki = false;
        }if (!params.containsKey(MAX_CONNECTIONS)) {
            params.put(MAX_CONNECTIONS, MAX_CONNECTIONS_NR);
        }

        if (this.polygonize) {
            log.info("Polygonize is configured as post action");
            collectionActions.add(new CollectionAction_Polygonize(new HashMap(properties)));
        }

        try {
            dataStore2Write = DataStoreLinker.openDataStore(params);
            initDone = (dataStore2Write != null);

        } catch (Exception ex) {
            constructorEx = ex;
        }
        if (this.polygonizeWithAttr) {
            log.info("Polygonize with attribute is configured as post action");
            try{
                collectionActions.add(new CollectionAction_PolygonizeWithAttr(dataStore2Write,new HashMap(properties)));
            }catch(Exception e){
                log.error("Can not create PolygonizeWithAttr post action",e);
            }
        }else if (this.polygonizeSufLki) {
            log.info("Polygonize with attribute is configured as post action");
            try{
                collectionActions.add(new CollectionAction_PolygonizeSufLki(dataStore2Write,new HashMap(properties)));
            }catch(Exception e){
                log.error("Can not create PolygonizeWithAttr post action",e);
            }
        }
    }
    /* public ActionDataStore_Writer(Map params, Boolean append, Boolean dropFirst) {
    this(params,append,dropFirst,null,null);
    }*/

    /*public ActionDataStore_Writer(Map params) {
    this(params,null,null);
    }*/
    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (!initDone) {
            throw new Exception("\nOpening dataStore failed; datastore could not be found, missing library or no access to file.\nUsed parameters:\n" + params.toString() + "\n\n" + constructorEx.getLocalizedMessage());

        } else {
            feature = fixFeatureTypeName(feature);
            String typename = feature.getFeatureType().getTypeName();
            //get the correct typename from the datastore
            String newTypeName=correctTypeName(typename, dataStore2Write);
            //if not the same (case sensitive) then change the typename
            if (!newTypeName.equals(typename)){
                feature.setTypeName(newTypeName);
                typename=newTypeName;
            }
            //store the typename
            if (!featureTypeNames.contains(typename)) {
                featureTypeNames.add(typename);
            }
            FeatureWriter writer;
            if (featureWriters.containsKey(typename)) {
                writer = featureWriters.get(typename);
            } else {

                if (featureWriters.size() + 1 == MAX_CONNECTIONS_NR) {
                    // If max connections reached, commit all data and continue
                    close();
                    dataStore2Write = DataStoreLinker.openDataStore(params);
                    log.warn("Closing all connections (too many featureWriters loaded)");
                }
                if (!checked.containsKey(params.toString() + typename)) {
                    checkSchema(feature.getFeatureType());
                    checked.put(params.toString() + typename, "");
                    processedTypes++;
                    //correct the typename after a possible creation of the schema
                    typename=correctTypeName(typename, dataStore2Write);
                }

                writer = dataStore2Write.getFeatureWriterAppend(typename, Transaction.AUTO_COMMIT);
                featureWriters.put(typename, writer);
            }


            if (feature.getAttribute(feature.getFeatureType().getGeometryDescriptor().getLocalName()) == null) {
                log.warn("No DefaultGeometry AttributeType found in feature " + feature.toString());
            } else {
                try {
                    Geometry the_geom = (Geometry) feature.getAttribute(feature.getFeatureType().getGeometryDescriptor().getLocalName());
                    if (the_geom instanceof GeometryCollection) {
                        if (((GeometryCollection) the_geom).getNumGeometries() == 0) {
                            log.warn("Skipping feature with empty GeometryCollection; " + feature.getAttributes().toString());
                        } else {
                            write(writer, feature.getFeature());
                        }
                    } else {
                        write(writer, feature.getFeature());
                    }
                } catch (Exception ex) {
                    log.error("Error getting geometry. Feature not written: "+feature.toString(), ex);
                    //Remove writer so a new writer is created when the next feature is processed
                    if (writer!=null){
                        writer.close();
                    }
                    featureWriters.remove(typename);
                }
            }
        }
        return feature;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing ActionDataStore Writer");
        closeConnections();
        if (dataStore2Write!=null)
            dataStore2Write.dispose();
    }

    @Override
    public void processPostCollectionActions() {
        log.info("Process Post actions ActionDataStoreWriter");
        for (int i = 0; i < collectionActions.size(); i++) {
            CollectionAction ca = collectionActions.get(i);
            //do the polygonize function
            if (ca instanceof CollectionAction_Polygonize) {
                for (int s = 0; s < featureTypeNames.size(); s++) {
                    try {
                        Class geometryTypeBinding = dataStore2Write.getSchema(featureTypeNames.get(s)).getGeometryDescriptor().getType().getBinding();
                        if (LineString.class == geometryTypeBinding || MultiLineString.class == geometryTypeBinding) {
                            FeatureSource fs = dataStore2Write.getFeatureSource(featureTypeNames.get(s));
                            FeatureCollection fc = fs.getFeatures();
                            ca.execute(fc,this);
                        }
                    } catch (Exception e) {
                        log.error("Error while Polygonizing the lines.", e);
                    }
                }
            }
            if(ca instanceof CollectionAction_PolygonizeWithAttr){
                DataStore ds=null;
                try{
                    CollectionAction_PolygonizeWithAttr cap = (CollectionAction_PolygonizeWithAttr) ca;
                    FeatureSource fs = dataStore2Write.getFeatureSource(cap.getAttributeFeatureName());
                    FeatureCollection fc = fs.getFeatures();
                    ds= DataStoreLinker.openDataStore(this.params);
                    cap.setDataStore2Write(ds);
                    cap.execute(fc,this);
                } catch (Exception e) {
                    log.error("Error while Polygonizing the lines with attributes.", e);
                }finally{
                    if (ds!=null){
                        ds.dispose();
                    }
                }
            }
        }
    }

    private void write(FeatureWriter writer, SimpleFeature feature) throws IOException {
        // Write to datastore
        SimpleFeature newFeature = (SimpleFeature) writer.next();

        try {
            newFeature.setAttributes(feature.getAttributes());
        } catch (IllegalAttributeException writeProblem) {
            throw new IllegalAttributeException("Could not create " + feature.getFeatureType().getTypeName() + " out of provided SimpleFeature: " + feature.getID() + "\n" + writeProblem);
        }
        writer.write();
    }

    private void closeConnections() throws Exception {
        // Commit datastore
        // close connection
        Iterator iter = featureWriters.keySet().iterator();
        while (iter.hasNext()) {
            FeatureWriter writer = featureWriters.get((String) iter.next());
            writer.close();
        }
        featureWriters.clear();
    }
    /**
     * Check the schema and return the name.
     */
    private String checkSchema(SimpleFeatureType featureType) throws Exception {
        if (initDone) {
            //boolean typeExists = false;
            //String[] typeNamesFound = dataStore2Write.getTypeNames();
            String typename2Write = featureType.getTypeName();
            boolean typeExists = Arrays.asList(dataStore2Write.getTypeNames()).contains(typename2Write);

            /*
            for (int i = 0; i < typeNamesFound.length; i++) {
            if (typename2Write.equals(typeNamesFound[i])) {
                    typeExists=true;
            break;
                }
            }
             */

            if (dropFirst && typeExists) {
                /*The drop schema bestaat nog niet in geotools. Wordt nu wel gemaakt en zal binnen
                 * kort beschikbaar zijn. Tot die tijd maar verwijderen dmv sql script....
                */

                // Check if DataStore is a Database
                if (dataStore2Write instanceof JDBCDataStore) {
                    log.debug("It's a JDBCDatastore so try to drop the table with sql");
                    // Drop table
                    JDBCDataStore database = null;
                    Connection con = null;
                    try{
                        database = (JDBCDataStore) dataStore2Write;
                        con = database.getDataSource().getConnection();
                        con.setAutoCommit(true);

                        // TODO make this function work with all databases
                        PreparedStatement ps=null;
                        if (database.getSQLDialect() instanceof PostGISDialect){
                            ps= con.prepareStatement("DROP TABLE \"" + database.getDatabaseSchema()+"\".\""+typename2Write + "\"; "
                                    + "DELETE FROM \"geometry_columns\" WHERE f_table_name = '" + featureType.getTypeName() + "'");
                            ps.execute();
                        } else if(database.getSQLDialect() instanceof OracleDialect){
                            ps= con.prepareStatement("DROP TABLE \"" + typename2Write + "\"");
                            ps.execute();
                            ps = con.prepareStatement("DELETE FROM MDSYS.SDO_GEOM_METADATA_TABLE WHERE SDO_TABLE_NAME = '"+featureType.getTypeName()+"'");
                            ps.execute();
                        }
                    }finally{
                        if (database != null)
                            database.closeSafe(con);
                    }
                }
                typeExists = false;
            }


            // If table does not exist, create new
            if (!typeExists) {
                dataStore2Write.createSchema(featureType);
                log.info("Creating new table with name: " + featureType.getTypeName());

            } else if (!append) {
                removeAllFeatures(dataStore2Write, typename2Write);
                // Check if DataStore is a Database
                /*if (dataStore2Write instanceof JDBCDataStore) {
                    // Empty table
                    JDBCDataStore database = (JDBCDataStore) dataStore2Write;
                    Connection con = database.getConnection(Transaction.AUTO_COMMIT);
                    con.setAutoCommit(true);

                    // TODO make this function work with all databases
                    PreparedStatement ps = con.prepareStatement("TRUNCATE TABLE \"" + typename2Write + "\"");
                    ps.execute();

                    con.close();
                }*/
            }
            return typename2Write;
        }
        return null;
    }

    private EasyFeature fixFeatureTypeName(EasyFeature feature) throws Exception {
        String typename = fixTypename(feature.getTypeName().replaceAll(" ", "_"));
        feature.setTypeName(typename);

        return feature;
    }

    public Map getParams() {
        return params;
    }

    public String toString() {
        return "Write to datastore: " + params.toString();
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

    private String correctTypeName(String typeName, DataStore dataStore2Write) throws IOException {
        String[] typeNames= dataStore2Write.getTypeNames();
        for (int i=0; i < typeNames.length; i++){
            if (typeNames[i].equalsIgnoreCase(typeName)){
                return typeNames[i];
            }
        }
        return typeName;
    }
    private void removeAllFeatures(DataStore datastore, String typeName) throws IOException, Exception{
        DefaultTransaction transaction = new DefaultTransaction("removeTransaction");
        FeatureStore<SimpleFeatureType, SimpleFeature> store = (FeatureStore<SimpleFeatureType, SimpleFeature>) datastore.getFeatureSource( typeName );

        store.removeFeatures(Filter.INCLUDE);
        try{
            transaction.commit();
        }catch(Exception e){
            transaction.rollback();
            throw e;
        }finally{
            transaction.close();
        }
    }
}
