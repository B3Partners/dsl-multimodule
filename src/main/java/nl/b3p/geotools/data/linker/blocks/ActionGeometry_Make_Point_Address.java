package nl.b3p.geotools.data.linker.blocks;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.Status;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.util.PDOKSearchClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author Boy de Wit
 */
public class ActionGeometry_Make_Point_Address extends Action {

    private int attributeIDAddres1;
    private int attributeIDAddres2;
    private int attributeIDAddres3;
    private int attributeIDCity;

    private String attributeNameAddres1;
    private String attributeNameAddres2;
    private String attributeNameAddres3;
    private String attributeNameCity;
    private String projectie;

    private boolean useID = true;
    private String srs;

    public ActionGeometry_Make_Point_Address(int attributeIDAddres1, int attributeIDAddres2,
                                             int attributeIDAddres3, int attributeIDCity, String projectie) {

        this.attributeIDAddres1 = attributeIDAddres1;
        this.attributeIDAddres2 = attributeIDAddres2;
        this.attributeIDAddres3 = attributeIDAddres3;
        this.attributeIDCity = attributeIDCity;
        this.srs = projectie;
    }

    public ActionGeometry_Make_Point_Address(String attributeNameAddres1, String attributeNameAddres2,
                                             String attributeNameAddres3, String attributeNameCity, String projectie) {

        this.attributeNameAddres1 = attributeNameAddres1;
        this.attributeNameAddres2 = attributeNameAddres2;
        this.attributeNameAddres3 = attributeNameAddres3;
        this.attributeNameCity = attributeNameCity;
        this.srs = projectie;

        this.useID = false;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {

        attributeIDAddres1 = -1;
        attributeIDAddres2 = -1;
        attributeIDAddres3 = -1;
        attributeIDCity = -1;

        if (!useID) {
            if (attributeNameAddres1 != null && !attributeNameAddres1.equals(""))
                attributeIDAddres1 = feature.getAttributeDescriptorIDbyName(attributeNameAddres1);

            if (attributeNameAddres2 != null && !attributeNameAddres2.equals(""))
                attributeIDAddres2 = feature.getAttributeDescriptorIDbyName(attributeNameAddres2);

            if (attributeNameAddres3 != null && !attributeNameAddres3.equals(""))
                attributeIDAddres3 = feature.getAttributeDescriptorIDbyName(attributeNameAddres3);

            if (attributeNameCity != null && !attributeNameCity.equals(""))
                attributeIDCity = feature.getAttributeDescriptorIDbyName(attributeNameCity);
        }

        EasyFeature f = setGeomToNewPoint(feature);

        return f;
    }

    public String toString() {
        return "";
    }

    public String getDescription_NL() {
        return "Adres omzetten naar een Point geometrie.";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                ActionFactory.ATTRIBUTE_NAME_ADDRESS1,
                ActionFactory.ATTRIBUTE_NAME_ADDRESS2,
                ActionFactory.ATTRIBUTE_NAME_ADDRESS3,
                ActionFactory.ATTRIBUTE_NAME_CITY,
                ActionFactory.SRS
        }));

        return constructors;
    }

    private static double fixDecimals(String value) {
        value = value.trim();
        if (value.contains(",")) {
            if (value.contains(".")) {
                value = value.replaceAll("[.]", "");
            }
            value = value.replaceAll("[,]", ".");
        }
        return Double.parseDouble(value);
    }

    private EasyFeature setGeomToNewPoint(EasyFeature feature) throws Exception {

        // Retrieve geometryColumn name
        String geometryDescriptorName = Action.THE_GEOM;
        if (feature.getFeatureType().getGeometryDescriptor() != null) {
            geometryDescriptorName = feature.getFeatureType().getGeometryDescriptor().getName().getLocalPart();
        }

        String address1 = "";
        String address2 = "";
        String address3 = "";
        String city = "";

        // variable to track down NullPointerException
        String current_attribute = "";
        try {
            if (attributeIDAddres1 > 0) {
                current_attribute = "address1";
                address1 = feature.getAttribute(attributeIDAddres1).toString();
            }
            if (attributeIDAddres2 > 0) {
                current_attribute = "address2";
                address2 = feature.getAttribute(attributeIDAddres2).toString();
            }
            if (attributeIDAddres3 > 0) {
                current_attribute = "address3";
                address3 = feature.getAttribute(attributeIDAddres3).toString();
            }
            if (attributeIDCity > 0) {
                current_attribute = "city";
                city = feature.getAttribute(attributeIDCity).toString();
            }
        } catch (NullPointerException e) {
            String errorMessage = current_attribute +  " attribute for selected feature was null, can't geolocate!";
            log.error(errorMessage);
            throw new Exception(errorMessage);

        }
        Point point = null;

        try {
            point = convertAddressToPointPDOK(address1 + " " + address2 + " " + address3 + " " + city);
        } catch (Exception ex) {
            throw new Exception(ex);
        }

        if (point != null) {
            feature.setAttribute(geometryDescriptorName, point);
        }

        return feature;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }


    private static Geometry createGeomFromWKTString(String wktstring) throws Exception {
        WKTReader wktreader = new WKTReader(new GeometryFactory(new PrecisionModel(), 28992));
        try {
            return wktreader.read(wktstring);
        } catch (ParseException ex) {
            throw new Exception(ex);
        }
    }


    private String createAlternateUrl(String address) throws Exception {

        String url = null;

        String baseUrl = "http://bag42.nl/api/v0/geocode/json?maxitems=1&address=";
        String encodedParams = null;

        try {
            encodedParams = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new Exception(ex);
        }

        url = baseUrl + encodedParams;

        return url;
    }

    private String createGisGraphyurl() {
        String url = null;

        return url;
    }


    private Point convertAddressToPointPDOK(String address) throws Exception {
        Point point = null;
        try {

            PDOKSearchClient pdokSearchClient = new PDOKSearchClient();
            JSONObject result = pdokSearchClient.search(address + " AND type:adres");


            if (result != null) {
                String pointString = (String) result.get("centroide_rd");

                Geometry sourceGeometry = createGeomFromWKTString(pointString);
                point = sourceGeometry.getCentroid();
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        }


        return point;
    }


    @Override
    public void flush(Status status, Map properties) throws Exception {
    }

    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
