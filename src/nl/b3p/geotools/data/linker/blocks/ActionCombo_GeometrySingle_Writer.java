package nl.b3p.geotools.data.linker.blocks;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.b3p.geotools.data.linker.Status;
import nl.b3p.geotools.data.linker.feature.EasyFeature;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public class ActionCombo_GeometrySingle_Writer extends ActionCombo implements WritableAction {
    public final ActionDataStore_Writer datastore;
    public ActionCombo_GeometrySingle_Writer(Map params, Map properties) {

        datastore = new ActionDataStore_Writer(new HashMap(params), new HashMap(properties));

        ActionCondition_Feature_Class condition_P = new ActionCondition_Feature_Class(Point.class);
        ActionCondition_Feature_Class condition_L = new ActionCondition_Feature_Class(LineString.class);
        ActionCondition_Feature_Class condition_V = new ActionCondition_Feature_Class(Polygon.class);

        ActionCondition_Feature_Class condition_MP = new ActionCondition_Feature_Class(MultiPoint.class);
        ActionCondition_Feature_Class condition_ML = new ActionCondition_Feature_Class(MultiLineString.class);
        ActionCondition_Feature_Class condition_MV = new ActionCondition_Feature_Class(MultiPolygon.class);

        actionList.add(condition_P);
        condition_P.addActionToList(false, condition_L);
        condition_L.addActionToList(false, condition_V);
        condition_V.addActionToList(false, condition_MP);
        condition_MP.addActionToList(false, condition_ML);
        condition_ML.addActionToList(false, condition_MV);
        condition_MV.addActionToList(false, datastore);

        condition_P.addActionToList(true, new ActionFeatureType_Replace_Class(Point.class, true));
       // ActionDataStore_Writer dsw_p = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_P.addActionToList(true, dsw_p);
        condition_P.addActionToList(true, datastore);

        condition_L.addActionToList(true, new ActionFeatureType_Replace_Class(LineString.class, true));
       // ActionDataStore_Writer dsw_l = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_L.addActionToList(true, dsw_l);
        condition_L.addActionToList(true, datastore);

        condition_V.addActionToList(true, new ActionFeatureType_Replace_Class(Polygon.class, true));
        //ActionDataStore_Writer dsw_v = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_V.addActionToList(true, dsw_v);
        condition_V.addActionToList(true, datastore);

        condition_MP.addActionToList(true, new ActionFeatureType_Replace_Class(MultiPoint.class, true));
        //ActionDataStore_Writer dsw_mp = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_MP.addActionToList(true, dsw_mp);
        condition_MP.addActionToList(true, datastore);


        condition_ML.addActionToList(true, new ActionFeatureType_Replace_Class(MultiLineString.class, true));
        //ActionDataStore_Writer dsw_ml = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_ML.addActionToList(true, dsw_ml);
        condition_ML.addActionToList(true, datastore);

        condition_MV.addActionToList(true, new ActionFeatureType_Replace_Class(MultiPolygon.class, true));
        //ActionDataStore_Writer dsw_mv = new ActionDataStore_Writer(new HashMap(params), append, dropFirst);
        //condition_MV.addActionToList(true, dsw_mv);
        condition_MV.addActionToList(true, datastore);
    }

    public static List<List<String>> getConstructors() {
        List<List<String>> constructors = new ArrayList<List<String>>();

        /*constructors.add(Arrays.asList(new String[]{
                    ActionFactory.PARAMS,
                    ActionFactory.APPEND,
                    ActionFactory.DROPFIRST
                }));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.PARAMS,
                    ActionFactory.APPEND,}));

        constructors.add(Arrays.asList(new String[]{
                    ActionFactory.PARAMS,
                    ActionFactory.DROPFIRST
                }));*/

        return constructors;
    }

    @Override
    public void processPostCollectionActions(Status status){
        datastore.processPostCollectionActions(status);
    }
    
    public String getDescription_NL() {
        return "Pas de geometry in het SimpleFeatureType aan aan de geometry in de SimpleFeature. Let op; er mag dus maar één geometry soort worden ingeladen";
    }

    @Override
    public void close() throws Exception{
        datastore.close();
    }

    @Override
    public void flush(String typeName2Read) throws Exception {
    }
}