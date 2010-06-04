/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker;

/**
 *
 * @author Erik van de Pol
 */
public class DataStoreLinkerStatus {
    private Thread thread;
    private Updatable updatable;

    public DataStoreLinkerStatus(Thread thread) {
        this(thread, null);
    }

    public DataStoreLinkerStatus(Thread thread, Updatable updatable) {
        this.thread = thread;
        this.updatable = updatable;
    }

    public boolean isCanceled() {
        return thread.isInterrupted();
    }

    public void cancel() {
        thread.interrupt();
    }

    public Updatable getUpdatable() {
        return updatable;
    }

    public void setUpdatable(Updatable updatable) {
        this.updatable = updatable;
    }
}
