package jdpbfx;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import jdpbfx.types.DBPFCohort;
import jdpbfx.types.DBPFDirectory;
import jdpbfx.types.DBPFExemplar;
import jdpbfx.types.DBPFFSH;
import jdpbfx.types.DBPFLText;
import jdpbfx.types.DBPFLUA;
import jdpbfx.types.DBPFLUAGen;
import jdpbfx.types.DBPFPNG;
import jdpbfx.types.DBPFRUL;
import jdpbfx.types.DBPFRaw;
import jdpbfx.types.DBPFS3D;
import jdpbfx.types.DBPFSC4Path;
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
        sb.append(DBPFUtil.toHex(this.getTGI().getType(), 8));
        sb.append(", ");
        sb.append(DBPFUtil.toHex(this.getTGI().getGroup(), 8));
        sb.append(", ");
        sb.append(DBPFUtil.toHex(this.getTGI().getInstance(), 8));
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
    
//    /**
//     * Returns the corresponding generic TGI mask constant matching this Type.
//     * 
//     * @return the TGI mask.
//     */
//    public abstract DBPFTGI getTGIMask();

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
        if (tgi != null && tgi.matches(this.getType().getTGIMask())) {
            return super.setTGI(tgi);
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
        /*
         * Order constraints: WAV comes before LTEXT because
         * WAV-TGI matches LTEXT-TGI; RAW comes last because
         * any TGI matches NULLTGI. 
         */
        EXEMPLAR(DBPFTGI.EXEMPLAR) {
            @Override
            DBPFExemplar createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFExemplar(dData, tgi, packager.isCompressed());
            }
        },
        COHORT(DBPFTGI.COHORT) {
            @Override
            DBPFCohort createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFCohort(dData, tgi, packager.isCompressed());
            }
        },
        PNG(DBPFTGI.PNG) {
            @Override
            DBPFPNG createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFPNG(dData, tgi, packager.isCompressed());
            }
        },
//        WAV(DBPFTGI.WAV) {
//            @Override
//            DBPFType createType(byte[] data, DBPFTGI tgi) {
//                // TODO not implemented yet, so use DBPFRaw
//                return null;
//            }
//        },
        LTEXT(DBPFTGI.LTEXT) {
            @Override
            DBPFLText createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFLText(dData, tgi, packager.isCompressed());
            }
        },
        DIRECTORY(DBPFTGI.DIRECTORY) {
            @Override
            DBPFDirectory createType(byte[] data, DBPFTGI tgi) {
                DBPFDirectory dir = new DBPFDirectory();
                dir.setData(data);
                return dir;
            }
        },
        LUA(DBPFTGI.LUA) {
            @Override
            DBPFLUA createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFLUA(dData, tgi, packager.isCompressed());
            }
        },
        LUA_GEN(DBPFTGI.LUA_GEN) {
            @Override
            DBPFLUAGen createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFLUAGen(dData, tgi, packager.isCompressed());
            }
        },
        RUL(DBPFTGI.RUL) {
            @Override
            DBPFRUL createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFRUL(dData, tgi, packager.isCompressed());
            }
        },
        FSH(DBPFTGI.FSH) {
            @Override
            DBPFFSH createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFFSH(dData, tgi, packager.isCompressed());
            }
        },
        S3D(DBPFTGI.S3D) {
            @Override
            DBPFS3D createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFS3D(dData, tgi, packager.isCompressed());
            }
        },
        SC4PATH(DBPFTGI.SC4PATH) {
            @Override
            DBPFSC4Path createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                byte[] dData = packager.decompress(data);
                return new DBPFSC4Path(dData, tgi, packager.isCompressed());
            }
        },
//        LD(DBPFTGI.LD) {
//            @Override
//            DBPFType createType(byte[] data, DBPFTGI tgi) {
//                // TODO not implemented yet, so use DBPFRaw
//                return null;
//            }
//        },
        RAW(DBPFTGI.NULLTGI) {
            @Override
            DBPFRaw createType(byte[] data, DBPFTGI tgi) {
                DBPFPackager packager = new DBPFPackager();
                packager.decompress(data);
                return new DBPFRaw(data, tgi, packager.isCompressed(), packager.getDecompressedSize());
            }
        };
        
        private final DBPFTGI tgiMask;
        
        private Type(DBPFTGI tgiMask) {
            this.tgiMask = tgiMask;
        }
        
        /**
         * Returns the corresponding generic TGI mask constant matching this Type.
         * 
         * @return the TGI mask.
         */
        public DBPFTGI getTGIMask() {
            return this.tgiMask;
        }
        
        /**
         * Creates a {@code DBPFType} from the byte data of an entry.
         * 
         * @param data the byte data of an entry as read from a file.
         *          May be compressed or uncompressed.
         * @param tgi the TGI of the entry.
         * @return a new {@code DBPFType} of this {@code Type} or
         *          {@code null} if this {@code Type} is not supported. 
         */
        abstract DBPFType createType(byte[] data, DBPFTGI tgi);
    }
}
