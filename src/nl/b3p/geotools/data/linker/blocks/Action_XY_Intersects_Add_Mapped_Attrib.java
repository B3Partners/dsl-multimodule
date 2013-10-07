package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import nl.b3p.commons.jpa.JpaUtilServlet;
import nl.b3p.datastorelinker.entity.Database;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

/**
 *
 * @author Boy de Wit
 */
public class Action_XY_Intersects_Add_Mapped_Attrib extends Action {

    protected static final Log logger = LogFactory.getLog(Action_XY_Intersects_Add_Mapped_Attrib.class);
    private Long outputDatabaseId = null;
    private String outputTableName = null;
    private String polyTableName = null;
    private String matchSourceColumn = null;
    private String matchPolyTableColumn = null;
    private Boolean matchGeom = null;
    public final ActionDataStore_Writer outputDatastore;

    public Action_XY_Intersects_Add_Mapped_Attrib(
            Long outputDatabaseId, String outputTableName,
            String polyTableName, String matchSourceColumn,
            String matchPolyTableColumn, Boolean matchGeom) {

        this.outputDatabaseId = outputDatabaseId;
        this.outputTableName = outputTableName;
        this.polyTableName = polyTableName;
        this.matchSourceColumn = matchSourceColumn;
        this.matchPolyTableColumn = matchPolyTableColumn;
        this.matchGeom = matchGeom;

        Database db = null;
        if (outputDatabaseId != null && outputDatabaseId > 0) {
            EntityManager em = JpaUtilServlet.getThreadEntityManager();
            Session session = (Session) em.getDelegate();

            db = (Database) session.get(Database.class, outputDatabaseId);
        }

        Map params = new HashMap();
        Map properties = new HashMap();

        if (db != null) {
            params.put("schema", db.getSchema());
            params.put("port", db.getPort());
            params.put("passwd", db.getPassword());
            params.put("dbtype", db.getType());
            params.put("host", db.getHost());
            params.put("validate connections", false);
            params.put("user", db.getUsername());
            params.put("database", db.getDatabaseName());
            
        } else { // normale uitvoer gebruiken
            
        }

        properties.put("append", false);
        properties.put("drop", true);
        properties.put("params", params);

        outputDatastore = new ActionDataStore_Writer(params, properties);
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

        /* Maak Point via Geometry maak punt uit waarde blok */
        Point point = null;
        Geometry geometry = (Geometry) feature.getFeature().getDefaultGeometry();

        if (geometry != null && geometry instanceof Point) {
            point = (Point) geometry;
        } else if (geometry != null) {
            point = geometry.getCentroid();
        }

        /* Mapping OCODE > LEVELS via Kolom waarde vervangen Blocks */

        /* Opgeven welke velden van gm_koppel mee gekopieerd moeten worden
         * via Mapping block */

        /* Intersects query op vlakken tabel met filter voor OCODE > LAYER */

        /* Zo ja dan nieuw vlak feature maken met aantal attrib velden van gm_koppel
         KOPP_ID_ADMIN, MAPID, X, Y, OCODE */

        if (outputDatastore != null) {
        }

        /* Hier kunnen daarna views op gemaakt worden */

        return feature;
    }

    public String toString() {
        return "";
    }

    public String getDescription_NL() {
        return "Verrijken met attributen na intersects en mapping match..";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.ATTRIBUTE_NAME_OUTPUTDB_ID,
            ActionFactory.ATTRIBUTE_NAME_OUTPUT_TABLE,
            ActionFactory.ATTRIBUTE_NAME_POLY_TABLE,
            ActionFactory.ATTRIBUTE_NAME_MATCH_SRC_COLUMN,
            ActionFactory.ATTRIBUTE_NAME_MATCH_POLY_COLUMN,
            ActionFactory.ATTRIBUTE_NAME_MATCH_GEOM
        }));

        return constructors;
    }

    @Override
    public void close() throws Exception {
        if (outputDatastore != null) {
            outputDatastore.close();
        }
    }

    public Long getOutputDatabaseId() {
        return outputDatabaseId;
    }

    public void setOutputDatabaseId(Long outputDatabaseId) {
        this.outputDatabaseId = outputDatabaseId;
    }

    public String getOutputTableName() {
        return outputTableName;
    }

    public void setOutputTableName(String outputTableName) {
        this.outputTableName = outputTableName;
    }

    public String getPolyTableName() {
        return polyTableName;
    }

    public void setPolyTableName(String polyTableName) {
        this.polyTableName = polyTableName;
    }

    public String getMatchSourceColumn() {
        return matchSourceColumn;
    }

    public void setMatchSourceColumn(String matchSourceColumn) {
        this.matchSourceColumn = matchSourceColumn;
    }

    public String getMatchPolyTableColumn() {
        return matchPolyTableColumn;
    }

    public void setMatchPolyTableColumn(String matchPolyTableColumn) {
        this.matchPolyTableColumn = matchPolyTableColumn;
    }

    public Boolean getMatchGeom() {
        return matchGeom;
    }

    public void setMatchGeom(Boolean matchGeom) {
        this.matchGeom = matchGeom;
    }
}
