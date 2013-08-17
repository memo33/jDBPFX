package jdpbfx.types;

import jdpbfx.DBPFTGI;

/**
 * @author Jon
 */
public class DBPFRaw extends DBPFType {

    /**
     * Constructor.<br/>
     */
    public DBPFRaw(byte[] data, DBPFTGI tgi) {
        super(tgi);
        this.rawData = data;
        this.compressed = false;
        this.decompressedSize = data.length;
    }

    /**
     * Constructor.<br/>
     */
    public DBPFRaw(byte[] data, DBPFTGI tgi, boolean compressed, long decompressedSize) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.decompressedSize = compressed ? decompressedSize : data.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", RawData-Size: ");
        sb.append(rawData.length);
        return sb.toString();
    }

    /**
     * Sets the data of the raw type.<br>
     * This data are normally the rawData.
     *
     * @param data
     *            The data
     */
    public void setData(byte[] data, boolean compressed, int decompressedSize) {
        this.rawData = data;
        this.compressed = compressed;
        this.decompressedSize = compressed ? decompressedSize : data.length;
    }

    /**
     * Returns the data of the raw type.<br>
     * This data is equivalent to the rawData.
     *
     * @return The data
     */
    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public Type getType() {
        return DBPFType.Type.RAW;
    }

    @Override
    public void setCompressed(boolean compressed) {
    }
}
