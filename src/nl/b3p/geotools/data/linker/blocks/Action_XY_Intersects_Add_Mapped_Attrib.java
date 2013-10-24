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
import org.geotools.filter.Capabilities;
import org.geotools.filter.Filter;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.visitor.IsSupportedFilterVisitor;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.Intersects;

/**
 *
 * @author Boy de Wit
 */
public class Action_XY_Intersects_Add_Mapped_Attrib extends Action {

    protected static final Log logger = LogFactory.getLog(Action_XY_Intersects_Add_Mapped_Attrib.class);
    private Long vlakkenDatabaseId = null;
    private String vlakkenGeomColumn = null;
    private String vlakkenTableName = null;
    private String sourceMatchColumn = null;
    private String vlakkenMatchColumn = null;
    private Boolean matchGeom = null;
    private static DataStore ds = null;
    private static FeatureSource vlakkenFs = null;
    private static final String SRS = "EPSG:28992";

    public Action_XY_Intersects_Add_Mapped_Attrib(
            Long vlakkenDatabaseId, String vlakkenGeomColumn,
            String vlakkenTableName, String sourceMatchColumn,
            String vlakkenMatchColumn, Boolean matchGeom) {

        this.vlakkenDatabaseId = vlakkenDatabaseId;
        this.vlakkenGeomColumn = vlakkenGeomColumn;
        this.vlakkenTableName = vlakkenTableName;
        this.sourceMatchColumn = sourceMatchColumn;
        this.vlakkenMatchColumn = vlakkenMatchColumn;
        this.matchGeom = matchGeom;

        Database db = null;
        if (vlakkenDatabaseId != null && vlakkenDatabaseId > 0) {
            EntityManager em = JpaUtilServlet.getThreadEntityManager();
            Session session = (Session) em.getDelegate();

            db = (Database) session.get(Database.class, vlakkenDatabaseId);
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
            vlakkenFs = ds.getFeatureSource(vlakkenTableName);
        } catch (Exception ex) {
            logger.error("Fout tijdens openen Datastore.", ex);
        }
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
	
		if (vlakkenFs==null) {
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
        }

        /* Make sure to replace all source values if you want them
         to map against the target column values */
        Object sourceMatchValue = null;
		SimpleFeature sourceF = feature.getFeature();
        if (sourceMatchColumn != null && !sourceMatchColumn.isEmpty()) {
            sourceMatchValue = sourceF.getAttribute(sourceMatchColumn);
        }

        /* Query op vlakken tabel */
 		String geomColumn = null;
		if (vlakkenGeomColumn == null || vlakkenGeomColumn.equals("")) {
			geomColumn = vlakkenFs.getSchema().getGeometryDescriptor().getLocalName();
		} else {
			geomColumn = vlakkenGeomColumn;
		}
		
		Capabilities capabilities = new Capabilities();
		capabilities.addType(Intersects.class);
		// is dit geen onzin, omdat deze capability net is toegevoegd?
		boolean canDoIntersects = capabilities.supports(doIntersects);            

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

		Filter doIntersects = null;
		Filter doAttrib = null;
		Filter doBoth = null;
		if (canDoIntersects && geomColumn!=null && !geomColumn.isEmpty()) {
			doIntersects = (Filter) ff.intersects(ff.property(geomColumn), ff.literal(point));
		}
		if (vlakkenMatchColumn!=null && !vlakkenMatchColumn.isEmpty() &&
				sourceMatchValue!=null && !sourceMatchValue.isEmpty()) {
			// temp fix voor leeg laten van invulvelden
			if (!vlakkenMatchColumn.equals("*") && !sourceMatchColumn.equals("*") {
				// geen filter tbv debug
				//doAttrib = (Filter) CQL.toFilter(vlakkenMatchColumn + " = '" + sourceMatchValue + "'");
			}
		}
		if (doIntersects!=null && doAttrib!=null) {
			doBoth = (Filter) ff.and(doIntersects, doAttrib);
		}
		
		FeatureCollection vlakkenBronCollectie = null;
		if (doBoth!=null) {
			vlakkenBronCollectie = vlakkenFs.getFeatures(doBoth);
		} else if (doAttrib!=null ) {
			vlakkenBronCollectie = vlakkenFs.getFeatures(doAttrib);
		} else if (doIntersects!=null) {
			vlakkenBronCollectie = vlakkenFs.getFeatures(doIntersects);
		} else {
			throw new Exception("No matching strategy found");
		}

		if (vlakkenBronCollectie == null || vlakkenBronCollectie.isEmpty()) {
			throw new FeatureException("Geometrie overlapt niet of geen match"
						+ " gevonden met de waarde in " + sourceMatchColumn;
		}
		//if (vlakkenBronCollectie.size()>1) {
		//	throw new Exception("Meer dan 1 geometrie met match"
		//				+ " gevonden met de waarde in " + sourceMatchColumn;
		//}
		
		EasyFeature newFeature = null;

		FeatureIterator it = null;
		try {
			it = vlakkenBronCollectie.features();
			if (it.hasNext() {
				SimpleFeature vlakkenF = (SimpleFeature) it.next();
				if (vlakkenF == null) {
					throw new FeatureException("No suitable polygon found!");
				}
				newFeature = buildNewFeature(sourceF, vlakkenF);
				
				// voeg attribuutwaarde van vlakkenFs toe aan doel voor debug
				if (vlakkenMatchColumn != null && !vlakkenMatchColumn.isEmpty() &&
						!vlakkenMatchColumn.equals("*")) {
					Object vlakkenMatchValue = vlakkenF.getAttribute(vlakkenMatchColumn);
					newFeature.setAttribute(sourceMatchColumn, vlakkenMatchValue);
				}
				
			}
		} finally {
			vlakkenBronCollectie.close(it);
		}
 
        return newFeature;
    }

    private EasyFeature buildNewFeature(SimpleFeature sourceF, SimpleFeature polyF) {
        SimpleFeatureType vlakkenFt = (SimpleFeatureType) sourceF.getFeatureType();
        SimpleFeatureType polyFt = (SimpleFeatureType) polyF.getFeatureType();

        List<AttributeDescriptor> sourceAttrDescr = new ArrayList<AttributeDescriptor>(vlakkenFt.getAttributeDescriptors());

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        String currentGeomColumn = vlakkenFt.getGeometryDescriptor().getName().getLocalPart();

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
                vlakkenFt,
                polyFt,
                geometryColumnIndex,
                Polygon.class,
                newAttrDescr);

        return createFeature(newFt, sourceF, polyF, geometryColumnIndex);
    }

    private SimpleFeatureType createNewFeatureType(
            SimpleFeatureType vlakkenFt,
            SimpleFeatureType polyFt,
            Integer geometryColumnIndex,
            Class binding,
            List<AttributeDescriptor> newAttrDescr) {

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setName(vlakkenFt.getGeometryDescriptor().getName().getLocalPart());

        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setAttributes(newAttrDescr);
        featureTypeBuilder.setSRS(SRS);
        featureTypeBuilder.setName(vlakkenFt.getTypeName());

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

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}
