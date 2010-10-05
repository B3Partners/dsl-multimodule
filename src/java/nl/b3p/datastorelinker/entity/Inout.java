/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Erik van de Pol
 */
@XmlType(name="inout"/*, propOrder={
    "file",
    "database",
    "tableName"
}*/)
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Table(name = "input_output")
@NamedQueries({
    @NamedQuery(name = "Inout.find", query =
        "from Inout where type = :typeName order by name")
})
public class Inout implements Serializable {

    @XmlTransient
    //@XmlType(name="inout_type")
    public enum Type {
        INPUT,
        OUTPUT
    }

    @XmlTransient
    //@XmlType(name="inout_datatype")
    public enum Datatype {
        FILE,
        DATABASE
    }

    @XmlTransient
    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue
    @XmlTransient
    private Long id;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "input_output_type")
    @XmlTransient
    private Inout.Type type;

    @Column(name = "table_name")
    private String tableName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "input")
    @XmlTransient
    private List<Process> inputProcessList;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "output")
    @XmlTransient
    private List<Process> outputProcessList;

    @JoinColumn(name = "database_id", referencedColumnName = "id")
    @ManyToOne
    //@XmlElement(name="database")
    private Database database;

    @Basic(optional = true)
    @Column(name = "file_name")
    private String file;

    @Basic(optional = true)
    @Column(name = "srs")
    private String srs;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "input_output_datatype")
    @XmlTransient
    private Inout.Datatype datatype;
    
    @Basic(optional = false)
    @Column(name = "name")
    @XmlTransient
    private String name;

    public Inout() {
    }

    public Inout(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inout.Type getType() {
        return type;
    }

    public void setType(Inout.Type type) {
        this.type = type;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Process> getInputProcessList() {
        return inputProcessList;
    }

    public void setInputProcessList(List<Process> inputProcessList) {
        this.inputProcessList = inputProcessList;
    }

    public List<Process> getOutputProcessList() {
        return outputProcessList;
    }

    public void setOutputProcessList(List<Process> outputProcessList) {
        this.outputProcessList = outputProcessList;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Inout.Datatype getDatatype() {
        return datatype;
    }

    public void setDatatype(Inout.Datatype datatype) {
        this.datatype = datatype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSrs() {
        return srs;
    }

    public void setSrs(String srs) {
        this.srs = srs;
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
        if (!(object instanceof Inout)) {
            return false;
        }
        Inout other = (Inout) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.Inout[id=" + id + "]";
    }

}
