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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author Erik van de Pol
 */
@XmlType(name="mail", propOrder={
    "smtpHost",
    "toEmailAddress",
    "subject",
    "fromEmailAddress"
})
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "mail")
public class Mail implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue
    private Long id;

    @Basic(optional = false)
    @Column(name = "smtp_host")
    private String smtpHost;

    @Basic(optional = false)
    @Column(name = "to_email_address")
    private String toEmailAddress;

    @Column(name = "subject")
    private String subject;

    @Column(name = "from_email_address")
    private String fromEmailAddress;

    @OneToMany(mappedBy = "mail")
    private List<Process> processList;
    

    public Mail() {
    }

    public Mail(Long id) {
        this.id = id;
    }

    public Mail(Long id, String smtpHost, String toEmailAddress) {
        this.id = id;
        this.smtpHost = smtpHost;
        this.toEmailAddress = toEmailAddress;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @XmlElement(required=true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    @XmlElement(required=true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    public String getToEmailAddress() {
        return toEmailAddress;
    }

    public void setToEmailAddress(String toEmailAddress) {
        this.toEmailAddress = toEmailAddress;
    }

    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    public String getFromEmailAddress() {
        return fromEmailAddress;
    }

    public void setFromEmailAddress(String fromEmailAddress) {
        this.fromEmailAddress = fromEmailAddress;
    }

    @XmlTransient
    public List<Process> getProcessList() {
        return processList;
    }

    public void setProcessList(List<Process> processList) {
        this.processList = processList;
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
        if (!(object instanceof Mail)) {
            return false;
        }
        Mail other = (Mail) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.Mail[id=" + id + "]";
    }

}
