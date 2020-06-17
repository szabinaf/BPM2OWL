package bpmn2owl;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;

/**
 *
 * Converts sourceFile bpmn file to targetFile owl file
 *
 * @throws SAXException
 * @throws IOException
 * @throws ParserConfigurationException
 * @throws TransformerException
 *
 */
public class BPMN2OWL {

    @SuppressWarnings({"unchecked", "resource"})
    public static void main(String argv[])
            throws SAXException, IOException, ParserConfigurationException, TransformerException {
        try {
            String src = Vocabulary.sourcePath + "\\" + Vocabulary.sourceFile;

            // Nodes
            @SuppressWarnings("rawtypes")
            Set<String> classes = new HashSet();
            Hashtable<String, String> ids = new Hashtable<String, String>();

            String classNodes = "";
            String dataProperties = "";
            String objectProperties = "";
            String restriction = "";
            //String namedIndividuals = "";
            //Integer individualsCounter = 1;

            // DocumentBuilder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            //Build Document
            Document document = builder.parse(new File(src));
            //Normalize the XML Structure; It's just too important !!
            document.getDocumentElement().normalize();

            //Here comes the root node
            Element root = document.getDocumentElement();
            //System.out.println(root.getNodeName());

            NodeList nodeList = document.getElementsByTagName("*");

            //get xmlns
            String xmlns = "";

            System.out.println("create classes and properties");
            String childNode = "";
            String parentNode = "";
            
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                setName4Id(node, ids);
            }
            boolean isImportant = false;
            boolean isSequenceFirst = true;
            boolean isInputDocumentFirst = true;
            boolean isOutputDocumentFirst = true;
            boolean isResponsibleFirst = true;
            boolean isOrderFirst=true;
            
            String currentTaskName="";

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                isImportant = false;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    childNode = node.getNodeName().toString();
                    parentNode = node.getParentNode().getNodeName();
                    
                    if (node.getNodeName().equals("task")) {
                        //String nodeName = getName(node);
                        childNode = getName(node);
                        currentTaskName = childNode;
                        parentNode = getName(node.getParentNode());

                        // Class can be root class -> then it belongs to owl:Thing
                        // Class can be subclass of other class -> then we need <rdfs:subClassOf....
                        if (node.getParentNode().getNodeName().equals("#document")) {
                            parentNode = "document";
                        }
                        classNodes = new StringBuilder(classNodes).append(
                                "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode + "\">\r\n"
                                + "<rdfs:subClassOf rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                + parentNode + "\"/>\r\n" + "<rdfs:subClassOf>\r\n" + "</rdfs:subClassOf>"
                                + "</owl:Class>")
                                .toString();
                        classes.add(childNode);
                        isImportant = true;
                    }
                    if (node.getNodeName().equals("startEvent")) {
                       childNode = getName(node);
                        parentNode = Vocabulary.StartEvent;

                        classNodes = new StringBuilder(classNodes).append(
                                "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode + "\">\r\n"
                                + "<rdfs:subClassOf rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                + parentNode + "\"/>\r\n" + "<rdfs:subClassOf>\r\n" + "</rdfs:subClassOf>"
                                + "</owl:Class>")
                                .toString();
                        classes.add(childNode);
                        isImportant = true;                        
                    } 
                    if (node.getNodeName().equals("adonis:target")) {
                        if (isDocument(node)) {
                            childNode = getDocumentName(node);
                            parentNode = Vocabulary.documentClassName;
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode + "\">\r\n"
                                    + "<rdfs:subClassOf rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                    + parentNode + "\"/>\r\n" + "<rdfs:subClassOf>\r\n" + "</rdfs:subClassOf>"
                                    + "</owl:Class>")
                                    .toString();
                            classes.add(childNode);
                            isImportant = true;
                        }
                        if (isRole(node)) {
                            childNode = getRoleName(node);
                            parentNode = Vocabulary.roleClassName;
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode + "\">\r\n"
                                    + "<rdfs:subClassOf rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                    + parentNode + "\"/>\r\n" + "<rdfs:subClassOf>\r\n" + "</rdfs:subClassOf>"
                                    + "</owl:Class>")
                                    .toString();
                            classes.add(childNode);
                            isImportant = true;
                        }
                        if (isIT(node)) {
                            parentNode = Vocabulary.ITClassName;
                            childNode = getDocumentName(node);
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode + "\">\r\n"
                                    + "<rdfs:subClassOf rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                    + parentNode + "\"/>\r\n" + "<rdfs:subClassOf>\r\n" + "</rdfs:subClassOf>"
                                    + "</owl:Class>")
                                    .toString();
                            classes.add(childNode);
                            isImportant = true;
                        }
                    }
                    if (node.getNodeName().equals("sequenceFlow")) {
                        childNode = getTargetRefName(node, ids);
                        parentNode = getSourceRefName(node, ids);
                        if (isSequenceFirst) {
                            objectProperties = new StringBuilder(objectProperties)
                                    .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#followed_by\"/>\r\n")
                                    .toString();
                            isSequenceFirst = false;
                        }
                        if (parentNode != null
                                && childNode != null) {
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode + "\">\r\n"
                                    + "<rdfs:subClassOf>\n\r<owl:Restriction>"
                                    + "     <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#followed_by\"/>"
                                    + "     <owl:hasValue rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode + "\"/>\n"
                                    + "</owl:Restriction></rdfs:subClassOf></owl:Class>")
                                    .toString();
                            classes.add(childNode);
                        }
                    }
                    if (getNodeClass(node).equals("RC_REFERENCED_INPUT_DATA_OPTIONAL_M")) {
                        childNode = "";
                        parentNode = "";
                        NodeList childnodesList = node.getChildNodes();
                        for (int j = 0; j < childnodesList.getLength(); j++) {
                            Node childnode = childnodesList.item(j);
                            if (childnode.getNodeName().equals("adonis:endpoints")) {
                                NodeList cnodesList = childnode.getChildNodes();
                                for (int k = 0; k < cnodesList.getLength(); k++) {
                                    Node cnode = cnodesList.item(k);
                                    if (getName(cnode).equals("TO")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_DOCUMENT")) {
                                                childNode = getDocumentName(targetNode);
                                            }
                                        }
                                    } else if (getName(cnode).equals("FROM")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_TASK")) {

                                                parentNode = ids.get(getId(targetNode));
                                            }
                                        }

                                    }

                                }
                            }
                        }
                        if (isInputDocumentFirst) {
                            objectProperties = new StringBuilder(objectProperties)
                                    .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#input_data\"/>\r\n")
                                    .toString();
                            isInputDocumentFirst = false;
                        }
                        if (!childNode.equals("") && !parentNode.equals("")) {
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode + "\">\r\n"
                                    + "<rdfs:subClassOf>\n\r<owl:Restriction>"
                                    + "     <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#input_data\"/>"
                                    + "     <owl:someValuesFrom rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode + "\"/>\n"
                                    + "</owl:Restriction></rdfs:subClassOf></owl:Class>")
                                    .toString();
                            classes.add(childNode);
                        }

                    }

                    if (getNodeClass(node).equals("RC_REFERENCED_OUTPUT_DATA_OPTIONAL_M")) {
                        childNode = "";
                        parentNode = "";
                        NodeList childnodesList = node.getChildNodes();
                        for (int j = 0; j < childnodesList.getLength(); j++) {
                            Node childnode = childnodesList.item(j);
                            if (childnode.getNodeName().equals("adonis:endpoints")) {
                                NodeList cnodesList = childnode.getChildNodes();
                                for (int k = 0; k < cnodesList.getLength(); k++) {
                                    Node cnode = cnodesList.item(k);
                                    if (getName(cnode).equals("TO")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_DOCUMENT")) {
                                                childNode = getDocumentName(targetNode);
                                            }
                                        }
                                    } else if (getName(cnode).equals("FROM")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_TASK")) {

                                                parentNode = ids.get(getId(targetNode));
                                            }
                                        }

                                    }

                                }
                            }
                        }
                        if (isOutputDocumentFirst) {
                            objectProperties = new StringBuilder(objectProperties)
                                    .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#output_data\"/>\r\n")
                                    .toString();
                            isOutputDocumentFirst = false;
                        }
                        if (!childNode.equals("") && !parentNode.equals("")) {
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode + "\">\r\n"
                                    + "<rdfs:subClassOf>\n\r<owl:Restriction>"
                                    + "     <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#output_data\"/>"
                                    + "     <owl:someValuesFrom rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode + "\"/>\n"
                                    + "</owl:Restriction></rdfs:subClassOf></owl:Class>")
                                    .toString();
                            classes.add(childNode);
                        }

                    }

                    if (getNodeClass(node).equals("RESPONSIBLE_FOR_EXECUTION")) {
                        childNode = "";
                        parentNode = "";
                        NodeList childnodesList = node.getChildNodes();
                        for (int j = 0; j < childnodesList.getLength(); j++) {
                            Node childnode = childnodesList.item(j);
                            if (childnode.getNodeName().equals("adonis:endpoints")) {
                                NodeList cnodesList = childnode.getChildNodes();
                                for (int k = 0; k < cnodesList.getLength(); k++) {
                                    Node cnode = cnodesList.item(k);
                                    if (getName(cnode).equals("TO")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_ROLE")) {
                                                childNode = getDocumentName(targetNode);
                                            }
                                        }
                                    } else if (getName(cnode).equals("FROM")) {
                                        NodeList targetNodeList = cnode.getChildNodes();
                                        for (int l = 0; l < targetNodeList.getLength(); l++) {
                                            Node targetNode = targetNodeList.item(l);
                                            if (targetNode.getNodeName().equals("adonis:target")
                                                    && getNodeClass(targetNode).equals("C_TASK")) {

                                                parentNode = ids.get(getId(targetNode));
                                            }
                                        }

                                    }

                                }
                            }
                        }
                        if (isResponsibleFirst) {
                            objectProperties = new StringBuilder(objectProperties)
                                    .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#responsible_for_execution\"/>\r\n")
                                    .toString();
                            isResponsibleFirst = false;
                        }
                        if (!childNode.equals("") && !parentNode.equals("")) {
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode + "\">\r\n"
                                    + "<rdfs:subClassOf>\n\r<owl:Restriction>"
                                    + "     <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#responsible_for_execution\"/>"
                                    + "     <owl:someValuesFrom rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode + "\"/>\n"
                                    + "</owl:Restriction></rdfs:subClassOf></owl:Class>")
                                    .toString();
                            classes.add(childNode);
                        }

                    }
                    
                    if (getName(node).equals("A_ORDER")) {
                        childNode = "";
                        Node pNode=node.getParentNode().getParentNode().getParentNode();
                        parentNode = getName(pNode);
                        NodeList childnodesList = node.getChildNodes();
                        for (int j = 0; j < childnodesList.getLength(); j++) {
                            Node childnode = childnodesList.item(j);
                            if (childnode.getNodeName().equals("adonis:value")) {
                                childNode=childnode.getChildNodes().item(0).getNodeValue();//childnode.getNodeValue();
                            }
                        }
                        if (isOrderFirst) {
                            objectProperties = new StringBuilder(objectProperties)
                                    .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#order\"/>\r\n")
                                    .toString();
                            isOrderFirst = false;
                        }
                        if (!childNode.equals("") && !parentNode.equals("")) {
                            classNodes = new StringBuilder(classNodes).append(
                                    "<owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode + "\">\r\n"
                                   + "<rdfs:subClassOf>\n\r<owl:Restriction>"
                           //    + "\n\r<owl:Restriction>"
                                    + "     <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#order\"/>"
                                    + "     <owl:hasValue rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode + "\"/>\n"
                                    + "</owl:Restriction></rdfs:subClassOf></owl:Class>")
                        //                    + "</owl:Restriction></owl:Class>")
                                    .toString();
                            classes.add(childNode);
                        }
                    
                    }
                    if (isImportant) {
                        String nodeName = node.getNodeName();
                        NamedNodeMap nodeAttributes = node.getAttributes();

                        // create object property
                        objectProperties = new StringBuilder(objectProperties)
                                .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#"
                                        + childNode + "\"/>\r\n")
                                .toString();

                        if (parentNode.equals("#document")) {
                            parentNode = "document";
                        }

                        // count node occurrences
                        Integer nodeCounter = 0;
                        NodeList parentNodeList = node.getParentNode().getChildNodes();
                        for (int c = 0; c < parentNodeList.getLength(); c++) {
                            Node currentChildNode = parentNodeList.item(c);
                            if (currentChildNode.getNodeName().equals(nodeName)) {
                                nodeCounter = nodeCounter + 1;
                            }
                        }

                        // parent node has x child nodes
                        restriction = new StringBuilder(restriction).append(""
                                + " <owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + parentNode
                                + "\">\r\n" + "<rdfs:subClassOf>\r\n" + "<owl:Restriction>\r\n"
                                + "<owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode
                                + "\"/>\r\n"
                                + "<owl:qualifiedCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#nonNegativeInteger\">"
                                + nodeCounter + "</owl:qualifiedCardinality>\r\n"
                                + "<owl:onClass rdf:resource=\"" + Vocabulary.baseIRI + "#" + childNode
                                + "\"/>\r\n" + "</owl:Restriction>\r\n" + "</rdfs:subClassOf>\r\n" + "</owl:Class>" + "")
                                .toString();

                        // Iterate over attributes
                        for (int a = 0; a < nodeAttributes.getLength(); a++) {

                            // Attribute Variables
                            String attributeName = nodeAttributes.item(a).toString().split("\\=")[0];
                            String attributeValue = nodeAttributes.item(a).toString().split("\\=")[1];

                            // Test Cases
                            String attributeIsFalse = "\"false\"";
                            String attributeIsTrue = "\"true\"";
                            Boolean attributeIsObjectProperty = false;
                            String attributeType = "string";

                            // fix präfix issue
                            workaroundIssues(attributeName);

                            // Boolean, String or Reference
                            if (attributeValue.equals(attributeIsFalse) || attributeValue.equals(attributeIsTrue)) {
                                attributeType = "boolean";
                            }

                            // Check if property is a reference
                            for (String c : classes) {
                                if (attributeName.equals("id") || attributeName.equals("targetNamespace")
                                        || attributeName.equals("name") || attributeName.startsWith("xmlns")) {
                                } else {
                                    // Prepare class
                                    String splittedClass = "";
                                    try {
                                        splittedClass = c.toString().split(":")[1];

                                    } catch (Exception e) {
                                        splittedClass = c.toString();
                                    }
                                    String upperSplittedClass = "";
                                    try {
                                        upperSplittedClass = splittedClass.substring(0, 1).toUpperCase()
                                                + splittedClass.substring(1);
                                    } catch (Exception e) {
                                        upperSplittedClass = splittedClass.toString();
                                    }
                                    // Prepare attribute value
                                    String splittedAttributeValue = attributeValue;
                                    if (attributeValue.contains("_")) {
                                        splittedAttributeValue = splittedAttributeValue.split("_")[0].replaceAll("\"", "");
                                    } else if (splittedAttributeValue.contains(":")) {
                                        splittedAttributeValue = attributeValue.split(":")[1].replaceAll("\"", "");
                                    }
                                    if (upperSplittedClass.equals(splittedAttributeValue)) {
                                        attributeType = c.toString();
                                        attributeIsObjectProperty = true;
                                    }
                                }
                            }

                            // create object Property
                            if (attributeIsObjectProperty) {

                                objectProperties = new StringBuilder(objectProperties)
                                        .append("<owl:ObjectProperty rdf:about=\"" + Vocabulary.baseIRI + "#"
                                                + attributeName + "\"/>\r\n")
                                        .toString();

                                restriction = new StringBuilder(restriction).append(
                                        " <owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#" + childNode
                                        + "\">\r\n" + "	<rdfs:subClassOf>\r\n" + "		<owl:Restriction>\r\n"
                                        + "				<owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                        + attributeName + "\"/>\r\n"
                                        + "				<owl:qualifiedCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#nonNegativeInteger\">"
                                        + nodeCounter + "</owl:qualifiedCardinality>\r\n"
                                        + "				<owl:onClass rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                        + childNode + "\"/>\r\n" + "			</owl:Restriction>\r\n"
                                        + "		</rdfs:subClassOf>\r\n" + "	</owl:Class>" + "")
                                        .toString();
                            } else {
                                // create data property
                                dataProperties = new StringBuilder(dataProperties)
                                        .append("<owl:DatatypeProperty rdf:about=\"" + Vocabulary.baseIRI + "#"
                                                + attributeName + "\"/>\r\n")
                                        .toString();

                                restriction = new StringBuilder(restriction)
                                        .append("    <owl:Class rdf:about=\"" + Vocabulary.baseIRI + "#"
                                                + childNode + "\">\r\n" + "        <rdfs:subClassOf>\r\n"
                                                + "            <owl:Restriction>\r\n"
                                                + "                <owl:onProperty rdf:resource=\"" + Vocabulary.baseIRI + "#"
                                                + attributeName + "\"/>\r\n"
                                                + "                <owl:qualifiedCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#nonNegativeInteger\">1</owl:qualifiedCardinality>\r\n"
                                                + "                <owl:onDataRange rdf:resource=\"http://www.w3.org/2001/XMLSchema#"
                                                + attributeType + "\"/>\r\n" + "            </owl:Restriction>\r\n"
                                                + "        </rdfs:subClassOf>\r\n" + "    </owl:Class>")
                                        .toString();
                            }
                        }
                    }
                } else {
                    System.out.println(node.getNodeName());
                
                }
            }

            System.out.println("Write output file");
            writeOutputFile(objectProperties, dataProperties, restriction, classNodes);

            System.out.println("BPMN2 transformed to OWL\n");
            removeEmptyNamespace(src);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setName4Id(Node node, Hashtable<String, String> ids) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {

            if (node.getNodeName().equals("task")) {
                String name = getName(node);
                String id = getId(node);
//        System.out.println("H:"+id+"|"+name);
                ids.put(id, name);
            }
        }
    }

    private static String getNodeClass(Node node) {
        String nodeClass = "";
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("class")) {
                    nodeClass = attributeValue;

                }
            }
        }
        return nodeClass;
    }

    private static String getId(Node node) {
        String id = "";
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("id")) {
                    id = attributeValue;

                }
            }
        }
        return id;
    }

    private static void writeOutputFile(String objectProperties, String dataProperties, String restriction, String classNodes) {
        PrintWriter writer = null;
        try {
            File dir = new File(Vocabulary.targetPath);
            dir.mkdir();
            writer = new PrintWriter(Vocabulary.targetPath + "\\" + Vocabulary.targetFile, "UTF-8");
            writer.println("<?xml version=\"1.0\"?>\r\n");
            String rdfNode = "<rdf:RDF " + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\r\n "
                    + "xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\r\n ";

            // finish rdf:RDF Tag
            rdfNode = new StringBuilder(rdfNode).append("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\r\n")
                    .append("<owl:Ontology rdf:about=\"" + Vocabulary.baseIRI + "\"/>").toString();
            writer.println(rdfNode);

            writer.println("<!-- \r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "//\r\n" + "// objectProperties\r\n" + "//\r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "     -->");
            writer.println(objectProperties);
            writer.println("<!-- \r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "//\r\n" + "// dataProperties\r\n" + "//\r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "     -->");
            writer.println(dataProperties);
            writer.println("<!-- \r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "//\r\n" + "// restriction\r\n" + "//\r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "     -->");
            writer.println(restriction);

            writer.println("<!-- \r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "//\r\n" + "// classNodes\r\n" + "//\r\n"
                    + "///////////////////////////////////////////////////////////////////////////////////////\r\n"
                    + "     -->");
            writer.println(classNodes);

            // Close rdf:RDF Tag
            writer.println("</rdf:RDF>\r\n");
            // Close Writer
            writer.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(BPMN2OWL.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(BPMN2OWL.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }

    }

    private static boolean isRole(Node node) {
        boolean answer = false;
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("class")
                        && attributeValue.equals("C_ROLE")) {
                    answer = true;
                }
            }
        }
        return answer;
    }

    private static boolean isDocument(Node node) {
        boolean answer = false;
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("class")
                        && attributeValue.equals("C_DOCUMENT")) {
                    answer = true;
                }
            }
        }
        return answer;
    }

    private static boolean isIT(Node node) {
        boolean answer = false;
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("class")
                        && (attributeValue.equals("C_APPLICATION")
                        || attributeValue.equals("C_INFRASTRUCTURE_ELEMENT")
                        || attributeValue.equals("C_SERVICE"))) {
                    answer = true;
                }
            }
        }
        return answer;
    }

    private static String getRoleName(Node node) {
        String name = "";
        if (isRole(node)) {
            NamedNodeMap attributeList = node.getAttributes();
            if (attributeList != null) {
                for (int a = 0; a < attributeList.getLength(); a++) {
                    String attributeName = attributeList.item(a).toString().split("\\=")[0];
                    String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                    if (attributeName.equals("info")) {
                        name = getCleanName(attributeValue);
                    }
                }
            }
        }
        return name;
    }

    private static String getDocumentName(Node node) {
        String name = "";
        NamedNodeMap attributeList = node.getAttributes();
        if (attributeList != null) {
            for (int a = 0; a < attributeList.getLength(); a++) {
                String attributeName = attributeList.item(a).toString().split("\\=")[0];
                String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                if (attributeName.equals("info")) {
                    name = getCleanName(attributeValue);
                }
            }
        }
        return name;
    }

    private static String getTargetRefName(Node node, Hashtable<String, String> ids) {
        String refName = "";
        NamedNodeMap nodeAttributes = node.getAttributes();
        // Iterate over attributes
        for (int a = 0; a < nodeAttributes.getLength(); a++) {
            // Attribute Variables
            String attributeName = nodeAttributes.item(a).toString().split("\\=")[0];
            String attributeValue = nodeAttributes.item(a).toString().split("\\=")[1].replace("\"", "");
            if (attributeName.equals("targetRef")) {
                refName = ids.get(attributeValue);
//                System.out.println("T:"+attributeValue+","+refName);
            }
        }

        return refName;
    }

    private static String getSourceRefName(Node node, Hashtable<String, String> ids) {
        String sourceName = "";
        NamedNodeMap nodeAttributes = node.getAttributes();
        // Iterate over attributes
        for (int a = 0; a < nodeAttributes.getLength(); a++) {
            // Attribute Variables
            String attributeName = nodeAttributes.item(a).toString().split("\\=")[0];
            String attributeValue = nodeAttributes.item(a).toString().split("\\=")[1].replace("\"", "");
            if (attributeName.equals("sourceRef")) {
                sourceName = ids.get(attributeValue);
            }
        }
        return sourceName;
    }

    private static String getName(Node node) {
        String name = "";
        if (node != null) {
            NamedNodeMap attributeList = node.getAttributes();
            if (attributeList != null) {
                for (int a = 0; a < attributeList.getLength(); a++) {
                    String attributeName = attributeList.item(a).toString().split("\\=")[0];
                    String attributeValue = attributeList.item(a).toString().split("\\=")[1].replace("\"", "");
                    if (attributeName.equals("name")) {
                        name = getCleanName(attributeValue);
                    }
                }
            }
        }
        return name;
    }

    //remove the space
    public static String getCleanName(String name) {
        String sname = name.trim();
        if (name != null) {
            sname = sname.replaceAll("\\s", "_");
        } else {
            sname = "_";
        }
        return sname;
    }

    private static void removeEmptyNamespace(String src) throws IOException {
        FileInputStream fstream = new FileInputStream(src);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        BufferedWriter writer = new BufferedWriter(new FileWriter(src + "2"));

        String strLine;
        while ((strLine = br.readLine()) != null) {
            if (strLine.contains("xmlns=\"\"")) {
                writer.write(strLine.replace("xmlns=\"\"", ""));
            } else {
                writer.write(strLine);
            }
        }
        writer.close();
        br.close();

    }

    private static String workaroundIssues(String attributeName) {
        if (attributeName.startsWith("xmlns:")) {
            attributeName = "ResolveNamingIssuexmlns" + attributeName.replace("xmlns:", "");
        } else if (attributeName.startsWith("xsi:")) {
            attributeName = "ResolveNamingIssuexsi" + attributeName.replace("xsi:", "");
        } else if (attributeName.startsWith("ext:")) {
            attributeName = "ResolveNamingIssueext" + attributeName.replace("ext:", "");
        }
        return attributeName;
    }
    
    public static String createNamedIndividuals(Node childNode, String namedIndividuals) {
        try {
            // Text Content can be empty or set
            String textValue = "";
            try {
                textValue = childNode.getFirstChild().getNodeValue().replaceAll("\\s+", "_");
            } catch (Exception e) {
            }
            // check if parent is root
            String parentName = childNode.getParentNode().getNodeName().toString().replace(":", "_");
            if (parentName.equals("#document")) {
                parentName = "document_IC0";
            }
            // create named individual
            String childName = childNode.getNodeName().toString().replace(":", "_");
            String name = childNode.getNodeName().toString().split("_IC")[0];
            namedIndividuals = new StringBuilder(namedIndividuals)
                    .append("" + "\r\n<owl:NamedIndividual rdf:about=\"http://www.reiter.at/ontology/generated/"
                            + childName + "-belongsTo-" + parentName + "\">\r\n"
                            + "<rdf:type rdf:resource=\""+Vocabulary.baseIRI+"generated#" + name + "\"/>\r\n")
                    .toString();
            // Add text content of node
            if (!textValue.equals("")) {
                namedIndividuals = new StringBuilder(namedIndividuals)
                        .append("<textValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + textValue
                                + "</textValue>\r\n")
                        .toString();
            }
            // Add Attributes to NamedIndividual
            NamedNodeMap nodeAttributes = childNode.getAttributes();
            for (int a = 0; a < nodeAttributes.getLength(); a++) {
                String fixedAttributeName = workaroundIssues(
                        nodeAttributes.item(a).toString().split("\\=")[0].replaceAll("\"", ""));
                namedIndividuals = new StringBuilder(namedIndividuals)
                        .append("<" + fixedAttributeName + " rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">"
                                + nodeAttributes.item(a).toString().split("\\=")[1].replaceAll("\"", "") + "</"
                                + fixedAttributeName + ">\r\n")
                        .toString();
            }
            // End NamedIndividual
            namedIndividuals = new StringBuilder(namedIndividuals).append("</owl:NamedIndividual>\r\n").toString();
        } catch (Exception e) {
        }
        return namedIndividuals;
    }
}
