package jdbpfx.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jdbpfx.util.DBPFUtil;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Jon
 */
public class ExemplarProperties {

    // The supported XML formats: iLive's Reader and SC4PIM
    public static final int XML_FORMAT_READER = 1;
    public static final int XML_FORMAT_SC4PIM = 2;

    /************************* Some common properties ***************************/
    public final ExemplarProperty OTHER;
    public final ExemplarProperty EXEMPLAR_NAME;
    public final ExemplarProperty EXEMPLAR_TYPE;

    /**************************************************************************/
    private static volatile ExemplarProperties currentProperties = null;

    public final Map<Long, ExemplarProperty> forID;

    /**
     * The strings are always UPPER_CASE for easier handling
     */
    public final Map<String, ExemplarProperty> forName;

    final Map<Long, ExemplarProperty> modifiableForID = new HashMap<Long, ExemplarProperty>();
    final Map<String, ExemplarProperty> modifiableForName = new HashMap<String, ExemplarProperty>();

    /**
     * Returns the exemplar properties.<br>
     *
     * @return The properties or NULL, if not loaded
     */
    public static ExemplarProperties getProperties() {
        return currentProperties;
    }

    /**
     * Loads the properties from a file and set as current properties.<br>
     *
     * @param propertiesFile
     *            The properties file
     * @param xmlFormat
     *            The format of the propertiesFile: XML_FORMAT_READER,
     *            XML_FORMAT_PIM
     * @return TRUE, if loaded successful; FALSE, otherwise
     */
    public static boolean loadProperties(File propertiesFile, int xmlFormat) {
        // Try internal property file
        InputStream is = ExemplarProperties.class.getResourceAsStream("/"
                + propertiesFile);
        // Try external property file
        if (is == null) {
            try {
                is = new FileInputStream(propertiesFile);
            } catch (FileNotFoundException e) {
                is = null;
                // ignore this
            }
        }

        if (is != null) {
            return loadProperties(is, xmlFormat);
        } else {
            DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] Can not load Exemplar Properties from file: {0}", propertiesFile);
        }
        return false;
    }

    /**
     * Loads the Properties from an InputStream and set as current properties.<br>
     *
     * @param is
     *            The inputStream, may be a FileInputStream or from Resource
     * @param xmlFormat
     *            The format of the propertiesFile: XML_FORMAT_READER,
     *            XML_FORMAT_PIM
     * @return TRUE, if loaded successful; FALSE, otherwise
     */
    public static boolean loadProperties(InputStream is, int xmlFormat) {
        currentProperties = new ExemplarProperties(is, xmlFormat);
        return (currentProperties != null);
    }

    /**
     * Constructor.<br>
     *
     * @param is
     *            The InputStream of the properties file
     * @param xmlFormat
     *            The format of the propertiesFile: XML_FORMAT_READER,
     *            XML_FORMAT_PIM
     */
    public ExemplarProperties(InputStream is, int xmlFormat) {
        forID = Collections.unmodifiableMap(modifiableForID);
        forName = Collections.unmodifiableMap(modifiableForName);

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document propDoc = builder.parse(is);

            if (xmlFormat == XML_FORMAT_READER) {
                readXMLFormatA(propDoc);
            } else if (xmlFormat == XML_FORMAT_SC4PIM) {
                readXMLFormatB(propDoc);
            }

        } catch (ParserConfigurationException e) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] ParserConfigurationException for InputStream", e);
        } catch (SAXException e) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] SAXException for InputStream", e);
        } catch (IOException e) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] IOException for InputStream", e);
        }

        EXEMPLAR_NAME = forID.get(0x00000020L);
        EXEMPLAR_TYPE = forID.get(0x00000010L);

        ExemplarProperty tempOther = forID.get(0L);

        if (tempOther == null)
            tempOther = ExemplarProperty.UNKNOWN;

        OTHER = tempOther;
    }

    /**
     * Reads the properties from the XML file used by iLive's Reader
     * (XML_FORMAT_READER).<br>
     * File: properties.xml<br>
     *
     * Format:<br>
     * &lt;exemplars&gt;<br>
     * &lt;properties&gt;<br>
     * &lt;group name="Common"&gt;<br>
     * &lt;property num="0x00000010" type="Uint32" lengthtype="fixed" length="1"
     * value="0x00000000" name="Exemplar Type" desc="Used by property editors to
     * group exemplars and filter properties"&gt;
     *
     */
    private void readXMLFormatA(Document propDoc) {
        NodeList nodeList = propDoc.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nExemplar = nodeList.item(i);
            if (nExemplar.getNodeName().equalsIgnoreCase("exemplars")) {
                NodeList typeList = nExemplar.getChildNodes();
                for (int j = 0; j < typeList.getLength(); j++) {
                    Node nType = typeList.item(j);
                    if (nType.getNodeName().equalsIgnoreCase("properties")) {
                        NodeList groupList = nType.getChildNodes();
                        for (int k = 0; k < groupList.getLength(); k++) {
                            setGroupA(groupList.item(k));
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the group to the list.<br>
     *
     * @param list
     *            The list
     * @param group
     *            The group
     */
    private void setGroupA(Node group) {
        if (group.getNodeName().equalsIgnoreCase("group")) {
            NodeList props = group.getChildNodes();
            for (int i = 0; i < props.getLength(); i++) {
                Node prop = props.item(i);
                if (prop.getNodeName().equalsIgnoreCase("group")) {
                    setGroupA(prop);
                } else {
                    setPropA(prop);
                }
            }
        }
    }

    /**
     * Sets the props to the list.<br>
     *
     * @param list
     *            The list
     * @param prop
     *            The property
     */
    private void setPropA(Node prop) {
        if (prop.getNodeName().equalsIgnoreCase("property")) {
            NamedNodeMap map = prop.getAttributes();
            Node nID = map.getNamedItem("num");
            Node nName = map.getNamedItem("name");
            Node nType = map.getNamedItem("type");
            String idStr = nID.getNodeValue();
            if (!idStr.startsWith("0x") || idStr.length() != 10) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] NumberFormatException for ID of Exemplar property: {0}", idStr);
            } else {
                long id = Long.decode(idStr);
                String name = nName.getNodeValue().trim();
                String sType = nType.getNodeValue().trim().toUpperCase();
                // System.out.println(DBPFUtil.toHex(id,8) + "," + name);
                DBPFPropertyType type = DBPFPropertyType.valueOf(sType);
                //
                ExemplarProperty exemProp = new ExemplarProperty(id, name, type);
                modifiableForID.put(exemProp.getId(), exemProp);
                modifiableForName.put(exemProp.getName().toUpperCase(),
                        exemProp);
            }
        }
    }

    /**
     * Reads the properties from a XML file used by SC4PIM (XML_FORMAT_PIM).<br>
     * File: new_properties.xml
     *
     * Format:<br>
     * &lt;ExemplarProperties&gt;<br>
     * &lt;PROPERTIES&gt;<br>
     * &lt;PROPERTY ID="0x00000000" Name="MiscType1" Type="Uint32"
     * ShowAsHex="N"&gt;<br>
     * &lt;HELP&gt;Function, values, &amp; DataType: varies from Exemplar file
     * to Exemplar file&lt;/HELP&gt;<br>
     * &lt;OPTION Value="0x00000000" Name="Other/Unknown"/&gt;
     *
     */
    private void readXMLFormatB(Document propDoc) {
        NodeList nodeList = propDoc.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nExemplar = nodeList.item(i);
            if (nExemplar.getNodeName().equalsIgnoreCase("ExemplarProperties")) {
                NodeList typeList = nExemplar.getChildNodes();
                for (int j = 0; j < typeList.getLength(); j++) {
                    Node nType = typeList.item(j);
                    if (nType.getNodeName().equalsIgnoreCase("PROPERTIES")) {
                        NodeList propList = nType.getChildNodes();
                        for (int k = 0; k < propList.getLength(); k++) {
                            setPropB(propList.item(k));
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the props to the list.<br>
     *
     * @param list
     *            The list
     * @param prop
     *            The property
     */
    private void setPropB(Node prop) {
        if (prop.getNodeName().equalsIgnoreCase("PROPERTY")) {
            NamedNodeMap map = prop.getAttributes();
            Node nID = map.getNamedItem("ID");
            Node nName = map.getNamedItem("Name");
            Node nType = map.getNamedItem("Type");
            if (nID != null && nName != null && nType != null) {
                String idStr = nID.getNodeValue();
                if (!idStr.startsWith("0x") || idStr.length() != 10) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] NumberFormatException for ID of Exemplar property: {0}", idStr);
                } else {
                    long id = Long.decode(idStr);
                    String name = nName.getNodeValue().trim();
                    String sType = nType.getNodeValue().trim().toUpperCase();
                    // System.out.println(DBPFUtil.toHex(id,8) + "," + name);
                    DBPFPropertyType type = DBPFPropertyType.valueOf(sType);
                    //
                    ExemplarProperty exemProp = new ExemplarProperty(id, name,
                            type);
                    modifiableForID.put(exemProp.getId(), exemProp);
                    modifiableForName.put(exemProp.getName().toUpperCase(),
                            exemProp);
                }
            } else {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperties] Can not analyze Exemplar property: {0}", prop.getNodeName());
            }
        }
    }
}
