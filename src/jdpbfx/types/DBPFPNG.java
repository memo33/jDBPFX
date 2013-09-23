package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.DBPFType;

/**
 * @author Jon
 */
public class DBPFPNG extends DBPFType {

    /**
     * Constructor.<br>
     */
    public DBPFPNG(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.decompressedSize = data.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", ImageData-Size: ");
        sb.append(rawData.length);
        return sb.toString();
    }

    /**
     * Returns the imageData of the PNG.<br>
     *
     * @return The data
     */
    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public Type getType() {
        return DBPFType.Type.PNG;
    }
}
