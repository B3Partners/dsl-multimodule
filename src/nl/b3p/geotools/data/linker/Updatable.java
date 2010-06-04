/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker;

/**
 *
 * @author Erik van de Pol
 */
public interface Updatable {
    public void update(int featuresProcessed, int totalNumberOfFeatures);
}
