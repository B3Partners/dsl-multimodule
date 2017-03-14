/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

import nl.b3p.geotools.data.linker.feature.EasyFeature;
import nl.b3p.geotools.data.linker.blocks.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
* @author Gertjan Al, B3Partners
 */
public class ActionList extends ArrayList<Action> {

    protected static final Log log = LogFactory.getLog(DataStoreLinker.class);

    public EasyFeature process(EasyFeature feature) throws Exception {
        Iterator iter = iterator();
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            feature = action.execute(feature);
            if(feature == null) {
                return null;
            }
        }
        return feature;
    }
    
    public void flush(Status status, Map properties) throws Exception {
        Iterator iter = iterator();
        
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            action.flush(status, properties);
        }
    }

    public void close() throws Exception {
        Iterator iter = iterator();
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            action.close();
        }
    }

    public void print() {
        log.info("\n" + toString());
    }

    @Override
    public String toString() {
        String result = "";
        Iterator iter = iterator();
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            result += action.toString() + "\n";
        }
        return result;
    }

    @Override
    public boolean add(Action action) {
        if (action != null) {
            return super.add(action);
        } else {
            return false;
        }
    }

    @Override
    public void add(int index, Action action) {
        if (action != null) {
            super.add(index, action);
        }
    }

    public String getDescription() {
        String result = "\n";
        Iterator iter = iterator();
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            result += action.getName() + ":\n";
            
            if (action instanceof ActionCondition) {
                ActionCondition condition = (ActionCondition) action;
                result += action.getDescription_NL() + "\n";
                result += "IF TRUE\n";
                result += condition.getActionList(true).getDescription();
                result += "ELSE\n";
                result += condition.getActionList(false).getDescription();
                result += "END IF\n";

            } else if(action instanceof ActionCombo){
                ActionCombo combo = (ActionCombo)action;
                result += combo.getActionList().getDescription();

            } else {
                result += action.getDescription_NL() + "\n - " + action.toString() + "\n\n";
            }
        }
        return result;
    }

    /**
     * processes after all features have been read and written
     *
     * @param status to collect messages on the way
     * @param properties the properties to inspect
     * @throws Exception generic exception
     */
    public void processPostCollectionActions(Status status, Map properties) throws Exception {
        Iterator iter = iterator();
        while (iter.hasNext()) {
            Action action = (Action) iter.next();
            action.processPostCollectionActions(status, properties);
        }
    }
}
