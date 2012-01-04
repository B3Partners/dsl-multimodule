package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Boy de Wit
 */
@Entity
@Table(name = "users")
@XmlTransient
public class Users implements Serializable {

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
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    @Basic(optional = false)
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;
    
    @ManyToOne(optional = true)
    @JoinTable(name = "organization_users", joinColumns = {
        @JoinColumn(name = "users_id")
    },
    inverseJoinColumns = {
        @JoinColumn(name = "organization_id")
    })
    private Organization organization;

    public Users() {
    }

    public Users(String name, String password, Boolean isAdmin, Organization organization) {
        this.name = name;
        this.password = password;
        this.isAdmin = isAdmin;
        this.organization = organization;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}