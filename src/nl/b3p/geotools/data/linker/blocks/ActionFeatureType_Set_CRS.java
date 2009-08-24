package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.geotools.data.DataSourceException;
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
    }

    public ActionFeatureType_Set_CRS(HashMap params) {
        this.metadata = params;
        this.useSRS = false;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        CoordinateReferenceSystem crs = (useSRS ? loadSRS(srs) : loadMetaData());
        feature.setCRS(crs);

        return feature;
    }

    public static CoordinateReferenceSystem loadSRS(String srs) throws DataSourceException {
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
                throw new DataSourceException("Error parsing CoordinateSystem WKT: \"" + wkt + "\": " + e.getLocalizedMessage());
            }
        } else {
            throw new DataSourceException("Coordinatesystem csMetadata is empty");
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

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.SRS
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.PARAMS
                }));
*/
        return constructors;
    }
}
