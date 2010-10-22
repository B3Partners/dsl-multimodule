/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker;

/**
 *
 * @author Erik van de Pol
 */
public class FeatureException extends Exception {

    public FeatureException(Throwable cause) {
        super(cause);
    }

    public FeatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeatureException(String message) {
        super(message);
    }

    public FeatureException() {
    }
    
}
