package jdpbfx.properties;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.regex.Pattern;

import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public abstract class DBPFProperty {

    protected long id;
    protected int count;
    protected DBPFPropertyType dataType;
    protected Object values;

    protected DBPFProperty(DBPFPropertyType dataType) {
        this.dataType = dataType;
        id = 0;
        count = -1;
        values = new Object[1];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ");
        sb.append(this.id);
        sb.append(", Type: ");
        sb.append(this.dataType);
        sb.append(", Reps: ");
        sb.append(Math.max(this.count, 0));
        sb.append("\nValues: ");
        Object[] vals = (Object[]) this.values;
        for(Object o : vals) {
            sb.append(o);
            sb.append(", ");
        }
        return sb.substring(0, sb.length()-2);
    }

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        if(this.count == count) return;

        Object oldValues = values;
        if(count == -1) {
            values = Array.newInstance(getReturnType(), 1);
            System.arraycopy(oldValues, 0, values, 0, Math.min(1, Array.getLength(oldValues)));
        } else if(count > -1) {
            values = Array.newInstance(getReturnType(), count);
            System.arraycopy(oldValues, 0, values, 0, Math.min(count, Array.getLength(oldValues)));
        } else {
            throw new IndexOutOfBoundsException();
        }
        this.count = count;
    }

    public DBPFPropertyType getDataType() {
        return dataType;
    }

    public Class<?> getReturnType() {
        return values.getClass().getComponentType();
    }

    public void setValue(int index, Object value) {
        if(index >=0 && index < count) {
            if(index == 0 || dataType != DBPFPropertyType.STRING) {
                if(Array.get(values, index).getClass().isAssignableFrom(value.getClass())) {
                    Array.set(values, index, value);
                } else {
                    throw new ArrayStoreException();
                }
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public void setValue(Object value) {
        if(count == -1 || dataType == DBPFPropertyType.STRING) {
            if(Array.get(values, 0).getClass().isAssignableFrom(value.getClass())) {
                Array.set(values, 0, value);
            } else {
                throw new ArrayStoreException();
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public Object getValue(int index) {
        if(count == -1 || dataType == DBPFPropertyType.STRING) {
            if(index == 0) {
                return Array.get(values, 0);
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else if(index >= 0 && index < count) {
            return Array.get(values, index);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public Object getValue() {
        return Array.get(values, 0);
    }

    /**
     * Decodes the property from the rawData at the given offset.<br>
     *
     * @param dData
     *            The rawData
     * @param offset
     *            The offset
     * @return The property or NULL, if cannot decoded
     */
    public static DBPFProperty decodeProperty(byte[] dData, int offset) {
        long id = DBPFUtil.getUint(dData, offset, 4);
        offset += 4;
        short typeID = (short) DBPFUtil.getUint(dData, offset, 2);
        DBPFPropertyType type = DBPFPropertyType.forID.get(typeID);

        offset += 2;
        long keyType = DBPFUtil.getUint(dData, offset, 1);
        offset += 1;
        @SuppressWarnings("unused")
        long unknown = DBPFUtil.getUint(dData, offset, 2);
        offset += 2;

        int count = -1;
        if (keyType == 0x80 || type == DBPFPropertyType.STRING) {
            // explicit check of PropertyType.STRING for some strange found
            // files
            count = (int) DBPFUtil.getUint(dData, offset, 4);
            offset += 4;
        }

        DBPFProperty prop = null;

        switch(type) {
            case STRING:
                prop = new DBPFStringProperty(DBPFUtil.getChars(dData, offset, count));
                break;
            case FLOAT32:
                if(count == -1) {
                    prop = new DBPFFloatProperty(new Float(DBPFUtil.getFloat32(dData, offset, type.length)));
                } else {
                    Float[] values = new Float[count];
                    for(int x=0;x<count;x++) {
                        values[x] = new Float(DBPFUtil.getFloat32(dData, offset, type.length));
                        offset += type.length;
                    }
                    prop = new DBPFFloatProperty(values);
                }
                break;
            default:
                if(type == null) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFProperty] Property Type not valid: id = 0x{0}, type = 0x{1}", new Object[]{DBPFUtil.toHex(id, 4), DBPFUtil.toHex(typeID, 2)});
                    return null;
                } else {
                    if(count == -1) {
                        prop = new DBPFLongProperty(type, Long.valueOf(DBPFUtil.getValue(type, dData, offset, type.length)));
                    } else {
                        Long[] values = new Long[count];
                        for(int x=0;x<count;x++) {
                            values[x] = Long.valueOf(DBPFUtil.getValue(type, dData, offset, type.length));
                            offset += type.length;
                        }
                        prop = new DBPFLongProperty(type, values);
                    }
                }
                break;
        }
        prop.setID(id);
        return prop;
    }

    /**
     * Decodes the property from the given string.<br>
     *
     * @param propString
     *            The property string
     * @return The DBPFProperty or NULL, if not known
     */
    public static DBPFProperty decodeProperty(String propString) {
        String[] tokens = propString.split("=");

        // now analyze the nameValue
        String[] nameValues = tokens[0].split(":");
        long id = Long.decode(nameValues[0]);

        // System.out.println(DBPFUtil.toHex(nameValue, 8));

        // now analyze the value
        String[] propValues = tokens[1].split(":");
        DBPFPropertyType type = DBPFPropertyType.valueOf(propValues[0].toUpperCase());

        int count = Integer.parseInt(propValues[1]);
        if (count == 0) {
            count = -1;
        }
        String s = propValues[2];
        s = s.replace('{', ' ');
        s = s.replace('}', ' ');
        s = s.replace('\"', ' ');
        String[] data = s.trim().split(",");

        DBPFProperty prop = null;

        switch(type) {
            case STRING:
                prop = new DBPFStringProperty(data[0].trim());
                break;
            case FLOAT32:
                if(count == -1) {
                    prop = new DBPFFloatProperty(Float.valueOf(data[0].trim()));
                } else {
                    Float[] values = new Float[count];
                    for(int x=0;x<count;x++) {
                        if(x < data.length && checkValidFloat(data[x]))
                            values[x] = Float.valueOf(data[x]);
                        else
                            values[x] = 0f;
                    }
                    prop = new DBPFFloatProperty(values);
                }
                break;
            default:
                Long[] values = new Long[Math.abs(count)];
                for(int x=0;x<Math.abs(count);x++) {
                    long value = 0;
                    String val = data[x].trim();
                    if(val.length() > 0) {
                        if(type == DBPFPropertyType.BOOL) {
                            if(val.toLowerCase().equals("true") ||
                                // wrong implemented in older DBPF4J versions:
                                            val.equals("0x01")) {
                                value = 0x01;
                            }
                        } else {
                            if(!val.contains("0x")) {
                                    val = "0x" + val;
                            }
                            boolean signed = (type == DBPFPropertyType.SINT32 || type == DBPFPropertyType.SINT64);
                            value = DBPFUtil.toValue(val, signed);
                        }
                    }
                    values[x] = value;
                }
                if(count == -1) {
                    prop = new DBPFLongProperty(type, values[0]);
                } else {
                    prop = new DBPFLongProperty(type, values);
                }
                break;
        }
        prop.setID(id);
        return prop;
    }

    /**
     * Returns a short array for this property.<br>
     *
     * The data is in the Binary-Format (0x42) and includes all data for the
     * property, e.g.:<br>
     * 20 00 00 00 00 0C 80 00 | 00 10 00 00 00 4C 4D 39<br>
     * 78 36 5F 53 61 63 72 65 | 43 6F 75 65 72<br>
     * for:<br>
     * ID:Exemplar-Name, Type:STRING, HasCount:True, Length:16, String:
     * LM9x6_SacreCouer
     *
     * Updates the hasCount from the length of values.
     *
     * @return An array
     */
    public byte[] toRaw() {
        // writes the basic property information
        byte[] data = new byte[getBinaryLength()];
        DBPFUtil.setUint(getID(), data, 0, 4);
        DBPFUtil.setUint(dataType.id, data, 4, 2);
        if (count != -1) {
            DBPFUtil.setUint(0x80, data, 6, 1);
            DBPFUtil.setUint(count, data, 9, 4);
        } else {
            DBPFUtil.setUint(0x00, data, 6, 1);
        }
        DBPFUtil.setUint(0x00, data, 7, 2);

        // gets the offset and sets the property values
        int offset = 9;
        if (count != -1) {
            offset += 4;
        }
        valueToRaw(data, offset);

        return data;
    }

    /**
     * Writes the property values to the array at the offset.<br>
     *
     * @param data
     *            The data
     * @param offset
     *            The offset
     */
    protected abstract void valueToRaw(byte[] data, int offset);

    /**
     * Returns the binary-format (0x42) data length for this property.
     *
     * @return binary-format length, in bytes
     */
    public int getBinaryLength() {
        int length = 9;
        if(count > -1) {
            length += 4 + (count * dataType.length);
        } else {
            length += dataType.length;
        }
        return length;
    }

    /**
     * Returns a string for this property.<br>
     *
     * The data is in the text-format (0x54), e.g.:<br>
     * 0x00000010:{"Exemplar Type"}=Uint32:0:{0x00000002}
     *
     * @throws IOException, if error creating the text
     */
    public String toText() throws IOException {
        int textCount = count;
        // Text-Format use never repeat for one value
        if (count == -1) {
            textCount = 0;
        }
        // if STRING, use zero for count
        if (dataType == DBPFPropertyType.STRING) {
            textCount = 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        sb.append(DBPFUtil.toHex(getID(), 8));
        sb.append(":{\"");
        String propName = DBPFProperties.getString(getID());
        if (propName == null) {
            propName = "UNKNOWN";
        }
        sb.append(propName);
        sb.append("\"}=");
        sb.append(dataType.toString());
        sb.append(':');

        sb.append(Integer.toString(textCount));
        sb.append(":{");
        valueToText(sb);
        sb.append('}');

        return sb.toString();
    }

    /**
     * Writes the property values to the Appendable destination.<br>
     *
     * @param destination
     *            The destination
     */
    protected abstract void valueToText(Appendable destination) throws IOException;

    /**
     *
     */
    protected static boolean checkValidFloat(String number) {
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
            ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
             "[+-]?(" + // Optional sign character
             "NaN|" +           // "NaN" string
             "Infinity|" +      // "Infinity" string

             // A decimal floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
             // edition, section 3.10.2.

             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
             "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

             // . Digits ExponentPart_opt FloatTypeSuffix_opt
             "(\\.("+Digits+")("+Exp+")?)|"+

             // Hexadecimal strings
             "((" +
             // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "(\\.)?)|" +

             // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
             "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

             ")[pP][+-]?" + Digits + "))" +
             "[fFdD]?))" +
             "[\\x00-\\x20]*");// Optional trailing "whitespace"

        return Pattern.matches(fpRegex, number);
    }
}
