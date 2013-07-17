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
@Table(name = "post_action")
@XmlTransient
public class PostAction implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Integer id;
    
    @Basic(optional = false)
    @Column(name = "label", nullable = false, length = 255)
    private String label;
    
    @Basic(optional = false)
    @Column(name = "class_name", nullable = false, length = 255)
    private String className;

    public PostAction() {
    }

    public PostAction(String label, String className) {
        this.label = label;
        this.className = className;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}