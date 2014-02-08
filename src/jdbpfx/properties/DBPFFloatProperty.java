package jdbpfx.properties;

import java.io.IOException;

import jdbpfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFFloatProperty extends DBPFProperty {

    public DBPFFloatProperty() {
        super(DBPFPropertyType.FLOAT32);
        values = new Float[1];
    }

    public DBPFFloatProperty(Float value) {
        this();
        this.count = -1;
        this.values = new Float[] {value};
    }

    public DBPFFloatProperty(Float value, long id) {
        this(value);
        this.id = id;
    }

    public DBPFFloatProperty(Float[] values) {
        this();
        this.count = values.length;
        this.values = new Float[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    public DBPFFloatProperty(Float[] values, long id) {
        this(values);
        this.id = id;
    }

    @Override
    public Float getValue(int index) {
        return (Float)super.getValue(index);
    }

    @Override
    public Float getValue() {
        return (Float)super.getValue();
    }

    @Override
    protected void valueToRaw(byte[] data, int offset) {
        if(getCount() == -1) {
            DBPFUtil.setFloat32(getValue(), data, offset, dataType.length);
        } else {
            for (int i = 0; i < getCount(); i++) {
                DBPFUtil.setFloat32(getValue(i), data, offset, dataType.length);
                offset += dataType.length;
            }
        }
    }

    @Override
    protected void valueToText(Appendable destination) throws IOException {
        int last = getCount() - 1;
        if(getCount() == -1) {
            float f = getValue();
            int fi = (int) f;
            float r = f % fi;
            if (r == 0 || f == 0) {
                // if float value is pure integer
                destination.append(String.valueOf(fi));
            } else {
                destination.append(DBPFUtil.FLOAT_FORMAT.format(f));
            }
        } else {
            for (int i = 0; i < getCount(); i++) {
                float f = getValue(i);
                int fi = (int) f;
                float r = f % fi;
                if (r == 0 || f == 0) {
                    // if float value is pure integer
                    destination.append(String.valueOf(fi));
                } else {
                    destination.append(DBPFUtil.FLOAT_FORMAT.format(f));
                }
                if (i != last) {
                    destination.append(",");
                }
            }
        }
    }
}
