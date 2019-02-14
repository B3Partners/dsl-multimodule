/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker.util;

import java.util.ResourceBundle;

/**
 *
 * @author Erik van de Pol
 */
public class LocalizationUtil {
    private static ResourceBundle resources =
            ResourceBundle.getBundle("nl.b3p.geotools.data.linker.resources.resources");

    public static ResourceBundle getResources() {
        return resources;
    }
}
