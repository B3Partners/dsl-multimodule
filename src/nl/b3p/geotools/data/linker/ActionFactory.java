/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import nl.b3p.geotools.data.linker.blocks.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create actionBlocks for given Classname and parameters (properties)
 * @author Gertjan Al, B3Partners
 */
public class ActionFactory {

    public static final String ATTRIBUTE_ID = "attribute_id";
    public static final String ATTRIBUTE_ID_X = "attribute_id_x";
    public static final String ATTRIBUTE_ID_Y = "attribute_id_y";
    public static final String ATTRIBUTE_CLASS = "attribute_class";
    public static final String NEW_ATTRIBUTE_CLASS = "new_attribute_class";
    public static final String ATTRIBUTE_NAME = "attribute_name";
    public static final String ATTRIBUTE_NAME_X = "attribute_name_x";
    public static final String ATTRIBUTE_NAME_Y = "attribute_name_y";
    public static final String NEW_ATTRIBUTE_NAME = "new_attribute_name";
    public static final String NEW_TYPENAME = "new_typename";
    public static final String PARAMS = "params";
    public static final String APPEND = "append";
    public static final String DROPFIRST = "drop";
    public static final String TRYCAST = "trycast";
    public static final String OBJECT_FIND = "object_find";
    public static final String OBJECT_REPLACE = "object_replace";
    public static final String BUFFERSIZE = "buffersize";
    public static final String UPPERCASE = "uppercase";
    public static final String BEGIN_INDEX = "beginIndex";
    public static final String END_INDEX = "endIndex";
    public static final String COMPARE_TYPE = "compare";
    public static final String LENGTH = "length";
    public static final String REVERSE = "reverse";
    public static final String SRS = "srs";
    public static final String URL = "url";
    public static final String SKIPFAILURES = "skip_failures";
    public static final Log log = LogFactory.getLog(DataStoreLinker.class);

    public static Action createAction(String actionClassName, Map<String, Object> properties) throws Exception {

        /**
         * Create ActionCombo_GeometrySplitter_Writer
         */
        if (isThisClass(actionClassName, ActionCombo_GeometrySplitter_Writer.class)) {
            if (propertyCheck(properties, PARAMS, APPEND, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionCombo_GeometrySplitter_Writer(params, append, dropFirst);

            } else if (propertyCheck(properties, PARAMS, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionCombo_GeometrySplitter_Writer(params, false, dropFirst);

            } else if (propertyCheck(properties, PARAMS, APPEND)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionCombo_GeometrySplitter_Writer(params, append, false);

            } else {
                failedConstructor(ActionCombo_GeometrySplitter_Writer.class, properties);
            }




        } else if (isThisClass(actionClassName, ActionCombo_GeometrySingle_Writer.class)) {
            if (propertyCheck(properties, PARAMS, APPEND, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionCombo_GeometrySingle_Writer(params, append, dropFirst);

            } else if (propertyCheck(properties, PARAMS, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionCombo_GeometrySingle_Writer(params, false, dropFirst);

            } else if (propertyCheck(properties, PARAMS, APPEND)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionCombo_GeometrySingle_Writer(params, append, false);

            } else {
                failedConstructor(ActionCombo_GeometrySingle_Writer.class, properties);
            }




        /**
         * Create ActionDataStore_Writer
         */
        } else if (isThisClass(actionClassName, ActionDataStore_Writer.class)) {
            if (propertyCheck(properties, PARAMS, APPEND, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionDataStore_Writer(params, append, dropFirst);

            } else if (propertyCheck(properties, PARAMS, DROPFIRST)) {
                Map params = (Map) properties.get(PARAMS);
                boolean dropFirst = (Boolean) properties.get(DROPFIRST);
                return new ActionDataStore_Writer(params, false, dropFirst);

            } else if (propertyCheck(properties, PARAMS, APPEND)) {
                Map params = (Map) properties.get(PARAMS);
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionDataStore_Writer(params, append, false);

            } else if (propertyCheck(properties, PARAMS)) {
                Map params = (Map) properties.get(PARAMS);
                return new ActionDataStore_Writer(params);

            } else {
                failedConstructor(ActionDataStore_Writer.class, properties);
            }



        /**
         * Create ActionFeatureType_Attribute_Insert
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Attribute_Insert.class)) {
            if (propertyCheck(properties, ATTRIBUTE_NAME, ATTRIBUTE_CLASS, ATTRIBUTE_ID)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                Class attributeClass = (Class) toClass((String) properties.get(ATTRIBUTE_CLASS));
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                return new ActionFeatureType_Attribute_Insert(attributeName, attributeClass, attributeID);

            } else if (propertyCheck(properties, ATTRIBUTE_NAME, ATTRIBUTE_CLASS)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                Class attributeClass = (Class) toClass((String) properties.get(ATTRIBUTE_CLASS));
                return new ActionFeatureType_Attribute_Insert(attributeName, attributeClass);

            } else {
                failedConstructor(ActionFeatureType_Attribute_Insert.class, properties);
            }



        /**
         * Create ActionFeatureType_Attribute_Remove
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Attribute_Remove.class)) {
            if (propertyCheck(properties, ATTRIBUTE_ID)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                return new ActionFeatureType_Attribute_Remove(attributeID);

            } else if (propertyCheck(properties, ATTRIBUTE_NAME)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                return new ActionFeatureType_Attribute_Remove(attributeName);

            } else {
                failedConstructor(ActionFeatureType_Attribute_Remove.class, properties);
            }



        /**
         * Create ActionFeatureType_Replace_Class
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Replace_Class.class)) {
            if (propertyCheck(properties, ATTRIBUTE_NAME, NEW_ATTRIBUTE_CLASS, TRYCAST)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                Class newAttributeClass = toClass((String) properties.get(NEW_ATTRIBUTE_CLASS));
                boolean tryCast = (Boolean) properties.get(TRYCAST);
                return new ActionFeatureType_Replace_Class(attributeName, newAttributeClass, tryCast);

            } else if (propertyCheck(properties, ATTRIBUTE_ID, NEW_ATTRIBUTE_CLASS, TRYCAST)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                Class newAttributeClass = (Class) toClass((String) properties.get(NEW_ATTRIBUTE_CLASS));
                boolean tryCast = (Boolean) properties.get(TRYCAST);
                return new ActionFeatureType_Replace_Class(attributeID, newAttributeClass, tryCast);

            } else {
                failedConstructor(ActionFeatureType_Replace_Class.class, properties);
            }



        /**
         * Create ActionFeatureType_Replace_Name
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Replace_Name.class)) {
            if (propertyCheck(properties, ATTRIBUTE_NAME, NEW_ATTRIBUTE_NAME)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                String newAttributeName = (String) properties.get(NEW_ATTRIBUTE_NAME);
                return new ActionFeatureType_Replace_Name(attributeName, newAttributeName);

            } else if (propertyCheck(properties, ATTRIBUTE_ID, NEW_ATTRIBUTE_NAME)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                String newAttributeName = (String) properties.get(NEW_ATTRIBUTE_NAME);
                return new ActionFeatureType_Replace_Name(attributeID, newAttributeName);

            } else {
                failedConstructor(ActionFeatureType_Replace_Name.class, properties);
            }



        /**
         * Create ActionFeatureType_Typename_Update
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Typename_Update.class)) {
            if (propertyCheck(properties, NEW_TYPENAME, APPEND)) {
                String newTypeName = (String) properties.get(NEW_TYPENAME);
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionFeatureType_Typename_Update(newTypeName, append);

            } else if (propertyCheck(properties, NEW_TYPENAME)) {
                String newTypeName = (String) properties.get(NEW_TYPENAME);
                return new ActionFeatureType_Typename_Update(newTypeName);

            } else {
                failedConstructor(ActionFeatureType_Typename_Update.class, properties);
            }



        /**
         * Create ActionFeature_Value_Replace
         */
        } else if (isThisClass(actionClassName, ActionFeature_Value_Replace.class)) {
            if (propertyCheck(properties, OBJECT_FIND, OBJECT_REPLACE)) {
                Object find = createObject((HashMap) properties.get(OBJECT_FIND));
                Object replace = createObject((HashMap) properties.get(OBJECT_REPLACE));
                return new ActionFeature_Value_Replace(find, replace);

            } else {
                failedConstructor(ActionFeature_Value_Replace.class, properties);
            }



        /**
         * Create ActionFeature_Value_Set
         */
        } else if (isThisClass(actionClassName, ActionFeature_Value_Set.class)) {
            if (propertyCheck(properties, ATTRIBUTE_NAME, OBJECT_REPLACE, APPEND)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                Object replace = createObject((HashMap) properties.get(OBJECT_REPLACE));
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionFeature_Value_Set(attributeName, replace, append);

            } else if (propertyCheck(properties, ATTRIBUTE_ID, OBJECT_REPLACE, APPEND)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                Object replace = createObject((HashMap) properties.get(OBJECT_REPLACE));
                boolean append = (Boolean) properties.get(APPEND);
                return new ActionFeature_Value_Set(attributeID, replace, append);

            } else if (propertyCheck(properties, ATTRIBUTE_NAME, OBJECT_REPLACE)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                Object replace = createObject((HashMap) properties.get(OBJECT_REPLACE));
                return new ActionFeature_Value_Set(attributeName, replace);

            } else if (propertyCheck(properties, ATTRIBUTE_ID, OBJECT_REPLACE)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                Object replace = createObject((HashMap) properties.get(OBJECT_REPLACE));
                return new ActionFeature_Value_Set(attributeID, replace);

            } else {
                failedConstructor(ActionFeature_Value_Set.class, properties);
            }



        /**
         * Create ActionGeometry_Buffer
         */
        } else if (isThisClass(actionClassName, ActionGeometry_Buffer.class)) {
            if (propertyCheck(properties, BUFFERSIZE)) {
                int bufferSize = toInteger((String) properties.get(BUFFERSIZE));

                return new ActionGeometry_Buffer(bufferSize);
            } else {
                failedConstructor(ActionGeometry_Buffer.class, properties);
            }


        /**
         * Create ActionFeatureType_Typename_Case
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Typename_Case.class)) {
            if (propertyCheck(properties, UPPERCASE)) {
                boolean toUppercase = (Boolean) properties.get(UPPERCASE);

                return new ActionFeatureType_Typename_Case(toUppercase);
            } else {
                failedConstructor(ActionFeatureType_Typename_Case.class, properties);
            }


        /**
         * Create ActionFeatureType_Typename_Substring
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Typename_Substring.class)) {
            if (propertyCheck(properties, BEGIN_INDEX, END_INDEX)) {
                int beginIndex = toInteger((String) properties.get(BEGIN_INDEX));
                int endIndex = toInteger((String) properties.get(END_INDEX));

                return new ActionFeatureType_Typename_Substring(beginIndex, endIndex);
            } else if (propertyCheck(properties, BEGIN_INDEX)) {
                int beginIndex = toInteger((String) properties.get(BEGIN_INDEX));

                return new ActionFeatureType_Typename_Substring(beginIndex);
            } else if (propertyCheck(properties, LENGTH, REVERSE)) {
                int length = toInteger((String) properties.get(LENGTH));
                boolean reverse = (Boolean) properties.get(REVERSE);

                return new ActionFeatureType_Typename_Substring(length, reverse);
            } else {
                failedConstructor(ActionFeatureType_Typename_Substring.class, properties);
            }


        /**
         * Create ActionCombo_Fix_To_Oracle
         */
        } else if (isThisClass(actionClassName, ActionCombo_Fix_To_Oracle.class)) {
            return new ActionCombo_Fix_To_Oracle();



        /**
         * Create ActionCombo_Fix_From_Oracle
         */
        } else if (isThisClass(actionClassName, ActionCombo_Fix_From_Oracle.class)) {
            return new ActionCombo_Fix_From_Oracle();





        } else if (isThisClass(actionClassName, ActionFeatureType_Set_CRS.class)) {
            if (propertyCheck(properties, SRS)) {
                String srs = (String) properties.get(SRS);

                return new ActionFeatureType_Set_CRS(srs);
            } else if (propertyCheck(properties, PARAMS)) {
                HashMap metadata = (HashMap) properties.get(PARAMS);

                return new ActionFeatureType_Set_CRS(metadata);
            } else {
                failedConstructor(ActionFeatureType_Set_CRS.class, properties);
            }





        } else if (isThisClass(actionClassName, ActionFeatureType_AttributeName_Case.class)) {
            if (propertyCheck(properties, ATTRIBUTE_ID, UPPERCASE)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                boolean toUppercase = (Boolean) properties.get(UPPERCASE);

                return new ActionFeatureType_AttributeName_Case(attributeID, toUppercase);
            } else if (propertyCheck(properties, ATTRIBUTE_NAME, UPPERCASE)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                boolean toUppercase = (Boolean) properties.get(UPPERCASE);

                return new ActionFeatureType_AttributeName_Case(attributeName, toUppercase);
            } else {
                failedConstructor(ActionFeatureType_AttributeName_Case.class, properties);
            }





        } else if (isThisClass(actionClassName, ActionFeatureType_AttributeNames_Case.class)) {
            if (propertyCheck(properties, UPPERCASE)) {
                boolean toUppercase = (Boolean) properties.get(UPPERCASE);

                return new ActionFeatureType_AttributeNames_Case(toUppercase);
            } else {
                failedConstructor(ActionFeatureType_AttributeName_Case.class, properties);
            }


        /**
         * ActionFeatureType_Typename_AppendAttribute
         */
        } else if (isThisClass(actionClassName, ActionFeatureType_Typename_AppendAttribute.class)) {
            if (propertyCheck(properties, ATTRIBUTE_ID, LENGTH)) {
                int attributeID = toInteger((String) properties.get(ATTRIBUTE_ID));
                int maxLength = toInteger((String) properties.get(LENGTH));

                return new ActionFeatureType_Typename_AppendAttribute(attributeID, maxLength);
            } else if (propertyCheck(properties, ATTRIBUTE_NAME, LENGTH)) {
                String attributeName = (String) properties.get(ATTRIBUTE_NAME);
                int maxLength = toInteger((String) properties.get(LENGTH));

                return new ActionFeatureType_Typename_AppendAttribute(attributeName, maxLength);
            } else {
                failedConstructor(ActionFeatureType_Typename_AppendAttribute.class, properties);
            }


        /**
         * ActionGeometry_Make_Point
         */
        } else if (isThisClass(actionClassName, ActionGeometry_Make_Point.class)) {
            if (propertyCheck(properties, ATTRIBUTE_ID_X, ATTRIBUTE_ID_Y)) {
                int attributeIDx = toInteger((String) properties.get(ATTRIBUTE_ID_X));
                int attributeIDy = toInteger((String) properties.get(ATTRIBUTE_ID_Y));

                return new ActionGeometry_Make_Point(attributeIDx, attributeIDy);
            } else if (propertyCheck(properties, ATTRIBUTE_NAME_X, ATTRIBUTE_NAME_Y)) {
                String attributeNamex = (String) properties.get(ATTRIBUTE_NAME_X);
                String attributeNamey = (String) properties.get(ATTRIBUTE_NAME_X);

                return new ActionGeometry_Make_Point(attributeNamex, attributeNamey);
            } else {
                failedConstructor(ActionFeatureType_Typename_AppendAttribute.class, properties);
            }


        /**
         * Action not found
         */
        } else {
            throw new UnsupportedOperationException(actionClassName + " is not yet implemented in ActionFactory");
        }

        return null;
    }

    /**
     * Check if properties map contains all properties wanted
     */
    private static boolean propertyCheck(Map properties, String... find) {
        boolean found = true;

        for (String prop : find) {

            // Check if map contains property
            if (!properties.containsKey(prop)) {

                /*
                // Check if class in propertyMap is right
                if (properties.get(prop) != null) {

                // Check if class is defined for this property
                if (mapping.containsKey(prop)) {
                boolean equals = properties.get(prop).getClass().equals(mapping.get(prop));
                boolean instanceOf = properties.get(prop).getClass().isInstance(mapping.get(prop));

                if (!equals && !instanceOf) {
                found = false;
                }
                }
                }
                } else {
                 */
                found = false;
            }
        }

        return found;
    }

    /**
     * Check if given class is equal to string
     */
    private static boolean isThisClass(String actionClassName, Class checkClass) {
        return actionClassName.equals(checkClass.getSimpleName());
    }

    /**
     *  Constructing the action failed. This function helps the user resolve the problem (find missing parameters)
     */
    public static void failedConstructor(Class actionClass, Map properties) throws Exception {
        Method[] methods = actionClass.getMethods();
        boolean found = false;
        String[][] constructors = null;

        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals("getConstructors")) {
                constructors = (String[][]) methods[i].invoke(actionClass);
                found = (constructors != null);
                break;
            }
        }

        if (found) {
            String constructorString = "";
            // Strip { and }
            String propertyString = properties.toString().substring(1, properties.toString().length() - 1).replaceAll(", ", "\n");

            for (int i = 0; i < constructors.length; i++) {
                String missing = "";
                constructorString += " " + actionClass.getSimpleName() + "(";
                for (int j = 0; j < constructors[i].length; j++) {
                    constructorString += constructors[i][j] + (j != constructors[i].length - 1 ? ", " : "");
                    missing += (!properties.containsKey(constructors[i][j]) ? " " + constructors[i][j] : "");
                }
                constructorString += ")\n - Missing parameter(s):" + missing + "\n\n";
                if (missing.equals("")) {
                    constructorString += " * Parameters appear to be fine for this action.\n   Go to class ActionFactory > createAction(...) > " + actionClass.getSimpleName() + "\n" +
                            "   Function propertyCheck(...) probably has wrong parameters.";
                }
            }

            throw new Exception("\nFailed to create " + actionClass.getClass().getSimpleName() + " using properties:\n" + propertyString + "\n\nAllowed constructors:\n" + constructorString + "\n\n");
        } else {
            throw new Exception("Class " + actionClass.getClass().getSimpleName() + " not supported; method getConstructors() not found");
        }
    }

    private static Object createObject(Map<String, String> map) throws Exception {
        final String CLASS = "class";
        final String VALUE = "value";

        final String STRING = "java.lang.String";
        final String DOUBLE = "java.lang.Double";
        final String INTEGER = "java.lang.Integer";

        if (map.containsKey(CLASS)) {
            if (map.containsKey(VALUE)) {
                String value = map.get(VALUE);

                if (map.get(CLASS).equals("null")) {
                    return null;

                } else if (map.get(CLASS).equals(STRING)) {
                    return new String(value);

                } else if (map.get(CLASS).equals(DOUBLE)) {
                    return Double.parseDouble(value);

                } else if (map.get(CLASS).equals(INTEGER)) {
                    return Integer.parseInt(value);
                }

            } else {
                return (Object) toClass(map.get(CLASS));
            }

        } else {
            throw new Exception("No objectClass specified: " + map.toString());
        }

        return null;
    }

    public static Class toClass(String className) throws Exception {
        if (className.equalsIgnoreCase("null")) {
            return null;

        } else {
            try {
                return Class.forName(className);
            } catch (Exception ex) {
                throw new Exception("String \'" + className + "' could not be converted to class");
            }
        }
    }

    public static boolean toBoolean(String value) throws Exception {
        if (value.equalsIgnoreCase("true")) {
            return new Boolean(true);

        } else if (value.equalsIgnoreCase("false")) {
            return new Boolean(false);

        } else {
            throw new Exception("String \'" + value + "' could not be converted to boolean");
        }
    }

    public static int toInteger(String value) {
        return Integer.parseInt(value);
    }

    public static ActionCondition.CompareType toCompareType(String value) {
        return ActionCondition.CompareType.byString(value);
    }

    /*
     * The following function
    public static String[][] getConstructors(Class clazz) throws Exception {
    if (clazz.equals(ActionCombo_Fix_From_Oracle.class)) {
    return ActionCombo_Fix_From_Oracle.getConstructors();

    } else if (clazz.equals(ActionCombo_Fix_To_Oracle.class)) {
    return ActionCombo_Fix_To_Oracle.getConstructors();

    } else if (clazz.equals(ActionCombo_GeometrySplitter_Writer.class)) {
    return ActionCombo_GeometrySplitter_Writer.getConstructors();

    } else if (clazz.equals(ActionCombo_GeometrySingle_Writer.class)) {
    return ActionCombo_GeometrySingle_Writer.getConstructors();

    } else if (clazz.equals(ActionCondition_FeatureType_Class.class)) {
    return ActionCondition_FeatureType_Class.getConstructors();

    } else if (clazz.equals(ActionCondition_FeatureType_Typename_Length.class)) {
    return ActionCondition_FeatureType_Typename_Length.getConstructors();

    } else if (clazz.equals(ActionCondition_FeatureType_Value.class)) {
    return ActionCondition_FeatureType_Value.getConstructors();

    } else if (clazz.equals(ActionCondition_Feature_Class.class)) {
    return ActionCondition_Feature_Class.getConstructors();

    } else if (clazz.equals(ActionCondition_Feature_Value.class)) {
    return ActionCondition_Feature_Value.getConstructors();

    } else if (clazz.equals(ActionDataStore_Writer.class)) {
    return ActionDataStore_Writer.getConstructors();

    } else if (clazz.equals(ActionFeatureType_AttributeName_Case.class)) {
    return ActionFeatureType_AttributeName_Case.getConstructors();

    } else if (clazz.equals(ActionFeatureType_AttributeNames_Case.class)) {
    return ActionFeatureType_AttributeNames_Case.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Attribute_Insert.class)) {
    return ActionFeatureType_Attribute_Insert.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Attribute_Remove.class)) {
    return ActionFeatureType_Attribute_Remove.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Replace_Class.class)) {
    return ActionFeatureType_Replace_Class.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Replace_Class_All.class)) {
    return ActionFeatureType_Replace_Class_All.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Replace_Name.class)) {
    return ActionFeatureType_Replace_Name.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Set_CRS.class)) {
    return ActionFeatureType_Set_CRS.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Typename_Update.class)) {
    return ActionFeatureType_Typename_Update.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Typename_Case.class)) {
    return ActionFeatureType_Typename_Case.getConstructors();

    } else if (clazz.equals(ActionFeatureType_Typename_Substring.class)) {
    return ActionFeatureType_Typename_Substring.getConstructors();

    } else if (clazz.equals(ActionFeature_Value_Replace.class)) {
    return ActionFeature_Value_Replace.getConstructors();

    } else if (clazz.equals(ActionFeature_Value_Set.class)) {
    return ActionFeature_Value_Set.getConstructors();

    } else if (clazz.equals(ActionGeometry_Buffer.class)) {
    return ActionGeometry_Buffer.getConstructors();

    } else {
    throw new Exception(clazz.getSimpleName() + " has not yet been implemented in Actionfactory.getConstructors()");
    }
    }
     */
}
