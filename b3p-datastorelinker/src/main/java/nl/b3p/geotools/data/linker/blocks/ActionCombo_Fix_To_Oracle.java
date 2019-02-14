/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import nl.b3p.geotools.data.linker.Status;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Transform SimpleFeature for use inside Oracle
 *
 * @author Gertjan
 */
public class ActionCombo_Fix_To_Oracle extends ActionCombo {


    // for Oracle <= 11 (and very possibly all future versions):
    protected final static int ORACLE_MAX_TABLE_NAME_LENGTH_VANILLA = 30;

    // the longest suffix used that we currently know of
    // (when creating a schema an index is created by Geotools using this suffix):
    protected final static int ORACLE_SPATIAL_MAX_LENGTH_SUFFIX = "_MV_THE_GEOM_IDX".length();

    protected final static int ORACLE_MAX_TABLE_NAME_LENGTH =
            ORACLE_MAX_TABLE_NAME_LENGTH_VANILLA - ORACLE_SPATIAL_MAX_LENGTH_SUFFIX;

    private Integer minx = null;
    private Integer miny = null;
    private Integer maxx = null;
    private Integer maxy = null;
    private String precision = null;

    public ActionCombo_Fix_To_Oracle(Integer minx, Integer miny, Integer maxx, Integer maxy, String precision) {
        this.minx = minx;
        this.miny = miny;
        this.maxx = maxx;
        this.maxy = maxy;
        this.precision = precision;

        // Typename to uppercase
        ActionFeatureType_Typename_Case actionTypenameCase =
                new ActionFeatureType_Typename_Case(true);
        actionList.add(actionTypenameCase);

        // If typename is longer than allowed 26 characters
        ActionCondition_FeatureType_Typename_Length actionTypenameLength =
                new ActionCondition_FeatureType_Typename_Length(ActionCondition.CompareType.GREATER, ORACLE_MAX_TABLE_NAME_LENGTH);
        actionList.add(actionTypenameLength);

        // Then trim typename
        ActionFeatureType_Typename_Substring actionTypenameSubstring =
                new ActionFeatureType_Typename_Substring(ORACLE_MAX_TABLE_NAME_LENGTH, true);
        actionTypenameLength.addActionToList(true, actionTypenameSubstring);
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                ActionFactory.METADATA_BBOX_MINX,
                ActionFactory.METADATA_BBOX_MINY,
                ActionFactory.METADATA_BBOX_MAXX,
                ActionFactory.METADATA_BBOX_MAXY,
                ActionFactory.METADATA_PRECISION
        }));
        return constructors;
    }


    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        for (int i = 0; i < feature.getAttributeCount(); i++) {
            Action action = new ActionFeatureType_AttributeName_Case(i, true);
            action.execute(feature);
        }

        return feature;
    }

    public String getDescription_NL() {
        return "In deze ActionCombo wordt de typename zo aangepast dat deze gebruikt kan worden als Oracle tabelnaam. Dit betekend dat de typenaam omgezet wordt naar hoofdlettes en dat de lengte word aangepast aan de maximale lengte " + ORACLE_MAX_TABLE_NAME_LENGTH + " (neem laatste deel van de typenaam)";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }

    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
        Map params = (Map) properties.get("params");
        DataStore ds = DataStoreLinker.openDataStore(params, false);
        if (ds instanceof JDBCDataStore) {
            JDBCDataStore jdbcDS = (JDBCDataStore) ds;
            Transaction t = new DefaultTransaction();
            try {
                String tableName = (String) properties.get("newFeatureTypeName");
                String geomColumnName = jdbcDS.getFeatureSource(tableName).getSchema().getGeometryDescriptor().getLocalName();

                String sql = "update USER_SDO_GEOM_METADATA set diminfo = MDSYS.SDO_DIM_ARRAY(\n";

                sql += "   MDSYS.SDO_DIM_ELEMENT('X', " + minx + ", " + maxx + ", " + precision + "),";
                sql += "   MDSYS.SDO_DIM_ELEMENT('Y', " + miny + ", " + maxy + ", " + precision + "))";
                sql += "   where column_name = '" + geomColumnName + "' and table_name = '" + tableName + "'";

                Connection connection = jdbcDS.getConnection(t);
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.executeUpdate();

                t.commit();
            } catch (Exception ex) {
                log.error("Error while creating index: ", ex);
                t.rollback();
            } finally {
                try {
                    t.close();
                } catch (IOException e) {
                    log.error("Error closing connection-transaction", e);
                }
            }
        }
    }

}
