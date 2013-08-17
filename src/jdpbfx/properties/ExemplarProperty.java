package jdpbfx.properties;

import java.util.logging.Level;

import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class ExemplarProperty {

    /** A constant representing an unknown exemplar property. */
    public static final ExemplarProperty UNKNOWN = new ExemplarProperty();

    private final long id;
    private final String name;
    private final DBPFPropertyType type;

    // public final Map<String, String> attributes;

    /**
     * Constructor.<br>
     * Creates the UNKNOWN ExemplarProperty
     */
    private ExemplarProperty() {
        id = 0;
        name = "";
        type = DBPFPropertyType.STRING;
        // attributes = Collections.emptyMap();
    }

    /**
     * Constructor.<br>
     *
     * @param id
     *            The id
     * @param name
     *            The name
     * @param type
     *            The type
     */
    public ExemplarProperty(long id, String name, DBPFPropertyType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * Constructor.<br>
     * Creates an ExemplarProperty from the given Node.
     *
     * @param node
     *            A node
     */
    // REMARK:
    // Do not use a specific constructor, the XML file might
    // be different

    // public ExemplarProperty(Node node) {
    // xmlElement = (Element) node;
    // HashMap<String, String> modAttr = new HashMap<String, String>();
    // attributes = Collections.unmodifiableMap(modAttr);
    //
    // NamedNodeMap nodeAttrib = node.getAttributes();
    // for (int i = 0; i < nodeAttrib.getLength(); i++) {
    // modAttr.put(nodeAttrib.item(i).getNodeName(), nodeAttrib.item(i)
    // .getNodeValue());
    // }
    //
    // name = attributes.get("Name");
    //
    // String idStr = attributes.get("ID");
    // if (!idStr.startsWith("0x") || idStr.length() != 10) {
    // Logger.getLogger(DBPFUtil.LOGGER_NAME).log(
    // Level.SEVERE,
    // "NumberFormatException for ID of Exemplar property: "
    // + idStr);
    // }
    // id = Long.parseLong(attributes.get("ID").substring("0x".length()), 16);
    //
    // type = PropertyType.valueOf(attributes.get("Type").toUpperCase());
    // }

    /**
     * Creates a DBPFProperty from the given values.<br>
     *
     * @param values
     *            The values
     * @return The DBPFProperty or NULL, if not createable
     */
    public DBPFProperty createProperty(Object values) {
        // REMARK:
        // Do not use attributes from the Exemplar Property, they might
        // be different between various XML files

        // int count;
        // String defaultVal;
        // boolean hasCount;
        // String countStr = attributes.get("Count");
        // hasCount = (countStr == null);
        // count = hasCount ? Integer.parseInt(countStr) : 1;
        // if (values == null) {
        // defaultVal = attributes.get("Default");
        // if (type.propertyClass.equals(DBPFLongProperty.class)) {
        // values = new long[count];
        // Arrays.fill((long[]) values, Long.decode(defaultVal));
        // } else if (type.propertyClass.equals(DBPFFloatProperty.class)) {
        // values = new float[count];
        // Arrays.fill((float[]) values, Float.parseFloat(defaultVal));
        // } else if (type.propertyClass.equals(DBPFStringProperty.class)) {
        // values = defaultVal;
        // }
        // }

        DBPFProperty prop = null;
        switch(type) {
            case UINT8:
            case UINT16:
            case UINT32:
            case SINT32:
            case SINT64:
                Long[] intArray = (Long[]) values;
                if(intArray.length > 1) {
                    prop = new DBPFLongProperty(type, intArray[0]);
                } else {
                    prop = new DBPFLongProperty(type, intArray);
                }
                break;
            case FLOAT32:
                Float[] floatArray = (Float[]) values;
                if(floatArray.length > 1) {
                    prop = new DBPFFloatProperty(floatArray[0]);
                } else {
                    prop = new DBPFFloatProperty(floatArray);
                }
                break;
            case STRING:
                prop = new DBPFStringProperty((String) values);
                break;
            default:
                DBPFUtil.LOGGER.log(Level.SEVERE, "[ExemplarProperty] UnsupportedOperationException: Can not create Property.");
        }
        return prop;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type
     */
    public DBPFPropertyType getType() {
        return type;
    }
}
