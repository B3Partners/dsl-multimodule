/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.opengis.feature.simple.SimpleFeature;
import nl.b3p.geotools.data.linker.*;

/**
 * Base of a condition
 * @author Gertjan Al, B3Partners
 */
public abstract class ActionCondition extends Action {

    protected ActionList actionListTrue = new ActionList();
    protected ActionList actionListFalse = new ActionList();

    public enum CompareType {

        EQUAL("=="),
        NOT_EQUAL("!="),
        GREATER(">"),
        SMALLER("<"),
        GREATER_EQUAL(">="),
        SMALLER_EQUAL("<=");
        private String math;

        private CompareType(String math) {
            this.math = math;
        }

        @Override
        public String toString() {
            return math;
        }

        public static CompareType byString(String type) {
            for (CompareType com : CompareType.values()) {
                if (com.toString().equals(type)) {
                    return com;
                }
            }
            return null;
        }
    }

    abstract public EasyFeature execute(EasyFeature feature) throws Exception;

    public ActionList getActionList(boolean type) {
        if (type) {
            return actionListTrue;
        } else {
            return actionListFalse;
        }
    }

    public void clearActionList(boolean type) {
        if (type) {
            actionListTrue.clear();
        } else {
            actionListFalse.clear();
        }
    }

    public void addActionList(boolean type, ActionList actionList) {
        if (type) {
            actionListTrue.addAll(actionList);
        } else {
            actionListFalse.addAll(actionList);
        }
    }

    public void addActionToList(boolean type, Action action) {
        if (type) {
            actionListTrue.add(action);
        } else {
            actionListFalse.add(action);
        }
    }

    protected Double parseDouble(Object object) throws Exception {
        try {
            return Double.parseDouble(object.toString());
        } catch (NumberFormatException ex) {
            throw new Exception("Unable to compare object '" + object.toString() + "'; Object is not a number");
        }
    }

    protected EasyFeature compare(EasyFeature feature, Object left, CompareType compareType, Object right) throws Exception {
        Boolean result = null;

        switch (compareType) {
            case EQUAL:
                result = left.equals(right);
                break;

            case NOT_EQUAL:
                result = !left.equals(right);
                break;

            case GREATER:
                result = parseDouble(left) > parseDouble(right);
                break;

            case SMALLER:
                result = parseDouble(left) < parseDouble(right);
                break;

            case GREATER_EQUAL:
                result = parseDouble(left) >= parseDouble(right);
                break;

            case SMALLER_EQUAL:
                result = parseDouble(left) <= parseDouble(right);
                break;

            default:
                break;
        }

        if (result != null) {
            return process(result, feature);
        } else {
            return feature;
        }
    }

    protected EasyFeature process(boolean actionListType, EasyFeature feature) throws Exception {
        if (actionListType) {
            return actionListTrue.process(feature);
        } else {
            return actionListFalse.process(feature);
        }
    }

    @Override
    public void close() throws Exception {
        actionListTrue.close();
        actionListFalse.close();
    }

    protected String listsToString() {
        return ") {\n" + actionListTrue.toString() + "\n} else {\n" + actionListFalse.toString() + "\n}";
    }

    protected String listsGetDescription() {
        return "\n IF TRUE" + actionListTrue.toString() + "\n ELSE\n" + actionListFalse.toString() + "\n END IF";
    }
}
