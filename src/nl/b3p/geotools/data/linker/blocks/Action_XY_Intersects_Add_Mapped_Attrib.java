package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import nl.b3p.commons.jpa.JpaUtilServlet;
import nl.b3p.datastorelinker.entity.Database;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import static nl.b3p.geotools.data.linker.blocks.CollectionAction_Point_Within_Polygon.log;
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
import org.geotools.referencing.crs.DefaultGeographicCRS;
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
    //private String outputTableName = null;
    private String outputGeomColumn = null;
    private String polyTableName = null;
    private String matchSourceColumn = null;
    private String matchPolyTableColumn = null;
    private Boolean matchGeom = null;
    private static ActionDataStore_Writer outputDatastore = null;
    private static FeatureSource outputFs = null;

    public Action_XY_Intersects_Add_Mapped_Attrib(
            Long outputDatabaseId, String outputGeomColumn,
            String polyTableName, String matchSourceColumn,
            String matchPolyTableColumn, Boolean matchGeom) {

        this.outputDatabaseId = outputDatabaseId;
        //this.outputTableName = outputTableName;
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

        properties.put("append", false);
        properties.put("drop", true);
        properties.put("params", params);

        outputDatastore = new ActionDataStore_Writer(params, properties);

        FeatureSource fs = null;
        try {
            DataStore ds = DataStoreLinker.openDataStore(params);

            if (ds != null) {
                outputFs = ds.getFeatureSource(polyTableName);
            }
        } catch (Exception ex) {
            logger.error("Fout tijdens openen Datastore.", ex);
        }
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

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

            FeatureIterator it = vlakken.features();
            while (it.hasNext()) {
                SimpleFeature polyF = (SimpleFeature) it.next();

                if (polyF != null) {
                    feature = buildNewFeature(feature.getFeature(), polyF);
                    break;
                }
            }

            /* close it */
            if (vlakken != null && it != null) {
                vlakken.close(it);
            }
        }

        /* Opgeven welke velden van gm_koppel mee gekopieerd moeten worden
         * via Mapping block */

        /* Zo ja dan nieuw vlak feature maken met aantal attrib velden van gm_koppel
         KOPP_ID_ADMIN, MAPID, X, Y, OCODE */

        /* Hier kunnen daarna views op gemaakt worden */

        return feature;
    }

    private EasyFeature buildNewFeature(SimpleFeature sourceF, SimpleFeature polyF) {
        EasyFeature newF = null;

        // Get the index of the geometryColumn/value
        SimpleFeatureType type = polyF.getFeatureType();
        int geometryColumnIndex = -1;
        String geomColumn = type.getGeometryDescriptor().getName().getLocalPart();
        List<AttributeDescriptor> attributeDescriptors = type.getAttributeDescriptors();
        for (int i = 0; i < attributeDescriptors.size(); i++) {
            if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(geomColumn)) {
                geometryColumnIndex = i;
            }
        }

        /* New columns(difference from both tables */
        SimpleFeatureType sourceFt = (SimpleFeatureType) sourceF.getFeatureType();
        SimpleFeatureType polyFt = (SimpleFeatureType) polyF.getFeatureType();

        List<AttributeDescriptor> sourceAttrDescr = new ArrayList<AttributeDescriptor>(sourceFt.getAttributeDescriptors());

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        String currentGeomColumn = sourceFt.getGeometryDescriptor().getName().getLocalPart();

        List<AttributeDescriptor> newAttrDescr = new ArrayList();
        for (AttributeDescriptor pDescr : sourceAttrDescr) {
            /* Do not add geom column again */
            if (pDescr.getName().getLocalPart().equals(currentGeomColumn)) {
                continue;
            }

            if (!newAttrDescr.contains(pDescr)) {
                newAttrDescr.add(pDescr);
            }
        }

        /* Create new table based on polygon and extra columns */
        SimpleFeatureType newFt = createNewFeatureType(sourceFt, polyFt, geometryColumnIndex, Polygon.class, newAttrDescr);
        
        return newF;
    }

    private SimpleFeatureType createNewFeatureType(
            SimpleFeatureType featureType,
            SimpleFeatureType polyF,
            int attributeId, Class binding,
            List<AttributeDescriptor> extraAttr) {

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setName(polyF.getGeometryDescriptor().getName().getLocalPart());
        attributeTypeBuilder.setLength(256);
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setCRS(polyF.getGeometryDescriptor().getCoordinateReferenceSystem());

        AttributeDescriptor geomDescr = attributeTypeBuilder.buildDescriptor(polyF.getGeometryDescriptor().getName().getLocalPart());
        extraAttr.set(attributeId, geomDescr);
        
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(featureType);
        featureTypeBuilder.setAttributes(extraAttr);
        featureTypeBuilder.setName(featureType.getName().getLocalPart());

        return featureTypeBuilder.buildFeatureType();
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
            //ActionFactory.ATTRIBUTE_NAME_OUTPUT_TABLE,
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
        if (outputDatastore != null) {
            outputDatastore.close();
        }
    }
}
