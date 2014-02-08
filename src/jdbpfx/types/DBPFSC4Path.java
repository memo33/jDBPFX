package jdbpfx.types;

import jdbpfx.DBPFTGI;
import jdbpfx.DBPFType;

/**
 * @author Jon
 * @author memo
 */
public class DBPFSC4Path extends AbstractTextType {

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
    public DBPFSC4Path(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(data, tgi, compressed);
    }

    @Override
    public Type getType() {
        return DBPFType.Type.SC4PATH;
    }
}
