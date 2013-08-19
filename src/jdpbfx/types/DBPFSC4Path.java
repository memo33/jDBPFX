package jdpbfx.types;

import jdpbfx.DBPFTGI;

/**
 * @author Jon
 */
public class DBPFSC4Path extends DBPFType {

    private char[] data;

    private boolean modified;

    /**
     * Constructor.<br>
     */
    public DBPFSC4Path(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.data = new char[data.length];
        for(int i = 0;i<data.length;i++) {
            this.data[i] = (char)data[i];
        }
        this.compressed = compressed;
        this.modified = false;

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", Data-Size: ");
        sb.append(data.length);
        return sb.toString();
    }

    @Override
    public String toDetailString() {
        StringBuilder sb = new StringBuilder(toString());
        if (data.length > 0) {
            sb.append("\n");
            sb.append(data);
        }
        return sb.toString();
    }

    /**
     * Sets the string.<br>
     *
     * @param s
     *            The string
     */
    public void setString(String s) {
        this.data = new char[s.length()];
        s.getChars(0, s.length(), data, 0);
        this.decompressedSize = data.length;
        this.modified = true;
    }

    /**
     * Returns the string.<br>
     *
     * @return The data
     */
    public String getString() {
        return new String(data);
    }

    @Override
    public byte[] getRawData() {
        if(!modified) {
            return rawData;
        } else {
            rawData = new byte[data.length];
            for(int i = 0;i<data.length;i++) {
                rawData[i] = (byte)data[i];
            }
            modified = false;
            return rawData;
        }
    }

    @Override
    public Type getType() {
        return DBPFType.Type.SC4PATH;
    }

    @Override
    public DBPFTGI getTGIMask() {
        return DBPFTGI.SC4PATH;
    }
}
