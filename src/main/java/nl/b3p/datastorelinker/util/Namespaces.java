/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.util;

import org.jdom.Namespace;

/**
 *
 * @author Erik van de Pol
 */
public class Namespaces {

    public static final String DSL_NAMESPACE_STRING = "http://www.b3partners.nl/schemas/dsl";
    public static final String DSL_PREFIX = "dsl";

    public static final Namespace DSL_NAMESPACE = 
            Namespace.getNamespace(DSL_NAMESPACE_STRING);

    public static final Namespace DSL_NAMESPACE_AND_PREFIX = 
            Namespace.getNamespace(DSL_PREFIX, DSL_NAMESPACE_STRING);
}
