package nl.b3p.geotools.data.linker.blocks;

import org.geotools.feature.Feature;

/**
 *
 * @author Gertjan Al, B3Partners
 */
public abstract class RunOnce extends Action {

    boolean run = true;

    public Feature execute(Feature feature) throws Exception {
        return feature;
    }

    protected abstract void exec() throws Exception;

    public void execute() throws Exception {
        if (run) {
            exec();
            run = false;
        }
    }
}
