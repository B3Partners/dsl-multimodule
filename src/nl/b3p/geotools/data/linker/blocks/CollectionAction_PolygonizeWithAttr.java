/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.type.AttributeDescriptor;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.ActionFactory;
/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 11-mrt-2010, 10:01:19
 */
public class CollectionAction_PolygonizeWithAttr extends CollectionAction {
    protected static final Log log = LogFactory.getLog(CollectionAction_PolygonizeWithAttr.class);
    
    private static final String POLYGONIZED="_polygonized";
    /*TODO vullen in constructor*/
    private DataStore dataStore2Write = null;
    private String cqlFilterString=null;
    private String attributeFeatureName=null;
    private String lineFeatureName=null;

    private int polygonCounter=0;
    private int multiPolygonCounter=0;

    public CollectionAction_PolygonizeWithAttr(DataStore dataStore2Write,Map properties) throws Exception{
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR_CQLFILTER_ATTRIBUTE)) {
            cqlFilterString = (String) properties.get(ActionFactory.POLYGONIZEWITHATTR_CQLFILTER_ATTRIBUTE);
        } else {
            cqlFilterString = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR_ATTRIBUTEFEATURENAME_ATTRIBUTE)) {
            attributeFeatureName = (String) properties.get(ActionFactory.POLYGONIZEWITHATTR_ATTRIBUTEFEATURENAME_ATTRIBUTE);
        } else {
            attributeFeatureName = null;
        }
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR_LINEFEATURENAME_ATTRIBUTE)) {
            lineFeatureName = (String) properties.get(ActionFactory.POLYGONIZEWITHATTR_LINEFEATURENAME_ATTRIBUTE);
        } else {
            lineFeatureName = null;
        }
        if (cqlFilterString==null || attributeFeatureName==null || lineFeatureName==null){
            throw new Exception("Missing one of the mandatory values in the properties map");
        }
    }
    @Override
    public void execute(FeatureCollection collection, Action nextAction) {        
        FeatureIterator features = null;
        try {            
            features = collection.features();
            SimpleFeatureType originalFt=(SimpleFeatureType) collection.getSchema();
            
            //create a list of attribute names.
            Iterator it=collection.getSchema().getDescriptors().iterator();
            ArrayList<String> featureNames= new ArrayList();
            while (it.hasNext()){
                PropertyDescriptor pd=(PropertyDescriptor) it.next();
                featureNames.add(pd.getName().getLocalPart());
            }
            //Get the index of the geometryColumn/value
            int geometryColumnIndex=-1;            
            String geomColumn=originalFt.getGeometryDescriptor().getName().getLocalPart();
            List<AttributeDescriptor> attributeDescriptors = originalFt.getAttributeDescriptors();
            for (int i = 0; i < attributeDescriptors.size(); i++) {
                if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(geomColumn)) {
                    geometryColumnIndex= i;
                }
            }
            //create a polygon featuretype and a multipolygon featuretype
            SimpleFeatureType polygonFt=createNewFeatureType(originalFt,geometryColumnIndex,Polygon.class);
            SimpleFeatureType multiPolygonFt=createNewFeatureType(originalFt,geometryColumnIndex,Polygon.class);

            while (features.hasNext()) {
                try{
                    SimpleFeature feature = (SimpleFeature) features.next();
                    String featureFilterString=new String(getCqlFilterString());
                    for (int i=0; i < featureNames.size() && featureFilterString.indexOf("[")>=0; i++){
                        if (featureFilterString.indexOf("["+featureNames.get(i)+"]")>=0 && feature.getProperty(featureNames.get(i))!=null){
                            String regExp="\\["+featureNames.get(i)+"\\]";
                            String value=feature.getProperty(featureNames.get(i)).getValue().toString();
                            if (value!=null)
                                value="'"+value+"'";
                            featureFilterString=featureFilterString.replaceAll(regExp,value);
                        }
                    }
                    if (featureFilterString.indexOf("[")>=0){
                        log.error("The CQL string is not correct: "+featureFilterString);
                        continue;
                    }
                    Filter filter= CQL.toFilter(featureFilterString);
                    //get lines
                    FeatureSource fs = dataStore2Write.getFeatureSource(getLineFeatureName());
                    FeatureCollection fc = fs.getFeatures(filter);
                    Polygonizer p = new Polygonizer();
                    FeatureIterator lineFeatures=fc.features();
                    while(lineFeatures.hasNext()){
                        SimpleFeature line = (SimpleFeature) lineFeatures.next();
                        Geometry featureGeom = (Geometry) line.getDefaultGeometryProperty().getValue();
                        p.add(featureGeom);
                    }
                    //do polygonize
                    if (p.getPolygons().size()<=0){
                        log.debug("can't create polygon for this feature");
                        continue;
                    }
                    //get the correct featuretype and geom (polygon or multipolygon)
                    Geometry geom=null;
                    SimpleFeatureType newFt=null;
                    int id=0;
                    if(p.getPolygons().size()>1){
                        Polygon[] polygons = new Polygon[p.getPolygons().size()];
                        int srid=28992;
                        //create a geometryFactory for making multipolygons
                        Iterator pit=p.getPolygons().iterator();
                        for(int i=0;pit.hasNext();i++){
                            polygons[i]=   (Polygon) pit.next();
                        }
                        if (polygons[0].getSRID()>0){
                            srid=polygons[0].getSRID();
                        }
                        GeometryFactory gf= new GeometryFactory(new PrecisionModel(),srid);
                        geom = new MultiPolygon(polygons, gf);
                    }else{
                        geom=(Geometry)p.getPolygons().toArray()[0];
                        newFt=polygonFt;
                        polygonCounter++;
                        id=polygonCounter;
                    }
                    //replace the geometry in the values
                    List<Object> attributes = feature.getAttributes();
                    attributes.set(geometryColumnIndex,geom);

                    nextAction.execute(new EasyFeature(SimpleFeatureBuilder.build(newFt, attributes, "" + id)));
                }catch(Exception e){
                    log.error("Error creating feature Polygon (in polygonize function): ",e);
                }
            }
        } catch (Exception e) {
            log.error("Error polygonizer for feature: "+collection.getSchema().getName().getLocalPart(), e);
        } finally {
            if (collection != null && features != null) {
                collection.close(features);
            }
        }

    }

    public SimpleFeatureType createNewFeatureType(SimpleFeatureType featureType, int attributeId,Class binding){
        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setCRS(featureType.getGeometryDescriptor().getCoordinateReferenceSystem());
        attributeTypeBuilder.setName(featureType.getGeometryDescriptor().getName().getLocalPart());
        attributeTypeBuilder.setNillable(featureType.getGeometryDescriptor().isNillable());

        // Prevent warnings; save as VARCHAR(256)
        attributeTypeBuilder.setLength(256);

        AttributeDescriptor attributeDescriptor = attributeTypeBuilder.buildDescriptor(featureType.getGeometryDescriptor().getName().getLocalPart());
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(featureType.getAttributeDescriptors());
        attributeDescriptors.set(attributeId, attributeDescriptor);

        // Build FeatureType
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(featureType);
        featureTypeBuilder.setAttributes(attributeDescriptors);
        if (binding==Polygon.class){
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + "_v");
        }else if (binding==MultiPolygon.class){
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + "_mv");
        }else{
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + POLYGONIZED);
        }
        return featureTypeBuilder.buildFeatureType();

    }

    public String getCqlFilterString() {
        return cqlFilterString;
    }

    public String getAttributeFeatureName() {
        return attributeFeatureName;
    }

    public String getLineFeatureName() {
        return lineFeatureName;
    }
    public void setDataStore2Write(DataStore dataStore2Write){
        this.dataStore2Write=dataStore2Write;
    }
}
