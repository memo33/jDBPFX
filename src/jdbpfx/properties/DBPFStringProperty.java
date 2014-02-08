package jdbpfx.properties;

import java.io.IOException;
import java.lang.reflect.Array;

import jdbpfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFStringProperty extends DBPFProperty {

    public DBPFStringProperty() {
        super(DBPFPropertyType.STRING);
        values = new String[1];
        this.count = 0;
    }

    public DBPFStringProperty(String value) {
        this();
        this.count = value.length();
        this.values = new String[] {value};
    }

    public DBPFStringProperty(String value, long id) {
        this(value);
        this.id = id;
    }

    @Override
    public void setCount(int count) {
        return;
    }

    @Override
    public void setValue(int index, Object value) {
        if(index == 0) {
            if(String.class.isAssignableFrom(value.getClass())) {
                setValue((String)value);
            } else {
                throw new ArrayStoreException();
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setValue(Object value) {
        if(String.class.isAssignableFrom(value.getClass())) {
            setValue((String)value);
        }
    }

    public void setValue(String value) {
        Array.set(values, 0, value);
        count = value.length();
    }

    @Override
    public String getValue(int index) {
        return getValue();
    }

    @Override
    public String getValue() {
        return (String)super.getValue();
    }
    @Override
    protected void valueToRaw(byte[] data, int offset) {
        DBPFUtil.setChars(getValue(), data, offset);
    }

    @Override
    protected void valueToText(Appendable destination) throws IOException {
        destination.append("\"");
        destination.append(getValue());
        destination.append("\"");
    }
}
