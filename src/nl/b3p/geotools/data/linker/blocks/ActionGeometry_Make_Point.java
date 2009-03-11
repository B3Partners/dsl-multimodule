package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.Feature;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.AttributeType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.GeometryAttributeType;
import org.geotools.feature.SimpleFeature;
import org.geotools.feature.type.GeometricAttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class ActionGeometry_Make_Point extends Action {

    private int attributeIDY;
    private String attributeNameY;
    private boolean useID = true;
    private String srs;

    public ActionGeometry_Make_Point(int attributeIDX, int attributeIDY, String srs) {
        this.attributeID = attributeIDX;
        this.attributeIDY = attributeIDY;
        this.srs = srs;
    }

    public ActionGeometry_Make_Point(String attributeNameX, String attributeNameY, String srs) {
        this.attributeName = attributeNameX;
        this.attributeNameY = attributeNameY;
        this.useID = false;
        this.srs = srs;
    }

    public Feature execute(Feature feature) throws Exception {
        // Fix attributeIDs
        int attributeIDX = -1;
        if (useID) {
            attributeIDX = attributeID;

        } else {
            fixAttributeID(feature);
            attributeIDX = attributeID;

            attributeName = attributeNameY;
            attributeID = -1;
            fixAttributeID(feature);

            attributeIDY = attributeID;
        }

        Coordinate coord = new Coordinate(Double.parseDouble(feature.getAttribute(attributeIDX).toString()), Double.parseDouble(feature.getAttribute(attributeIDY).toString()));
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(coord);

        CoordinateReferenceSystem crs = ActionFeatureType_Set_CRS.loadSRS(srs);
        GeometricAttributeType geometryType = new GeometricAttributeType(
                THE_GEOM,
                Point.class,
                true,
                null,
                crs,
                null);

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());
        ftb.addType(geometryType);

        Object[] objects = new Object[feature.getFeatureType().getAttributeCount() +1];
        objects[objects.length -1] = point;
        
        for (int i = 0; i < feature.getFeatureType().getAttributeCount(); i++) {
            //simpleFeature.setAttribute(i, feature.getAttribute(i));
            objects[i] = feature.getAttribute(i);
        }
        
        return ftb.getFeatureType().create(objects);
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
