/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.geotools.data.linker.util;

import java.util.List;

/**
 *
 * @author Erik van de Pol
 */
public class DataTypeList {
    private List<String> good;
    private List<String> bad;

    public DataTypeList(List<String> good, List<String> bad) {
        this.good = good;
        this.bad = bad;
    }

    public List<String> getGood() {
        return good;
    }
    public List<String> getBad() {
        return bad;
    }
}
