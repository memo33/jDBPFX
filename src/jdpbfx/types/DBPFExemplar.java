package jdpbfx.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;

import jdpbfx.DBPFTGI;
import jdpbfx.properties.DBPFProperty;
import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFExemplar extends DBPFType {

    private DBPFTGI cohortTGI;

    private short format;

    private boolean modified;

    private TreeMap<Long, DBPFProperty> propertyMap;

    /**
     * Constructor.<br/>
     *
     */
    public DBPFExemplar(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.modified = false;
        this.decompressedSize = data.length;
        this.propertyMap = new TreeMap<Long, DBPFProperty>();

        String fileType = DBPFUtil.getChars(data, 0x00, 3);
        if (fileType.equals(DBPFUtil.MAGICNUMBER_EQZ)) {
            long exFormat = DBPFUtil.getUint(data, 0x03, 1);
            @SuppressWarnings("unused")
            long unknown1 = DBPFUtil.getUint(data, 0x04, 1);
            @SuppressWarnings("unused")
            long unknown2 = DBPFUtil.getUint(data, 0x05, 3);

            // B-Format
            if (exFormat == DBPFUtil.FORMAT_BINARY) {
                createExemplarB(data);
            }
            // T-Format
            else if (exFormat == DBPFUtil.FORMAT_TEXT) {
                createExemplarT(data);
            }
        }
    }
    
    public DBPFExemplar(DBPFTGI tgi, boolean compressed, short format, DBPFTGI cohortTGI) {
        super(tgi);
        this.cohortTGI = cohortTGI;
        this.modified = true;
        this.rawData = null;
        this.compressed = compressed;
        this.propertyMap = new TreeMap<Long, DBPFProperty>();
        if(format == DBPFUtil.FORMAT_BINARY || format == DBPFUtil.FORMAT_TEXT)
            this.format = format;
        else
            this.format = DBPFUtil.FORMAT_BINARY;
    }
    
    public DBPFExemplar(DBPFTGI tgi, boolean compressed, short format) {
        this(tgi, compressed, format, DBPFTGI.BLANKTGI);
    }
    
    
    /*
    public DBPFExemplar(DBPFCohort cohort) {
        compressed = cohort.isCompressed();
        decompressedSize = cohort.getDecompressedSize();
        tgi = cohort.getTGI();

        cohortTGI = cohort.getCohortTGI();
        format = cohort.getFormat();
        propertyMap = cohort.getPropertyMap();
    }*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("\nProperty-Size: ");
        sb.append(propertyMap.size());
        sb.append(", Format: ");
        sb.append(DBPFUtil.getExemplarFormat(format));
        sb.append(", Cohort TGI: ");
        sb.append(DBPFUtil.toHex(cohortTGI.getType(), 8));
        sb.append(" ");
        sb.append(DBPFUtil.toHex(cohortTGI.getGroup(), 8));
        sb.append(" ");
        sb.append(DBPFUtil.toHex(cohortTGI.getInstance(), 8));
        return sb.toString();
    }

    @Override
    public String toDetailString() {
        StringBuilder sb = new StringBuilder(toString());
        if (propertyMap.size() > 0) {
            sb.append("\n");
            for (DBPFProperty prop : propertyMap.values()) {
                sb.append(prop.toString());
                sb.append("\n");
            }
        }
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * @return The number of properties in this exemplar
     */
    public int getNumProperties() {
        return propertyMap.size();
    }

    /**
     * Add a property to this exemplar.
     * This method returns false if the property is null or
     * if another property with the same id already exists.<br/>
     * 
     * @param prop
     *            the property to add
     *
     * @return TRUE is the property is added, or FALSE if it is not
     */
    public boolean addProperty(DBPFProperty prop) {
        if(prop != null && !propertyMap.containsKey(prop.getID())) {
            this.propertyMap.put(prop.getID(), prop);
            modified = true;
            return true;
        }
        return false;
    }

    /**
     * Update a property of this exemplar.
     * This method returns false if the property is null or
     * if another property with the same id does not already exist.<br/>
     *
     * @param prop
     *            the property to update
     *
     * @return TRUE is the property is updated, or FALSE if it is not
     */
    public boolean updateProperty(DBPFProperty prop) {
        if(prop != null && propertyMap.containsKey(prop.getID())) {
            this.propertyMap.put(prop.getID(), prop);
            modified = true;
            return true;
        }
        return false;
    }

    /**
     * Add or update a property in this exemplar.
     * This method returns false only if the property is null.<br/>
     *
     * @param prop
     *            the property to add
     *
     * @return TRUE is the property is added or updated, or FALSE if it is not
     */
    public boolean putProperty(DBPFProperty prop) {
        if(prop != null) {
            this.propertyMap.put(prop.getID(), prop);
            modified = true;
            return true;
        }
        return false;
    }

    /**
     * @param id
     *            the id of the property to be removed
     * @return the property that was removed
     *         or null if no property with the specified id exists.
     */
    public DBPFProperty removeProperty(long id) {
        if(propertyMap.containsKey(id)) {
            DBPFProperty prop = propertyMap.remove(id);
            modified = true;
            return prop;
        }
        return null;
    }

    /**
     * Returns the property for the given id.<br/>
     *
     * @param id
     *            The id of the property to be retrieved
     * @return The property or NULL, if not found
     */
    public DBPFProperty getProperty(long id) {
        return propertyMap.get(id);
    }
    
    /**
     * Removes all the properties from this exemplar<br/>
     */
    public void clearProperties() {
        propertyMap.clear();
    }

    @Override
    public Type getType() {
        return DBPFType.Type.EXEMPLAR;
    }

    @Override
    public DBPFTGI getTGIMask() {
        return DBPFTGI.EXEMPLAR;
    }

    /**
     * @return the cohortT
     */
    public long getCohortT() {
        return cohortTGI.getType();
    }

    /**
     * @return the cohortG
     */
    public long getCohortG() {
        return cohortTGI.getGroup();
    }

    /**
     * @return the cohortI
     */
    public long getCohortI() {
        return cohortTGI.getInstance();
    }

    /**
     * @return the cohort TGI
     */
    public DBPFTGI getCohortTGI() {
        return cohortTGI;
    }

    /**
     * @param tgi
     *            the TGI to set
     */
    public void setCohortTGI(DBPFTGI tgi) {
        this.cohortTGI = tgi;
        modified = true;
    }

    /**
     * @return the format
     */
    public short getFormat() {
        return format;
    }

    /**
     * @param format
     *            the format to set
     */
    public void setFormat(short format) {
        this.format = format;
        modified = true;
    }

    /**
     * Returns the data length for the given exemplar after calculating.<br/>
     *
     * The length is for the Binary-format (0x42).
     *
     * @return The length in bytes
     */
    public int getBinaryLength() {
        int dataLength = 0x18;
        for (DBPFProperty prop : propertyMap.values()) {
            dataLength += prop.getBinaryLength();
        }
        return dataLength;
    }

    @Override
    public byte[] getRawData() {
        if(!modified) {
            return rawData;
        } else {
            byte[] data = null;
            if (format == DBPFUtil.FORMAT_BINARY) {
                data = createExemplarDataB();
                rawData = data;
                decompressedSize = data.length;
                modified = false;
            } else if (format == DBPFUtil.FORMAT_TEXT) {
                data = createExemplarDataT();
                rawData = data;
                decompressedSize = data.length;
                modified = false;
            }
            return data;
        }
    }
    
    @Override
    public long getDecompressedSize() {
        return getRawData().length;
    }

    /**
     * Creates an exemplar from the given data.<br/>
     *
     * The data is in the Binary-format (0x42).<br/>
     * The header is EQZB1###.
     *
     * @param dData
     *            The decompressed data
     * @return The exemplar or NULL, if not exemplar
     */
    private void createExemplarB(byte[] dData) {
        // Reading the exemplars TGI
        this.cohortTGI = new DBPFTGI(DBPFUtil.getUint(dData, 0x08, 4),
                                          DBPFUtil.getUint(dData, 0x0C, 4),
                                          DBPFUtil.getUint(dData, 0x10, 4));

        // Reading the properties
        int propCount = (int) DBPFUtil.getUint(dData, 0x14, 4);
        // Size of header
        int pos = 0x18;
        for (int i = 0; i < propCount; i++) {
            DBPFProperty prop = DBPFProperty.decodeProperty(dData, pos);
            if(prop == null) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFExemplar] Could not read " +
                		"property: TGI: 0x{0}, 0x{1}, 0x{2}", new Object[]{
                        DBPFUtil.toHex(this.getTGI().getType(), 4),
                        DBPFUtil.toHex(this.getTGI().getGroup(), 4),
                        DBPFUtil.toHex(this.getTGI().getInstance(), 4)});
                throw new RuntimeException("Bad Property Data");
            }
            addProperty(prop);
            pos += prop.getBinaryLength();
        }
        format = DBPFUtil.FORMAT_BINARY;
    }

    /**
     * Creates an exemplar from the given data.<br/>
     *
     * The data is in the Text-format (0x54).
     *
     * @param exemplar
     *            The exemplar
     * @param dData
     *            The decompressed data
     */
    private void createExemplarT(byte[] dData) {
        // Read all lines
        ArrayList<String> lines = DBPFUtil.getLines(dData, 0x08);
        // for (String string : lines) {
        // System.out.println(string);
        // }
        int start = 0;
        while (!lines.get(start).contains("=")) {
            start++;
        }
        // Get cohort from first line
        String[] nameValue = lines.get(start).split("=");
        String[] value = nameValue[1].split(":");
        String[] coTGI = value[1].split(",");
        cohortTGI = new DBPFTGI(Long.decode(coTGI[0].substring(1)),
                                          Long.decode(coTGI[1]),
                                          Long.decode(coTGI[2].substring(0, 10)));

        // Get propCount from second line
        nameValue = lines.get(start + 1).split("=");
        int propCount = Integer.decode(nameValue[1]);
        for (int i = 0; i < propCount; i++) {
            DBPFProperty prop = DBPFProperty.decodeProperty(lines.get(start + 2 + i));
            addProperty(prop);
        }
        format = DBPFUtil.FORMAT_TEXT;
    }

    /**
     * Create the data for the given exemplar.<br/>
     *
     * The data is in the Binary-format (0x42).
     *
     * @param exemplar
     *            The exemplar
     * @return The data
     */
    private byte[] createExemplarDataB() {
        byte[] data = new byte[this.getBinaryLength()];
        DBPFUtil.setChars(DBPFUtil.MAGICNUMBER_EQZ, data, 0x00);
        DBPFUtil.setUint(DBPFUtil.FORMAT_BINARY, data, 0x03, 1);
        long unknown1 = 0x31;
        DBPFUtil.setUint(unknown1, data, 0x04, 1);
        long unknown2 = 0x232323;
        DBPFUtil.setUint(unknown2, data, 0x05, 3);
        DBPFUtil.setUint(this.getCohortT(), data, 0x08, 4);
        DBPFUtil.setUint(this.getCohortG(), data, 0x0c, 4);
        DBPFUtil.setUint(this.getCohortI(), data, 0x10, 4);
        DBPFUtil.setUint(this.propertyMap.size(), data, 0x14, 4);
        int pos = 0x18;
        try {
            for (DBPFProperty prop : this.propertyMap.values()) {
                byte[] pdata = prop.toRaw();
                System.arraycopy(pdata, 0, data, pos, pdata.length);
                pos += pdata.length;
            }
        } catch (Exception e) {
            System.out.println(this.toDetailString());
        }
        return data;
    }

    /**
     * Create the data for the given exemplar.<br/>
     *
     * The data is in the Text-format (0x54).
     *
     * @param exemplar
     *            The exemplar
     * @return The data
     */
    private byte[] createExemplarDataT() {
        final String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append(DBPFUtil.MAGICNUMBER_EQZ);
        sb.append("T1###");
        sb.append(CRLF);
        // Parent Cohort Key
        sb.append("ParentCohort=Key:{0x");
        sb.append(DBPFUtil.toHex(this.getCohortT(), 8));
        sb.append(",0x");
        sb.append(DBPFUtil.toHex(this.getCohortG(), 8));
        sb.append(",0x");
        sb.append(DBPFUtil.toHex(this.getCohortI(), 8));
        sb.append("}");
        sb.append(CRLF);
        // PropCount
        sb.append("PropCount=0x");
        sb.append(DBPFUtil.toHex(this.propertyMap.size(), 8));
        sb.append(CRLF);
        // Propertys
        for(DBPFProperty prop : this.propertyMap.values()) {
            try {
                sb.append(prop.toText());
                sb.append(CRLF);
            } catch (IOException e) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFExemplar] " + e.getMessage(), e);
            }
        }
        byte[] data = new byte[sb.length()];
        DBPFUtil.setChars(sb.toString(), data, 0);
        return data;
    }
}
