package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;
import org.geotools.feature.type.GeometricAttributeType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Set all attributeNames to upper- or lowercase
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_AttributeNames_Case extends Action {

    private boolean toUpper;

    public ActionFeatureType_AttributeNames_Case(boolean toUpper) {
        this.toUpper = toUpper;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {

        FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
        ftb.importType(feature.getFeatureType());

        int count = feature.getFeatureType().getAttributeCount();
        for (int i = 0; i < count; i++) {
            String newAttributeName = feature.getFeatureType().getAttributeType(i).getName();

            if (toUpper) {
                newAttributeName = newAttributeName.toUpperCase();
            } else {
                newAttributeName = newAttributeName.toLowerCase();
            }

            AttributeType type;
            if (feature.getFeatureType().getAttributeType(i).getClass().equals(GeometricAttributeType.class)) {

                CoordinateReferenceSystem crs = ((GeometricAttributeType)feature.getFeatureType().getAttributeType(i)).getCoordinateSystem();

                GeometryAttributeType gat = (GeometryAttributeType) feature.getFeatureType().getAttributeType(THE_GEOM);
                GeometricAttributeType geometryType = new GeometricAttributeType(
                        gat.getLocalName(),
                        feature.getFeatureType().getAttributeType(i).getType(),
                        feature.getFeatureType().getDefaultGeometry().isNillable(),
                        null,
                        crs,
                        null);

                ftb.setDefaultGeometry(gat);

                type = geometryType;
            //type = AttributeTypeFactory.newAttributeType(newAttributeName, feature.getFeatureType().getAttributeType(i).getType());
            } else {
                type = AttributeTypeFactory.newAttributeType(newAttributeName, feature.getFeatureType().getAttributeType(i).getType());
            }


            ftb.removeType(i);
            ftb.addType(i, type);
        }

        feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());

        return feature;
    }

    @Override
    public String toString() {
        return "Set typename to " + (toUpper ? "upper" : "lower") + "case";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.UPPERCASE
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan de naam van een attribuut in upper- of lowercase gezet worden";
    }
}
