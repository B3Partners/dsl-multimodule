/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.util;

import java.util.List;
import nl.b3p.datastorelinker.entity.File;

/**
 *
 * @author Erik van de Pol
 */
public class DirContent {
    protected List<File> dirs;
    protected List<File> files;

    public DirContent() {
        
    }

    public List<File> getDirs() {
        return dirs;
    }

    public void setDirs(List<File> dirs) {
        this.dirs = dirs;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }


}
