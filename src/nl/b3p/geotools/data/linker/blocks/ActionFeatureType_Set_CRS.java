package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.Geometry;
import java.util.HashMap;
import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.data.DataSourceException;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.GeometryAttributeType;
import org.geotools.feature.type.GeometricAttributeType;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Set_CRS extends Action {

    private HashMap metadata;
    private String srs;
    private boolean useSRS = true;

    public ActionFeatureType_Set_CRS(String srs) {
        this.srs = srs;
        attributeName = THE_GEOM;
    }

    public ActionFeatureType_Set_CRS(HashMap params) {
        this.metadata = params;
        this.useSRS = false;
        attributeName = THE_GEOM;
    }

    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        CoordinateReferenceSystem crs = (useSRS ? loadSRS() : loadMetaData());
        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());

        GeometryAttributeType gat = (GeometryAttributeType) feature.getFeatureType().getAttributeType(THE_GEOM);
        GeometricAttributeType geometryType = new GeometricAttributeType(
                gat.getLocalName(),
                Geometry.class,
                feature.getFeatureType().getDefaultGeometry().isNillable(),
                null,
                crs,
                null);

        ftb.importType(feature.getFeatureType());
        ftb.setDefaultGeometry(geometryType);
        ftb.removeType(attributeID);
        ftb.addType(attributeID, geometryType);

        return ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());
    }

    public CoordinateReferenceSystem loadSRS() throws DataSourceException {
        // Override srs when provided
        if (srs != null) {
            try {
                return CRS.decode(srs);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem srs: \"" + srs + "\"");
            }
        } else {
            throw new DataSourceException("CoordinateSystem srs can not be null");
        }
    }

    public CoordinateReferenceSystem loadMetaData() throws DataSourceException {

        String[] csMetadata = (String[]) metadata.values().toArray(new String[metadata.size()]);
        if (csMetadata != null) {
            String wkt = csMetadata[0];
            try {
                // Parse WKT
                CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
                return crsFactory.createFromWKT(wkt);
            } catch (Exception e) {
                throw new DataSourceException("Error parsing CoordinateSystem WKT: \"" + wkt + "\"");
            }
        } else {
            throw new DataSourceException("coordinatesystem csMetadata is empty");
        }
    }

    public String toString() {
        if (useSRS) {
            return "Set SRS to " + srs;
        } else {
            return "Set CRS using " + metadata.toString();
        }
    }

    public String getDescription_NL() {
        return "Stel een ander SRS in (standaard EPSG:28992)";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.SRS
                    }, new String[]{
                        ActionFactory.PARAMS
                    }
                };
    }
}
