package jdpbfx.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.logging.Logger;

import jdpbfx.properties.DBPFPropertyType;

/**
 * @author Jon
 * @author memo
 */
public class DBPFUtil {

    /**
     * The logger name for logging events
     */
    public static final String LOGGER_NAME = "JDBPFX";

    /**
     * The logger object logging events
     */
    public static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);
    
    /**
     * Magic number for DBPF
     */
    public static final String MAGICNUMBER_DBPF = "DBPF";

    /**
     * Magic number for Exemplar files
     */
    public static final String MAGICNUMBER_EQZ = "EQZ";

    /**
     * Magic number for Cohort files
     */
    public static final String MAGICNUMBER_CQZ = "CQZ";

    /**
     * Magic number for Compressed data
     */
    public static final long MAGICNUMBER_QFS = 0xFB10;

    /**
     * Magic number for FSH files
     */
    public static final String MAGICNUMBER_SHPI = "SHPI";

    /**
     * Magic number for S3D files
     */
    public static final String MAGICNUMBER_3DMD = "3DMD";

    /**
     * Magic number for the B-Format of an exemplar
     *
     */
    public static final short FORMAT_BINARY = 0x42;

    /**
     * Magic number for the T-Format of an exemplar
     *
     */
    public static final short FORMAT_TEXT = 0x54;

    /**
     * The format for FLOAT values, used for Text-Format
     */
    public static final DecimalFormat FLOAT_FORMAT;

    // Creates the FLOAT_FORMAT
    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        FLOAT_FORMAT = new DecimalFormat("#0.#");
        FLOAT_FORMAT.setMaximumFractionDigits(6);
        FLOAT_FORMAT.setDecimalFormatSymbols(dfs);
        //LOGGER.addHandler(new ConsoleHandler());
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // EXEMPLAR
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Returns a string for the given exemplar format.<br>
     *
     * @param format
     *            The format
     * @return A string; Unknown, if not known format
     */
    public static String getExemplarFormat(short format) {
        String ret = "Unknown";
        switch (format) {
        case FORMAT_BINARY:
            ret = "B-Format (0x42)";
            break;
        case FORMAT_TEXT:
            ret = "T-Format (0x54)";
            break;
        }
        return ret;
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // CONVERT, FORMAT, DATE
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Converts a hex value given by a long to a float value.<br>
     * 
     * @param hexValue
     *            The hex value
     * @return The float
     */
    public static float convertHexToFloat(long hexValue) {
        return Float.intBitsToFloat((int) hexValue);
    }

    /**
     * Convert a float value to a hex value given as a long.<br>
     * 
     * @param floatValue
     *            The float value
     * @return The hex value
     */
    public static long convertFloatToHex(float floatValue) {
        return Float.floatToIntBits(floatValue) & 0xFFFFFFFFL;
    }

    /**
     * Converts a long value to an HEX string.<br>
     *
     * @param value
     *            The long value
     * @param length
     *            The length of the string
     * @return The hex string
     */
    public static String toHex(long value, int length) {
        return String.format("%0" + length + "X", value);
    }

    /**
     * Converts an float value to an HEX string.<br>
     *
     * @param value
     *            The float value
     * @param length
     *            The length of the string
     * @return The hex string
     */
    public static String toHex(float value, int length) {
        return toHex(convertFloatToHex(value), length);
    }

    /**
     * Returns the boolean string for the value.<br/>
     *
     * @param value
     *            The value: 0x00=False, 0x01=True
     * @return True or False as string
     */
    public static String toBooleanString(long value) {
        if (value == 0x01L) {
            return "True";
        }
        return "False";
    }

    /**
     * Return the long value of the boolean.<br/>
     * If boolean is TRUE this will return 1, else 0.
     *
     * @param b
     *            The boolean
     * @return 1, if boolean is TRUE; 0, otherwise
     */
    public static long toLong(boolean b) {
        if (b) {
            return 1L;
        }
        return 0L;
    }

    /**
     * Returns the boolean for the given long.<br>
     * If long is 1L this will return TRUE, else FALSE.
     *
     * @param l
     *            The long value
     * @return TRUE, if value = 1L; FALSE, otherwise
     */
    public static boolean toBool(long l) {
        if (l == 1L) {
            return true;
        }
        return false;
    }

    /**
     * Returns a formatted date.<br>
     * The pattern is: yyyy-MM-dd,HH:mm:ss
     *
     * @param date
     *            The date
     * @return The formatted date
     */
    public static String formatDate(long date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date * 1000);
        return format.format(cal.getTime());
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ARRAY: convert, read, write
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Converts a long value to a short array.<br>
     * The long value could be positive or negative. The result will be set into
     * the array from lowest to highest, truncating bytes if the length of the
     * array is too small. The array will be sorted in Low-To-High-Order!
     *
     * e.g.:<br>
     * 530 dec is 0212 hex, so with length 2 it will be [0]=0x12,[1]=0x02<br>
     * 530 dec is 0212 hex, so with length 3 it will be
     * [0]=0x12,[1]=0x02,[2]=0x00<br>
     *
     * @param value
     *            The long value
     * @param length
     *            The length of the array
     * @return An array in Low-To-High-Order
     */
    public static byte[] toShortArray(long value, int length) {
        byte[] ret = new byte[length];
        toArray(value, ret, 0, length);
        return ret;
    }

    /**
     * Converts a float value to a short array.<br>
     * The float value could be positive or negative. The result will be set
     * into the array from lowest to highest, truncating bytes if the length of
     * the array is too small. The array will be sorted in Low-To-High-Order!
     *
     * e.g.:<br>
     * 530 dec is 0212 hex, so with length 2 it will be [0]=0x12,[1]=0x02<br>
     * 530 dec is 0212 hex, so with length 3 it will be
     * [0]=0x12,[1]=0x02,[2]=0x00<br>
     *
     * @param value
     *            The float value
     * @param length
     *            The length of the array
     * @return An array in Low-To-High-Order
     */
    public static byte[] toShortArray(float value, int length) {
        return toShortArray(convertFloatToHex(value), length);
    }

    /**
     * Converts a long value to a short array.<br>
     * The long value could be positive or negative. The result will be set into
     * the array from lowest to highest, truncating bytes if the length of the
     * array is too small. The array will be sorted in Low-To-High-Order!
     *
     * e.g.:<br>
     * 530 dec is 0212 hex, so with length 2 it will be [0]=0x12,[1]=0x02<br>
     * 530 dec is 0212 hex, so with length 3 it will be
     * [0]=0x12,[1]=0x02,[2]=0x00<br>
     *
     * @param value
     *            The long value
     * @param dest
     *            The destination array
     * @param offset
     *            The offset inside the array
     * @param length
     *            The length of the array
     */
    public static void toArray(long value, byte[] dest, int offset, int length) {
        for(int x=0;x<length;x++) {
            dest[offset + x] = (byte)(value >>> (8*x));
        }
    }

    /**
     * Converts a float value to a short array.<br>
     * The float value could be positive or negative. The result will be set
     * into the array from lowest to highest, truncating bytes if the length of
     * the array is too small. The array will be sorted in Low-To-High-Order!
     *
     * e.g.:<br>
     * 530 dec is 0212 hex, so with length 2 it will be [0]=0x12,[1]=0x02<br>
     * 530 dec is 0212 hex, so with length 3 it will be
     * [0]=0x12,[1]=0x02,[2]=0x00<br>
     *
     * @param value
     *            The float value
     * @param dest
     *            The destination array
     * @param offset
     *            The offset inside the array
     * @param length
     *            The length of the array
     */
    public static void toArray(float value, byte[] dest, int offset, int length) {
        toArray(convertFloatToHex(value), dest, offset, length);
    }

    /**
     * Converts an short array to a long value.<br>
     *
     * This method is normally used for Binary format of an exemplar.
     *
     * The array is sorted Low-To-High-Order. The value could be signed
     * interpreted or not.
     *
     *
     * @param data
     *            The array
     * @param start
     *            The start in the array
     * @param length
     *            The length
     * @param signed
     *            TRUE, if value is signed; FALSE, otherwise
     * @return The long value
     */
    public static long toValue(byte[] data, int start, int length, boolean signed) {
        long result = 0;
        long mask = 0;
        for(int x=0;x < length;x++) {
            result |= ((data[start + x] & 0xFFL) << (8*x));
            mask |= (0xFFL << (8*x));
        }
        if(signed) {
            result |= ~mask;
        }
        return result;
    }

    /**
     * Converts a hexString to a long value.<br>
     *
     * The method is normally used for Text format of an exemplar.
     *
     * The value could be signed interpreted or not. The string could start with
     * 0x or without.
     *
     * @param hexString
     *            The hexString
     * @param signed
     *            TRUE, if value is signed; FALSE, otherwise
     * @return The long value
     */
    public static long toValue(String hexString, boolean signed) {
        long result = 0;
        hexString = hexString.toLowerCase();
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }
        result = Long.parseLong(hexString, 16);
        if(signed) {
            long signBit = 1L << ((hexString.length() * 4) - 1);
            if((result & signBit) == signBit) {
                result |= ~(signBit * 2 - 1);
            }
        }
        return result;
    }

    /**
     * Reads a value till length reached.<br>
     *
     * @param type
     *            PropertyType.SINT32 or PropertyType.SINT64 for signed values;
     *            Otherwise for unsigned
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     * @return A long value
     */
    public static long getValue(DBPFPropertyType type, byte[] data, int start, int length) {
        if (type == DBPFPropertyType.SINT32 || type == DBPFPropertyType.SINT64) {
            return getSint(data, start, length);
        }
        return getUint(data, start, length);
    }

    /**
     * Writes a value till length reached.<br>
     *
     * @param type
     *            PropertyType.SINT32 or PropertyType.SINT64 for signed values;
     *            Otherwise for unsigned
     * @param value
     *            The value
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     */
    public static void setValue(DBPFPropertyType type, long value, byte[] data, int start, int length) {
        if (type == DBPFPropertyType.SINT32 || type == DBPFPropertyType.SINT64) {
            setSint(value, data, start, length);
        }
        setUint(value, data, start, length);
    }

    /**
     * Reads an UINT32 till length reached.<br>
     *
     * Only for positive values for the result!
     *
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     * @return A long value to store UINT32
     */
    public static long getUint(byte[] data, int start, int length) {
        return toValue(data, start, length, false);
    }

    /**
     * Writes an UINT32 till length reached.<br>
     *
     * @param value
     *            The value
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     */
    public static void setUint(long value, byte[] data, int start, int length) {
        toArray(value, data, start, length);
    }

    /**
     * Reads an SINT32 till length reached.<br>
     *
     * For positive and negative values for the result.
     *
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     * @return A long value to store SINT32
     */
    public static long getSint(byte[] data, int start, int length) {
        return toValue(data, start, length, true);
    }

    /**
     * Writes an SINT32 till length reached.<br>
     *
     * @param value
     *            The value
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     */
    public static void setSint(long value, byte[] data, int start, int length) {
        toArray(value, data, start, length);
    }

    /**
     * Reads an FLOAT32 till length reached.<br/>
     *
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     *
     * @return A long value to store UINT32
     */
    public static float getFloat32(byte[] data, int start, int length) {
        return convertHexToFloat(getUint(data, start, length));
    }

    /**
     * Writes an FLOAT32 till length reached.<br>
     *
     * @param value
     *            The float value
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     */
    public static void setFloat32(float value, byte[] data, int start, int length) {
        setUint(convertFloatToHex(value), data, start, length);
    }

    /**
     * Reads chars till length reached.<br>
     *
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     * @return A string
     */
    public static String getChars(byte[] data, int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) data[start + i]);
        }
        return sb.toString();
    }

    /**
     * Writes chars till length of string reached.<br>
     *
     * @param s
     *            The string
     * @param data
     *            The data
     * @param start
     *            The start offset
     */
    public static void setChars(String s, byte[] data, int start) {
        for (int i = 0; i < s.length(); i++) {
            data[start + i] = (byte) (s.charAt(i) & 0xFF);
        }
    }

    /**
     * Reads UNICODE till length reached.<br>
     *
     * @param data
     *            The data
     * @param start
     *            The start offset
     * @param length
     *            The length
     * @return A string
     */
    public static String getUnicode(byte[] data, int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int val = data[start + 2 * i] + 256 * data[start + 2 * i + 1];
            sb.append((char) val);
        }
        return sb.toString();
    }

    /**
     * Writes UNICODE till length of string reached.<br>
     *
     * @param s
     *            The string
     * @param data
     *            The data
     * @param start
     *            The start offset
     */
    public static void setUnicode(String s, byte[] data, int start) {
        for (int i = 0; i < s.length(); i++) {
            char[] c = Character.toChars(s.codePointAt(i));
            data[start + 2 * i] = (byte) (c[0] & 0xFF);
            if (c.length > 1) {
                data[start + 2 * i + 1] = (byte) (c[1] & 0xFF);
            } else {
                data[start + 2 * i + 1] = 0;
            }
        }
    }

    /**
     * Reads bytes as strings till end reached.<br>
     *
     * The strings terminates with 0x0D and/or 0x0A. If length of string is
     * zero, it will not be added.
     *
     * @param data
     *            The data
     * @param start
     *            The start index
     * @return The list with strings
     */
    public static ArrayList<String> getLines(byte[] data, int start) {
        ArrayList<String> readData = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < data.length) {
            short dat = data[i];
            if (dat == 0x0D || dat == 0x0A) {
                if (sb.length() != 0) {
                    readData.add(sb.toString());
                    sb = new StringBuilder();
                }
            } else {
                sb.append((char) dat);
            }
            i++;
        }
        // check if last element without CRLF and add it
        if (sb.length() != 0) {
            readData.add(sb.toString());
        }
        return readData;
    }
}
