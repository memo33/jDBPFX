package jdbpfx.types;

import jdbpfx.DBPFTGI;
import jdbpfx.DBPFType;

/**
 * This class behaves exactly as {@link DBPFLUA} except that its method
 * {@link #getType} returns {@code LUA_GEN} instead of {@code LUA}, which has
 * a different TGI mask.
 *
 * @author memo
 */
public class DBPFLUAGen extends DBPFLUA {

    /**
     * Constructor.
     *
     * @param data
     *          the uncompressed byte data of the entry.
     * @param tgi
     *          the TGI.
     * @param compressed
     *          If {@code true}, the method {@link DBPFType#createData} will
     *          return the compressed byte data, else uncompressed.
     */
    public DBPFLUAGen(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(data, tgi, compressed);
    }

    @Override
    public Type getType() {
        return DBPFType.Type.LUA_GEN;
    }
}
