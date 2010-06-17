package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.opengis.filter.Filter;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.type.AttributeDescriptor;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.data.DataStoreFinder;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 11-mrt-2010, 10:01:19
 *
 * Deze functie is niet heel erg specifiek SUF-lki. Op 2 plekken zitten specifieke dingen in.
 */
public class CollectionAction_PolygonizeWithAttr extends CollectionAction {

    protected static final Log log = LogFactory.getLog(CollectionAction_PolygonizeWithAttr.class);
    private static final String POLYGONIZED = "_polygonized";
    /*TODO vullen in constructor*/
    private DataStore dataStore2Write = null;
    private String cqlFilterString = null;
    private String attributeFeatureName = null;
    private String lineFeatureName = null;
    private int polygonCounter = 0;
    private int multiPolygonCounter = 0;
    /* a line close tolerance to close lines that are not closed:
     * if < 0: always close
     * if ==0: never close if not closed
     * if > 0: close when distance is smaller then the lineCloseTolerance
    */
    protected double lineCloseTolerance= 0;

    public CollectionAction_PolygonizeWithAttr(DataStore dataStore2Write, Map properties) throws Exception {
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
        if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR_LINECLOSETOLERANCE_ATTRIBUTE)) {
            lineCloseTolerance = Double.parseDouble((String) properties.get(ActionFactory.POLYGONIZEWITHATTR_LINECLOSETOLERANCE_ATTRIBUTE));
        } else {
            lineCloseTolerance = 0;
        }
        if (attributeFeatureName == null || lineFeatureName == null) {
            throw new Exception("Missing one of the mandatory values in the properties map");
        }
    }

    @Override
    public void execute(FeatureCollection originalCollection, Action nextAction) {
        preExecute();
        if (cqlFilterString == null) {
            log.error("CqlFilter not set!");
            return;
        }
        FeatureIterator features = null;
        FeatureCollection collection = null;
        try {
            //get all propertynames that are needed for this cql filter completion.
            ArrayList<String> propertyNames = getPropertyNamesInCql(getCqlFilterString());
            //do only the features with  the CQL values is not null;
            Filter usableFilter = createFilterWithOnlyUsableFeatures(propertyNames);
            collection = originalCollection.subCollection(usableFilter);
            features = collection.features();
            SimpleFeatureType originalFt = (SimpleFeatureType) collection.getSchema();

            //Get the index of the geometryColumn/value
            int geometryColumnIndex = -1;
            String geomColumn = originalFt.getGeometryDescriptor().getName().getLocalPart();
            List<AttributeDescriptor> attributeDescriptors = originalFt.getAttributeDescriptors();
            for (int i = 0; i < attributeDescriptors.size(); i++) {
                if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(geomColumn)) {
                    geometryColumnIndex = i;
                }
            }
            //create a polygon featuretype and a multipolygon featuretype
            SimpleFeatureType polygonFt = createNewFeatureType(originalFt, geometryColumnIndex, Polygon.class);
            SimpleFeatureType multiPolygonFt = createNewFeatureType(originalFt, geometryColumnIndex, Polygon.class);
            //walk through all features
            int featureCounter = -1;
            int totalFeatures = collection.size();
            while (features.hasNext()) {
                featureCounter++;
                try {
                    if (featureCounter % 10000 == 0) {
                        log.info("PolygonizeWithAttr featurecount: " + featureCounter + "/" + totalFeatures);
                    }
                    SimpleFeature feature = (SimpleFeature) features.next();
                    String featureFilterString = new String(getCqlFilterString());

                    //make the filter.
                    Filter filter = createLineFilter(feature, propertyNames);
                    //get lines
                    FeatureSource fs = dataStore2Write.getFeatureSource(getLineFeatureName());
                    FeatureCollection fc = fs.getFeatures(filter);

                    FeatureIterator lineFeatures = fc.features();
                    ArrayList<SimpleFeature> correctLineFeatures = filterInvalidLines(lineFeatures);

                    if (correctLineFeatures.size() <= 1) {
                        log.debug("not more then 2 valid lines found for feature with cql: " + featureFilterString);
                        continue;
                    }
                    ArrayList<Polygon> polygons=createPolygonWithLines(correctLineFeatures);
                    //if there are no polygons found continue to next feature.
                    if (polygons.size() == 0) {
                        log.debug("No polygons created with lines.\nQuery: " + featureFilterString);
                        continue;
                    }
                    //check if its a positive or negative geom (negative == hole)
                    ArrayList<Polygon> positivePolygons = new ArrayList();
                    ArrayList<Polygon> negativePolygons = new ArrayList();
                    for (int i = 0; i < polygons.size(); i++) {
                        Polygon polygon = polygons.get(i);
                        Boolean ispositivePolygon = isPositivePolygon(polygon, correctLineFeatures, feature);
                        if (ispositivePolygon == null) {
                            log.error("error getting the value of the polygon (positive or negative)");
                        } else if (!ispositivePolygon.booleanValue()) {
                            negativePolygons.add(polygon);
                        } else {
                            positivePolygons.add(polygon);
                        }
                    }
                    if (positivePolygons.size() == 0) {
                        log.error("no positive geometries found.\n Query: " + featureFilterString);
                        continue;
                    }
                    Geometry geom = null;
                    geom = positivePolygons.get(0);
                    for (int i = 1; i < positivePolygons.size(); i++) {
                        geom = geom.union(positivePolygons.get(i));
                    }
                    for (int i = 0; i < negativePolygons.size(); i++) {
                        geom = geom.difference(negativePolygons.get(i));
                    }
                    //get the correct featuretype                        
                    SimpleFeatureType newFt = null;
                    int id = 0;
                    if (geom instanceof MultiPolygon) {
                        multiPolygonCounter++;
                        id = multiPolygonCounter;
                        newFt = multiPolygonFt;
                    } else {
                        polygonCounter++;
                        id = polygonCounter;
                        newFt = polygonFt;
                    }
                    //replace the geometry in the values
                    List<Object> attributes = feature.getAttributes();
                    attributes.set(geometryColumnIndex, geom);
                    nextAction.execute(new EasyFeature(SimpleFeatureBuilder.build(newFt, attributes, "" + id)));

                } catch (Exception e) {
                    log.error("Error creating feature Polygon (in polygonize function): ", e);
                }
            }
        } catch (Exception e) {
            log.error("Error polygonizer for feature: " + originalCollection.getSchema().getName().getLocalPart(), e);
        } finally {
            if (collection != null && features != null) {
                collection.close(features);
            }
        }

    }

    /**
     * PreExecute function is called by the execute function before the rest is done.
     */
    public void preExecute() {
        return;
    }

    private Filter createFilterWithOnlyUsableFeatures(ArrayList<String> propertyNames) throws CQLException {
        String cqlFilter = getCqlFilterString();
        Filter filter = Filter.INCLUDE;
        if (propertyNames.size() > 0) {
            String newCqlFilter = "";
            for (int i = 0; i < propertyNames.size(); i++) {
                if (newCqlFilter.length() > 0) {
                    newCqlFilter += " and ";
                }
                newCqlFilter += propertyNames.get(i) + " is not null";
            }
            filter = CQL.toFilter(newCqlFilter);
        }
        return filter;
    }

    private ArrayList<String> getPropertyNamesInCql(String cqlFilter) {
        int beginIndex = cqlFilter.indexOf("[");
        int endIndex = cqlFilter.indexOf("]");
        ArrayList<String> propertyNames = new ArrayList();
        while (beginIndex >= 0) {
            if (endIndex > beginIndex) {
                String propName = cqlFilter.substring(beginIndex + 1, endIndex);
                if (!propertyNames.contains(propName)) {
                    propertyNames.add(propName);
                }
            }
            beginIndex = cqlFilter.indexOf("[", endIndex + 1);
            endIndex = cqlFilter.indexOf("]", endIndex + 1);
        }
        return propertyNames;
    }

    /*replace al the [value] with the correct value of the feature.*/
    public Filter createLineFilter(SimpleFeature feature, ArrayList<String> propertyNames) throws CQLException {
        String featureFilterString = new String(getCqlFilterString());
        //replace al the [value] with the correct value of the feature.
        for (int i = 0; i < propertyNames.size() && featureFilterString.indexOf("[") >= 0; i++) {
            if (featureFilterString.indexOf("[" + propertyNames.get(i) + "]") >= 0 && feature.getProperty(propertyNames.get(i)) != null) {
                String regExp = "\\[" + propertyNames.get(i) + "\\]";
                String value = null;
                if (feature.getProperty(propertyNames.get(i)).getValue() != null) {
                    value = feature.getProperty(propertyNames.get(i)).getValue().toString();
                }
                if (value != null) {
                    value = "'" + value + "'";
                    featureFilterString = featureFilterString.replaceAll(regExp, value);
                } else {
                    continue;
                }
            }
        }
        if (featureFilterString.indexOf("[") >= 0) {
            log.error("The CQL string is not correct: " + featureFilterString);
            return null;
        }
        //make the filter.
        return CQL.toFilter(featureFilterString);
    }

    /*check if the found lines can form a polygon. Remove the ones that are not.*/
    public ArrayList<SimpleFeature> filterInvalidLines(FeatureIterator lineFeatures) {
        ArrayList<SimpleFeature> correctLineFeatures = new ArrayList();
        while (lineFeatures.hasNext()) {
            SimpleFeature line = (SimpleFeature) lineFeatures.next();
            Geometry featureGeom = (Geometry) line.getDefaultGeometryProperty().getValue();
            boolean addLine = true;
            ArrayList<Integer> removeIndex = new ArrayList();
            for (int i = 0; i < correctLineFeatures.size() && addLine; i++) {
                Geometry lineGeom = (Geometry) correctLineFeatures.get(i).getDefaultGeometryProperty().getValue();
                //don't add if the geom is already added
                if (lineGeom.equals(featureGeom)) {
                    addLine = false;
                    //don't add if the line is crossing a already added line (otherwise we get a invalid polygon)
                } else if (featureGeom.crosses(lineGeom)) {
                    addLine = false;
                } //check for contains with a 0.5mm buffer (rounding problems with kadaster files.)
                else if (lineGeom.buffer(0.0005).contains(featureGeom)) {
                    addLine = false;
                } else if (featureGeom.buffer(0.0005).contains(lineGeom)) {
                    removeIndex.add(new Integer(i));
                }
            }
            if (addLine) {
                if (removeIndex.size() > 0) {
                    ArrayList<SimpleFeature> newCorrectLines = new ArrayList();
                    for (int i = 0; i < correctLineFeatures.size(); i++) {
                        boolean doAdd = true;
                        for (int r = 0; r < removeIndex.size() && doAdd; r++) {
                            if (i == removeIndex.get(r).intValue()) {
                                doAdd = false;
                            }
                        }
                        if (doAdd) {
                            newCorrectLines.add(correctLineFeatures.get(i));
                        }
                    }
                    correctLineFeatures = newCorrectLines;
                }
                correctLineFeatures.add(line);
            }
        }
        return correctLineFeatures;
    }

    public ArrayList<Polygon> createPolygonWithLines(ArrayList<SimpleFeature> lines){
        //merge the lines
        LineMerger merger = new LineMerger();
        for (int i = 0; i < lines.size(); i++) {
            Geometry lineGeom = (Geometry) lines.get(i).getDefaultGeometryProperty().getValue();
            merger.add(lineGeom);
        }
        //Create a polygon for every mergedlinestring
        Collection coll = merger.getMergedLineStrings();
        Iterator geomIt = coll.iterator();        
        ArrayList<Polygon> polygons = new ArrayList();
        while (geomIt.hasNext()) {
            Geometry linestring = (Geometry) geomIt.next();
            if (getLineCloseTolerance()!=0){
                if (linestring instanceof LineString)
                    linestring=closeLineString((LineString)linestring);
            }
            Polygonizer p = new Polygonizer();
            p.add(linestring);
            //do polygonize
            if (p.getPolygons().size() <= 0) {
                if (log.isDebugEnabled()){
                    log.debug("No polygon created with: "+linestring.toText()+" cause: \n");
                    if(p.getCutEdges().size()>0){
                        log.debug("Geometry has Cut Edges");
                    }if (p.getDangles().size()>0){
                        log.debug("Geometry has Dangles");
                    }if (p.getInvalidRingLines().size()>0){
                        log.debug("Geometry has Invalid Ring Lines");
                    }
                }
                continue;
            }
            polygons.addAll(p.getPolygons());
        }
        return polygons;
    }

    public Geometry closeLineString(LineString ls){
        if (ls.isClosed()){
            return ls;
        }
        Coordinate[] oriCoords=ls.getCoordinates();
        if(getLineCloseTolerance()<0||
                oriCoords[0].distance(oriCoords[oriCoords.length-1])<getLineCloseTolerance()){
            log.debug("Line not closed. Close the line with a tolerance of: "+getLineCloseTolerance());
            Coordinate newCoords[] = new Coordinate[oriCoords.length+1];
            for (int i=0; i < oriCoords.length; i++){
                newCoords[i]=oriCoords[i];
            }
            newCoords[newCoords.length-1]=oriCoords[0];
            GeometryFactory gf= new GeometryFactory(new PrecisionModel(),ls.getSRID());
            ls=gf.createLineString(newCoords);
        }
        return ls;
    }
    public SimpleFeatureType createNewFeatureType(SimpleFeatureType featureType, int attributeId, Class binding) {
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
        if (binding == Polygon.class) {
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + "_v");
        } else if (binding == MultiPolygon.class) {
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + "_mv");
        } else {
            featureTypeBuilder.setName(featureType.getName().getLocalPart() + POLYGONIZED);
        }
        return featureTypeBuilder.buildFeatureType();

    }

    public String getCqlFilterString() {
        return cqlFilterString;
    }

    public void setCqlFilterString(String cqlFilterString) {
        this.cqlFilterString = cqlFilterString;
    }

    public void setAttributeFeatureName(String attributeFeatureName) {
        this.attributeFeatureName = attributeFeatureName;
    }

    public String getAttributeFeatureName() {
        return attributeFeatureName;
    }

    public String getLineFeatureName() {
        return lineFeatureName;
    }

    public void setLineFeatureName(String lineFeatureName) {
        this.lineFeatureName = lineFeatureName;
    }

    public void setDataStore2Write(DataStore dataStore2Write) {
        this.dataStore2Write = dataStore2Write;
    }

    public DataStore getDataStore2Write() {
        return this.dataStore2Write;
    }

    public Boolean isPositivePolygon(Polygon polygon, ArrayList<SimpleFeature> correctLineFeatures, SimpleFeature feature) {
        return true;
    }
    public void setLineCloseTolerance(int lineCloseTolerance){
        this.lineCloseTolerance=lineCloseTolerance;
    }
    public double getLineCloseTolerance(){
        return this.lineCloseTolerance;
    }
    public static void main(String[] args) throws Exception {
        testMerge2();
        //System.out.println("boe");
    }
    public static void testMerge2() throws IOException, CQLException, Exception{
        Map params = new HashMap();
        params.put("host", "localhost");
        params.put("schema", "public");
        params.put("database", "uploadDL");
        params.put("dbtype", "postgis");
        params.put("user", "postgres");
        params.put("port", "5432");
        params.put("passwd", "***REMOVED***");
        params.put(ActionFactory.POLYGONIZEWITHATTR_LINEFEATURENAME_ATTRIBUTE,"bla");
        params.put(ActionFactory.POLYGONIZEWITHATTR_ATTRIBUTEFEATURENAME_ATTRIBUTE,"bla");
        String cqlFilter="(gemeentecode_perceel_links = 'AHM01' AND sectie_perceel_links = 'N' AND perceelnummer_perceel_links = '7508' AND indexnummer_perceel_links ='0000') OR (gemeentecode_perceel_rechts = 'AHM01' AND sectie_perceel_rechts = 'N' AND perceelnummer_perceel_rechts ='7508' AND indexnummer_perceel_rechts ='0000')";
        String featureType="arnhem_new_l";
        

        DataStore ds = DataStoreFinder.getDataStore(params);

        CollectionAction_PolygonizeSufLki polygonizer= new CollectionAction_PolygonizeSufLki(ds,params);
        
        FeatureSource fs=ds.getFeatureSource(featureType);
        FeatureCollection fc=fs.getFeatures(CQL.toFilter(cqlFilter));
        
        ArrayList<SimpleFeature> correctLineFeatures = polygonizer.filterInvalidLines(fc.features());
        ArrayList<Polygon> polygons=polygonizer.createPolygonWithLines(correctLineFeatures);
        System.out.println("polygons found: "+polygons.size());
    }
    public static void testMerge() throws ParseException{
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), 28992));
        ArrayList<Geometry> al = new ArrayList();
        /*al.add(wktreader.read("LINESTRING(27464.844 396139.529,27468.19 396139.65,27474.02 396140.659,27481.07 396141.77,27483.846 396143,27484.68 396143.37,27489.37 396146.62,27494.58 396150.38,27497.66 396151.77,27500.35 396152.66,27502.507 396152.997,27503.74 396153.19,27506.83 396153.36,27511.21 396153.12,27512.935 396152.811,27515.12 396152.42,27520.54 396150.98,27525.79 396149.21,27527.78 396148.25,27530.92 396146.43,27534.8 396144.51,27538.67 396142.55,27541.54 396140.69,27545.88 396139.27,27547.947 396138.934)"));
        al.add(wktreader.read("LINESTRING(27464.844 396139.529,27468.19 396139.65,27474.02 396140.659,27481.07 396141.77,27483.846 396143,27484.68 396143.37,27489.37 396146.62,27494.58 396150.38,27497.66 396151.77,27500.35 396152.66,27502.507 396152.997,27503.74 396153.19,27506.83 396153.36,27511.21 396153.12,27512.935 396152.811,27515.12 396152.42,27520.54 396150.98,27525.79 396149.21,27527.78 396148.25,27530.92 396146.43,27534.8 396144.51,27538.67 396142.55,27541.54 396140.69,27545.88 396139.27,27547.947 396138.934)"));
        al.add(wktreader.read("LINESTRING(27474.722 396048.875,27464.844 396139.529)"));
        al.add(wktreader.read("LINESTRING(27474.722 396048.875,27464.844 396139.529)"));
        al.add(wktreader.read("LINESTRING(27477.037 396027.631,27476.889 396028.993,27476.618 396031.478,27476.293 396034.461,27476.219 396035.144,27475.941 396037.691,27474.722 396048.875)"));
        al.add(wktreader.read("LINESTRING(27477.037 396027.631,27476.889 396028.993,27476.618 396031.478,27476.293 396034.461,27476.219 396035.144,27475.941 396037.691,27474.722 396048.875)"));
        al.add(wktreader.read("LINESTRING(27480.047 396000,27477.051 396027.502,27477.037 396027.631)"));
        al.add(wktreader.read("LINESTRING(27480.808 395993.019,27477.051 396027.502,27477.037 396027.631)"));
        al.add(wktreader.read("LINESTRING(27480.808 395993.019,27480.047 396000)"));
        al.add(wktreader.read("LINESTRING(27481.98 395982.144,27480.996 395991.301,27480.91 395992.087,27480.808 395993.019)"));
        al.add(wktreader.read("LINESTRING(27481.98 395982.144,27480.996 395991.301,27480.91 395992.087,27480.808 395993.019)"));
        al.add(wktreader.read("LINESTRING(27487.32 395932.43,27487.093 395934.542,27485.491 395949.456,27481.98 395982.144)"));
        al.add(wktreader.read("LINESTRING(27487.32 395932.43,27487.093 395934.542,27485.491 395949.456,27481.98 395982.144)"));
        al.add(wktreader.read("LINESTRING(27496.804 395844.146,27487.32 395932.43)"));
        al.add(wktreader.read("LINESTRING(27496.804 395844.146,27487.32 395932.43)"));
        al.add(wktreader.read("LINESTRING(27497.314 395839.332,27496.804 395844.146)"));
        al.add(wktreader.read("LINESTRING(27497.314 395839.332,27496.804 395844.146)"));
        al.add(wktreader.read("LINESTRING(27497.986 395839.127,27497.314 395839.332)"));
        al.add(wktreader.read("LINESTRING(27497.986 395839.127,27497.314 395839.332)"));
        al.add(wktreader.read("LINESTRING(27500.678 395838.59,27497.986 395839.127)"));
        al.add(wktreader.read("LINESTRING(27500.678 395838.59,27497.986 395839.127)"));
        al.add(wktreader.read("LINESTRING(27547.947 396138.934,27562.977 396000)"));
        al.add(wktreader.read("LINESTRING(27547.947 396138.934,27581.189 395831.661)"));
        al.add(wktreader.read("LINESTRING(27548.404 395837.161,27548.02 395837.22,27540 395837.69,27534.96 395837.9,27501.59 395838.52,27500.678 395838.59)"));
        al.add(wktreader.read("LINESTRING(27548.404 395837.161,27548.02 395837.22,27540 395837.69,27534.96 395837.9,27501.59 395838.52,27500.678 395838.59)"));
        al.add(wktreader.read("LINESTRING(27562.977 396000,27581.189 395831.661)"));
        al.add(wktreader.read("LINESTRING(27573.24 395833.2,27565.86 395834.56,27558.92 395835.78,27554.6 395836.52,27550.12 395836.9,27548.404 395837.161)"));
        al.add(wktreader.read("LINESTRING(27573.24 395833.2,27565.86 395834.56,27558.92 395835.78,27554.6 395836.52,27550.12 395836.9,27548.404 395837.161)"));
        al.add(wktreader.read("LINESTRING(27581.189 395831.661,27579.94 395831.96,27573.24 395833.2)"));
        al.add(wktreader.read("LINESTRING(27581.189 395831.661,27579.94 395831.96,27573.24 395833.2)"));*/
        al.add(wktreader.read("LINESTRING(27573.24 395833.2,27565.86 395834.56,27558.92 395835.78,27554.6 395836.52,27550.12 395836.9,27548.404 395837.161)"));//0
        al.add(wktreader.read("LINESTRING(27497.986 395839.127,27497.314 395839.332)"));//1
        al.add(wktreader.read("LINESTRING(27548.404 395837.161,27548.02 395837.22,27540 395837.69,27534.96 395837.9,27501.59 395838.52,27500.678 395838.59)"));//2
        al.add(wktreader.read("LINESTRING(27581.189 395831.661,27579.94 395831.96,27573.24 395833.2)"));//3
        al.add(wktreader.read("LINESTRING(27480.808 395993.019,27480.047 396000)"));//4
        al.add(wktreader.read("LINESTRING(27481.98 395982.144,27480.996 395991.301,27480.91 395992.087,27480.808 395993.019)"));//5
        al.add(wktreader.read("LINESTRING(27487.32 395932.43,27487.093 395934.542,27485.491 395949.456,27481.98 395982.144)"));//6
        al.add(wktreader.read("LINESTRING(27497.314 395839.332,27496.804 395844.146)"));//7
        al.add(wktreader.read("LINESTRING(27562.977 396000,27581.189 395831.661)"));//8
        al.add(wktreader.read("LINESTRING(27496.804 395844.146,27487.32 395932.43)"));//9
        al.add(wktreader.read("LINESTRING(27500.678 395838.59,27497.986 395839.127)"));//10
        al.add(wktreader.read("LINESTRING(27474.722 396048.875,27464.844 396139.529)"));//11
        al.add(wktreader.read("LINESTRING(27464.844 396139.529,27468.19 396139.65,27474.02 396140.659,27481.07 396141.77,27483.846 396143,27484.68 396143.37,27489.37 396146.62,27494.58 396150.38,27497.66 396151.77,27500.35 396152.66,27502.507 396152.997,27503.74 396153.19,27506.83 396153.36,27511.21 396153.12,27512.935 396152.811,27515.12 396152.42,27520.54 396150.98,27525.79 396149.21,27527.78 396148.25,27530.92 396146.43,27534.8 396144.51,27538.67 396142.55,27541.54 396140.69,27545.88 396139.27,27547.947 396138.934)"));//12
        al.add(wktreader.read("LINESTRING(27477.037 396027.631,27476.889 396028.993,27476.618 396031.478,27476.293 396034.461,27476.219 396035.144,27475.941 396037.691,27474.722 396048.875)"));//13
        al.add(wktreader.read("LINESTRING(27480.047 396000,27477.051 396027.502,27477.037 396027.631)"));//fout //14
        al.add(wktreader.read("LINESTRING(27547.947 396138.934,27562.977 396000)"));//15
        al.add(wktreader.read("LINESTRING(27573.24 395833.2,27565.86 395834.56,27558.92 395835.78,27554.6 395836.52,27550.12 395836.9,27548.404 395837.161)"));//16
        al.add(wktreader.read("LINESTRING(27497.986 395839.127,27497.314 395839.332)"));//17
        al.add(wktreader.read("LINESTRING(27548.404 395837.161,27548.02 395837.22,27540 395837.69,27534.96 395837.9,27501.59 395838.52,27500.678 395838.59)"));//18
        al.add(wktreader.read("LINESTRING(27581.189 395831.661,27579.94 395831.96,27573.24 395833.2)"));//19
        al.add(wktreader.read("LINESTRING(27474.722 396048.875,27464.844 396139.529)"));//20
        al.add(wktreader.read("LINESTRING(27464.844 396139.529,27468.19 396139.65,27474.02 396140.659,27481.07 396141.77,27483.846 396143,27484.68 396143.37,27489.37 396146.62,27494.58 396150.38,27497.66 396151.77,27500.35 396152.66,27502.507 396152.997,27503.74 396153.19,27506.83 396153.36,27511.21 396153.12,27512.935 396152.811,27515.12 396152.42,27520.54 396150.98,27525.79 396149.21,27527.78 396148.25,27530.92 396146.43,27534.8 396144.51,27538.67 396142.55,27541.54 396140.69,27545.88 396139.27,27547.947 396138.934)"));//21
        al.add(wktreader.read("LINESTRING(27477.037 396027.631,27476.889 396028.993,27476.618 396031.478,27476.293 396034.461,27476.219 396035.144,27475.941 396037.691,27474.722 396048.875)"));//22
        al.add(wktreader.read("LINESTRING(27480.808 395993.019,27477.051 396027.502,27477.037 396027.631)"));//goed//23
        al.add(wktreader.read("LINESTRING(27481.98 395982.144,27480.996 395991.301,27480.91 395992.087,27480.808 395993.019)"));//24
        al.add(wktreader.read("LINESTRING(27487.32 395932.43,27487.093 395934.542,27485.491 395949.456,27481.98 395982.144)"));//25
        al.add(wktreader.read("LINESTRING(27497.314 395839.332,27496.804 395844.146)"));//26
        al.add(wktreader.read("LINESTRING(27547.947 396138.934,27581.189 395831.661)"));//27
        al.add(wktreader.read("LINESTRING(27496.804 395844.146,27487.32 395932.43)"));//28
        al.add(wktreader.read("LINESTRING(27500.678 395838.59,27497.986 395839.127)"));//29


        ArrayList<Geometry> correctLines = new ArrayList();
        for (int c = 0; c < al.size(); c++) {
            Geometry featureGeom = al.get(c);
            boolean addLine = true;
            //int position=correctLines.size();
            ArrayList<Integer> removeIndex = new ArrayList();
            for (int i = 0; i < correctLines.size() && addLine; i++) {
                Geometry lineGeom = correctLines.get(i);
                /*System.out.println("CorrectLine"+lineGeom.toText());
                System.out.println("new Geom   "+featureGeom.toText());*/
                if (lineGeom.equals(featureGeom)) {
                    addLine = false;
                } else if (featureGeom.crosses(lineGeom)) {
                    addLine = false;
                } else if (lineGeom.buffer(0.0005).contains(featureGeom)) {
                    addLine = false;
                } else if (featureGeom.buffer(0.0005).contains(lineGeom)) {
                    removeIndex.add(new Integer(i));
                    //position=i;
                    //break;
                } else {
                }

            }
            if (addLine) {
                //System.out.println(featureGeom.toText());
                //System.out.println("Pos: "+position);
                if (removeIndex.size() > 0) {
                    ArrayList<Geometry> newCorrectLines = new ArrayList();
                    for (int i = 0; i < correctLines.size(); i++) {
                        boolean doAdd = true;
                        for (int r = 0; r < removeIndex.size() && doAdd; r++) {
                            if (i == removeIndex.get(r).intValue()) {
                                doAdd = false;
                            }
                        }
                        if (doAdd) {
                            newCorrectLines.add(correctLines.get(i));
                        }
                    }
                    correctLines = newCorrectLines;
                }
                correctLines.add(featureGeom);
            }
        }
        System.out.println("To linemerger: ");
        LineMerger merger = new LineMerger();
        for (int i = 0; i < correctLines.size(); i++) {
            Geometry lineGeom = correctLines.get(i);
            //System.out.println(lineGeom.toText());
            merger.add(lineGeom);
        }
        //Create a polygon for every mergedlinestring
        Collection coll = merger.getMergedLineStrings();
        Iterator geomIt = coll.iterator();
        Geometry geom = null;
        ArrayList<Polygon> polygons = new ArrayList();
        while (geomIt.hasNext()) {
            Geometry linestring = (Geometry) geomIt.next();
            System.out.println(linestring.toText());
            Polygonizer p = new Polygonizer();
            p.add(linestring);
            //do polygonize
            Iterator it = p.getPolygons().iterator();
            while (it.hasNext()) {
                System.out.println(((Geometry) it.next()).toText());
            }

        }
    }
}
