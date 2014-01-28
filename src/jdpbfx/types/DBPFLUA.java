package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.DBPFType;

/**
 * @author Jon
 * @author memo
 */
public class DBPFLUA extends AbstractTextType {

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
    public DBPFLUA(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(data, tgi, compressed);
    }

    /**
     * Constructor.<br>
     *
    public DBPFLUA(char[] data, DBPFTGI tgi, boolean compressed) {
        super();
        this.tgi = tgi;
        this.data = data;
        this.compressed = compressed;
        this.decompressedSize = data.length;
    }*/

    @Override
    public Type getType() {
        return DBPFType.Type.LUA;
    }
}
