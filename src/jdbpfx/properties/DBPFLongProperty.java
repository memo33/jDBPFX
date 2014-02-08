package jdbpfx.properties;

import java.io.IOException;

import jdbpfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFLongProperty extends DBPFProperty {

    public DBPFLongProperty(DBPFPropertyType dataType) throws IllegalArgumentException {
        super(dataType);
        if(dataType == DBPFPropertyType.FLOAT32 || dataType == DBPFPropertyType.STRING)
            throw new IllegalArgumentException("DBPFLongProperty cannot contain float or string data.");
        values = new Long[1];
    }

    public DBPFLongProperty(DBPFPropertyType dataType, Long value) throws IllegalArgumentException {
        this(dataType);
        this.count = -1;
        this.values = new Long[] {value};
    }

    public DBPFLongProperty(DBPFPropertyType dataType, Long value, long id) throws IllegalArgumentException {
        this(dataType, value);
        this.id = id;
    }

    public DBPFLongProperty(DBPFPropertyType dataType, Long[] values) throws IllegalArgumentException {
        this(dataType);
        this.count = values.length;
        this.values = new Long[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public DBPFLongProperty(DBPFPropertyType dataType, Long[] values, long id) throws IllegalArgumentException {
        this(dataType, values);
        this.id = id;
    }

    @Override
    public Long getValue(int index) {
        return (Long)super.getValue(index);
    }

    @Override
    public Long getValue() {
        return (Long)super.getValue();
    }

    @Override
    protected void valueToRaw(byte[] data, int offset) {
        if(getCount() == -1) {
            DBPFUtil.setValue(dataType, getValue(), data, offset, dataType.length);
        } else {
            for (int i = 0; i < getCount(); i++) {
                DBPFUtil.setValue(dataType, getValue(i), data, offset, dataType.length);
                offset += dataType.length;
            }
        }
    }

    @Override
    protected void valueToText(Appendable destination) throws IOException {
        int last = getCount() - 1;
        if(getCount() == -1) {
            if (dataType == DBPFPropertyType.BOOL) {
                destination.append(DBPFUtil.toBooleanString(getValue()));
            } else {
                destination.append("0x");
                destination.append(DBPFUtil.toHex(getValue(), 2 * dataType.length));
            }
        } else {
            for (int i = 0; i < getCount(); i++) {
                if (dataType == DBPFPropertyType.BOOL) {
                    destination.append(DBPFUtil.toBooleanString(getValue(i)));
                } else {
                    destination.append("0x");
                    destination.append(DBPFUtil.toHex(getValue(i), 2 * dataType.length));
                }
                if (i != last) {
                    destination.append(",");
                }
            }
        }
    }
}
