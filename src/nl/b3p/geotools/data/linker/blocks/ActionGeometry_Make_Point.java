package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.Feature;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.SimpleFeature;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class ActionGeometry_Make_Point extends Action {

    private int attributeIDY;
    private String attributeNameY;

    public ActionGeometry_Make_Point(int attributeIDX, int attributeIDY) {
        this.attributeID = attributeIDX;
        this.attributeIDY = attributeIDY;
    }

    public ActionGeometry_Make_Point(String attributeNameX, String attributeNameY) {
        this.attributeName = attributeNameX;
        this.attributeNameY = attributeNameY;
    }

    public Feature execute(Feature feature) throws Exception {
        // Fix attributeIDs
        fixAttributeID(feature);
        int attributeIDX = attributeID;
        attributeID = attributeIDY;
        attributeName = attributeNameY;
        fixAttributeID(feature);
        attributeIDY = attributeID;

        Coordinate coord = new Coordinate(Double.parseDouble(feature.getAttribute(attributeIDX).toString()), Double.parseDouble(feature.getAttribute(attributeIDY).toString()));
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(coord);
        SimpleFeature simpleFeature = (SimpleFeature) feature;

        simpleFeature.setAttribute(THE_GEOM, point);
        return simpleFeature;
    }

    public String toString() {
        return "Create geometry point(" + (attributeName.equals("") ? attributeID : attributeName) + ", " + (attributeNameY.equals("") ? attributeIDY : attributeNameY) + ")";
    }

    public String getDescription_NL() {
        return "Maak van twee attributen een punt; bijvoorbeeld van COORD_X en COORD_Y";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_ID_X,
                        ActionFactory.ATTRIBUTE_ID_Y
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_NAME_X,
                        ActionFactory.ATTRIBUTE_NAME_Y
                    }
                };
    }
}
