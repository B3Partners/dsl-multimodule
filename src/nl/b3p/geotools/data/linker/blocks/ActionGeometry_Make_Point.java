package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.AttributeDescriptor;

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

    public EasyFeature execute(EasyFeature feature) throws Exception {
        int attributeIDX = -1;

        if (useID) {
            attributeIDX = attributeID;

        } else {
            attributeIDX = feature.getAttributeDescriptorIDbyName(attributeName);
            attributeIDY = feature.getAttributeDescriptorIDbyName(attributeNameY);
        }

        // Retrieve geometryColumn name
        String geometryDescriptorName = Action.THE_GEOM;
        if (feature.getFeatureType().getGeometryDescriptor() != null) {
            geometryDescriptorName = feature.getFeatureType().getGeometryDescriptor().getName().getLocalPart();
        }

        // Create or replace geometry AttributeType
        AttributeDescriptor geometryAttributeType = EasyFeature.buildGeometryAttributeDescriptor(geometryDescriptorName, Point.class, feature.getFeature().isNillable(), CRS.decode(srs));
        feature.setAttributeDescriptor(geometryDescriptorName, geometryAttributeType);


        if (feature.getAttribute(attributeIDX) == null || feature.getAttribute(attributeIDY) == null) {
            log.warn("Unable to create point with " + toString());

        } else {
            // Point Geometry Creation
            Coordinate coord = new Coordinate(Double.parseDouble(feature.getAttribute(attributeIDX).toString()), Double.parseDouble(feature.getAttribute(attributeIDY).toString()));

            GeometryFactory gf = new GeometryFactory();
            Point point = gf.createPoint(coord);

            // Set attribute
            feature.setAttribute(geometryDescriptorName, point);
        }

        return feature;
    }

    public String toString() {
        return "Create geometry point(" + (attributeName.equals("") ? attributeID : attributeName) + ", " + (attributeNameY.equals("") ? attributeIDY : attributeNameY) + ")";
    }

    public String getDescription_NL() {
        return "Maak van twee attributen een punt; bijvoorbeeld van COORD_X en COORD_Y";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID_X,
                    ActionFactory.ATTRIBUTE_ID_Y,
                    ActionFactory.SRS
                }));
*/
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME_X,
                    ActionFactory.ATTRIBUTE_NAME_Y,
                    ActionFactory.SRS
                }));

        return constructors;
    }
}
