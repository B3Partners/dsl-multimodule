/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.opengis.feature.type.GeometryDescriptor;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Buffer geometries (make them thicker).
 *
 * @author Gertjan Al, B3Partners
 * @mprins
 */
public class ActionGeometry_Buffer extends Action {

    private double bufferSize;

    public ActionGeometry_Buffer(double bufferSize) {
        this.bufferSize = bufferSize;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (bufferSize < 0) {
            throw new Exception("Buffersize is " + bufferSize + "; must be zero or higher");
        }

        GeometryDescriptor gd = feature.getFeatureType().getGeometryDescriptor();
        if (gd == null) {
            return feature;
        }
        attributeName = gd.getName().getLocalPart();
        fixAttributeID(feature);

        // Get current geometry
        Geometry geometry = (Geometry) feature.getAttribute(attributeName);

        // Buffer geometry to polygon
        geometry = BufferOp.bufferOp(geometry, bufferSize);

        // Change AttributeType to Polygon
        Action action = new ActionFeatureType_Replace_Class(attributeName, Polygon.class, false);
        action.execute(feature);

        // Save buffered geometry
        feature.setAttribute(attributeName, geometry);

        return feature;
    }

    public String toString() {
        return "Buffer geometry to '" + bufferSize + "'";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.BUFFERSIZE
                }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeature de geometrie worden aangepast door bijvoorbeeld een lijn om te zetten in een dikkere lijn. De lijn zal worden omgezet in een vlak";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}

