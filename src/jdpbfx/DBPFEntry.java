package jdpbfx;

import java.nio.channels.ReadableByteChannel;

import jdpbfx.DBPFFile.DirectDBPFEntry;
import jdpbfx.types.DBPFType;

/**
 * An abstract form of an entry of a {@link DBPFFile}, representing an instance
 * of a subfile that may be contained in a DBPF file. The {@link DBPFFile.Writer}
 * takes these entries to write to a new DBPF file.
 * <p>
 * There are two fundamentally different implementing Subclasses:
 * <dl>
 * <dt><b>{@link DirectDBPFEntry}</b>
 * <dd>
 * A direct DBPF entry basically represents an entry of the index table of a DBPF
 * file. The entry inherently linked to this
 * {@code DBPFFile}, as it does not maintain a copy of its data, but holds a
 * link to the data stored on disk within the DBPF file. The only way to obtain
 * an instance of a {@code DirectDBPFEntry} is to call one of the
 * {@link DBPFFile#getEntry(long) getEntry} or {@link DBPFFile#getEntries() getEntries}
 * methods in {@code DBPFFile}.
 * <p>
 * A direct DBPF entry is best suited for DBPF file IO where its data is merely
 * transferred, but not changed, as the actual type represented by the entry does
 * not matter. It is not necessary to hold all the data in memory simultaneosly,
 * as the data will be read and discarded as needed.
 * <p>
 * <b>Attention</b> should be paid to the fact that the data is not permanently in memory.
 * It is the responsibility of the calling programm to ensure that the enclosing
 * DBPFFile is not modified or removed during the lifetime of the direct entry,
 * or its data may be lost. The behaviour in this case is unspecified and should
 * be avoided (see also {@link DBPFFile.Reader#readCached(java.io.File)
 * DBPFFile.Reader.readCached}).
 * 
 * <dt><b>{@link DBPFType}</b>
 * <dd>
 * A {@code DBPFType} usually holds its data itself and can exist without an
 * enclosing DBPF file. <b>Attention</b> should be paid to the fact that creating a large
 * amount of DBPFTypes will be memory-intensive, as all their data resides in heap space
 * of the JVM.
 * <p>
 * DBPFTypes may be obtained by calling one of the
 * {@link DirectDBPFEntry#createType() createType} methods of a {@code
 * DirectDBPFEntry}, whenever working with the actual type of the entry is
 * desired.
 * </dl>
 * 
 * @author memo
 *
 * @see DBPFType
 * @see DirectDBPFEntry
 * @see DBPFFile.Reader#readCached(java.io.File)
 */
public abstract class DBPFEntry {
    
    /**
     * The TGI of this entry.
     */
    protected DBPFTGI tgi;
    
    /**
     * Constructor. 
     * 
     * @param tgi the TGI of this entry.
     */
    protected DBPFEntry(DBPFTGI tgi) {
        this.tgi = tgi;
    }
    
    /**
     * Returns the TGI of this entry.
     * 
     * @return the TGI of this entry.
     */
    public DBPFTGI getTGI() {
        return this.tgi;
    }
    
    /**
     * Sets the TGI of this entry to the specified TGI.
     * 
     * @param tgi the new TGI.
     * @return TRUE if the TGI was successfully set,
     *         FALSE otherwise (as specified by the implementing subclasses).
     */
    public abstract boolean setTGI(DBPFTGI tgi);

    /**
     * Creates an array with the data from this entry.
     * 
     * @return the data array,
     *      or {@code null} in case an IO issue occured.
     *      
     * @see #createDataChannel()
     */
    public abstract byte[] createData();
    
    /**
     * Creates a {@link ReadableByteChannel} that can be used to read the data
     * of this entry in buffered mode. This method is an alternative to the
     * {@link #createData()} method, mainly to avoid the creation of large
     * byte arrays in case the underlying data is huge.
     * <p>
     * It is guaranteed that the {@link DBPFFile.Writer} will only ever use
     * this method instead of {@code createData} in order to avoid the
     * aforementioned problem. The {@code createData} method is mainly
     * for convenience.
     * 
     * @return the channel providing the data of this entry,
     *      or {@code null} in case an IO issue occured.
     */
    public abstract ReadableByteChannel createDataChannel();
}
