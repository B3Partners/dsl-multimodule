package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.persistence.EntityManager;
import nl.b3p.commons.jpa.JpaUtilServlet;
import nl.b3p.datastorelinker.entity.Database;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.Capabilities;
import org.geotools.filter.Filter;
import org.geotools.filter.text.cql2.CQL;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.Intersects;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;
import org.geotools.feature.DefaultFeatureCollection;


/**
 *
 * @author Boy de Wit
 */
public class Action_XY_Intersects_Add_Mapped_Attrib extends Action {

    protected static final Log logger = LogFactory.getLog(Action_XY_Intersects_Add_Mapped_Attrib.class);
    private String vlakkenGeomColumn = null;
    private String sourceMatchColumn = null;
    private String vlakkenMatchColumn = null;
    private static DataStore ds = null;
    private static FeatureSource vlakkenFs = null;

    public Action_XY_Intersects_Add_Mapped_Attrib(
            Long vlakkenDatabaseId, String vlakkenGeomColumn,
            String vlakkenTableName, String sourceMatchColumn,
            String vlakkenMatchColumn, Boolean matchGeom) {

        this.vlakkenGeomColumn = vlakkenGeomColumn;
        this.sourceMatchColumn = sourceMatchColumn;
        this.vlakkenMatchColumn = vlakkenMatchColumn;

        Database db = null;
        if (vlakkenDatabaseId != null && vlakkenDatabaseId > 0) {
            EntityManager em = JpaUtilServlet.getThreadEntityManager();
            Session session = (Session) em.getDelegate();

            db = (Database) session.get(Database.class, vlakkenDatabaseId);
        }

        Map params = new HashMap();

        if (db != null) {
            params.put("schema", db.getSchema());
            params.put("port", db.getPort());
            params.put("passwd", db.getPassword());

            String dbType = db.getType().toString().toLowerCase();

            params.put("dbtype", dbType);
            params.put("host", db.getHost());
            params.put("validate connections", false);
            params.put("user", db.getUsername());
            params.put("database", db.getDatabaseName());
        } else { // normale uitvoer gebruiken
        }

        try {
            ds = DataStoreLinker.openDataStore(params);
            vlakkenFs = ds.getFeatureSource(vlakkenTableName);
        } catch (Exception ex) {
            logger.error("Fout tijdens openen Datastore.", ex);
        }
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

        if (vlakkenFs == null) {
            throw new Exception("No second source to combine found");
        }

        /* Make sure you are using The create geometry from 
         * values Action Block. */
        Point point = null;
        Geometry geometry = (Geometry) feature.getFeature().getDefaultGeometry();
        if (geometry != null && geometry instanceof Point) {
            point = (Point) geometry;
        } else if (geometry != null) {
            point = geometry.getCentroid();
        } else {
            throw new Exception("No default geometry found in source");
        }

        /* Make sure to replace all source values if you want them
         to map against the target column values */
        Object sourceMatchValue = null;
        SimpleFeature sourceF = feature.getFeature();
        if (sourceMatchColumn != null && !sourceMatchColumn.isEmpty()) {
            sourceMatchValue = sourceF.getAttribute(sourceMatchColumn);
        }

        /* Query op vlakken tabel */
        String localVlakkenGeomColumn = null;
        if (vlakkenGeomColumn == null || vlakkenGeomColumn.equals("")) {
            localVlakkenGeomColumn = vlakkenFs.getSchema().getGeometryDescriptor().getLocalName();
        } else {
            localVlakkenGeomColumn = vlakkenGeomColumn;
        }

        Capabilities capabilities = new Capabilities();
        capabilities.addType(Intersects.class);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Filter doIntersects = null;
        Filter doAttrib = null;
        Filter doBoth = null;
        if (localVlakkenGeomColumn != null && !localVlakkenGeomColumn.isEmpty()) {
            doIntersects = (Filter) ff.intersects(ff.property(localVlakkenGeomColumn), ff.literal(point));
            // is dit geen onzin, omdat deze capability net is toegevoegd?
            if (!capabilities.supports(doIntersects)) {
                doIntersects = null;
            }
        }
        if (vlakkenMatchColumn != null && !vlakkenMatchColumn.isEmpty()
                && sourceMatchValue != null && !sourceMatchValue.toString().isEmpty()) {
            // temp fix voor leeg laten van invulvelden
            if (!vlakkenMatchColumn.equals("*") && !sourceMatchColumn.equals("*")) {
                doAttrib = (Filter) CQL.toFilter(vlakkenMatchColumn + " = '" + sourceMatchValue + "'");
            }
        }
        if (doIntersects != null && doAttrib != null) {
            doBoth = (Filter) ff.and(doIntersects, doAttrib);
        }

        DefaultFeatureCollection vlakkenBronCollectie = null;
        if (doBoth != null) {
            vlakkenBronCollectie = (DefaultFeatureCollection)vlakkenFs.getFeatures(doBoth);
        } else if (doAttrib != null) {
            vlakkenBronCollectie = (DefaultFeatureCollection)vlakkenFs.getFeatures(doAttrib);
        } else if (doIntersects != null) {
            vlakkenBronCollectie = (DefaultFeatureCollection)vlakkenFs.getFeatures(doIntersects);
        } else {
            throw new Exception("No matching strategy found");
        }

        if (vlakkenBronCollectie == null || vlakkenBronCollectie.isEmpty()) {
            throw new Exception("Geometrie overlapt niet of geen match"
                    + " gevonden met de waarde in " + sourceMatchColumn);
        }
        //if (vlakkenBronCollectie.size()>1) {
        //	throw new Exception("Meer dan 1 geometrie met match"
        //				+ " gevonden met de waarde in " + sourceMatchColumn;
        //}

        FeatureIterator it = null;
        try {
            it = vlakkenBronCollectie.features();
            if (it.hasNext()) {
                SimpleFeature vlakkenF = (SimpleFeature) it.next();
                if (vlakkenF == null) {
                    throw new Exception("No suitable polygon found!");
                }

                // zet geom van vlakkenF in sourceF, repareer geom type
                String geometryName = feature.getFeature()
                        .getDefaultGeometryProperty().getDescriptor()
                        .getLocalName();
                int dId = feature.getAttributeDescriptorIDbyName(geometryName);
                GeometryDescriptor gd = vlakkenF.getDefaultGeometryProperty().getDescriptor();
                 AttributeDescriptor gad = EasyFeature
                        .buildGeometryAttributeDescriptor(geometryName, 
                        gd.getType().getBinding(), gd.isNillable(), 
                        gd.getType().getCoordinateReferenceSystem());
                feature.setAttributeDescriptor(dId, gad, false);
                
                feature.setAttribute(geometryName, vlakkenF.getAttribute(localVlakkenGeomColumn));
                feature.repairGeometry();

                // loop alle attributen van vlakkenF na of de naam voorkomt in
                // sourceF, zo ja check of waarde in sourceF is leeg, weer zo ja
                // zet de waarde van vlakkenF over naar sourceF
                List<AttributeDescriptor> ads = vlakkenF.getFeatureType()
                        .getAttributeDescriptors();
                for (AttributeDescriptor ad : ads) {
                    String aln = ad.getLocalName();
                    if (aln.equalsIgnoreCase(localVlakkenGeomColumn)) {
                        // geometry column is al gekopieerd
                        continue;
                    }
                    AttributeDescriptor sad = sourceF.getFeatureType().getDescriptor(aln);
                    if (sad == null) {
                        // kolom bestaat niet, dus niet overnemen
                        continue;
                    }
                    Object o = sourceF.getAttribute(aln);
                    if (o != null && !o.toString().isEmpty()) {
                        // waarde aanwezig, dus niet overnemen
                        continue;
                    }
                    Object vo = vlakkenF.getAttribute(aln);
                    if (vo != null && !vo.toString().isEmpty()) {
                        // waarde aanwezig en nu overnemen
                        feature.setAttribute(aln, vo);
                    }
                }
            }
        } finally {
            vlakkenBronCollectie.close(it);
        }

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
                    ActionFactory.ATTRIBUTE_NAME_OUTPUT_GEOM_COLUMN,
                    ActionFactory.ATTRIBUTE_NAME_POLY_TABLE,
                    ActionFactory.ATTRIBUTE_NAME_MATCH_SRC_COLUMN,
                    ActionFactory.ATTRIBUTE_NAME_MATCH_POLY_COLUMN,
                    ActionFactory.ATTRIBUTE_NAME_MATCH_GEOM
                }));

        return constructors;
    }

    @Override
    public void close() throws Exception {
        if (ds != null) {
            ds.dispose();
        }
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
