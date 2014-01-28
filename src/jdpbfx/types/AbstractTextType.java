package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.DBPFType;

/**
 * @author memo
 */
abstract class AbstractTextType extends DBPFType {

    char[] data;
    boolean modified;

    /**
     * Constructor which initializes the fields {@link #rawData}, {@link #data},
     * {@link #compressed}, {@link #modified} and {@link #tgi}.
     *
     * @param data
     *          the uncompressed byte data of the entry.
     * @param tgi
     *          the TGI.
     * @param compressed
     *          If {@code true}, the method {@link DBPFType#createData} will
     *          return the compressed byte data, else uncompressed.
     */
    AbstractTextType(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.data = new char[data.length];
        for(int i = 0;i<data.length;i++) {
            this.data[i] = (char)data[i];
        }
        this.compressed = compressed;
        this.modified = false;
    }

    /**
     * Constructor that does not initialize any fields apart from the TGI, so
     * this has to be handled by the subclass.
     *
     * @param tgi the TGI.
     */
    AbstractTextType(DBPFTGI tgi) {
        super(tgi);
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
     * Sets the string.
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
     * Returns the string.
     *
     * @return The data
     */
    public String getString() {
        return String.valueOf(data);
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
}
