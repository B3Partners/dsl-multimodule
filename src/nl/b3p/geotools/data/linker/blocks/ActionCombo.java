/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.Feature;
import nl.b3p.geotools.data.linker.ActionList;

/**
 * A combo has a private actionlist and is a combination of actions, which can be called by this one action
 * @author Gertjan Al, B3Partners
 */
public abstract class ActionCombo extends Action {

    protected ActionList actionList = new ActionList();

    @Override
    public Feature execute(Feature feature) throws Exception {
        return actionList.process(feature);
    }

    @Override
    public String toString() {
        return "\n<" + getClass().getSimpleName() + ">\n" + actionList.toString() + "\n</" + getClass().getSimpleName() + ">";
    }

    @Override
    public void close() throws Exception {
        actionList.close();
    }

    public ActionList getActionList() {
        return actionList;
    }
}
