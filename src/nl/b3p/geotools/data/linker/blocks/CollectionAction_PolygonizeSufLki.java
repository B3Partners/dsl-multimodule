package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;
import nl.b3p.suf2.records.SUF2Record03;
import nl.b3p.suf2.records.SUF2Record06;
/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 11-mrt-2010, 10:01:19
 * 
 */
public class CollectionAction_PolygonizeSufLki extends CollectionAction_PolygonizeWithAttr {
    protected static final Log log = LogFactory.getLog(CollectionAction_PolygonizeSufLki.class);
    
    public CollectionAction_PolygonizeSufLki(DataStore dataStore2Write,Map properties) throws Exception{
        super(dataStore2Write,properties);
        if (this.getCqlFilterString()==null){
            StringBuffer cqlFilter=new StringBuffer();
            cqlFilter.append("(");
            cqlFilter.append(SUF2Record03.GEMEENTECODEPERCEELLINKS);
            cqlFilter.append(" = ["+SUF2Record06.GEMEENTECODE+"] AND ");
            cqlFilter.append(SUF2Record03.SECTIEPERCEELLINKS);
            cqlFilter.append(" = ["+SUF2Record06.SECTIE+"] AND ");
            cqlFilter.append(SUF2Record03.PERCEELNUMMERLINKS);
            cqlFilter.append(" = ["+SUF2Record06.PERCEELNUMMER+"] AND ");
            cqlFilter.append(SUF2Record03.INDEXLETTERPERCEELLINKS);
            cqlFilter.append(" =["+SUF2Record06.INDEXLETTER+"] AND ");
            cqlFilter.append(SUF2Record03.INDEXNUMMERLINKS);
            cqlFilter.append(" =["+SUF2Record06.INDEXNUMMER+"]) OR (");
            cqlFilter.append(SUF2Record03.GEMEENTECODEPERCEELRECHTS);
            cqlFilter.append(" = ["+SUF2Record06.GEMEENTECODE+"] AND ");
            cqlFilter.append(SUF2Record03.SECTIEPERCEELRECHTS);
            cqlFilter.append(" = ["+SUF2Record06.SECTIE+"] AND ");
            cqlFilter.append(SUF2Record03.PERCEELNUMMERRECHTS);
            cqlFilter.append(" =["+SUF2Record06.PERCEELNUMMER+"] AND ");
            cqlFilter.append(SUF2Record03.INDEXLETTERPERCEELRECHTS);
            cqlFilter.append(" =["+SUF2Record06.INDEXLETTER+"] AND ");
            cqlFilter.append(SUF2Record03.INDEXNUMMERRECHTS);
            cqlFilter.append(" =["+SUF2Record06.INDEXNUMMER+"])");
            setCqlFilterString(cqlFilter.toString());
        }
    /*    if (ActionFactory.propertyCheck(properties, ActionFactory.POLYGONIZEWITHATTR_CQLFILTER_ATTRIBUTE)) {
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
        }*/
    }
    public Boolean isPositivePolygon(Polygon polygon, ArrayList<SimpleFeature> correctLineFeatures,SimpleFeature feature) {
        //get the perceelNummer to determine if the line is right or left oriented.
        String perceelNummer=feature.getAttribute(SUF2Record06.PERCEELNUMMER).toString();
        //get the coords of the polygon.
        Coordinate[] polyCoords= polygon.getCoordinates();
        for (int l=0; l < correctLineFeatures.size(); l++){
            //get the line geom
            SimpleFeature lineFeature=correctLineFeatures.get(l);
            Geometry lineGeom=(Geometry) lineFeature.getDefaultGeometryProperty().getValue();
            //does the linegeom touches the polygon?
            if (lineGeom.touches(polygon)){
                Coordinate[] lineCoords=lineGeom.getCoordinates();
                for (int pc=0; pc < polyCoords.length; pc++){
                    if (polyCoords[pc].equals(lineCoords[0])){
                        int beforeIndex=pc-1;
                        if (beforeIndex<0){
                            beforeIndex=polyCoords.length-1;
                        }
                        int afterIndex=pc+1;
                        if (afterIndex >= polyCoords.length){
                            afterIndex=0;
                        }

                        if (lineCoords[1].equals(polyCoords[afterIndex])){
                            if (lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERLINKS)!=null &&
                                    lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERLINKS).toString().equals(perceelNummer)){
                                return false;
                            }else if (lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERRECHTS)!=null &&
                                    lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERRECHTS).toString().equals(perceelNummer)){
                                return true;
                            }
                        }else if (lineCoords[1].equals(polyCoords[beforeIndex])){
                            if (lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERLINKS)!=null &&
                                    lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERLINKS).toString().equals(perceelNummer)){
                                return true;
                            }else if (lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERRECHTS)!=null &&
                                    lineFeature.getAttribute(SUF2Record03.PERCEELNUMMERRECHTS).toString().equals(perceelNummer)){
                                return false;
                            }

                        }
                    }
                }
            }
        }
        return null;
    }
}
