/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 12-jan-2010, 11:42:19
 */
public class CollectionAction_Polygonize extends CollectionAction {
    protected static final Log log = LogFactory.getLog(CollectionAction_Polygonize.class);
    
    private final String polygonizeClassificationAttribute;
    private final Integer polygonizeClassificationBegin;
    private final Integer polygonizeClassificationEnd;
    private HashMap<Object, Polygonizer> polygonizers;
    private final boolean splitInClasses;
    private static final String POLYGONIZED = "_poligonized";
    private static final String DEFAULT_CLASSIFICATION = "default";

    public CollectionAction_Polygonize(Map properties) {
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_ATTRIBUTE)) {
            polygonizeClassificationAttribute = (String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_ATTRIBUTE);
        } else {
            polygonizeClassificationAttribute = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_BEGIN)) {
            polygonizeClassificationBegin = Integer.parseInt((String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_BEGIN));
        } else {
            polygonizeClassificationBegin = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_END)) {
            polygonizeClassificationEnd = Integer.parseInt((String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_END));
        } else {
            polygonizeClassificationEnd = null;
        }

        if (this.polygonizeClassificationAttribute != null && this.polygonizeClassificationAttribute.length() > 0) {
            this.splitInClasses = true;
        } else {
            this.splitInClasses = false;
        }
        polygonizers = new HashMap();
        
    }

    @Override
    public ArrayList<EasyFeature> execute(FeatureCollection collection) {
        FeatureIterator features = null;
        
        ArrayList<EasyFeature> polygonizedFeatures = new ArrayList();
        try {
            features = collection.features();
            while (features.hasNext()) {
                Feature feature = features.next();
                Object classification = DEFAULT_CLASSIFICATION;
                if (splitInClasses) {
                    //determine classification value
                    if (feature.getProperty(polygonizeClassificationAttribute)!=null){
                        classification = feature.getProperty(polygonizeClassificationAttribute).getValue();
                        if (classification!=null && (polygonizeClassificationEnd!=null || polygonizeClassificationBegin!=null)){
                            String s= classification.toString();
                            int begin=0;
                            int end=s.length();
                            if (polygonizeClassificationEnd!=null && polygonizeClassificationEnd.intValue() < s.length())
                                end=polygonizeClassificationEnd.intValue();
                            if (polygonizeClassificationBegin!=null){
                                if (polygonizeClassificationBegin.intValue()>end)
                                    begin=end;
                                else
                                    begin=polygonizeClassificationBegin.intValue();
                            }
                            classification=s.substring(begin, end);
                        }

                    }
                    if (classification == null) {
                        classification = DEFAULT_CLASSIFICATION;
                    }
                    if (polygonizers.get(classification) == null) {
                        polygonizers.put(classification, new Polygonizer());
                    }
                    Polygonizer polygonizer = polygonizers.get(classification);
                    Geometry featureGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                    if (featureGeom.isValid()) {
                        polygonizer.add(featureGeom);
                    }
                }
            }

            SimpleFeatureType newFt = createFeatureType(collection.getSchema());
            Iterator it = polygonizers.keySet().iterator();
            
            log.info("Try to polygonize with "+collection.size()+" line features over "+polygonizers.size()+" classifications");
            int polygonCounter = 0;    
            int totalDangles=0;
            int invalidRingLines=0;
            int cutEdges=0;
            while (it.hasNext()) {
                Object nextKey= it.next();
                String keyString = nextKey.toString();
                Polygonizer p = (Polygonizer) polygonizers.get(nextKey);
                Collection c = p.getPolygons();
                totalDangles+=p.getDangles().size();
                invalidRingLines+=p.getInvalidRingLines().size();
                cutEdges+=p.getCutEdges().size();
                if (log.isDebugEnabled()){
                    if (p.getDangles().size()>0)
                        log.debug("Dangles for class '"+keyString+"':"+geometryCollectionToWKTString(p.getDangles()));
                    if (p.getInvalidRingLines().size()>0)
                        log.debug("Invalid Ring Lines for class '"+keyString+"':"+geometryCollectionToWKTString(p.getInvalidRingLines()));
                    if (p.getCutEdges().size()>0)
                        log.debug("Cut Edges for class '"+keyString+"':"+geometryCollectionToWKTString(p.getCutEdges()));
                }
                Iterator cit = c.iterator();
                while (cit.hasNext()) {
                    List values = new ArrayList();
                    if (splitInClasses) {
                        values.add(keyString);
                    }
                    values.add((Polygon) cit.next());
                    polygonizedFeatures.add(new EasyFeature(SimpleFeatureBuilder.build(newFt, values, "" + polygonCounter)));
                    polygonCounter++;
                }
            }
            log.info("Polygonization done.\n"+polygonCounter+" polygons created by polygonization. \n"+totalDangles +" lines are not connected with both sides (Dangles). \n"+invalidRingLines+" invalid ring lines are formed (e.g. the component lines contain a self-intersectin). \n"+cutEdges +" lines are connected with both ends but don't form part of a polygon. (Cutted Edges)");
        } finally {
            if (collection != null && features != null) {
                collection.close(features);
            }
        }
        return polygonizedFeatures;
    }

    public void close() {
    }

    private SimpleFeatureType createFeatureType(FeatureType ft) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();

        typeBuilder.setName(ft.getName().getLocalPart() + POLYGONIZED);
        typeBuilder.setCRS(ft.getCoordinateReferenceSystem());

        if (splitInClasses) {
            typeBuilder.add("classification", String.class);
        }
        typeBuilder.add(ft.getGeometryDescriptor().getLocalName(), Polygon.class);

        return typeBuilder.buildFeatureType();
    }
}
