package jdpbfx.types;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import jdpbfx.DBPFEntry;
import jdpbfx.DBPFTGI;
import jdpbfx.util.DBPFPackager;
import jdpbfx.util.DBPFUtil;

/**
 * An autonomous abstract type containing data representing a DBPF subfile entry that
 * may be written to a file.
 * 
 * <dl><dt><b>Specified by:</b>
 * <dd>{@link DBPFEntry}.
 * </dl>
 * 
 * @author Jon
 * @author memo
 */
public abstract class DBPFType extends DBPFEntry {

    /** determines whether this Type is compressed */
    protected boolean compressed = false;
    /** determines the size of this Type when decompressed */
    protected long decompressedSize = 0;
    /**
     * contains the byte data of this Type. Whether or not these are
     * compressed does not have to match {@link #compressed}, but instead
     * it is determined dynamically by {@link #createData()}.
     */
    // TODO uncompressed unless raw?
    protected byte[] rawData;

    /**
     * Constructor.
     */
    public DBPFType(DBPFTGI tgi) {
        super(tgi);
    }

    /**
     * Creates an array with the data from this DBPFType.
     * <p>
     * {@link DBPFRaw} types will return the data exactly as stored, other types
     * will return the data compressed if {@link #isCompressed()} returns true.
     * 
     * @return the data.
     */
    @Override
    public byte[] createData() {
        byte[] data = this.getRawData();

        // Compress the known files, if they were compressed,
        // the unknown are RAW and leave as they were!!!
        if(this.isCompressed() && (this.getType() != Type.RAW)) {
            DBPFPackager packager = new DBPFPackager();
            data = packager.compress(data);
        }
        return data;
    }
    
    /**
     * By default, creates a {@code ReadableByteChannel} from the byte array
     * returned by {@link #createData()} (which is against the intents of this
     * method - see super method for details).
     * 
     * @see DBPFEntry#createDataChannel()
     */
    @Override
    public ReadableByteChannel createDataChannel() {
        return Channels.newChannel(new ByteArrayInputStream(this.createData()));
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TGI: ");
        sb.append(DBPFUtil.toHex(tgi.getType(), 8));
        sb.append(", ");
        sb.append(DBPFUtil.toHex(tgi.getGroup(), 8));
        sb.append(", ");
        sb.append(DBPFUtil.toHex(tgi.getInstance(), 8));
        sb.append(", Compressed: ");
        sb.append(compressed);
        return sb.toString();
    }

    /**
     * By default, returns the {@link #toString()} value of this type.
     * 
     * @return the detailed String representation.
     */
    public String toDetailString() {
        return toString();
    }

    /**
     * Returns the actual {@link DBPFType.Type} of this type.
     * 
     * @return the Type.
     */
    public abstract Type getType();
    
    /**
     * Returns the corresponding generic TGI mask constant matching this Type.
     * 
     * @return the TGI mask.
     */
    public abstract DBPFTGI getTGIMask();

    /**
     * Returns the raw byte data of this type.
     * 
     * @return the raw byte data.
     */
    public abstract byte[] getRawData();

    /**
     * @return TRUE if and only if the TGI was successfully set, that is,
     *      tgi is not {@code null} and matches the {@link Type} of this instance
     *      of {@code DBPFType}.
     */
    @Override
    public boolean setTGI(DBPFTGI tgi) {
        if (tgi != null && tgi.matches(this.getTGIMask())) {
            this.tgi = tgi;
            return true;
        }
        return false;
    }

    /**
     * Returns whether this type is set to be compressed.
     * @return TRUE if compressed
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * Sets the compression flag of this type
     * @param compressed TRUE for compressed
     */
    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * Returns the size of this type when decompressed.
     * 
     * @return the decompressed size.
     */
    public long getDecompressedSize() {
        return decompressedSize;
    }
    
    /**
     * An Enumeration of known file types that can be returned by
     * {@link DBPFType#getType()} of a {@code DBPFType}.
     * 
     * @author memo
     */
    public static enum Type {
        RAW,
        DIRECTORY,
        LD,
        EXEMPLAR,
        COHORT,
        PNG,
        FSH,
        S3D,
        SC4PATH,
        LUA,
        RUL,
        LTEXT,
        WAV;
    }
}
