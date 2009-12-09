package nl.b3p.geotools.data.linker.feature;

import com.vividsolutions.jts.geom.Geometry;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class EasyFeature {
    private static final Log log = LogFactory.getLog(EasyFeature.class);

    private SimpleFeature feature;

    /**
     * Constructor of this easy feature, define the SimpleFeature here
     * @param feature The SimpleFeature to use
     */
    public EasyFeature(SimpleFeature feature) {
        this.feature = feature;
    }

    /**
     * Release a usable OpenGIS feature
     * @return The transformed feature
     */
    public SimpleFeature getFeature() {
        return feature;
    }

    /**
     * Get attribute using attributeID as location in attribute array
     * @param attributeID
     * @return
     */
    public AttributeType getAttributeType(int attributeID) {
        return feature.getFeatureType().getType(attributeID);
    }

    /**
     * Get attribute with given atrributename
     * @param name
     * @return
     */
    public AttributeType getAttributeType(String name) {
        return feature.getFeatureType().getType(name);
    }

    /**
     * Insert default attribute at a given postition, with name and classtype
     * @param attributeID
     * @param name
     * @param binding
     */
    public void insertAttributeDescriptor(int attributeID, String name, Class binding) {
        // Create new attribute
        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setName(name);
        attributeTypeBuilder.setBinding(binding);

        insertAttributeDescriptor(attributeID, attributeTypeBuilder.buildDescriptor(name));
    }

    /**
     * Used for extended AttributeType inserting. Build your own AttributeType instead of using the default method
     * @param attributeID
     * @param attributeDescriptor
     */
    public void insertAttributeDescriptor(int attributeID, AttributeDescriptor attributeDescriptor) {
        // Add attributeType to current attributeList
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(feature.getFeatureType().getAttributeDescriptors());
        attributeDescriptors.add(attributeID, attributeDescriptor);

        // Build FeatureType
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(feature.getFeatureType());
        featureTypeBuilder.setAttributes(attributeDescriptors);

        // Create new Feature
        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());

        // Create feature attributes list
        List<Object> attributes = feature.getAttributes();
        attributes.add(attributeID, null);

        // Build new feature with new values array
        feature = simpleFeatureBuilder.buildFeature(getID(), attributes.toArray(new Object[attributes.size()]));
    }

    /**
     * Used for extended AttributeType adding. Build your own AttributeType instead of using the default method
     * @param attributeDescriptor
     */
    public void addAttributeDescriptor(AttributeDescriptor attributeDescriptor) {
        insertAttributeDescriptor(feature.getAttributeCount(), attributeDescriptor);
    }

    public void addAttributeDescriptor(String name, Class binding) {
        insertAttributeDescriptor(feature.getAttributeCount(), name, binding);
    }

    /**
     * Remove AttributeType at attributeTypeID
     * @param attributeID
     */
    public void removeAttributeDescriptor(int attributeID) throws Exception {
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(feature.getFeatureType());

        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(feature.getFeatureType().getAttributeDescriptors());
        attributeDescriptors.remove(attributeID);
        featureTypeBuilder.setAttributes(attributeDescriptors);

        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());
        List<Object> attributes = feature.getAttributes();
        attributes.remove(attributeID);

        feature = simpleFeatureBuilder.buildFeature(getID(), attributes.toArray(new Object[attributes.size()]));
    }

    /**
     * Remove AttributeType by name
     * @param name
     */
    public void removeAttributeDescriptor(String name) throws Exception {
        removeAttributeDescriptor(getAttributeDescriptorIDbyName(name));
    }

    /**
     * Defaiult way to set AttributeType at specified attributeID, overwrites the current AttributeType at that index
     * @param attributeID
     * @param name
     * @param binding
     */
    public void setAttributeDescriptor(int attributeID, String name, Class binding, boolean keepValue) {
        // Create new attribute
        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setName(name);
        attributeTypeBuilder.setBinding(binding);

        setAttributeDescriptor(attributeID, attributeTypeBuilder.buildDescriptor(name), keepValue);
    }

    /**
     * Extended way to set a AttributeType; overwrites the current AttributeType at that index
     * @param attributeID
     * @param attributeDescriptor
     */
    public void setAttributeDescriptor(int attributeID, AttributeDescriptor attributeDescriptor, boolean keepValue) {
        // Add attributeType to current attributeList
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(feature.getFeatureType().getAttributeDescriptors());
        attributeDescriptors.set(attributeID, attributeDescriptor);

        // Build FeatureType
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.init(feature.getFeatureType());
        featureTypeBuilder.setAttributes(attributeDescriptors);

        // Create new Feature
        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());

        // Create feature attributes list
        List<Object> attributes = feature.getAttributes();
        if (!keepValue) {
            attributes.set(attributeID, null);
        }

        feature = simpleFeatureBuilder.buildFeature(getID(), attributes.toArray(new Object[attributes.size()]));
    }

    public void setAttributeDescriptor(String attributeName, AttributeDescriptor attributeDescriptor) throws Exception {
        if (containsAttributeDescriptor(attributeName)) {
            int attributeID = getAttributeDescriptorIDbyName(attributeName);
            removeAttributeDescriptor(attributeName);
            insertAttributeDescriptor(attributeID, attributeDescriptor);
        } else {
            addAttributeDescriptor(attributeDescriptor);
        }
    }

    /**
     * Lookup attributeID of AttributeType name
     * @param name
     * @return
     * @throws java.lang.Exception
     */
    public int getAttributeDescriptorIDbyName(String name) throws Exception {
        List<AttributeDescriptor> attributeDescriptors = feature.getFeatureType().getAttributeDescriptors();
        for (int i = 0; i < attributeDescriptors.size(); i++) {
            if (attributeDescriptors.get(i).getLocalName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new Exception("Unable to locate attributeID of '" + name + "' " + toString());
    }

    /**
     * Get name of AttributeType at given position
     * @param attributeID
     * @return
     * @throws java.lang.Exception
     */
    public String getAttributeDescriptorNameByID(int attributeID) throws Exception {
        if (attributeID < 0 || attributeID >= getAttributeCount()) {
            throw new Exception("AttributeType attributeID " + attributeID + " not allowed");
        }
        return feature.getFeatureType().getAttributeDescriptors().get(attributeID).getLocalName();
    }

    /**
     * Get number of Attributes
     * @return
     */
    public int getAttributeCount() {
        return feature.getAttributeCount();
    }

    /**
     * Set Attribute at a specified position
     * @param attributeID
     * @param attribute
     */
    public void setAttribute(int attributeID, Object attribute) {
        feature.setAttribute(attributeID, attribute);
    }

    /**
     * Set Attribute at a specified name
     * @param name
     * @param attribute
     */
    public void setAttribute(String name, Object attribute) {
        feature.setAttribute(name, attribute);
    }

    public Object getAttribute(int attributeID) {
        return feature.getAttribute(attributeID);
    }

    public Object getAttribute(String name) {
        return feature.getAttribute(name);
    }

    public List<Object> getAttributes() {
        return feature.getAttributes();
    }

    @Override
    public String toString() {
        String attributes = "";
        for (AttributeType attribute : feature.getFeatureType().getTypes()) {
            attributes += ", " + attribute.getName().toString();
        }

        if (attributes.length() > 2) {
            attributes = attributes.substring(2);
        } else {
            attributes = "[empty]";
        }

        return "Feature(" + attributes + ")";
    }

    public String getTypeName() {
        return feature.getFeatureType().getTypeName();
    }

    public void setTypeName(String name) {
        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.addAll(feature.getFeatureType().getAttributeDescriptors());
        featureTypeBuilder.setName(name);

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());
        feature = featureBuilder.buildFeature(feature.getID(), feature.getAttributes().toArray(new Object[feature.getAttributeCount()]));
    }

    public String getID() {
        return feature.getID();
    }

    public SimpleFeatureType getFeatureType() {
        return feature.getFeatureType();
    }

    /**
     * Check if attributeID is legal (above zero and above attributeCount)
     * @param attributeID
     * @return
     */
    public boolean containsAttributeDescriptor(int attributeID) {
        return (attributeID >= 0) && (attributeID < getAttributeCount());
    }

    public boolean containsAttributeDescriptor(String attributeName) {
        for (AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors()) {
            if (descriptor.getName().getLocalPart().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    public static AttributeDescriptor buildGeometryAttributeDescriptor(String attributeName, Class binding, boolean isNillable, CoordinateReferenceSystem crs) {
        AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();
        attributeTypeBuilder.setBinding(binding);
        attributeTypeBuilder.setCRS(crs);
        attributeTypeBuilder.setName(attributeName);
        attributeTypeBuilder.setNillable(isNillable);

        // Prevent warnings; save as VARCHAR(256)
        attributeTypeBuilder.setLength(256);

        return attributeTypeBuilder.buildDescriptor(attributeName);
    }

    public void setCRS(CoordinateReferenceSystem crs) throws Exception {
        String geometryName = feature.getDefaultGeometryProperty().getDescriptor().getLocalName();
        boolean isNillable = feature.getFeatureType().getGeometryDescriptor().isNillable();
        Class binding = feature.getFeatureType().getGeometryDescriptor().getType().getBinding();
        int attributeID = getAttributeDescriptorIDbyName(geometryName);

        // Cache current geometry value
        Geometry geom = (Geometry) getAttribute(geometryName);

        // Remove geometryColumn
        removeAttributeDescriptor(geometryName);

        // Create new geometryColumn with previous settings
        insertAttributeDescriptor(attributeID, buildGeometryAttributeDescriptor(geometryName, binding, isNillable, crs));

        // Set cached geometry back
        setAttribute(geometryName, geom);
    }
}
