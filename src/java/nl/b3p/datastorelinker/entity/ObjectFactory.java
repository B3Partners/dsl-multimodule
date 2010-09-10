/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import javax.xml.bind.annotation.XmlRegistry;

/**
 *
 * @author Erik van de Pol
 */
@XmlRegistry
public class ObjectFactory {
    public ObjectFactory() {
        
    }

    public nl.b3p.datastorelinker.entity.Process createProcess() {
        return new nl.b3p.datastorelinker.entity.Process();
    }

    public Inout createInout() {
        return new Inout();
    }

    public Database createDatabase() {
        return new Database();
    }

    public File createFile() {
        return new File();
    }

    public Mail createMail() {
        return new Mail();
    }

    public ProcessStatus createProcessStatus() {
        return new ProcessStatus();
    }

    public Schedule createSchedule() {
        return new Schedule();
    }
    
}
