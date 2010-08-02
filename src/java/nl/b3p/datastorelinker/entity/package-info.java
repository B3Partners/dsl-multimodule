@XmlSchema(
    namespace = "http://www.b3partners.nl/schemas/dsl",
    elementFormDefault = XmlNsForm.UNQUALIFIED,//QUALIFIED,
    xmlns = {
        @XmlNs(namespaceURI="http://www.b3partners.nl/schemas/dsl", prefix="dsl")
    }
)
package nl.b3p.datastorelinker.entity;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

