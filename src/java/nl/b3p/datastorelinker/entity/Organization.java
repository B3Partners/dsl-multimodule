package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
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
    private Integer id;
    
    @Basic(optional = false)
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Basic(optional = false)
    @Column(name = "upload_path", nullable = false, length = 255)
    private String uploadPath;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "organization_users", joinColumns = {
        @JoinColumn(name = "organization_id", unique = true)
    },
    inverseJoinColumns = {
        @JoinColumn(name = "users_id")
    })
    private List<Users> users;
    
    @ManyToMany
    @JoinTable(name = "output_organization", joinColumns = {
        @JoinColumn(name = "organization_id", unique = false)
    },
    inverseJoinColumns = {
        @JoinColumn(name = "output_id")
    })
    private List<Inout> outputs;

    public Organization() {
    }

    public Organization(String name, String uploadPath) {
        this.name = name;
        this.uploadPath = uploadPath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public List<Users> getUsers() {
        return users;
    }

    public void setUsers(List<Users> users) {
        this.users = users;
    }

    public List<Inout> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Inout> outputs) {
        this.outputs = outputs;
    }
}