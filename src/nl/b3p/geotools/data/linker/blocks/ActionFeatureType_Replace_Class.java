/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 * Change a SimpleFeatureType class and try to cast the value
 * @author Gertjan Al, B3Partners
 */
public class ActionFeatureType_Replace_Class extends Action {

    private Class newAttributeClass;
    private boolean tryCast;
    private boolean useGeometry = false;

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

    public ActionFeatureType_Replace_Class(Class newAttributeClass, boolean tryCast) {
        this.newAttributeClass = newAttributeClass;
        this.tryCast = tryCast;
        useGeometry = true;
    }

    @Override
    public EasyFeature execute(EasyFeature feature) throws Exception {
        if (useGeometry) {
            attributeName = feature.getFeatureType().getGeometryDescriptor().getLocalName();
        }

        fixAttributeID(feature);

        if (feature.containsAttributeDescriptor(attributeID)) {
            boolean hasGeometry = feature.getFeatureType().getGeometryDescriptor() != null;
            boolean isGeometry = false;

            if (hasGeometry) {
                String geometryColumn = feature.getFeatureType().getGeometryDescriptor().getName().getLocalPart();

                if (attributeName.equals(geometryColumn)) {
                    feature.setAttributeDescriptor(attributeID, EasyFeature.buildGeometryAttributeDescriptor(attributeName, newAttributeClass, feature.getFeature().isNillable(), feature.getFeature().getFeatureType().getCoordinateReferenceSystem()), true);
                    isGeometry = true;
                }
            }

            if (!isGeometry) {
                String value = feature.getAttribute(attributeID).toString();

                if (tryCast) {
                    feature.setAttributeDescriptor(attributeID, attributeName, newAttributeClass, false);

                    if (newAttributeClass.equals(Integer.class)) {
                        
                        Integer waarde = null;
                        try {
                            waarde = Integer.parseInt(value);
                        } catch (NumberFormatException nfe) {
                            waarde = 0;
                        }
                        
                        feature.setAttribute(attributeID, waarde);

                    } else if (newAttributeClass.equals(String.class)) {
                        feature.setAttribute(attributeID, value);

                    } else if (newAttributeClass.equals(Double.class)) {
                        feature.setAttribute(attributeID, Double.parseDouble(value));

                    } else if (newAttributeClass.equals(Short.class)) {
                        feature.setAttribute(attributeID, Short.parseShort(value));

                    } else {
                        feature.setAttribute(attributeID, value);
                    }
                }
            }
        }
        return feature;
    }

    @Override
    public String toString() {
        String findAttribute = (useGeometry ? "geometry class" : "class  at '" + (attributeID == -1 ? attributeName : attributeID) + "'");
        return "Change " + findAttribute + " to \"" + newAttributeClass.getSimpleName() + "\"";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_NAME,
                    ActionFactory.NEW_ATTRIBUTE_CLASS,
                    ActionFactory.TRYCAST
                }));
/*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.ATTRIBUTE_ID,
                    ActionFactory.NEW_ATTRIBUTE_CLASS,
                    ActionFactory.TRYCAST
                }));
*/
        /*
        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.NEW_ATTRIBUTE_CLASS,
                    ActionFactory.TRYCAST
                }));
*/
        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de class van een attribuut worden gewijzigd";
    }
}
