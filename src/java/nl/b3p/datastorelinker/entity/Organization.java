package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Boy de Wit
 */
@Entity
@Table(name = "organization")
@XmlTransient
public class Organization implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;
    
    @Basic(optional = false)
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Basic(optional = false)
    @Column(name = "upload_path", nullable = false, length = 255)
    private String uploadPath;

    public Organization() {
    }

    public Organization(String name, String uploadPath) {
        this.name = name;
        this.uploadPath = uploadPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }
}