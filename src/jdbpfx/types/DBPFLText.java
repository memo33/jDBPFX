package jdbpfx.types;

import jdbpfx.DBPFTGI;
import jdbpfx.DBPFType;
import jdbpfx.util.DBPFUtil;

/**
 * @author Jon
 * @author memo
 */
public class DBPFLText extends AbstractTextType {

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
    public DBPFLText(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi); // does not initialize data fields
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
    @Override
    public void setString(String s) {
        this.data = new char[s.length()];
        s.getChars(0, s.length(), data, 0);
        // 4 (UNICODE Identifier), 2*dataLength (UNICODE Char)
        this.decompressedSize = 4 + 2 * data.length;
        this.modified = true;
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
}
