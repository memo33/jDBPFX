package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.DBPFType;

/**
 * @author Jon
 */
public class DBPFDirectory extends DBPFType {

    /**
     * Constructor.<br>
     */
    public DBPFDirectory() {
        super(DBPFTGI.DIRECTORY);
        this.compressed = false;
        this.decompressedSize = 0;
        this.rawData = new byte[0];
    }

    /**
     * Constructor.<br>
     */
    public DBPFDirectory(byte[] data) {
        super(DBPFTGI.DIRECTORY);
        this.compressed = false;
        this.decompressedSize = data.length;
        this.rawData = data;
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
    public void setData(byte[] data) {
        this.rawData = data;
        this.decompressedSize = rawData.length;
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
        return DBPFType.Type.DIRECTORY;
    }

    @Override
    public void setCompressed(boolean compressed) {
        super.setCompressed(false);
    }
}
