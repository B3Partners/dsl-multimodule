/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.b3p.geotools.data.linker;

/**
 *
 * @author Erik van de Pol
 */
public class TypeNameStatus {

    private boolean runOnce = true;
    private boolean dsReportsError = false;
    private String dsHasErrorAttribute = "";
    private String dsErrorAttribute = "";

    public synchronized void reset() {
        runOnce = true;
        dsReportsError = false;

        dsHasErrorAttribute = "";
        dsErrorAttribute = "";
    }

    public synchronized boolean isRunOnce() {
        return runOnce;
    }

    public synchronized void setRunOnce(boolean runOnce) {
        this.runOnce = runOnce;
    }

    public synchronized boolean isDsReportsError() {
        return dsReportsError;
    }

    public synchronized void setDsReportsError(boolean dsReportsError) {
        this.dsReportsError = dsReportsError;
    }

    public synchronized String getDsHasErrorAttribute() {
        return dsHasErrorAttribute;
    }

    public synchronized void setDsHasErrorAttribute(String dsHasErrorAttribute) {
        this.dsHasErrorAttribute = dsHasErrorAttribute;
    }

    public synchronized String getDsErrorAttribute() {
        return dsErrorAttribute;
    }

    public synchronized void setDsErrorAttribute(String dsErrorAttribute) {
        this.dsErrorAttribute = dsErrorAttribute;
    }
}
