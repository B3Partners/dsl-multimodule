package nl.b3p.geotools.data.linker.blocks;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.filter.Filter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;

/**
 * @author Boy de Wit B3Partners
 *
 */
public class CollectionAction_Point_Within_Polygon extends CollectionAction {

    protected static final Log log = LogFactory.getLog(CollectionAction_Point_Within_Polygon.class);
    private DataStore dataStore2Write = null;
    private String pointsTable = null;
    private String polygonTable = null;
    
    private static final String MATCHED = "_matched";
    private static final int BUFFER_POINT = 1;
    
    public CollectionAction_Point_Within_Polygon(DataStore dataStore2Write, Map properties) throws Exception {
        /* TODO: Add post actions to GUI with params.
         New table which links post actions to process. */
        properties.put("pointWithinPolygonPointsTable", "gouda_sonderingen_p");
        properties.put("pointWithinPolygonPolygonTable", "gouda_sonderingen_v");

        if (ActionFactory.propertyCheck(properties, "pointWithinPolygonPointsTable")) {
            pointsTable = (String) properties.get("pointWithinPolygonPointsTable");
        } else {
            pointsTable = null;
        }

        if (ActionFactory.propertyCheck(properties, "pointWithinPolygonPolygonTable")) {
            polygonTable = (String) properties.get("pointWithinPolygonPolygonTable");
        } else {
            polygonTable = null;
        }

        if (pointsTable == null || polygonTable == null) {
            throw new Exception("Missing one of the mandatory values in the properties map");
        }
    }

    @Override
    public void execute(FeatureCollection collection, Action writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void execute(DefaultFeatureCollection pointCollection,
            DefaultFeatureCollection polygonCollection, Action nextAction) {

        /* Loop trough points */
        SimpleFeature feature = null;
        FeatureIterator it = pointCollection.features();

        /* Add polygons intersecting with point to result */
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        Filter boundsCheck;
        Filter polyCheck;
        Filter andFil;

        DefaultFeatureCollection newFc = null;

        Map<SimpleFeature, SimpleFeature> resultMap = new HashMap();

        try {
            while (it.hasNext()) {
                feature = (SimpleFeature) it.next();

                BoundingBox bounds = feature.getBounds();
                boundsCheck = (Filter) ff.bbox(ff.property("the_geom"), bounds);

                Geometry geom = (Geometry) feature.getDefaultGeometry();

                /* Small buffer for Point with intersects filter */
                geom = geom.buffer(BUFFER_POINT);

                /* Filters */
                polyCheck = (Filter) ff.intersects(ff.property("the_geom"), ff.literal(geom));
                andFil = (Filter) ff.and(boundsCheck, polyCheck);

                newFc = (DefaultFeatureCollection) polygonCollection.subCollection(andFil);
                
                if (newFc != null && newFc.size() > 0) {
                    SimpleFeature sf = (SimpleFeature) newFc.features().next();
                    resultMap.put(feature, sf);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Problem filtering feature id " + feature.getID() + ":", ex);
        } finally {
            if (newFc != null && it != null) {
                newFc.close(it);
            }
        }

        log.info("Number of matches found: " + resultMap.size());
        
        if (resultMap == null || resultMap.size() < 1) {
            return;
        }

        /* Add to result polygon features attributes of Point and insert
         * into new table with suffix _matched ? */
        SimpleFeatureType pointFt = (SimpleFeatureType) pointCollection.getSchema();
        SimpleFeatureType polygonFt = (SimpleFeatureType) polygonCollection.getSchema();

        // Get the index of the geometryColumn/value
        int geometryColumnIndex = -1;
        String geomColumn = polygonFt.getGeometryDescriptor().getName().getLocalPart();
        List<AttributeDescriptor> attributeDescriptors = polygonFt.getAttributeDescriptors();
        for (int i = 0; i < attributeDescriptors.size(); i++) {
            if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(geomColumn)) {
                geometryColumnIndex = i;
            }
        }

        /* New columns(difference from both tables */
        List<AttributeDescriptor> pointAttrDescr = new ArrayList<AttributeDescriptor>(pointFt.getAttributeDescriptors());
        List<AttributeDescriptor> polyAttrDescr = new ArrayList<AttributeDescriptor>(polygonFt.getAttributeDescriptors());

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        String pointGeomColumn = pointFt.getGeometryDescriptor().getName().getLocalPart();

        List<AttributeDescriptor> newAttrDescr = new ArrayList();
        for (AttributeDescriptor pDescr : pointAttrDescr) {
            /* Do not add geom column again */
            if (pDescr.getName().getLocalPart().equals(pointGeomColumn)) {
                continue;
            }

            if (!polyAttrDescr.contains(pDescr)) {
                newAttrDescr.add(pDescr);
            }
        }

        /* Create new table based on polygon and extra columns */
        SimpleFeatureType newFt = createNewFeatureType(polygonFt, geometryColumnIndex, Polygon.class, newAttrDescr);

        /* Loop polygons and add columns to feature */
        FeatureIterator it2 = polygonCollection.features();
        try {
            while (it2.hasNext()) {
                feature = (SimpleFeature) it2.next();

                Geometry geom = (Geometry) feature.getDefaultGeometry();

                /* TODO: Set extra attributen with correct values */
                List<Object> pointAttribs = null;
                Iterator it3 = resultMap.entrySet().iterator();
                while (it3.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it3.next();
                    SimpleFeature polyF = (SimpleFeature) pairs.getValue();
                    
                    if (polyF.getID().equals(feature.getID())) {
                        SimpleFeature pointF = (SimpleFeature) pairs.getKey();
                        
                        pointAttribs = pointF.getAttributes();                    
                    }
                }
                
                /* Create record and set all attributes */
                List<Object> attributes = feature.getAttributes();
                attributes.set(geometryColumnIndex, geom);

                if (pointAttribs != null && pointAttribs.size() > 0) {                  
                    List<Object> newAttribs = fillExtraAttributes(attributes, pointAttribs, feature);
                    attributes.addAll(newAttribs);
                }                
                
                nextAction.execute(new EasyFeature(SimpleFeatureBuilder.build(newFt, attributes, "" + feature.getID())));
            }
        } catch (Exception ex) {
            log.error("Problem adding feature id " + feature.getID() + ":", ex);
        } finally {
            if (polygonCollection != null && it2 != null) {
                polygonCollection.close(it2);
            }
        }

    }
    
    private List<Object> fillExtraAttributes(List<Object> currentAttribs, 
            List<Object> pointAttribs, SimpleFeature feature) {
        
        List<Object> newAttribs = new ArrayList();
        
        int pointSize = pointAttribs.size();
        int currentSize = currentAttribs.size();
        
        int diff = pointSize - currentSize;
        
        if (diff > 0) {
            for (int i = diff; i > 0; i--) {
                Object object = pointAttribs.get(pointSize - i);
                
                newAttribs.add(object);
            }
        }
        
        return newAttribs;
    }

    public SimpleFeatureType createNewFeatureType(SimpleFeatureType featureType,
            int attributeId, Class binding, List<AttributeDescriptor> extraAttr) {

        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setName(featureType.getGeometryDescriptor().getName().getLocalPart());
        attributeTypeBuilder.setLength(256);
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setCRS(featureType.getGeometryDescriptor().getCoordinateReferenceSystem());
        
        AttributeDescriptor geomDescr = attributeTypeBuilder.buildDescriptor(featureType.getGeometryDescriptor().getName().getLocalPart());
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(featureType.getAttributeDescriptors());
        attributeDescriptors.set(attributeId, geomDescr);

        /* Add extra columns */
        if (extraAttr != null && extraAttr.size() > 0) {
            attributeDescriptors.addAll(extraAttr);
        }

        /* Build FeatureType */
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(featureType);
        featureTypeBuilder.setAttributes(attributeDescriptors);
        featureTypeBuilder.setName(featureType.getName().getLocalPart() + MATCHED);

        return featureTypeBuilder.buildFeatureType();
    }

    public DataStore getDataStore2Write() {
        return dataStore2Write;
    }

    public void setDataStore2Write(DataStore dataStore2Write) {
        this.dataStore2Write = dataStore2Write;
    }

    public String getPointsTable() {
        return pointsTable;
    }

    public void setPointsTable(String pointsTable) {
        this.pointsTable = pointsTable;
    }

    public String getPolygonTable() {
        return polygonTable;
    }

    public void setPolygonTable(String polygonTable) {
        this.polygonTable = polygonTable;
    }
}
