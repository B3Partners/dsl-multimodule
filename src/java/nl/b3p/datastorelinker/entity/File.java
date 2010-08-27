/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import net.sourceforge.stripes.util.Log;
import nl.b3p.datastorelinker.util.DirContent;
import nl.b3p.datastorelinker.util.Mappable;
import nl.b3p.datastorelinker.util.Util;

/**
 *
 * @author Erik van de Pol
 */
@XmlType(name="file")
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "file_inout")
public class File implements Serializable, Mappable {
    private static final long serialVersionUID = 1L;

    @XmlTransient
    private final static Log log = Log.getInstance(File.class);

    @Basic(optional = false)
    @Column(name = "name")
    private String name;
    @Basic(optional = false)
    @Column(name = "directory")
    private String directory;
    @Basic(optional = false)
    @Column(name = "is_directory")
    private Boolean isDirectory;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue
    private Long id;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "file")
    private List<Inout> inoutList;

    @Transient
    private DirContent dirContent;

    public File() {
    }

    public File(Long id) {
        this.id = id;
    }

    public File(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Map<String, Object> toMap() {
        return toMap("");
    }

    public Map<String, Object> toMap(String keyPrefix) {
        if (isDirectory) {
            // TODO: throw ex?
            log.error("Attempt to run dsl with a directory as input");
        }

        Map<String, Object> map = new HashMap<String, Object>();

        Object qname;
        java.io.File file = new java.io.File(directory, name);
        if (file.exists()) {
            //qname = file.toURI().getPath();
            try {
                // heel belangrijk voor de DatastoreLinker / Geotools!!
                qname = file.toURI().toURL();
            } catch(Exception e) {
                log.error("Malformed file url: " + e.getMessage());
                qname = name;
            }
        } else {
            log.error("File does not exist: " + file);
            qname = name;
        }

        Util.addToMapIfNotNull(map, "url", qname, keyPrefix);
        Util.addToMapIfNotNull(map, "srs", "EPSG:28992", keyPrefix);

        return map;
    }

    @XmlElement(required=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @XmlTransient
    public List<Inout> getInoutList() {
        return inoutList;
    }

    public void setInoutList(List<Inout> inoutList) {
        this.inoutList = inoutList;
    }

    @XmlTransient
    public DirContent getDirContent() {
        return dirContent;
    }

    public void setDirContent(DirContent dirContent) {
        this.dirContent = dirContent;
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
        if (!(object instanceof File)) {
            return false;
        }
        File other = (File) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.File[id=" + id + "]";
    }

}
