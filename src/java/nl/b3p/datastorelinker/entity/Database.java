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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import nl.b3p.datastorelinker.util.Mappable;
import nl.b3p.datastorelinker.util.Util;

/**
 *
 * @author Erik van de Pol
 */
@XmlType(name="database", propOrder={
    //"name",
    //"dbtype",
    "type",
    "host",
    "databaseName",
    "username",
    "password",
    "schema",
    "port",
    "instance",
    "alias",
    "url",
    "srs",
    "colX",
    "colY"
})
@XmlAccessorType(XmlAccessType.PROPERTY)
@Entity
@Table(name = "database")
@NamedQueries({
    @NamedQuery(name = "Database.findInput", query =
        "select distinct d from Database d left join d.inoutList l where l.type.id = null or l.type.id = 1 order by d.name")
})
public class Database implements Serializable, Mappable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Basic(optional = false)
    @Column(name = "name")
    private String name;
    @Column(name = "host")
    private String host;
    @Column(name = "database_name")
    private String databaseName;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "schema")
    private String schema;
    @Column(name = "port")
    private Integer port;
    @Column(name = "instance")
    private String instance;
    @Column(name = "alias")
    private String alias;
    @Column(name = "url")
    private String url;
    @Column(name = "srs")
    private String srs;
    @Column(name = "col_x")
    private String colX;
    @Column(name = "col_y")
    private String colY;
    @XmlTransient
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "database")
    private List<Inout> inoutList;
    //@XmlTransient
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private DatabaseType type;

    public Database() {
    }

    public Database(Long id) {
        this.id = id;
    }

    public Map<String, Object> toMap() {
        return toMap("");
    }

    public Map<String, Object> toMap(String keyPrefix) {
        Map<String, Object> map = new HashMap<String, Object>();

        Util.addToMapIfNotNull(map, "dbtype", type.getName(), keyPrefix);
        Util.addToMapIfNotNull(map, "host", host, keyPrefix);
        Util.addToMapIfNotNull(map, "port", port, keyPrefix);
        Util.addToMapIfNotNull(map, "database", databaseName, keyPrefix);
        Util.addToMapIfNotNull(map, "user", username, keyPrefix);
        Util.addToMapIfNotNull(map, "passwd", password, keyPrefix);
        // Oracle specific:
        Util.addToMapIfNotNull(map, "schema", schema, keyPrefix);
        Util.addToMapIfNotNull(map, "instance", instance, keyPrefix);
        // MS Access specific:
        Util.addToMapIfNotNull(map, "url", url, keyPrefix);
        Util.addToMapIfNotNull(map, "srs", srs, keyPrefix);
        // TODO: check of deze ok zijn!
        Util.addToMapIfNotNull(map, "column_x", colX, keyPrefix);
        Util.addToMapIfNotNull(map, "column_y", colY, keyPrefix);

        return map;
    }

    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    //@XmlElement(required=false)
    @XmlTransient
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSrs() {
        return srs;
    }

    public void setSrs(String srs) {
        this.srs = srs;
    }

    public String getColX() {
        return colX;
    }

    public void setColX(String colX) {
        this.colX = colX;
    }

    public String getColY() {
        return colY;
    }

    public void setColY(String colY) {
        this.colY = colY;
    }

    @XmlTransient
    public List<Inout> getInoutList() {
        return inoutList;
    }

    public void setInoutList(List<Inout> inoutList) {
        this.inoutList = inoutList;
    }

    //@XmlTransient
    @XmlElement(required=true, name="type")
    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
    }

    @XmlTransient
    //@XmlElement(required=true, name="dbtype")
    public String getDbtype() {
        return type.getName();
    }

    public void setDbtype(String typeName) {
        this.type = new DatabaseType();
        this.type.setName(typeName);
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
        if (!(object instanceof Database)) {
            return false;
        }
        Database other = (Database) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.Database[id=" + id + "]";
    }

}
