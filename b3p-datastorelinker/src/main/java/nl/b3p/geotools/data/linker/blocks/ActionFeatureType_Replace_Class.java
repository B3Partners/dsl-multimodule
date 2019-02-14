/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import nl.b3p.geotools.data.linker.ActionFactory;
import nl.b3p.geotools.data.linker.FeatureException;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.poi.ExcelReader;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;


/**
 * Change a SimpleFeatureType class and try to cast the value
 *
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
        if (useGeometry && feature.getFeatureType().getGeometryDescriptor() != null) {
            attributeName = feature.getFeatureType().getGeometryDescriptor().getLocalName();
        }

        try {
            fixAttributeID(feature);
        } catch (FeatureException fex) {
            log.debug("Attribuut " + attributeName + " niet gevonden voor feature.");
            
            return feature;
        }        

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
                String value = null;

                if (feature.getAttribute(attributeID) != null) {
                    value = feature.getAttribute(attributeID).toString();
                }

                if (tryCast) {
                    feature.setAttributeDescriptor(attributeID, attributeName, newAttributeClass, false);

                    if (newAttributeClass.equals(Integer.class)) {
                        Integer waarde;

                        try {
                            waarde = (int) Double.parseDouble(value);
                        } catch (NumberFormatException nfe) {
                            waarde = 0;
                        }

                        feature.setAttribute(attributeID, waarde);

                    } else if (newAttributeClass.equals(String.class)) {

                        if (value == null || value.equals("")) {
                            feature.setAttribute(attributeID, null);
                        } else {
                            feature.setAttribute(attributeID, value);
                        }

                    } else if (newAttributeClass.equals(Double.class)) {

                        if (value == null) {
                            feature.setAttribute(attributeID, null);
                        } else {
                            feature.setAttribute(attributeID, Double.parseDouble(value));
                        }

                    } else if (newAttributeClass.equals(Float.class)) {

                        if (value == null) {
                            feature.setAttribute(attributeID, null);
                        } else {
                            feature.setAttribute(attributeID, Float.parseFloat(value));
                        }

                    } else if (newAttributeClass.equals(Date.class)) {

                        if (value == null || value.equals("")) {
                            feature.setAttribute(attributeID, null);
                        } else {
                            /* Geldige value 2013010100000000 */
                            if (value.length() == 16) {
                                String day = value.substring(7, 8);
                                String month = value.substring(5, 6);
                                String year = value.substring(0, 4);

                                value = day + "-" + month + "-" + year;
                            }

                            Locale loc = ExcelReader.LOCALE_NL;
                            String format = ExcelReader.DATE_FORMAT;
                            
                            Date date = new SimpleDateFormat(format, loc).parse(value);

                            feature.setAttribute(attributeID, date);
                        }

                    } else if (newAttributeClass.equals(Short.class)) {

                        if (value == null) {
                            feature.setAttribute(attributeID, null);
                        } else {
                            feature.setAttribute(attributeID, (short) Double.parseDouble(value));
                        }

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
        return "Verander kolomtype.";
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        constructors.add(Arrays.asList(new String[]{
            ActionFactory.ATTRIBUTE_NAME,
            ActionFactory.NEW_ATTRIBUTE_CLASS,
            ActionFactory.TRYCAST
        }));

        return constructors;
    }

    public String getDescription_NL() {
        return "Met deze Action kan bij een SimpleFeatureType de class van een attribuut worden gewijzigd";
    }

    @Override
    public void flush(Status status, Map properties) throws Exception {
    }
    
    @Override
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
    }
}
