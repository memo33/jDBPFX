package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFLText extends DBPFType {

    private char[] data;

    private boolean modified;

    /**
     * Constructor.
     */
    public DBPFLText(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.modified = false;

        String s = "";
        int unicode = 0x00;
        if (data.length > 3) {
            unicode = (int) DBPFUtil.getUint(data, 0x03, 1);
        }
        // fourth is 0x10 as unicode indicator
        if (unicode == 0x10) {
            int numberOfChars = (int) DBPFUtil.getUint(data, 0x00, 3);
            s = DBPFUtil.getUnicode(data, 4, numberOfChars);
        } else {
            s = DBPFUtil.getChars(data, 0, data.length);
        }
        this.data = new char[s.length()];
        s.getChars(0, s.length(), this.data, 0);
        // 4 (UNICODE Identifier), 2*dataLength (UNICODE Char)
        this.decompressedSize = 4 + 2 * this.data.length;
    }

    /**
     * Constructor.<br>
     *
    public DBPFLText(char[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.data = data;
        this.compressed = compressed;
        this.decompressedSize = 4 + 2 * data.length;
    }*/

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

    @Override
    public boolean setTGI(DBPFTGI tgi) {
        if(tgi != null && tgi.matches(DBPFTGI.WAV)) {
            return false;
        } else {
            return super.setTGI(tgi);
        }
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
        // 4 (UNICODE Identifier), 2*dataLength (UNICODE Char)
        this.decompressedSize = 4 + 2 * data.length;
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
            String s = getString();
            byte[] dData = new byte[2 * s.length() + 4];
            DBPFUtil.setUint(s.length(), dData, 0x00, 3);
            // fourth is always 0x10 as UNICODE indicator
            DBPFUtil.setUint(0x10, dData, 0x03, 1);
            DBPFUtil.setUnicode(s, dData, 0x04);
            rawData = dData;
            modified = false;
            return dData;
        }
    }

    @Override
    public Type getType() {
        return DBPFType.Type.LTEXT;
    }

    @Override
    public DBPFTGI getTGIMask() {
        return DBPFTGI.LTEXT;
    }
}
