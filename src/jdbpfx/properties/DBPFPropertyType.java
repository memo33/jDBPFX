package jdbpfx.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jon
 */
public enum DBPFPropertyType {
    UINT8  ((short) 0x0100, "Uint8", 1),
    UINT16 ((short) 0x0200, "Uint16", 2),
    UINT32 ((short) 0x0300, "Uint32", 4),
    SINT32 ((short) 0x0700, "Sint32", 4),
    SINT64 ((short) 0x0800, "Sint64", 8),
    FLOAT32((short) 0x0900, "Float32", 4),
    BOOL   ((short) 0x0B00, "Bool", 1),
    STRING ((short) 0x0C00, "String", 1);

    /** A map of data type IDs ({@link #id}) to PropertyType constants. */
    public static final Map<Short, DBPFPropertyType> forID;

    static {
        HashMap<Short, DBPFPropertyType> modifiable = new HashMap<Short, DBPFPropertyType>(
                DBPFPropertyType.values().length);

        for (DBPFPropertyType type : DBPFPropertyType.values())
            modifiable.put(type.id, type);

        forID = Collections.unmodifiableMap(modifiable);
    }

    public final short id;
    public final String name;
    public final int length;

    private DBPFPropertyType(short id, String name, int length) {
        this.id = id;
        this.name = name;
        this.length = length;
    }

    @Override
    public String toString() {
        return name;
    }
}
