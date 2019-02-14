/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker;

/**
 *
 * @author Erik van de Pol
 */
public class ConfigurationException extends Exception {

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException() {
    }
    
}
