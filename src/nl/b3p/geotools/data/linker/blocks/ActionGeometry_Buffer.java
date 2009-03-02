/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.buffer.BufferBuilder;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;
import org.geotools.feature.type.GeometricAttributeType;

/**
 * Buffer geometries (make them thicker)
 * @author Gertjan Al, B3Partners
 */
public class ActionGeometry_Buffer extends Action {

    int bufferSize;

    public ActionGeometry_Buffer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.attributeName = THE_GEOM;
    }

    public Feature execute(Feature feature) throws Exception {
        if (bufferSize <= 0) {
            throw new Exception("Buffersize is " + bufferSize + "; must be above zero");
        }

        // Allow use of attributeID
        fixAttributeID(feature);

        // Buffer current geometry
        Geometry geometry = (Geometry) feature.getAttribute(THE_GEOM);

        BufferBuilder dhe = new BufferBuilder();
        geometry = dhe.buffer(geometry, bufferSize);


        // Update FeatureType
        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());

        GeometryAttributeType geometryAttributeType = new GeometricAttributeType(
                THE_GEOM,
                Polygon.class,
                feature.getFeatureType().getDefaultGeometry().isNillable(),
                null,
                feature.getFeatureType().getDefaultGeometry().getCoordinateSystem(),
                feature.getFeatureType().getDefaultGeometry().getRestriction());

        // Replace current geometryType with new Geometry (Polygon)
        ftb.setDefaultGeometry(geometryAttributeType);
        ftb.removeType(attributeID);
        ftb.addType(attributeID, geometryAttributeType);

        // Set current feature geometry to null
        SimpleFeature simpleFeature = (SimpleFeature) feature;
        simpleFeature.setAttribute(THE_GEOM, null);

        // Build new feature with featureType
        feature = ftb.getFeatureType().create(simpleFeature.getAttributes(null), feature.getID());

        // Set new geometry in new feature
        simpleFeature = (SimpleFeature) feature;
        simpleFeature.setAttribute(THE_GEOM, geometry);

        return simpleFeature;
    }

    public String toString() {
        return "Buffer geometry to '" + bufferSize + "'";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.BUFFERSIZE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een feature de geometrie worden aangepast door bijvoorbeeld een lijn om te zetten in een dikkere lijn. De lijn zal worden omgezet in een vlak";
    }
}

