/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Iterator;
import org.geotools.feature.*;
import org.geotools.data.*;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import org.geotools.data.jdbc.JDBCDataStore;

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
    private Exception constructorEx;
    private HashMap<String, FeatureWriter> featureWriters = new HashMap();
    private HashMap<String, String> checked = new HashMap();
    private static final int MAX_CONNECTIONS = 50;
    private static int processedTypes = 0;

    public ActionDataStore_Writer(Map params, boolean append, boolean dropFirst) {
        this.params = params;
        this.append = append;
        this.dropFirst = dropFirst;

        params.put("max connections", MAX_CONNECTIONS);
        try {
            dataStore2Write = DataStoreLinker.openDataStore(params);
            initDone = (dataStore2Write != null);

        } catch (Exception ex) {
            constructorEx = ex;
        }
    }

    public ActionDataStore_Writer(Map params) {
        this.params = params;
        this.append = false;
        this.dropFirst = true;

        params.put("max connections", MAX_CONNECTIONS);
        try {
            dataStore2Write = DataStoreLinker.openDataStore(params);
            initDone = (dataStore2Write != null);

        } catch (Exception ex) {
            constructorEx = ex;
        }
    }

    public Feature execute(Feature feature) throws Exception {
        if (!initDone) {
            throw new Exception("\nOpening dataStore failed; datastore could not be found, missing library or no access to file.\nUsed parameters:\n" + params.toString() + "\n\n" + constructorEx.getLocalizedMessage());

        } else {
            feature = fixFeatureTypeName(feature);
            String typename = feature.getFeatureType().getTypeName();

            FeatureWriter writer;
            if (featureWriters.containsKey(typename)) {
                writer = featureWriters.get(typename);

            } else {

                if (featureWriters.size() + 1 == MAX_CONNECTIONS) {
                    // If max connections reached, commit all data and continue
                    close();
                    dataStore2Write = DataStoreLinker.openDataStore(params);
                    log.warn("Closing all connections (too many featureWriters loaded)");
                }

                if (!checked.containsKey(params.toString() + typename)) {
                    checkSchema(feature.getFeatureType());
                    checked.put(params.toString() + typename, "");
                    processedTypes++;
                }

                writer = dataStore2Write.getFeatureWriterAppend(feature.getFeatureType().getTypeName(), Transaction.AUTO_COMMIT);
                featureWriters.put(typename, writer);
            }

            boolean isOracle = params.get("dbtype").toString().equals("oracle");

            // Check if able to add
            if (!writer.getFeatureType().equals(feature.getFeatureType()) && !isOracle) {
                throw new Exception("FeatureType of feature is not equal to featureType in Database");

            } else {
                // Write to datastore
                SimpleFeature newFeature = (SimpleFeature) writer.next();

                try {
                    newFeature.setAttributes(feature.getAttributes(null));
                } catch (IllegalAttributeException writeProblem) {
                    throw new IllegalAttributeException("Could not create " + typename + " out of provided feature: " + feature.getID() + "\n" + writeProblem);
                }

                writer.write();
            }
        }
        return feature;
    }

    @Override
    public void close() throws Exception {
        closeConnections();
        dataStore2Write.dispose();
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

    private void checkSchema(FeatureType featureType) throws Exception {
        if (initDone) {
            boolean typeExists = false;
            String[] typeNamesFound = dataStore2Write.getTypeNames();
            String typename2Write = featureType.getTypeName();

            for (int i = 0; i < typeNamesFound.length; i++) {
                if (typename2Write.equals(typeNamesFound[i])) {
                    typeExists = true;
                    break;
                }
            }

            if (dropFirst && typeExists) {
                // Check if DataStore is a Database
                if (dataStore2Write instanceof JDBCDataStore) {
                    // Drop table
                    JDBCDataStore database = (JDBCDataStore) dataStore2Write;
                    Connection con = database.getConnection(Transaction.AUTO_COMMIT);
                    con.setAutoCommit(true);

                    // TODO make this function work with all databases
                    PreparedStatement ps = con.prepareStatement("DROP TABLE \"" + featureType.getTypeName() + "\"; DELETE FROM \"geometry_columns\" WHERE f_table_name = '" + featureType.getTypeName() + "'");
                    ps.execute();

                    con.close();
                }
                typeExists = false;
            }


            // If table does not exist, create new
            if (!typeExists) {
                dataStore2Write.createSchema(featureType);
                log.info("Creating new table with name: " + featureType.getTypeName());

            } else if (!append) {

                // Check if DataStore is a Database
                if (dataStore2Write instanceof JDBCDataStore) {
                    // Empty table
                    JDBCDataStore database = (JDBCDataStore) dataStore2Write;
                    Connection con = database.getConnection(Transaction.AUTO_COMMIT);
                    con.setAutoCommit(true);

                    // TODO make this function work with all databases
                    PreparedStatement ps = con.prepareStatement("TRUNCATE TABLE \"" + featureType.getTypeName() + "\"");
                    ps.execute();

                    con.close();
                }
            }
        }
    }

    private Feature fixFeatureTypeName(Feature feature) throws Exception {
        String typename = fixTypename(feature.getFeatureType().getTypeName().replaceAll(" ", "_"));

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(typename);
        ftb.importType(feature.getFeatureType());
        ftb.setName(typename);

        return ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());
    }

    public Map getParams() {
        return params;
    }

    public String toString() {
        return "Write to datastore: " + params.toString();
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.PARAMS,
                        ActionFactory.APPEND,
                        ActionFactory.DROPFIRST
                    }, new String[]{
                        ActionFactory.PARAMS
                    },
                    // Managed by ActionFactory:
                    new String[]{
                        ActionFactory.PARAMS,
                        ActionFactory.APPEND,}, new String[]{
                        ActionFactory.PARAMS,
                        ActionFactory.DROPFIRST
                    }
                };
    }

    public String getDescription_NL() {
        return "Schrijf de feature weg naar een datastore. Als de datastore een database is, kan de feature worden toegevoegd of kan de tabel worden geleegd voor het toevoegen";
    }
}
