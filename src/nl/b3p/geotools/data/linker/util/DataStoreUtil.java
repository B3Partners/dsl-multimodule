/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import nl.b3p.geotools.data.linker.DataStoreLinker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

/**
 *
 * @author Erik van de Pol
 */
public class DataStoreUtil {
    protected static Log log = LogFactory.getLog(DataStoreUtil.class);

    public static DataTypeList getDataTypeList(Map<String, Object> inputParameters) throws Exception {
        List<String> good = new ArrayList<String>();
        List<String> bad = new ArrayList<String>();

        DataStore dataStore = DataStoreLinker.openDataStore(inputParameters);

        if (dataStore == null) {
            return null;
        }

        for (String typename : dataStore.getTypeNames()) {
            FeatureIterator iterator = null;
            try {
                iterator = dataStore.getFeatureSource(typename).getFeatures().features();
                if (iterator.hasNext()) {
                    Feature feature = iterator.next();
                    if (feature != null) {
                        good.add(typename);
                        //typenameMap.put(typename, translateFeature(feature));
                    }
                }

            } catch (DataSourceException e) {
                bad.add(typename);
                log.error("Error reading features, cause: " + e.getLocalizedMessage());

            } catch (NoSuchElementException e) {
                bad.add(typename);
                log.warn("Table '" + typename + "' contains unsupported datatypes? " + e.getLocalizedMessage());

            } catch (Exception e) {
                bad.add(typename);
                log.warn("Table '" + typename + "' contains error: " + e.getLocalizedMessage());

            } finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        }
        dataStore.dispose();

        return new DataTypeList(good, bad);
    }
}