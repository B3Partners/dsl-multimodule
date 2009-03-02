/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.ActionFactory;
import org.geotools.feature.*;
import org.geotools.feature.type.GeometricAttributeType;

/**
 * Change a featureType class and try to cast the value
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Replace_Class extends Action {

    private Class newAttributeClass;
    private boolean tryCast;

    public ActionFeatureType_Replace_Class(String attributeName, Class newAttributeClass, boolean tryCast) {
        this.attributeName = attributeName;
        this.newAttributeClass = newAttributeClass;
        this.tryCast = tryCast;
    }

    public ActionFeatureType_Replace_Class(int attributeID, Class newAttributeClass, boolean tryCast) {
        this.attributeID = attributeID;
        this.newAttributeClass = newAttributeClass;
        this.tryCast = tryCast;
    }

    @Override
    public Feature execute(Feature feature) throws Exception {
        fixAttributeID(feature);

        if (hasLegalAttributeID(feature)) {
            if (feature.getFeatureType().getAttributeType(attributeID).getClass().equals(GeometricAttributeType.class)) {
                FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
                ftb.importType(feature.getFeatureType());

                GeometryAttributeType gat = (GeometryAttributeType) feature.getFeatureType().getAttributeType(attributeID);
                GeometricAttributeType geometryType = new GeometricAttributeType(
                        gat.getLocalName(),
                        newAttributeClass,
                        feature.getFeatureType().getDefaultGeometry().isNillable(),
                        null,
                        gat.getCoordinateSystem(),
                        null);

                ftb.setDefaultGeometry(geometryType);
                ftb.removeType(attributeID);
                ftb.addType(attributeID, geometryType);

                feature = ftb.getFeatureType().create(feature.getAttributes(null), feature.getID());


            } else {
                // Do something else
                FeatureTypeBuilder ftb = FeatureTypeBuilder.newInstance(feature.getFeatureType().getTypeName());
                ftb.importType(feature.getFeatureType());

                String oldAttributeName = feature.getFeatureType().getAttributeType(attributeID).getName();
                AttributeType attributeType = AttributeTypeFactory.newAttributeType(oldAttributeName, newAttributeClass);

                ftb.removeType(attributeID);
                ftb.addType(attributeID, attributeType);

                if (!tryCast) {
                    feature = ftb.getFeatureType().create(clearAttribute(feature, attributeID), feature.getID());
                } else {
                    feature = ftb.getFeatureType().create(tryCast(feature, attributeID, newAttributeClass), feature.getID());
                }
            }
        }

        return feature;
    }

    protected Object[] clearAttribute(Feature feature, int attributeID) {
        Object[] newAttributes = feature.getAttributes(null);

        if (attributeID >= 0 && attributeID < newAttributes.length) {
            newAttributes[attributeID] = null;
        }
        return newAttributes;
    }

    protected Object[] tryCast(Feature feature, int clearID, Class castClass) {
        Object[] newAttributes = feature.getAttributes(null);

        if (attributeID >= 0 && attributeID < newAttributes.length) {
            try {
                if (castClass.equals(String.class)) {
                    newAttributes[attributeID] = newAttributes[attributeID].toString();

                } else if (castClass.equals(Integer.class)) {
                    newAttributes[attributeID] = Integer.parseInt(newAttributes[attributeID].toString());

                } else if (castClass.equals(Double.class)) {
                    newAttributes[attributeID] = Double.parseDouble(newAttributes[attributeID].toString());

                } else if (castClass.equals(Short.class)) {
                    newAttributes[attributeID] = Short.parseShort(newAttributes[attributeID].toString());

                } else {
                    String info = "Unknown casting '" + newAttributes[attributeID].getClass().getSimpleName() + "' to '" + castClass.getSimpleName() + "' failed. Consider FeatureType_Replace_Class without tryCast";

                    log.info(info);
                    throw new Exception(info);
                }

            } catch (Exception ex) {
                throw new ClassCastException("Casting '" + newAttributes[attributeID].getClass().getSimpleName() + "' to '" + castClass.getSimpleName() + "' failed. Consider FeatureType_Replace_Class without tryCast");
            }
        }
        return newAttributes;
    }

    @Override
    public String toString() {
        return "Change class  at '" + (attributeID == -1 ? attributeName : attributeID) + "' to \"" + newAttributeClass.getSimpleName() + "\"";
    }

    public static String[][] getConstructors() {
        return new String[][]{
                    new String[]{
                        ActionFactory.ATTRIBUTE_NAME,
                        ActionFactory.NEW_ATTRIBUTE_CLASS,
                        ActionFactory.TRYCAST
                    }, new String[]{
                        ActionFactory.ATTRIBUTE_ID,
                        ActionFactory.NEW_ATTRIBUTE_CLASS,
                        ActionFactory.TRYCAST
                    }
                };
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een featureType de class van een attribuut worden gewijzigd";
    }
}
