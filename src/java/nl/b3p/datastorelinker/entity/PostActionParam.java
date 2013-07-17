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
@Table(name = "post_action_param")
@XmlTransient
public class PostActionParam implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Integer id;
    
    @Basic(optional = false)
    @Column(name = "param", nullable = false, length = 255)
    private String param;
    
    @Basic(optional = false)
    @Column(name = "value", nullable = false, length = 255)
    private String value;

    public PostActionParam() {
    }

    public PostActionParam(String param, String value) {
        this.param = param;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}