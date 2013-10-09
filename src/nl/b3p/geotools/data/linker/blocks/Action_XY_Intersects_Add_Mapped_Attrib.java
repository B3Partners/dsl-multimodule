package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.Filter;
import org.geotools.filter.text.cql2.CQL;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;

/**
 *
 * @author Boy de Wit
 */
public class Action_XY_Intersects_Add_Mapped_Attrib extends Action {

    protected static final Log logger = LogFactory.getLog(Action_XY_Intersects_Add_Mapped_Attrib.class);
    private Long outputDatabaseId = null;
    private String outputGeomColumn = null;
    private String polyTableName = null;
    private String matchSourceColumn = null;
    private String matchPolyTableColumn = null;
    private Boolean matchGeom = null;
    private static DataStore ds = null;
    private static FeatureSource outputFs = null;
    private static final String SRS = "EPSG:28992";
    private static final String KEY_LAST_FEATURE = "lastFeature";

    public Action_XY_Intersects_Add_Mapped_Attrib(
            Long outputDatabaseId, String outputGeomColumn,
            String polyTableName, String matchSourceColumn,
            String matchPolyTableColumn, Boolean matchGeom) {

        this.outputDatabaseId = outputDatabaseId;
        this.outputGeomColumn = outputGeomColumn;
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

            String dbType = db.getType().toString().toLowerCase();

            params.put("dbtype", dbType);
            params.put("host", db.getHost());
            params.put("validate connections", false);
            params.put("user", db.getUsername());
            params.put("database", db.getDatabaseName());
        } else { // normale uitvoer gebruiken
        }

        FeatureSource fs = null;
        try {
            ds = DataStoreLinker.openDataStore(params);
            outputFs = ds.getFeatureSource(polyTableName);
        } catch (Exception ex) {
            logger.error("Fout tijdens openen Datastore.", ex);
        }
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        Boolean lastFeature = false;
        Map currentUserData = feature.getFeature().getUserData();

        if (currentUserData != null && currentUserData.containsKey(KEY_LAST_FEATURE)) {
            lastFeature = true;
        }

        /* Make sure you are using The create geometry from 
         * values Action Block. */
        Point point = null;
        Geometry geometry = (Geometry) feature.getFeature().getDefaultGeometry();
        if (geometry != null && geometry instanceof Point) {
            point = (Point) geometry;
        } else if (geometry != null) {
            point = geometry.getCentroid();
        }

        /* Make sure to replace all source values if you want them
         to map against the target column values */
        String sourceValue = null;
        String polyValue = null;

        if (matchSourceColumn != null) {
            SimpleFeature sourceF = feature.getFeature();

            if (sourceF.getAttribute(matchSourceColumn) instanceof String) {
                sourceValue = (String) sourceF.getAttribute(matchSourceColumn);
            }
        }

        /* Query op vlakken tabel */
        EasyFeature newFeature = null;
        if (outputFs != null) {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

            /* TODO: Check if Filters are supported by service or 
             * database. */

            String geomColumn = null;
            if (outputGeomColumn == null || outputGeomColumn.equals("")) {
                geomColumn = outputFs.getSchema().getGeometryDescriptor().getLocalName();
            } else {
                geomColumn = outputGeomColumn;
            }

            Filter doIntersects = (Filter) ff.intersects(ff.property(geomColumn), ff.literal(point));
            Filter doAttrib = (Filter) CQL.toFilter(matchPolyTableColumn + " = '" + sourceValue + "'");
            Filter doBoth = (Filter) ff.and(doIntersects, doAttrib);

            FeatureCollection vlakken = null;
            if (!matchGeom && matchSourceColumn == null || matchPolyTableColumn == null) {
                vlakken = outputFs.getFeatures();
            } else if (matchGeom && matchSourceColumn == null || matchPolyTableColumn == null) {
                vlakken = outputFs.getFeatures(doIntersects);
            } else {
                vlakken = outputFs.getFeatures(doBoth);
            }

            if (vlakken == null || vlakken.size() < 1 && !lastFeature) {
                return null;
            }

            if (vlakken != null && vlakken.size() > 0) {
                FeatureIterator it = vlakken.features();
                while (it.hasNext()) {
                    SimpleFeature polyF = (SimpleFeature) it.next();

                    if (polyF != null) {
                        newFeature = buildNewFeature(feature.getFeature(), polyF);
                        break;
                    }
                }
                vlakken.close(it);
            }

            /* TODO: Currently the batched processing assumes
             * the ActionBlock always returns a feature. This Block and
             * the new Filter block wont always return a feature.
             * 
             * Still when the last input feature passes this Block the
             * Writer still needs to write the remaining batched collection.
             */
            if (lastFeature && newFeature == null) {
                return feature;
            } else if (lastFeature && newFeature != null) {
                Map userData = newFeature.getFeature().getUserData();
                userData.put(KEY_LAST_FEATURE, Boolean.TRUE);
            }
        }

        return newFeature;
    }

    private EasyFeature buildNewFeature(SimpleFeature sourceF, SimpleFeature polyF) {
        SimpleFeatureType sourceFt = (SimpleFeatureType) sourceF.getFeatureType();
        SimpleFeatureType polyFt = (SimpleFeatureType) polyF.getFeatureType();

        List<AttributeDescriptor> sourceAttrDescr = new ArrayList<AttributeDescriptor>(sourceFt.getAttributeDescriptors());

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        String currentGeomColumn = sourceFt.getGeometryDescriptor().getName().getLocalPart();

        List<AttributeDescriptor> newAttrDescr = new ArrayList();
        for (AttributeDescriptor pDescr : sourceAttrDescr) {
            if (pDescr.getLocalName().equals(currentGeomColumn)) {
                continue;
            }

            if (!newAttrDescr.contains(pDescr)) {
                newAttrDescr.add(pDescr);
            }
        }

        newAttrDescr.add(polyFt.getGeometryDescriptor());

        Integer geometryColumnIndex = newAttrDescr.size() - 1;

        /* Create new table based on polygon and extra columns */
        SimpleFeatureType newFt = createNewFeatureType(
                sourceFt,
                polyFt,
                geometryColumnIndex,
                Polygon.class,
                newAttrDescr);

        return createFeature(newFt, sourceF, polyF, geometryColumnIndex);
    }

    private SimpleFeatureType createNewFeatureType(
            SimpleFeatureType sourceFt,
            SimpleFeatureType polyFt,
            Integer geometryColumnIndex,
            Class binding,
            List<AttributeDescriptor> newAttrDescr) {

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setName(sourceFt.getGeometryDescriptor().getName().getLocalPart());

        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setAttributes(newAttrDescr);
        featureTypeBuilder.setSRS(SRS);
        featureTypeBuilder.setName(sourceFt.getTypeName());

        return featureTypeBuilder.buildFeatureType();
    }

    private EasyFeature createFeature(SimpleFeatureType newFt,
            SimpleFeature sourceF, SimpleFeature polyF, Integer geometryColumnIndex) {

        Geometry geom = (Geometry) polyF.getDefaultGeometry();

        List<Object> srcAttrib = sourceF.getAttributes();
        srcAttrib.set(geometryColumnIndex, geom);

        return new EasyFeature(SimpleFeatureBuilder.build(newFt, srcAttrib, "" + sourceF.getID()));
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
}
