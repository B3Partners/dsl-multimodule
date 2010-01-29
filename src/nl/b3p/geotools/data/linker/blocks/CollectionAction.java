/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker.blocks;

import java.util.ArrayList;
import java.util.Collection;
import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.feature.FeatureCollection;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 12-jan-2010, 11:41:49
 */
public abstract class CollectionAction {
    abstract public void execute(FeatureCollection collection, Action writer);

    protected String geometryCollectionToWKTString(Collection c){
        if (c==null)
            return null;
        return c.toString().replaceAll(", LINESTRING"," LINESTRING").replaceAll("\\[","").replaceAll("\\]","");
    }
}
