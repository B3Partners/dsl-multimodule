/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Erik van de Pol
 */
@Entity
@Table(name = "schedule")
@XmlTransient
public class Schedule implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR,
        ADVANCED
    }

    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;

    @Basic(optional = false)
    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    @Basic(optional = false)
    @Column(name = "job_name", nullable = false, length = 120)
    private String jobName;

    @Column(name = "from_date")
    @Temporal(TemporalType.DATE)
    private Date fromDate;

    @OneToMany(mappedBy = "schedule")
    private List<Process> processList;

    @Basic(optional = false)
    @Column(name = "schedule_type")
    @Enumerated(EnumType.STRING)
    private Schedule.Type scheduleType = Schedule.Type.ADVANCED;
    

    public Schedule() {
    }

    public Schedule(Long id) {
        this.id = id;
    }

    public Schedule(Long id, String cronExpression) {
        this.id = id;
        this.cronExpression = cronExpression;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public List<Process> getProcessList() {
        return processList;
    }

    public void setProcessList(List<Process> processList) {
        this.processList = processList;
    }

    public Schedule.Type getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(Schedule.Type scheduleType) {
        this.scheduleType = scheduleType;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Schedule)) {
            return false;
        }
        Schedule other = (Schedule) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.Schedule[id=" + id + "]";
    }

}
