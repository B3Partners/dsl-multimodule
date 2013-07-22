package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import nl.b3p.geotools.data.linker.FeatureException;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.oracle.OracleDialect;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCFeatureStore;
import org.geotools.jdbc.PrimaryKey;
import org.geotools.jdbc.PrimaryKeyColumn;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
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
    private final boolean polygonize;
    private final boolean polygonizeWithAttr;
    private final boolean polygonizeSufLki;
    private final boolean postPointWithinPolygon;
    private Exception constructorEx;
    private HashMap<String, FeatureWriter> featureWriters = new HashMap();
    private HashMap<String, PrimaryKey> featurePKs = new HashMap();
    private HashMap<String, String> checked = new HashMap();
    private ArrayList<String> featureTypeNames = new ArrayList();
    private static final int MAX_CONNECTIONS_NR = 50;
    private static final String MAX_CONNECTIONS = "max connections";
    private static int processedTypes = 0;
    private ArrayList<CollectionAction> collectionActions = new ArrayList();

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
        }
        if (!params.containsKey(MAX_CONNECTIONS)) {
            params.put(MAX_CONNECTIONS, MAX_CONNECTIONS_NR);
        }

        if (ActionFactory.propertyCheck(properties, "postPointWithinPolygon")) {
            postPointWithinPolygon = (Boolean) properties.get("postPointWithinPolygon");
        } else {
            postPointWithinPolygon = false;
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
        }

        feature = fixFeatureTypeName(feature);
        String typename = feature.getFeatureType().getTypeName();
        //get the correct typename from the datastore
        String newTypeName = correctTypeName(typename, dataStore2Write);
        //if not the same (case sensitive) then change the typename
        if (!newTypeName.equals(typename)) {
            feature.setTypeName(newTypeName);
            typename = newTypeName;
        }

        FeatureWriter writer = null;
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
                typename = correctTypeName(typename, dataStore2Write);
            }

            writer = dataStore2Write.getFeatureWriterAppend(typename, Transaction.AUTO_COMMIT);
            featureWriters.put(typename, writer);
        }

        PrimaryKey pks = null;
        if (featurePKs.containsKey(typename)) {
            pks = featurePKs.get(typename);
        } else if (dataStore2Write != null && (dataStore2Write instanceof JDBCDataStore)) {
            JDBCFeatureStore fs = (JDBCFeatureStore) ((JDBCDataStore) dataStore2Write).getFeatureSource(typename);
            pks = fs.getPrimaryKey();
            featurePKs.put(typename, pks);
        }

        //store the typename
        if (!featureTypeNames.contains(typename)) {
            featureTypeNames.add(typename);
        }

        try {
            write(writer, pks, feature.getFeature());
        } catch (Exception ex) {
            //log.debug("Error getting geometry. Feature not written: "+feature.toString(), ex);
            // moeten dit soort dingen niet gewoon in een finally block?!?
            //Remove writer so a new writer is created when the next feature is processed
            if (writer != null) {
                writer.close();
            }
            featureWriters.remove(typename);
            featurePKs.remove(typename);

            throw new FeatureException("Error getting geometry. Feature not written.", ex);
        }

        return feature;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing ActionDataStore Writer");
        closeConnections();
        if (dataStore2Write != null) {
            dataStore2Write.dispose();
        }
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
                        log.error("Error while Polygonizing the lines.", e);
                    }
                }
            }
            if (ca instanceof CollectionAction_Point_Within_Polygon) {
                DataStore ds = null;
                try {
                    CollectionAction_Point_Within_Polygon cap = (CollectionAction_Point_Within_Polygon) ca;

                    FeatureSource fs = dataStore2Write.getFeatureSource(cap.getPointsTable());
                    FeatureCollection fc = fs.getFeatures();

                    FeatureSource fs2 = dataStore2Write.getFeatureSource(cap.getPolygonTable());
                    FeatureCollection fc2 = fs2.getFeatures();

                    ds = DataStoreLinker.openDataStore(this.params);
                    cap.setDataStore2Write(ds);

                    cap.execute(fc, fc2, this);
                } catch (Exception e) {
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
                    log.error("Error while Polygonizing the lines with attributes.", e);
                } finally {
                    if (ds != null) {
                        ds.dispose();
                    }
                }
            }
        }
    }

    private void write(FeatureWriter writer, PrimaryKey pk, SimpleFeature feature) throws IOException {

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
            if(oldfid != null){
                oldfid.setLength(oldfid.length() - 1);
            }
        }

        SimpleFeature newFeature = (SimpleFeature) writer.next();

        if (oldfid!=null && oldfid.length() > 0
                && newFeature.getClass().getName()
                .equals("org.geotools.jdbc.JDBCFeatureReader$ResultSetFeature")) {
            // feature heeft automatisch gegenereerde fid, pas truc toe om te
            // overriden. Bug met HINTS.USE_PROVIDED_FID zit ook nog in Geotools 9.3
            // TODO: kan mogelijk beter door andere systematiek via addAttributes op store
            try {
                Method m = newFeature.getClass().getMethod("setID", String.class);
                m.setAccessible(true);
                m.invoke(newFeature, oldfid.toString());
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException("Cannot set FID", e);
                }
            }
            newFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
        }
 
        try {
            /* Ingebouwd dat bij een append alleen gelijke kolomnamen
             * worden toegevoegd. Je hoeft bij een append dus niet meer van te voren te
             * zorgen dat bron en doel exact gelijke kolommen bevatten.
             * 
             * CvL: altijd alleen geldig kolommen overzetten, ongeldige kolommen geven
             * altijd problemen, dus waarom dan toch proberen.
             * 
             * TODO: Test zonder target tabel!
             */
//            if (append && !dropFirst) {
            List<AttributeDescriptor> targets = newFeature.getFeatureType()
                    .getAttributeDescriptors();

            for (AttributeDescriptor descr : targets) {
                Name name = descr.getName();
                AttributeDescriptor tmp = feature.getFeatureType().getDescriptor(name);

                if (tmp != null) {
                    newFeature.setAttribute(name, feature.getAttribute(name));
                }
            }

//            } else {
//                newFeature.setAttributes(feature.getAttributes());
//            }

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
                    log.info("It's a JDBCDatastore so try to drop the table with sql");
                    // Drop table
                    JDBCDataStore database = null;
                    Connection con = null;
                    try {
                        database = (JDBCDataStore) dataStore2Write;
                        con = database.getDataSource().getConnection();
                        con.setAutoCommit(true);

                        // TODO make this function work with all databases
                        PreparedStatement ps = null;
                        if (database.getSQLDialect() instanceof PostGISDialect) {
                            ps = con.prepareStatement("DROP TABLE \"" + database.getDatabaseSchema() + "\".\"" + typename2Write + "\"; "
                                    + "DELETE FROM \"geometry_columns\" WHERE f_table_name = '" + featureType.getTypeName() + "'");
                            ps.execute();
                        } else if (database.getSQLDialect() instanceof OracleDialect) {
                            ps = con.prepareStatement("DROP TABLE \"" + database.getDatabaseSchema() + "\".\"" + typename2Write + "\"");
                            ps.execute();
                            ps = con.prepareStatement("DELETE FROM MDSYS.SDO_GEOM_METADATA_TABLE WHERE SDO_TABLE_NAME = '" + featureType.getTypeName() + "'");
                            ps.execute();
                        }
                    } finally {
                        if (database != null) {
                            database.closeSafe(con);
                        }
                    }
                }
                typeExists = false;
            }


            // If table does not exist, create new
            if (!typeExists) {
                log.info("Creating new table with name: " + featureType.getTypeName());
                dataStore2Write.createSchema(featureType);

            } else if (!append) {
                log.info("Removing all features from: " + typename2Write);
				boolean deleteSuccess = false;
                // Check if DataStore is a Database
                if (dataStore2Write instanceof JDBCDataStore) {
                    // Empty table
                    JDBCDataStore database = (JDBCDataStore) dataStore2Write;
					// try truncate
					try {
						Connection con = database.getConnection(Transaction.AUTO_COMMIT);
                        PreparedStatement ps = null;
                        if (database.getSQLDialect() instanceof PostGISDialect) {
							ps = con.prepareStatement("TRUNCATE TABLE \"" + typename2Write + "\" CASCADE");
                        } else { //if (database.getSQLDialect() instanceof OracleDialect) {
                            ps = con.prepareStatement("TRUNCATE TABLE \"" + typename2Write + "\"");
                        }
						ps.execute();
						deleteSuccess = true;
		                log.info("Removing using truncate");
					} catch (Exception e) {
						log.debug("Removing using truncate failed: ", e);
 						try {
							Connection con1 = database.getConnection(Transaction.AUTO_COMMIT);
							PreparedStatement ps = con1.prepareStatement("DELETE FROM \"" + typename2Write + "\"");
							ps.execute();
							deleteSuccess = true;
							log.info("Removing using delete from table");
						} catch (Exception e) {
							log.debug("Removing using delete from table failed: ", e);
						} finally {
							if (con1!=null) {
								con1.close();
							}
						}
					} finally {
					if (con!=null ) {
							con.close();
						}
					}
                } 
				if	(!deleteSuccess) {
                    removeAllFeatures(dataStore2Write, typename2Write);
		            log.info("Removing using geotools");
                }
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
        String[] typeNames = dataStore2Write.getTypeNames();
        for (int i = 0; i < typeNames.length; i++) {
            if (typeNames[i].equalsIgnoreCase(typeName)) {
                return typeNames[i];
            }
        }
        return typeName;
    }

    private void removeAllFeatures(DataStore datastore, String typeName) throws IOException, Exception {
        DefaultTransaction transaction = new DefaultTransaction("removeTransaction");
        FeatureStore<SimpleFeatureType, SimpleFeature> store = (FeatureStore<SimpleFeatureType, SimpleFeature>) datastore.getFeatureSource(typeName);

        /* TODO: Moet deze niet bin nen try ? Anders transaction nooit geclosed ? */        
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
}
