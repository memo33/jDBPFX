package jdpbfx.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;

import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFProperties {

    /*
     * The most used nameValues
     */
    public static final long UNKNOWN = 0x00000000L;
    public static final long EXEMPLAR_TYPE = 0x00000010L;
    public static final long EXEMPLAR_NAME = 0x00000020L;
    public static final long BULLDOZE_COST = 0x099afacdL;
    public static final long OCCUPANT_SIZE = 0x27812810L;
    public static final long WEALTH = 0x27812832L;
    public static final long PURPOSE = 0x27812833L;
    public static final long DEMAND_SATISFIED = 0x27812840L;
    public static final long LANDMARK_EFFEKT = 0x2781284fL;
    public static final long PARK_EFFEKT = 0x27812850L;
    public static final long POLLUTION_AT_CENTER = 0x27812851L;
    public static final long POWER_CONSUMED = 0x27812854L;
    public static final long FLAMMABILITY = 0x29244db5L;
    public static final long QUERY_EXEMPLAR_GUID = 0x2a499f85L;
    public static final long MAX_FIRE_STAGE = 0x49beda31L;
    public static final long PLOP_COST = 0x49cac341L;
    public static final long CATALOG_CAPACITY = 0x4aa60ebcL;
    public static final long POLLUTION_RADII = 0x68ee9764L;
    public static final long LOT_CONFIG_PROPERTY_VERSION = 0x88edc789L;
    public static final long LOT_CONFIG_PROPERTY_SIZE = 0x88edc790L;
    public static final long ITEM_NAME = 0x899afbadL;
    public static final long ITEM_DESCRIPTION = 0x8a2602a9L;
    public static final long ITEM_ICON = 0x8a2602b8L;
    public static final long ITEM_ORDER = 0x8a2602b9L;
    public static final long USER_VISIBLE_NAME_KEY = 0x8a416a99L;
    public static final long OCCUPANT_GROUPS = 0xaa1dd396L;
    public static final long ITEM_DESCRIPTION_KEY = 0xca416ab5L;
    public static final long WATER_CONSUMED = 0xc8ed2d84L;
    public static final long LOT_RESOURCE_KEY = 0xea260589L;
    public static final long CONDITIONAL_BUILDING = 0xea3209f8L;
    public static final long MONTHLY_COST = 0xea54d286L;
    public static final long ITEM_BUTTON_ID = 0x8a2602bbL;

    /**
     * Initialize the properties for first use.
     */
    static {
        String propertiesFile = "resources/properties/properties.xml";
        InputStream is;
        // Try external property file
        try {
            is = new FileInputStream(propertiesFile);
        } catch (FileNotFoundException e) {
            is = null;
            // ignore this
        }

        // Try internal property file
        if (is == null) {
            is = DBPFProperties.class.getResourceAsStream("/" + propertiesFile);
        }

        if (is != null) {
            ExemplarProperties.loadProperties(is, ExemplarProperties.XML_FORMAT_READER);
        } else {
            DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFProperties] Can not load Exemplar Properties from file: {0}", propertiesFile);
        }
    }

    /*
     * All property values and names in this list
     */
    // public static LinkedHashMap<Long, String> propertyList = new
    // LinkedHashMap<Long, String>();

    /**
     * Returns the first value for the given key.<br>
     *
     * @param key
     *            The key
     * @return The value or NULL, if not found
     */
    public static String getString(long key) {
        ExemplarProperty prop = ExemplarProperties.getProperties().forID.get(key);
        if (prop != null) {
            return prop.getName();
        }

        // Can not find the prop and prop name for T format
//        System.out.println("No Prop found for: "+DBPFUtil.toHex(key, 8));
        return null;
        // return propertyList.get(key);
    }

    /**
     * Returns the first key, which has the given value.<br>
     *
     * @param value
     *            The value
     * @return The key or -1, if not found
     */
    public static long getKey(String value) {
        return ExemplarProperties.getProperties().forName.get(value).getId();
        // long key = -1;
        // if (propertyList.containsValue(value)) {
        // Iterator<Long> itera = propertyList.keySet().iterator();
        // while (key == -1 && itera.hasNext()) {
        // long keyTemp = itera.next();
        // String elem = propertyList.get(keyTemp);
        // if (elem.equals(value)) {
        // key = keyTemp;
        // }
        // }
        // }
        // return key;
    }
}
