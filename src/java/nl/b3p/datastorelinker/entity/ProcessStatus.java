/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlType;
import net.sourceforge.stripes.util.Log;

/**
 *
 * @author Erik van de Pol
 */
@Entity
@Table(name = "process_status")
public class ProcessStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final static Log log = Log.getInstance(ProcessStatus.class);

    @XmlType(name="ps_type")
    public enum Type {
        HAS_NEVER_RUN,
        RUNNING,
        LAST_RUN_OK,
        LAST_RUN_OK_WITH_ERRORS,
        LAST_RUN_FATAL_ERROR,
        CANCELED_BY_USER
    }

    @Id
    @Basic(optional = false)
    @GeneratedValue
    private Long id;

    private String message;

    @Basic(optional = false)
    @Column(name = "process_status_type")
    @Enumerated(EnumType.STRING)
    private ProcessStatus.Type processStatusType;

    @OneToMany(mappedBy = "processStatus")
    private List<Process> processList;

    
    public static ProcessStatus getDefault() {
        return new ProcessStatus(ProcessStatus.Type.HAS_NEVER_RUN);
    }

    public ProcessStatus() {

    }

    public ProcessStatus(ProcessStatus.Type processStatusType) {
        this.processStatusType = processStatusType;
    }

    public ProcessStatus(ProcessStatus.Type processStatusType, String message) {
        this(processStatusType);
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ProcessStatus.Type getProcessStatusType() {
        return processStatusType;
    }

    public void setProcessStatusType(ProcessStatus.Type processStatusType) {
        this.processStatusType = processStatusType;
    }

    public List<Process> getProcessList() {
        return processList;
    }

    public void setProcessList(List<Process> processList) {
        this.processList = processList;
    }

    @Override
    public String toString() {
        return "StatusType: " + processStatusType + "\n"
             + "Message: " + message;
    }

}
