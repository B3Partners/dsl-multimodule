/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.b3p.datastorelinker.entity;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sourceforge.stripes.util.Log;
import nl.b3p.datastorelinker.util.Nameable;
import nl.b3p.datastorelinker.util.Namespaces;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Erik van de Pol
 */
@XmlRootElement
@XmlType(name="processType"/*, propOrder={
    //"name",
    "input",
    "output",
    "actions",
    "featuresStart",
    "featuresEnd",
    "drop",
    "writerType",
    "mail"
}*/)
@XmlAccessorType(XmlAccessType.FIELD)
@Entity
@Table(name = "process")
public class Process implements Serializable, Nameable {
    @XmlTransient
    private static final long serialVersionUID = 1L;

    @XmlTransient
    private static final boolean DEFAULT_DROP = true;
    @XmlTransient
    private static final boolean DEFAULT_APPEND = false;
    @XmlTransient
    private static final String DEFAULT_WRITER_TYPE = "ActionCombo_GeometrySplitter_Writer";

    @XmlTransient
    private final static Log log = Log.getInstance(nl.b3p.datastorelinker.entity.Process.class);

    @Id
    @Basic(optional = false)
    @Column(name = "id")
    @GeneratedValue
    // default GeneratedValue strategy is AUTO
    // wat weer default naar SEQUENCE in Oracle maar ook in Postgres
    // (hier verwachtte ik IDENTITY; volgens de documentatie ook)
    @XmlTransient
    private Long id;

    @Basic(optional = true)
    @Column(name = "name")
    @XmlTransient
    private String name;

    @Basic(optional = false)
    @Column(name = "actions")
    @Lob
    @XmlTransient
    private String actions;

    @JoinColumn(name = "input_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    @XmlElement(required=true, name="input")
    private Inout input;

    @JoinColumn(name = "output_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    @XmlElement(required=true, name="output")
    private Inout output;

    @Basic(optional = true)
    @Column(name = "features_start")
    private Integer featuresStart;

    @Basic(optional = true)
    @Column(name = "features_end")
    private Integer featuresEnd;

    @Basic(optional = false)
    @Column(name = "drop_table")
    private Boolean drop = DEFAULT_DROP;

    @Basic(optional = false)
    @Column(name = "append_table")
    private Boolean append = DEFAULT_APPEND;

    @Basic(optional = false)
    @Column(name = "writer_type")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    private String writerType = DEFAULT_WRITER_TYPE;

    @JoinColumn(name = "mail_id", referencedColumnName = "id")
    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    private Mail mail;

    @JoinColumn(name = "schedule", referencedColumnName = "id")
    @ManyToOne(optional = true, cascade = CascadeType.ALL)
    @XmlTransient
    private Schedule schedule;

    @JoinColumn(name = "process_status_id", referencedColumnName = "id")
    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @XmlTransient
    private ProcessStatus processStatus;
    

    public Process() {
    }

    public Process(Long id) {
        this.id = id;
    }

    public Process(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Map toOutputMap() {
        Map outputMap = new HashMap();

        outputMap.put("drop", getDrop());
        outputMap.put("append", getAppend());
        outputMap.put("params", getOutput().getDatabase().toGeotoolsDataStoreParametersMap());

        return outputMap;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        if (name != null)
            return name;
        else
            return input.getName() + " -> " + output.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActionsString() {
        return actions;
    }

    public void setActionsString(String actions) {
        this.actions = actions;
    }

    @XmlAnyElement(lax=true)
    //@XmlElement(required=false)
    public Element getActions() {
        try {
            log.debug("getActions: " + actions);
            org.jdom.Document jdoc = new SAXBuilder().build(new StringReader(actions));
            assignDslNS(jdoc.getRootElement());
            Document doc = new DOMOutputter().output(jdoc);

            /*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(actions)));*/
            return doc.getDocumentElement();
        } catch(Exception ex) {
            log.error(ex);
            return null;
        }
    }

    private void assignDslNS(org.jdom.Element elem) {
        // This is very ugly; there has to be a better way to do this.
        elem.setNamespace(Namespace.getNamespace(Namespaces.DSL_NAMESPACE_STRING));
        for (Object childElem : elem.getChildren()) {
            assignDslNS((org.jdom.Element)childElem);
        }
    }

    public void setActions(Element element) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            this.actions = writer.toString();
        } catch(Exception ex) {
            log.error(ex);
            this.actions = "";
        }
        log.debug("setActions: " + actions);
    }

    public Inout getInput() {
        return input;
    }

    public void setInput(Inout input) {
        this.input = input;
    }

    public Inout getOutput() {
        return output;
    }

    public void setOutput(Inout output) {
        this.output = output;
    }

    public Integer getFeaturesStart() {
        return featuresStart;
    }

    public void setFeaturesStart(Integer featuresStart) {
        this.featuresStart = featuresStart;
    }

    public Integer getFeaturesEnd() {
        return featuresEnd;
    }

    public void setFeaturesEnd(Integer featuresEnd) {
        this.featuresEnd = featuresEnd;
    }

    public Boolean getDrop() {
        return drop;
    }

    public void setDrop(Boolean drop) {
        this.drop = drop;
    }

    public Boolean getAppend() {
        return append;
    }

    public void setAppend(Boolean append) {
        this.append = append;
    }

    public String getWriterType() {
        return writerType;
    }

    public void setWriterType(String writerType) {
        this.writerType = writerType;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
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
        if (!(object instanceof Process)) {
            return false;
        }
        Process other = (Process) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "nl.b3p.datastorelinker.entity.Process[id=" + id + "]";
    }

}
