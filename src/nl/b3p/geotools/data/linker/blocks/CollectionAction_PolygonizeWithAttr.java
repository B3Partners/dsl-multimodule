package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import java.util.ArrayList;
import java.util.Collection;
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
import org.geotools.filter.text.cql2.CQLException;
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
 *
 * Deze functie is niet heel erg specifiek SUF-lki. Op 2 plekken zitten specifieke dingen in.
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
        if (attributeFeatureName==null || lineFeatureName==null){
            throw new Exception("Missing one of the mandatory values in the properties map");
        }
    }
    @Override
    public void execute(FeatureCollection originalCollection, Action nextAction) {
        if(cqlFilterString==null){
            log.error("CqlFilter not set!");
            return;
        }
        FeatureIterator features = null;
        FeatureCollection collection=null;
        try {
            //get all propertynames that are needed for this cql filter completion.
            ArrayList<String> propertyNames =getPropertyNamesInCql(getCqlFilterString());
            //do only the features with  the CQL values is not null;
            Filter usableFilter=createFilterWithOnlyUsableFeatures(propertyNames);
            collection=originalCollection.subCollection(usableFilter);
            features = collection.features();
            SimpleFeatureType originalFt=(SimpleFeatureType) collection.getSchema();
           
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
            //walk through all features
            int featureCounter=-1;
            int totalFeatures=collection.size();
            while (features.hasNext()) {
                featureCounter++;
                try{
                    if (featureCounter % 10000 ==0){
                        log.info("PolygonizeWithAttr featurecount: "+featureCounter+"/"+totalFeatures);
                    }
                    SimpleFeature feature = (SimpleFeature) features.next();
                    String featureFilterString=new String(getCqlFilterString());
                    //replace al the [value] with the correct value of the feature.
                    for (int i=0; i < propertyNames.size() && featureFilterString.indexOf("[")>=0; i++){
                        if (featureFilterString.indexOf("["+propertyNames.get(i)+"]")>=0 && feature.getProperty(propertyNames.get(i))!=null){
                            String regExp="\\["+propertyNames.get(i)+"\\]";
                            String value=null;
                            if (feature.getProperty(propertyNames.get(i)).getValue()!=null){
                                value=feature.getProperty(propertyNames.get(i)).getValue().toString();
                            }
                            if (value!=null){
                                value="'"+value+"'";
                                featureFilterString=featureFilterString.replaceAll(regExp,value);
                            }else{
                                continue;
                            }                            
                        }
                    }
                    if (featureFilterString.indexOf("[")>=0){
                        log.error("The CQL string is not correct: "+featureFilterString);
                        continue;
                    }                    
                    //make the filter.
                    Filter filter= CQL.toFilter(featureFilterString);
                    //get lines
                    FeatureSource fs = dataStore2Write.getFeatureSource(getLineFeatureName());
                    FeatureCollection fc = fs.getFeatures(filter);
                    
                    FeatureIterator lineFeatures=fc.features();
                    ArrayList<SimpleFeature> correctLineFeatures= new ArrayList();
                    //check if the found lines can form a polygon. Remove the ones that are not.
                    while(lineFeatures.hasNext()){
                        SimpleFeature line = (SimpleFeature) lineFeatures.next();
                        Geometry featureGeom = (Geometry) line.getDefaultGeometryProperty().getValue();
                        boolean addLine=true;
                        int position=correctLineFeatures.size();
                        for (int i=0; i < correctLineFeatures.size() && addLine;i++){
                            Geometry lineGeom=(Geometry) correctLineFeatures.get(i).getDefaultGeometryProperty().getValue();
                            if(lineGeom.crosses(featureGeom) ||
                                    lineGeom.contains(featureGeom)){
                                addLine=false;
                            }else if(featureGeom.contains(lineGeom)){
                                //if the new line contains the old line: Replace (the new one is bigger)
                                position=i;
                            }
                        }
                        if (addLine){
                            correctLineFeatures.add(position,line);
                        }
                    }
                    if (correctLineFeatures.size()<=1){
                        log.debug("not more then 2 lines found for feature with cql: "+featureFilterString);
                        continue;
                    }
                    //merge the lines
                    LineMerger merger = new LineMerger();
                    for (int i=0; i < correctLineFeatures.size();i++){
                        Geometry lineGeom=(Geometry) correctLineFeatures.get(i).getDefaultGeometryProperty().getValue();
                        merger.add(lineGeom);
                    }
                    //Create a polygon for every mergedlinestring
                    Collection coll=merger.getMergedLineStrings();                    
                    Iterator geomIt=coll.iterator();
                    Geometry geom=null;
                    ArrayList<Polygon> polygons = new ArrayList();
                    while (geomIt.hasNext()){
                        Geometry linestring=(Geometry)geomIt.next();
                        Polygonizer p = new Polygonizer();
                        p.add(linestring);
                        //do polygonize
                        if (p.getPolygons().size()<=0){
                            log.debug("can't create polygon with this this line:");
                            log.debug(linestring.toText());
                            log.debug("Cutedges: "+p.getCutEdges().size()+" Dangles: "+p.getDangles().size()+" InvalidRingLines: "+p.getInvalidRingLines().size());
                            continue;
                        }
                        polygons.addAll(p.getPolygons());
                    }
                    //if there are no polygons found continue to next feature.
                    if (polygons.size()==0){
                        continue;
                    }
                    //check if its a positive or negative geom (negative == hole)
                    ArrayList<Polygon> positivePolygons=new ArrayList();
                    ArrayList<Polygon> negativePolygons=new ArrayList();
                    for (int i=0; i < polygons.size(); i ++){
                        Polygon polygon=polygons.get(i);
                        Boolean ispositivePolygon=isPositivePolygon(polygon,correctLineFeatures,feature);
                        if (ispositivePolygon==null)
                            log.error("error getting the value of the polygon (positive or negative)");
                        else if (!ispositivePolygon.booleanValue()){
                            negativePolygons.add(polygon);
                        }else{
                            positivePolygons.add(polygon);
                        }
                    }
                    if (positivePolygons.size()==0){
                        throw new Exception("no positive geometries found.");
                    }
                    geom=positivePolygons.get(0);
                    for (int i=1; i < positivePolygons.size(); i++){
                        geom=geom.union(positivePolygons.get(i));
                    }
                    for (int i=0; i < negativePolygons.size(); i++){
                        geom=geom.difference(negativePolygons.get(i));
                    }
                    //get the correct featuretype                        
                    SimpleFeatureType newFt=null;
                    int id=0;
                    if(geom instanceof MultiPolygon){
                        multiPolygonCounter++;
                        id=multiPolygonCounter;
                        newFt=multiPolygonFt;
                    }else{
                        polygonCounter++;
                        id=polygonCounter;
                        newFt=polygonFt;
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
            log.error("Error polygonizer for feature: "+originalCollection.getSchema().getName().getLocalPart(), e);
        } finally {
            if (collection != null && features != null) {
                collection.close(features);
            }
        }

    }
    private Filter createFilterWithOnlyUsableFeatures(ArrayList<String> propertyNames) throws CQLException{
        String cqlFilter= getCqlFilterString();              
        Filter filter = Filter.INCLUDE;
        if (propertyNames.size()>0){
            String newCqlFilter="";
            for (int i=0; i < propertyNames.size(); i++){
                if (newCqlFilter.length()>0){
                    newCqlFilter+=" and ";
                }
                newCqlFilter+=propertyNames.get(i)+ " is not null";
            }
            filter=CQL.toFilter(newCqlFilter);
        }
        return filter;
    }

    private ArrayList<String> getPropertyNamesInCql(String cqlFilter){
        int beginIndex= cqlFilter.indexOf("[");
        int endIndex=cqlFilter.indexOf("]");
        ArrayList<String> propertyNames=new ArrayList();
        while (beginIndex >=0){
            if (endIndex > beginIndex){
                String propName= cqlFilter.substring(beginIndex+1,endIndex);
                if (!propertyNames.contains(propName)){
                    propertyNames.add(propName);
                }
            }
            beginIndex=cqlFilter.indexOf("[", endIndex+1);
            endIndex=cqlFilter.indexOf("]", endIndex+1);
        }
        return propertyNames;
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
    public void setCqlFilterString(String cqlFilterString){
        this.cqlFilterString=cqlFilterString;
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

    public Boolean isPositivePolygon(Polygon polygon, ArrayList<SimpleFeature> correctLineFeatures,SimpleFeature feature) {
        return true;
    }
}
