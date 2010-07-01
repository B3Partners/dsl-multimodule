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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Erik van de Pol
 */
@XmlType(namespace="http://www.b3partners.nl/schemas/dsl", propOrder={
    "file",
    "database",
    "tableName"
})
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlSeeAlso({
    Database.class,
    File.class,
    InoutDatatype.class
})
@Entity
@Table(name = "inout")
public class Inout implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private InoutType type;
    @Column(name = "table_name")
    private String tableName;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "input")
    private List<Process> inputProcessList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "output")
    private List<Process> outputProcessList;
    @JoinColumn(name = "database_id", referencedColumnName = "id")
    @ManyToOne
    private Database database;
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    @ManyToOne
    private File file;
    @JoinColumn(name = "datatype_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private InoutDatatype datatype;
    @Basic(optional = false)
    @Column(name = "name")
    private String name;

    public Inout() {
    }

    public Inout(Long id) {
        this.id = id;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @XmlTransient
    //@XmlElement(required=true, name="type")
    public InoutType getType() {
        return type;
    }

    public void setType(InoutType type) {
        this.type = type;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @XmlTransient
    public List<Process> getInputProcessList() {
        return inputProcessList;
    }

    public void setInputProcessList(List<Process> inputProcessList) {
        this.inputProcessList = inputProcessList;
    }

    @XmlTransient
    public List<Process> getOutputProcessList() {
        return outputProcessList;
    }

    public void setOutputProcessList(List<Process> outputProcessList) {
        this.outputProcessList = outputProcessList;
    }

    @XmlElement(name="database")
    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    @XmlElement(name="file")
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @XmlTransient
    //@XmlElement(required=true, name="datatype")
    public InoutDatatype getDatatype() {
        return datatype;
    }

    public void setDatatype(InoutDatatype datatype) {
        this.datatype = datatype;
    }

    //@XmlElement(required=true)
    @XmlTransient
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
