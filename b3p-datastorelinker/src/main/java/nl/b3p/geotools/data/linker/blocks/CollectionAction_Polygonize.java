/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.PolygonizerWithoutInvalidLists;
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
import org.geotools.feature.DefaultFeatureCollection;
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
    private final String CLASS_STATUS_TODO = "waiting";
    private final String CLASS_STATUS_DONE = "done";
    private final boolean splitInClasses;
    private static final String POLYGONIZED = "_poligonized";
    private static final String DEFAULT_CLASSIFICATION = "default";
    private final boolean oneClassInMemory;

    public CollectionAction_Polygonize(Map properties) {
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_ATTRIBUTE)) {
            polygonizeClassificationAttribute = (String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_ATTRIBUTE);
        } else {
            polygonizeClassificationAttribute = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_BEGIN)) {
            Integer i;
            try {
                i = Integer.parseInt((String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_BEGIN));
            } catch (NumberFormatException nfe) {
                i = null;
            }
            polygonizeClassificationBegin = i;
        } else {
            polygonizeClassificationBegin = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_CLASSIFICATION_END)) {
            Integer i;
            try {
                i = Integer.parseInt((String) properties.get(ActionFactory.POLYGONIZE_CLASSIFICATION_END));
            } catch (NumberFormatException nfe) {
                i = null;
            }
            polygonizeClassificationEnd = i;
        } else {
            polygonizeClassificationEnd = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZE_ONECLASSINMEMORY)){
           Boolean b;
            try {
                b = (Boolean)properties.get(ActionFactory.POLYGONIZE_ONECLASSINMEMORY);
            } catch (NumberFormatException nfe) {
                b = null;
            }
            oneClassInMemory = b;
        }else{
            oneClassInMemory=true;
        }

        if (this.polygonizeClassificationAttribute != null && this.polygonizeClassificationAttribute.length() > 0) {
            this.splitInClasses = true;
        } else {
            this.splitInClasses = false;
        }


    }
    
    public void execute(DefaultFeatureCollection collection, Action nextAction){
        if (oneClassInMemory){
            executeOneClassInMemory(collection, nextAction);
        }else{
            executeAllInMemory(collection,nextAction);
        }
    }
    /**
     * alle classificaties worden meteen in het geheugen geladen.
     *
     * @param collection the collection of features
     * @param nextAction ??
     */
    public void executeAllInMemory (DefaultFeatureCollection collection, Action nextAction){
        log.info("execute Polygonize with all classifications at once");
        SimpleFeatureType newFt = createFeatureType(collection.getSchema());
        HashMap<Object, PolygonizerWithoutInvalidLists> polygonizers = new HashMap();
        FeatureIterator features = null;
        try {
            features = collection.features();
            while (features.hasNext()) {
                Feature feature = features.next();
                Object classification = getClassification(feature);
                if (polygonizers.get(classification)==null){
                    polygonizers.put(classification,new PolygonizerWithoutInvalidLists());
                }
                Geometry featureGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                if (featureGeom.isValid()) {
                    polygonizers.get(classification).add(featureGeom);
                }
            }

            Iterator pit=polygonizers.keySet().iterator();
            while(pit.hasNext()){
                Object classification = pit.next();
                 log.info("Polygonize features with classification: " + classification);
                PolygonizerWithoutInvalidLists polygonizer= polygonizers.get(classification);
                createPolygonsFeatures(polygonizer, classification, newFt, nextAction);
            }
        } catch (Exception e) {
            log.error("Error polygonizer for feature: "+newFt.getTypeName(), e);
        } finally {
            if (collection != null && features != null) {
                collection.close(features);
            }
        }
    }
    /**
     * Deze execute loopt meerdere malen over de de data (featureCollection) als er een classificatie is aangegeven.
     * Per classificatie wordt er een keer doorheen gelopen. Dit om te voorkomen dat alle objecten te gelijk in het
     * geheugen worden geladen. Dit is dus wel minder snel, maar heeft meer kans om te slagen.
     *
     * @param collection the collection of features
     * @param nextAction ??
     */
    public void executeOneClassInMemory(DefaultFeatureCollection collection, Action nextAction) {
        log.info("execute Polygonize with one classification in the memory at once");
        HashMap<Object, String> classificationStatuses = null;
        SimpleFeatureType newFt = createFeatureType(collection.getSchema());
        //HashMap<Object, Polygonizer> polygonizers = new HashMap();
        for (int i = 0; classificationStatuses == null || i < classificationStatuses.size() ; i++) {
            if (classificationStatuses == null) {
                classificationStatuses = new HashMap();
            }
            FeatureIterator features = null;
            Object currentClassification = null;
            try {
                features = collection.features();
                PolygonizerWithoutInvalidLists polygonizer = new PolygonizerWithoutInvalidLists();
                while (features.hasNext()) {
                    Feature feature = features.next();
                    Object classification = getClassification(feature);
                    String classificationStatus = classificationStatuses.get(classification);

                    if (classificationStatus == null) {
                        classificationStatuses.put(classification, CLASS_STATUS_TODO);
                    } else if (classificationStatus.equalsIgnoreCase(CLASS_STATUS_DONE)) {
                        continue;
                    }
                    if (currentClassification == null) {
                        currentClassification = classification;
                    }
                    if (classification.equals(currentClassification)) {
                        Geometry featureGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                        if (featureGeom.isValid()) {
                            polygonizer.add(featureGeom);
                        }
                    }
                }                
                log.info("Polygonize features with classification: " + currentClassification);
                createPolygonsFeatures(polygonizer, currentClassification, newFt, nextAction);
            } catch (Exception e) {
                log.error("Error polygonizer for feature: "+newFt.getTypeName()+" with classification: "+currentClassification, e);
            } finally {
                if (currentClassification != null) {
                    classificationStatuses.put(currentClassification, CLASS_STATUS_DONE);
                }
                if (collection != null && features != null) {
                    collection.close(features);
                }
            }
        }
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

    private void createPolygonsFeatures(PolygonizerWithoutInvalidLists polygonizer, Object classification, SimpleFeatureType newFt, Action nextAction) throws Exception {
        int successPolygonCounter = 0;
        int totalDangles = -1;
        int invalidRingLines = -1;
        int cutEdges = -1;
        Collection c = polygonizer.getPolygons();
        if (log.isDebugEnabled()){
            /*totalDangles = polygonizer.getDangles().size();
            invalidRingLines = polygonizer.getInvalidRingLines().size();
            cutEdges = polygonizer.getCutEdges().size();*/
        }
        /*if (log.isDebugEnabled()){
        if (p.getDangles().size()>0)
        log.debug("Dangles for class '"+keyString+"':"+geometryCollectionToWKTString(p.getDangles()));
        if (p.getInvalidRingLines().size()>0)
        log.debug("Invalid Ring Lines for class '"+keyString+"':"+geometryCollectionToWKTString(p.getInvalidRingLines()));
        if (p.getCutEdges().size()>0)
        log.debug("Cut Edges for class '"+keyString+"':"+geometryCollectionToWKTString(p.getCutEdges()));
        }*/
        log.info("Polygonization for feature " + newFt.getTypeName() + " and classification " + classification + " done.\n" + c.size() + " polygons created by polygonization. \n" + totalDangles + " lines are not connected with both sides (Dangles). \n" + invalidRingLines + " invalid ring lines are formed (e.g. the component lines contain a self-intersectin). \n" + cutEdges + " lines are connected with both ends but don't form part of a polygon. (Cutted Edges)");
        Iterator cit = c.iterator();
        while (cit.hasNext()) {
            try {
                List values = new ArrayList();
                if (splitInClasses) {
                    values.add(classification);
                }
                values.add((Polygon) cit.next());
                nextAction.execute(new EasyFeature(SimpleFeatureBuilder.build(newFt, values, "" + successPolygonCounter)));
                successPolygonCounter++;
            } catch (Exception e) {
                log.error("Error passing feature", e);
            }
        }
        log.info(c.size() + " Polygons with featureName: " + newFt.getTypeName() + "and classification: " + classification + " are processed. " + successPolygonCounter + " are successful.");
    }

    private Object getClassification(Feature feature) {
        Object classification = DEFAULT_CLASSIFICATION;
        if (splitInClasses) {
            //determine classification value
            if (feature.getProperty(polygonizeClassificationAttribute) != null) {
                classification = feature.getProperty(polygonizeClassificationAttribute).getValue();
                if (classification != null && (polygonizeClassificationEnd != null || polygonizeClassificationBegin != null)) {
                    String s = classification.toString();
                    int begin = 0;
                    int end = s.length();
                    if (polygonizeClassificationEnd != null && polygonizeClassificationEnd.intValue() < s.length()) {
                        end = polygonizeClassificationEnd.intValue();
                    }
                    if (polygonizeClassificationBegin != null) {
                        if (polygonizeClassificationBegin.intValue() > end) {
                            begin = end;
                        } else {
                            begin = polygonizeClassificationBegin.intValue();
                        }
                    }
                    classification = s.substring(begin, end);
                }
            }
            if (classification == null) {
                classification = DEFAULT_CLASSIFICATION;
            }
        }
        return classification;
    }

    @Override
    public void execute(FeatureCollection collection, Action writer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
