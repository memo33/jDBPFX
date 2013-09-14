package jdpbfx;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;

import jdpbfx.types.DBPFCohort;
import jdpbfx.types.DBPFDirectory;
import jdpbfx.types.DBPFExemplar;
import jdpbfx.types.DBPFFSH;
import jdpbfx.types.DBPFLText;
import jdpbfx.types.DBPFLUA;
import jdpbfx.types.DBPFPNG;
import jdpbfx.types.DBPFRUL;
import jdpbfx.types.DBPFRaw;
import jdpbfx.types.DBPFS3D;
import jdpbfx.types.DBPFSC4Path;
import jdpbfx.types.DBPFType;
import jdpbfx.util.DBPFPackager;
import jdpbfx.util.DBPFUtil;

/**
 * The DBPFFile class encapsulates the header data and entry list as read from
 * disk of a DBPF formatted file.
 * <p>
 * Instances of this class can be obtained by calling one of the static
 * {@link DBPFFile.Reader#read(File)}, {@link Reader#readMapped(File) readMapped}
 * or {@link Reader#readCached(File) readCached} methods. For a discussion of these
 * methods, see the Warning below.
 * <p>
 * Writing a DBPF formatted file to disk is handled by the static
 * {@link DBPFFile.Writer} {@code write} and {@code update} methods.
 * <p>
 * The header data itself is encapsulated in the inner {@link DBPFFile.Header}
 * class and can be obtained from the public field {@link #header} of an instance
 * of {@code DBPFFile}.
 * <p>
 * To obtain the entry list/index table of a DBPF file, the method
 * {@link #getEntries()} can be used. Its entries are of type
 * {@link DirectDBPFEntry}.
 * 
 * <dl><dt>
 * <b>Warning:</b>
 * <dd>
 * Reading and writing of DBPF files involves several problems. The entry list
 * maintained by an instance of this class only holds file pointers to the
 * underlying file on disk, but does not copy all the data into heap space.
 * For a {@code DBPFFile} created via the {@link Reader#read(File)} method this means
 * that its {@code DirectDBPFEntries} cannot be written to the same file they are read
 * from (which will result in an {@link UnsupportedOperationException}), unless
 * you convert all the entries to {@link DBPFType DBPFTypes} (which in turn is
 * memory-intensive and does not take advantage of streaming).
 * <p>
 * It <i>is</i> possible to write to a <i>different</i> file, but it depends on
 * the source file on disk being unchanged. It is therefore the responsibility of
 * the calling program to ensure that the source file does not get changed or removed
 * during the lifetime of the {@code DBPFFile} instance that was opened on that file.
 * <p>
 * The {@link Reader#readMapped(File)} method maps the entire file into memory.
 * Instantiating a DBPF file with this method may be minimally slower than
 * calling the {@code read(File)} method, but subsequent converting to Types
 * and writing of DBPF files is supposedly <i>much</i> faster. The downside of
 * this is that (depending on the operating system) mapping a file may cause
 * unforseeable issues, especially if the mapping is still in memory (even though
 * the DBPF file has already been deallocated) and a process tries to access
 * the file otherwise. If there is no more need for the mapping, {@link #releaseMapping()}
 * <i>attempts</i> to close the {@code MappedByteBuffer}. For more information on this
 * issue, see {@link FileChannel#map(FileChannel.MapMode, long, long)} and bug
 * <a href="http://bugs.sun.com/view_bug.do?bug_id=4724038">JDK-4724038</a>.
 * <p>
 * To avoid the problem of overwriting the source file and the need of preserving
 * the state of the source file, the method {@link Reader#readCached(File)} is
 * offered. It copies the source file into temporary memory (thus creating a
 * snapshot of the current state of the file) and guarantees it will not be changed
 * until the JVM is terminated. Invoking {@code readCached} on the same file twice
 * (with the same last-modified timestamp) will only involve one copy-process,
 * if both calls come from the same JVM. An attempt to delete the temp
 * file is made upon termination of the JVM, but the deletion cannot be assured,
 * especially because the temp file will also be mapped into memory. You should
 * therefore <i>always</i> call the {@code releaseMapping()} method on a cached {@code
 * DBPFFile}, before discarding the instance.
 * <p>
 * The temporary files will be copied into the folder "jdbpx_tmp" which will be
 * created in the System's temporary directory, given by
 * {@code System.getProperty("java.io.tmpdir")}. You may occasionally want to
 * check this directory for unused remaining files.
 * 
 * <dt>
 * <b>Warning:</b>
 * <dd>None of these Classes are Thread-safe. It is advisable to handle multi-threaded
 * setups with extra caution. Consider using a single event-dispatching thread.
 * </dl>
 * 
 * @author jondor
 * @author memo
 */
public class DBPFFile {

    /**
     * Holds the header data of this DBPF file.
     */
    public final Header header;
    
    private LinkedHashMap<Long, DirectDBPFEntry> entryMap;
    private HashMap<DBPFTGI, Long> tgiMap;
    private File file;
    private File tmpFile;
    private MappedByteBuffer mappedByteBuffer;

    /**
     * Instantiates all of the fields of this DBPFFile object and sets the
     * capacity of the entry map.
     *
     * @param file The {@link File} object that this DBPFFile
     *                 corresponds to for later reading of individual entries.
     * @param majorVersion The major version number as read from the file.
     * @param minorVersion The minor version number as read from the file.
     * @param dateCreated The date this file was created as read from the file.
     * @param dateModified The date this file was last modified as read from the
     *                     file.
     * @param indexType The index version as read from the file. (Indicating
     *                  3 or 4 32bit per file identifiers.)
     * @param indexEntryCount The number of entries contained in the index.
     * @param indexOffsetLocation The byte offset of the start of the index.
     * @param indexSize The byte size of the index.
     */
    private DBPFFile(File filename, long majorVersion, long minorVersion,
                    long dateCreated, long dateModified, long indexType,
                    long indexEntryCount, long indexOffsetLocation, long indexSize) {
        this.file = filename;
        this.header = new Header();
        this.header.majorVersion = majorVersion;
        this.header.minorVersion = minorVersion;
        this.header.dateCreated = dateCreated;
        this.header.dateModified = dateModified;
        this.header.indexType = indexType;
        this.header.indexEntryCount = indexEntryCount;
        this.header.indexOffsetLocation = indexOffsetLocation;
        this.header.indexSize = indexSize;
        this.entryMap = new LinkedHashMap<Long, DirectDBPFEntry>((int)(indexEntryCount / 0.75) + 1);
        this.tgiMap = new HashMap<DBPFTGI, Long>((int)(indexEntryCount / 0.75) + 1);
    }

    private boolean addEntry(DirectDBPFEntry entry) {
        if (entry == null)
            return false;
        entryMap.put(entry.index, entry);
        tgiMap.put(entry.getTGI(), entry.index);
        return true;
    }

    /**
     * Returns a string containing the raw fields of this File.
     * <br/>
     * Data is in the following format:
     * <br/>
     * <br/>
     * File: <code>file</code><br/>
     * Version: <code>major.minor</code>, Created: <code>dateCreated</code>,
     * Modified: <code>dateModified</code><br/>
     * IndexType: <code>indexType</code>, IndexEntryCount:
     * <code>indexEntryCount</code>, IndexOffsetLocation:
     * <code>indexOffsetLocation</code>, IndexSize: <code>indexSize</code><br/>
     * 
     * @return A String with the above information.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ");
        sb.append(getFile());
        sb.append("\n");
        sb.append(this.header.toString());
        return sb.toString();
    }

    /**
     * Includes all of the information in the toString method and appends the
     * toString method of each contained {@link DBPFFile.DirectDBPFEntry}.
     *
     * @return A String with the above information.
     *
     * @see #toString()
     * @see DBPFFile.DirectDBPFEntry#toString()
     */
    public String toDetailString() {
        StringBuilder sb = new StringBuilder(toString());
        for (DirectDBPFEntry element : entryMap.values()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the {@link File} associated with this DBPFFile.
     * 
     * @return the file.
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Returns the file name associated with this DBPFFile.
     * 
     * @return the file name.
     */
    public String getName() {
        return this.getFile().getName();
    }

    /**
     * Returns the TGIs stored in this file as an unmodifiable {@link Collection},
     * does not include duplicated TGIs. The order of the TGIs returned is arbitrary.
     * <p>
     * <b>Important:</b> Do not use this function to read the file for subsequent writing, as
     * duplicated TGIs will be lost. Instead, use {@link #getEntries()}.
     *
     * @return a collection of the TGIs stored in this file.
     */
    public Collection<DBPFTGI> getTGIs() {
        return Collections.unmodifiableSet(this.tgiMap.keySet());
    }

    /**
     * Returns an unmodifiable {@link Collection} of all entries contained in this
     * file, preserving their order within the file.
     * 
     * @return an ordered collection of the entries stored in this file.
     */
    public Collection<DirectDBPFEntry> getEntries() {
        return Collections.unmodifiableCollection(this.entryMap.values());
    }
    
    /**
     * Returns the entry at a particular index within this file.  
     * 
     * @param index the index. 
     * @return the entry at the specified index within this file,
     *      or {@code null} if the index is larger than the number of
     *      entries contained.
     */
    public DirectDBPFEntry getEntry(long index) {
        return entryMap.get(index);
    }
    
    /**
     * Returns an entry of a particular TGI from this file.
     * <p>
     * <b>Important:</b> If there are multiple entries for this TGI, it is
     * unspecified which one of them is returned. It is advisable to use
     * {@link #getEntry(long)} instead. The method {@link #countTGI(DBPFTGI)}
     * may be used to determine the number of occurrences of the TGI.
     * 
     * @param tgi the TGI.
     * @return an entry of the specified TGI, or {@code null} if there
     *      is no such TGI within this file.
     *      
     * @see #getEntry(long)
     * @see #getEntries()
     */
    public DirectDBPFEntry getEntry(DBPFTGI tgi) {
        return this.getEntry(tgiMap.get(tgi));
    }
    
    /**
     * Returns a count of entries matching the specified TGI mask. TGIs may
     * include null (-1) components that will be masked against.
     * <p>
     * {@link DBPFTGI} constants are useful here.
     *
     * @param tgiMask The TGI mask to count entries against.
     *
     * @return The number of entries that match the DBPFTGI mask passed in.
     *
     * @see DBPFTGI#matches(DBPFTGI)
     */
    public int countTGI(DBPFTGI tgiMask) {
        int count = 0;
        for (DirectDBPFEntry entry : this.entryMap.values()) {
            if (entry.getTGI().matches(tgiMask)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Specifies whether this file was read in mapped (or cached) mode.
     * 
     * @return TRUE if this DBPF file was read via {@code readMapped} or
     *      {@code readCached}, and the method {@link #releaseMapping()} has not
     *      been invoked yet,
     *      FALSE otherwise.
     */
    public boolean isMapped() {
        return this.mappedByteBuffer != null;
    }
    
    /**
     * Specifies whether this file was read in cached mode.
     * 
     * @return TRUE if the temporary created file and the mapping both
     *      (still) exist,
     *      FALSE otherwise.
     */
    public boolean isCached() {
        return this.tmpFile != null && this.tmpFile.exists()
                && this.isMapped();
    }
    
    /**
     * Attempts to release the {@link MappedByteBuffer} associated with
     * the mapping of this file.
     * <p>
     * No guarantee can be made as for whether the buffer will actually be
     * closed. It may still block the file.
     * <p>
     * Calling this method on a file that is not mapped has no effect.
     * Calling this method more than once has no effect.
     * 
     * @see DBPFFile
     * @see Reader#readMapped(File)
     */
    public void releaseMapping() {
        if (this.isMapped()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (DBPFFile.this) {
                        DBPFFile.this.mappedByteBuffer = null;
                        DBPFFile.this.tmpFile = null;
                        System.gc();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * Encapsulates the elements that are specific to the Header of the
     * DBPF file.
     * 
     * @author memo
     */
    public class Header {
        /**
         * Header size of the DBPF file (96 bytes).
         */
        public static final long HEADER_SIZE = 0x60; // =96dec
        
        private long majorVersion;
        private long minorVersion;
        private long dateCreated;
        private long dateModified;
        private long indexType;
        private long indexEntryCount;
        private long indexOffsetLocation;
        private long indexSize;
        
        private Header() {};
        
        /**
         * @return The major version number.
         */
        public long getMajorVersion() {
            return majorVersion;
        }
    
        /**
         * @return The minor version number.
         */
        public long getMinorVersion() {
            return minorVersion;
        }
    
        /**
         * @return The creation date as read from the file.
         */
        public long getDateCreated() {
            return dateCreated;
        }
    
        /**
         * @return The last modified date as read from the file.
         */
        public long getDateModified() {
            return dateModified;
        }
    
        /**
         * @return The index type specifier.
         */
        public long getIndexType() {
            return indexType;
        }
    
        /**
         * @return The number of entries as read from the file.
         */
        public long getIndexEntryCount() {
            return indexEntryCount;
        }
    
        /**
         * @return The byte offset of the index.
         */
        public long getIndexOffsetLocation() {
            return indexOffsetLocation;
        }
    
        /**
         * @return The byte size of the index.
         */
        public long getIndexSize() {
            return indexSize;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Version: ");
            sb.append(majorVersion);
            sb.append(".");
            sb.append(minorVersion);
            sb.append(", Created: ");
            sb.append(DBPFUtil.formatDate(dateCreated));
            sb.append(", Modified: ");
            sb.append(DBPFUtil.formatDate(dateModified));
            sb.append("\nIndexType: ");
            sb.append(indexType);
            sb.append(", IndexEntryCount: ");
            sb.append(indexEntryCount);
            sb.append(", IndexOffsetLocation: ");
            sb.append(indexOffsetLocation);
            sb.append(", IndexSize: ");
            sb.append(indexSize);
            sb.append("\n");
            return sb.toString();
        }
    }

    /**
     * Provides static methods for reading a DBPF formatted file from disk.
     *
     * @see DBPFFile
     */
    public static class Reader {
        
        private Reader() {};

        private static final String TMP_DIR_NAME = "jdbpfx_tmp";
        private static final File TMP_DIR =
                new File(System.getProperty("java.io.tmpdir"), TMP_DIR_NAME);
        private static final String JVM_ID;
        private static final int MAX_POLL_TIME = 10000;
        private static final int HEADER_BUFFER_SIZE = 4 * 11;
        
        static {
            Random rand = new Random();
            JVM_ID = String.format("%08X", rand.nextInt());
        }

        /**
         * Checks only the fileType of the file, if it is DBPF.
         *
         * @param filename
         *            The file.
         * @return TRUE, if file is a DBPF file; FALSE, if not or error occured.
         */
        public static boolean checkFileType(File filename) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(filename, "r");
                String fileType = readChars(raf, 4);
                if (fileType.equals(DBPFUtil.MAGICNUMBER_DBPF)) {
                    return true;
                }
            } catch (FileNotFoundException e) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] File not found: " + filename, e);
            } catch (IOException e) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] IOException for file: " + filename, e);
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] IOException for file: " + filename, e);
                    }
                }
            }
            return false;
        }

        /**
         * Reads a DBPF file.
         * <p>
         * This is best suited for short-lived DBPF files, the content of
         * which does not change on disk, or it does not matter if it does, for
         * example if you only scan the TGIs of a file, or if the file is small.
         * <p>
         * <b>Important:</b> Observe the annotations in {@link DBPFFile}
         *
         * @param filename
         *            The file of the DBPF file to read.
         * @return The DBPF file object.
         * 
         * @throws DBPFFileFormatException if {@link #checkFileType} returns {@code false}.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         * 
         * @see #readMapped(File)
         * @see #readCached(File)
         * @see DBPFFile
         */
        public static DBPFFile read(File filename) throws DBPFFileFormatException, FileNotFoundException, IOException {
            
            RandomAccessFile raf = null;
            try {
                raf  = new RandomAccessFile(filename, "r");
                DBPFUtil.LOGGER.log(Level.INFO, "[DBPFFile.Reader] Reading {0}", filename.getName());
                // Analyze the fileType
                String fileType = readChars(raf, 4);
                if (fileType.equals(DBPFUtil.MAGICNUMBER_DBPF)) {
                    ByteBuffer buf = ByteBuffer.allocate(HEADER_BUFFER_SIZE);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    raf.readFully(buf.array());
                    long majorVersion = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long minorVersion = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    buf.position(20); //raf.skipBytes(12);
                    long dateCreated = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long dateModified = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long indexType = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long indexEntryCount = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long indexOffsetLocation = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                    long indexSize = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);

                    DBPFFile dbpfFile = new DBPFFile(filename, majorVersion, minorVersion,
                                            dateCreated, dateModified, indexType,
                                            indexEntryCount, indexOffsetLocation, indexSize);

                    // Read the index
                    raf.seek(indexOffsetLocation);
                    buf = ByteBuffer.allocate((int) indexSize);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    raf.readFully(buf.array());
                    for (int i = 0; i < indexEntryCount; i++) {
                        long tid = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                        long gid = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                        long iid = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                        long offset = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                        long size = buf.getInt() & 0xffffffffL; //readUint32(raf, 4);
                        DBPFTGI tgi = DBPFTGI.valueOf(tid, gid, iid);
                        DirectDBPFEntry entry = dbpfFile.new DirectDBPFEntry(tgi, offset, size, i);
                        dbpfFile.addEntry(entry);

                        // System.out.println(entry.toString());
                    }
                    return dbpfFile;
                } else {
                    throw new DBPFFileFormatException("Not a DBPF formatted file: " + filename);
                }
//            } catch (FileNotFoundException e) {
//                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] File not found: " + filename, e);
//                dbpfFile = null;
//            } catch (IOException e) {
//                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] IOException for file: " + filename, e);
//                dbpfFile = null;
            } finally {
                closeAll(raf);
            }
        }
        
        /**
         * Reads a DBPF file and maps the file from disk to memory.
         * <p>
         * This is best suited for similar purposes as {@link #read(File)},
         * with the difference that subsequent writing of a large amount of
         * entries will be much faster, which is suitable for large files and
         * files for which the {@code createType} methods of {@code DirectDBPFEntry}
         * will be called a lot.
         * <p>
         * <b>Important:</b> Always release the mapping via {@link DBPFFile#releaseMapping()}
         * before discarding the DBPFFile.
         * <p>
         * <b>Important:</b> Observe the annotations in {@link DBPFFile}
         * 
         * @param filename
         *            The file of the DBPF file to read.
         * @return The DBPF file object.
         * 
         * @throws DBPFFileFormatException if {@link #checkFileType} returns {@code false}.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         * 
         * @see #read(File)
         * @see #readCached(File)
         * @see DBPFFile
         * @see FileChannel#map(FileChannel.MapMode, long, long)
         */
        public static DBPFFile readMapped(File filename) throws DBPFFileFormatException, FileNotFoundException, IOException {
            return readMapped(filename, filename);
        }

        private static DBPFFile readMapped(File filename, File mapFile) throws DBPFFileFormatException, FileNotFoundException, IOException {
            DBPFFile dbpfFile = read(filename);
            
            FileInputStream fis = null;
            FileChannel fc = null;
            try {
                fis = new FileInputStream(mapFile);
                fc = fis.getChannel();
                MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                dbpfFile.mappedByteBuffer = mbb;
                return dbpfFile;
//                } catch (FileNotFoundException e) {
//                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] File not found: " + mapFile, e);
//                    dbpfFile = null;
//                } catch (IOException e) {
//                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] IOException for file: " + mapFile, e);
//                    dbpfFile = null;
            } finally {
                closeAll(fc, fis);
            }
        }

        private static void initTmpDir() throws IOException {
            if (!TMP_DIR.exists()) {
                if (!TMP_DIR.mkdirs() && !TMP_DIR.exists()) {
                    throw new IOException("Could not create temp dir: " + TMP_DIR);
                }
            }
            TMP_DIR.deleteOnExit(); // TODO will only work if dir is empty?
        }
        
        /**
         * Reads a DBPF file, creating a temporary copy of the file, and maps
         * the temporary file from disk to memory.
         * <p>
         * This is best suited for long-lived DBPF files, the current content
         * of which must assuredly be retained for later reading and writing, no
         * matter if the underlying file on disk gets changed in between. This is
         * achieved by creating a temporary copy (a snapshot) of the file. Due
         * to this copy-process, this method takes considerably more time than the previous
         * two - any other operations on the created DBPF file are just as fast as
         * with a DBPF file obtained via {@link #readMapped(File)}.
         * <p>
         * <b>Important:</b> Always release the mapping via {@link DBPFFile#releaseMapping()}
         * before discarding the DBPFFile.
         * <p>
         * <b>Important:</b> Observe the annotations in {@link DBPFFile}
         * 
         * @param filename
         *            The file of the DBPF file to read.
         * @return The DBPF file object.
         * 
         * @throws DBPFFileFormatException if {@link #checkFileType} returns {@code false}.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         *
         * @see #read(File)
         * @see #readMapped(File)
         * @see DBPFFile
         * @see FileChannel#map(FileChannel.MapMode, long, long)
         */
        public static DBPFFile readCached(File filename) throws DBPFFileFormatException, FileNotFoundException, IOException {
            initTmpDir();
            
            File tmpFile = new File(TMP_DIR, String.format("%s_%08X_%X_%s",
                    filename.getName(), filename.getAbsolutePath().hashCode(),
                    filename.lastModified(), JVM_ID));
            if (!tmpFile.exists()) {
                // copy file to temp file
                // TODO without blocking?
                DBPFUtil.LOGGER.log(Level.INFO, "[DBPFFile.Reader] Caching {0}", filename.getName());
                FileInputStream fis = null;
                FileOutputStream fos = null;
                FileChannel src = null;
                FileChannel target = null;
                try {
                    fis = new FileInputStream(filename);
                    fos = new FileOutputStream(tmpFile);
                    src = fis.getChannel();
                    target = fos.getChannel();
                    long size = src.size();
                    long count = target.transferFrom(src, 0, size);
                    if (count != size) {
                        throw new IOException("Could not transfer file: " + filename);
                    }
//                } catch (FileNotFoundException e) {
//                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] File not found: " + filename, e);
//                    return null;
//                } catch (IOException e) {
//                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Reader] IOException for file: " + filename, e);
//                    return null;
                } finally {
                    closeAll(src, fis, target, fos);
                }
            } else {
                // TODO temporary! should make use of existing copied files across different JVMs
                // if tmp file has not been copied completely yet, then poll in 0.2s time steps
                long fileLength = filename.length();
                for (int step = 200, total = 0; tmpFile.length() != fileLength; total += step) {
                    if (total >= MAX_POLL_TIME) {
                        throw new IOException("Maximum poll time exceeded. Cannot read cached file: " + tmpFile);
                    }
                    try {
                        Thread.sleep(step);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException("Interrupted during caching of file: " + filename);
                    }
                }
                DBPFUtil.LOGGER.log(Level.INFO, "[DBPFFile.Reader] File {0} is already cached", filename.getName());
            }
            tmpFile.deleteOnExit(); // okay since every JVM creates its own copy currently
            
            // tmp file exists now
            DBPFFile dbpfFile = readMapped(filename, tmpFile);
            assert dbpfFile != null;
            if (dbpfFile != null) {
                dbpfFile.tmpFile = tmpFile;
            }
            return dbpfFile;
        }
        
        private static void closeAll(Closeable... closeables) throws IOException {
            IOException cause = null;
            for (Closeable c : closeables) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (IOException e) {
//                        DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile." + sourceName + "] IOException for file: " + filename, e);
                        if (cause != null) {
                            cause = e;
                        }
                    }
                }
            }
            if (cause != null) {
                throw new IOException(cause);
            }
        }
        
        private static String readChars(RandomAccessFile raf, int length) throws IOException {
            byte[] b = new byte[length];
            raf.readFully(b);
            return new String(b, Charset.forName("US-ASCII"));
        }
    }
    
    /**
     * Provides static methods for writing a DBPF formatted file to disk.
     * <p>
     * None of the {@code write} or {@code update} methods update the delivered
     * DBPFFile objects, so in order to get an updated copy of DBPF file it is
     * necessary to either re-read the written file or (preferrably) retain a
     * copy of the written {@code DBPFEntries}.
     * <p>
     * A customization of the {@code update} methods can easily be provided
     * by the calling programm by using one of the {@code write} methods.
     *
     * @see DBPFFile
     */
    public static class Writer {
        
        private Writer() {};
        
        private static final int COMPRESSION_HEADER_LENGTH = 9;
        private static final int BUFFER_SIZE = 16 * 1024;
        
        /**
         * Updates the given DBPF file by the {@code DBPFEntries} in {@code writeList}.
         * <p>
         * Any entry from the dbpfFile that has a TGI (first occurrence only) which is
         * contained in the {@code writeList} will be replaced by the corresponding
         * entry from the writeList. Afterwards, any remaining non-written entry from
         * the writeList will be appended to the DBPF file. 
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         * 
         * @param dbpfFile a DBPF file. It specifies the destination location and
         *      the creation date.
         * @param writeList a list of entries that have been updated.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean update(DBPFFile dbpfFile, Collection<? extends DBPFEntry> writeList) throws UnsupportedOperationException, FileNotFoundException, IOException {
            return update(dbpfFile, writeList, dbpfFile.getFile(), true);
        }

        /**
         * Takes the {@code DBPFEntries} from the given DBPF file, updates them
         * by the entries in {@code writeList} and writes everything to a new file
         * location, thereby preserving the creation date.
         * <p>
         * Any entry from the dbpfFile that has a TGI (first occurrence only) which is
         * contained in the {@code writeList} will be replaced by the corresponding
         * entry from the writeList. Afterwards, any remaining non-written entry from
         * the writeList will be appended to the DBPF file. 
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         * 
         * @param dbpfFile a DBPF file specifying the creation date.
         * @param writeList a list of entries that have been updated.
         * @param newFile the new file location.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean update(DBPFFile dbpfFile, Collection<? extends DBPFEntry> writeList, File newFile) throws UnsupportedOperationException, FileNotFoundException, IOException {
            return update(dbpfFile, writeList, newFile, false);
        }
        
        /**
         * Takes the {@code DBPFEntries} from the given DBPF file, updates them
         * by the entries in {@code writeList} and writes everything to a new file
         * location, thereby preserving the creation date if {@code preserveDateCreated}
         * is set.
         * <p>
         * Any entry from the dbpfFile that has a TGI (first occurrence only) which is
         * contained in the {@code writeList} will be replaced by the corresponding
         * entry from the writeList. Afterwards, any remaining non-written entry from
         * the writeList will be appended to the DBPF file. 
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         * 
         * @param dbpfFile a DBPF file specifying the creation date.
         * @param writeList a list of entries that have been updated.
         * @param newFile the new file location.
         * @param preserveDateCreated TRUE if the creation date is to preserved, FALSE if
         *      the current date is to be used.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean update(DBPFFile dbpfFile, Collection<? extends DBPFEntry> writeList, File newFile, boolean preserveDateCreated) throws UnsupportedOperationException, FileNotFoundException, IOException {
            // create map view of writeList for fast look-up
            int writeListSize = writeList.size();
            Map<DBPFTGI, DBPFEntry> updatedEntries =
                    new LinkedHashMap<DBPFTGI, DBPFEntry>((int) (writeListSize / 0.75) + 1);
            for (DBPFEntry entry : writeList) {
                updatedEntries.put(entry.getTGI(), entry);
            }
            // collect all the entries to write, replaced by updated types
            Queue<DBPFEntry> updatedWriteList =
                    new ArrayDeque<DBPFEntry>(dbpfFile.entryMap.size() + writeListSize);
            for (DBPFEntry entry : dbpfFile.entryMap.values()) {
                DBPFEntry updatedEntry = updatedEntries.remove(entry.getTGI());
                updatedWriteList.add(updatedEntry == null ? entry : updatedEntry);
            }
            // add remaining types from writeList
            updatedWriteList.addAll(updatedEntries.values());
            return write(newFile, updatedWriteList, preserveDateCreated ? dbpfFile.header.getDateCreated() : (System.currentTimeMillis() / 1000));
        }
        
        /**
         * Writes a list of DBPFEntries to a DBPF file, preserving the date created field.
         * <p>
         * This method will completely overwrite the current contents of the file on disk.
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         *
         * @param dbpfFile
         *      the DBPFFile whose creation date should be preserved and which links
         *      to the file on disk.
         * @param writeList
         *      the list of DBPFEntries to write to file.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean write(DBPFFile dbpfFile, Collection<? extends DBPFEntry> writeList) throws UnsupportedOperationException, FileNotFoundException, IOException {
            return write(dbpfFile.getFile(), writeList, dbpfFile.header.getDateCreated());
        }

        /**
         * Writes a list of DBPFEntries to a DBPF file, preserving the date created field.
         * <p>
         * This method will completely overwrite the current contents of the file on disk.
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         *
         * @param dbpfFile
         *      the DBPFFile whose creation date should be preserved and which links
         *      to the file on disk.
         * @param writeList
         *      the list of DBPFEntries to write to file.
         * @param newFile
         *      the new file location.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean write(DBPFFile dbpfFile, Collection<? extends DBPFEntry> writeList, File newFile) throws UnsupportedOperationException, FileNotFoundException, IOException {
            return write(newFile, writeList, dbpfFile.header.getDateCreated());
        }

        /**
         * Writes a list of DBPFEntries to a DBPF formatted file.
         * <p>
         * Any Directory file will be skipped, instead an appropriate Directory file
         * will be appended as last entry to the DBPF file.
         *
         * @param file
         *      the new file location.
         * @param writeList
         *      the list of DBPFEntries to write to file.
         * @return TRUE, if successfully written; FALSE, otherwise.
         * 
         * @throws UnsupportedOperationException
         *      if the file is not cached and the writeList contains DirectDBPFEntries
         *      and the target file is the same as the source file,
         *      which would result in the source file being overwritten.
         * @throws FileNotFoundException if the file does not exist or is inaccessible.
         * @throws IOException in case of an IO issue.
         */
        public static boolean write(File file, Collection<? extends DBPFEntry> writeList) throws UnsupportedOperationException, FileNotFoundException, IOException {
            return write(file, writeList, System.currentTimeMillis() / 1000);
        }

        private static void testForOverwritingCollision(File file, Collection<? extends DBPFEntry> writeList) throws UnsupportedOperationException {
            for (DBPFEntry entry : writeList) {
                if (entry instanceof DirectDBPFEntry) {
                    DirectDBPFEntry directEntry = ((DirectDBPFEntry) entry);
                    if (!directEntry.getEnclosingDBPFFile().isCached() &&
                            directEntry.getEnclosingDBPFFile().getFile().equals(file)) {
                        // if the dbpf file is not cached into temp memory, overwriting
                        // the same file risks bad data
                        throw new UnsupportedOperationException("Cannot read from and write" +
                        		" to the same file, if it is not cached into temporary" +
                                " memory. Instead, write to a different file or try" +
                                " to cache the file: " + file);
                    }
                }
            }
        }
        
        private static boolean write(File file, Collection<? extends DBPFEntry> writeList, long dateCreated)
                throws UnsupportedOperationException, FileNotFoundException, IOException {
            // make sure not to overwrite a file we want to read from
            testForOverwritingCollision(file, writeList);

            RandomAccessFile raf = null;
            FileChannel fc = null;
            try {
                raf = new RandomAccessFile(file, "rw");
                fc = raf.getChannel();
                DBPFUtil.LOGGER.log(Level.INFO, "[DBPFFile.Writer] Writing {0}", file.getName());
                
                final ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                long indexOffsetLocation = DBPFFile.Header.HEADER_SIZE;
                
                writeHeader(raf, fc, buf, indexOffsetLocation, dateCreated);
                
                // Write rawData, remember offset position and store length
                // Also remember information about compressed files for directory file
                Queue<DirListData> dirData = new ArrayDeque<DirListData>();
                Queue<WriteListData> indexData = new ArrayDeque<WriteListData>();
                final ByteBuffer headerBuf = ByteBuffer.allocate(COMPRESSION_HEADER_LENGTH); // for header of compressed files, allocated here only once for efficiency 
                for (DBPFEntry entry : writeList) {
                    if (entry.getTGI().matches(DBPFTGI.DIRECTORY)) {
                        continue;
                    }
                    indexOffsetLocation += transferData(entry,
                            indexOffsetLocation, dirData, indexData, buf, fc, headerBuf);
                }
                // build directory file
                if (!dirData.isEmpty()) {
                    DBPFDirectory dir = buildDirectoryFile(dirData);
                    indexOffsetLocation += transferData(dir,
                            indexOffsetLocation, dirData, indexData, buf, fc, headerBuf);
                }
                flushBuffer(buf, fc);
                // write index table
                writeIndex(fc, buf, indexData);
                // Update index entry count, location and size
                updateHeader(fc, buf, indexOffsetLocation, indexData.size());
                fc.force(false);
//            } catch (FileNotFoundException e) {
//                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Writer] File not found: " + file, e);
//                return false;
//            } catch (IOException e) {
//                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Writer] IOException for file: " + file, e);
//                return false;
            } finally {
                Reader.closeAll(fc, raf);
            }
            return true;
        }
        
        private static void writeHeader(RandomAccessFile raf, FileChannel fc, ByteBuffer buf, long indexOffsetLocation, long dateCreated) throws IOException {
            // create necessary file data
            String fileType = DBPFUtil.MAGICNUMBER_DBPF;
            long majorVersion = 1;
            long minorVersion = 0;
            //long dateCreated = System.currentTimeMillis() / 1000;
            long dateModified = System.currentTimeMillis() / 1000;
            long indexType = 7;
            long indexEntryCount = 0;//writeList.size();
            long indexSize = 0;//5 * 4 * indexEntryCount;

            // set minimum file size
            long count = indexOffsetLocation;// + indexSize;
            raf.setLength(count);
            
            // Write header
            buf.put(fileType.getBytes(Charset.forName("US-ASCII")));//writeChars(raf, fileType);
            buf.putInt((int) majorVersion);//writeUINT32(raf, majorVersion, 4);
            buf.putInt((int) minorVersion);//writeUINT32(raf, minorVersion, 4);
            buf.position(24);//writeUINT32(raf, 0, 12);
            buf.putInt((int) dateCreated);//writeUINT32(raf, dateCreated, 4);
            buf.putInt((int) dateModified);//writeUINT32(raf, dateModified, 4);
            buf.putInt((int) indexType);//writeUINT32(raf, indexType, 4);
            buf.putInt((int) indexEntryCount);//writeUINT32(raf, indexEntryCount, 4);
            buf.putInt((int) indexOffsetLocation);//writeUINT32(raf, indexOffsetLocation, 4);
            buf.putInt((int) indexSize);//writeUINT32(raf, indexSize, 4);
            buf.position((int) DBPFFile.Header.HEADER_SIZE);//writeUINT32(raf, 0, 48);
            flushBuffer(buf, fc);
        }
        
        private static void updateHeader(FileChannel fc, ByteBuffer buf, long indexOffsetLocation, long indexEntryCount) throws IOException {
            // Update index entry count, location and size
            long indexSize = 5 * 4 * indexEntryCount;
            fc.position(36);
            buf.putInt((int) indexEntryCount);
            buf.putInt((int) indexOffsetLocation);
            buf.putInt((int) indexSize);
            flushBuffer(buf, fc);
        }
        
        private static void writeIndex(FileChannel fc, ByteBuffer buf, Queue<WriteListData> indexData) throws IOException {
          for (WriteListData data : indexData) {
              buf.putInt((int) data.tgi.getType());//writeUINT32(raf, data.tgi.getType(), 4);
              buf.putInt((int) data.tgi.getGroup());//writeUINT32(raf, data.tgi.getGroup(), 4);
              buf.putInt((int) data.tgi.getInstance());//writeUINT32(raf, data.tgi.getInstance(), 4);
              buf.putInt((int) data.offset);//writeUINT32(raf, data.offset, 4);
              buf.putInt((int) data.size);//writeUINT32(raf, data.size, 4);
              if (buf.remaining() < 20) {
                  flushBuffer(buf, fc);
              }
          }
          flushBuffer(buf, fc);
        }

        private static DBPFDirectory buildDirectoryFile(Queue<DirListData> dirEntries) {
            ByteBuffer buf = ByteBuffer.allocate(dirEntries.size() * 16);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            for (DirListData dataEntry: dirEntries) {
                buf.putInt((int) dataEntry.tgi.getType());
                buf.putInt((int) dataEntry.tgi.getGroup());
                buf.putInt((int) dataEntry.tgi.getInstance());
                buf.putInt((int) dataEntry.decompressedSize);
            }
            return new DBPFDirectory(buf.array());
        }
        
        /**
         * Transfers this entry's data into the target channel, starting at the
         * channel's current position combined with possible remaining data in the delivered
         * <code>ByteBuffer</code>. The data will be retrieved by calling the
         * <code>createData</code> method.<p>
         * 
         * The <code>offset</code> is the offset to which this data will
         * be transferred. You will have to ensure to add this method's return value
         * (the length of the data) to your seperate index offset location pointer.<p>
         * 
         * The buffer will not be emptied by this method, unless it is full. You will have
         * to call {@link #flushBuffer} manually, the last time you transfer data.<p>
         * 
         * @param entry the entry whose data is to be transferred.
         * @param offset the offset of the DBPF file's index before
         *      calling this method. This will be the offset of the data in the
         *      written file.
         * @param dirData a queue to which this entry's data will be added that are
         *      relevant for the DBPF file's index table.
         * @param indexData a queue to which this entry's data will be added that are
         *      relevant for the DBPF file's directory file (that is, if this entry
         *      is compressed).
         * @param buf a <code>ByteBuffer</code> that will be used for buffering of
         *      the bytes that are to be transferred. Does not need to be empty. 
         * @param target the channel the data will be transferred to, relative to
         *      its current position.
         * @param headerBuf a <code>ByteBuffer</code> of size 9 that may be used
         *      to read the header of compressed files.
         * @return the number of bytes that were transferred, possibly zero
         *      if the data could not successfully be read.
         * @throws IOException if any type of IO issue occurs, specifically those
         *      exceptions thrown by {@link FileChannel#write(ByteBuffer)}.
         */
        private static long transferData(DBPFEntry entry, long offset,
                Queue<DirListData> dirData, Queue<WriteListData> indexData,
                ByteBuffer buf, FileChannel target, ByteBuffer headerBuf) throws IOException {

            ReadableByteChannel src = null;
            int size = 0;
            try {
                src = entry.createDataChannel();
                if (src == null) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.DBPFEntry] " +
                            "Cannot read data of TGI: " + entry.getTGI());
                    return 0;
                } // else
    
                boolean tooShort = false; // for compression
                int pos = 0;
                {
                    // read first nine bytes to determine possible compression
                    headerBuf.clear();
                    for (int count = 0; headerBuf.hasRemaining() && count != -1; ) {
                        count = src.read(headerBuf);
                    }
                    headerBuf.flip();
                    // put headerBuf's array into larger buf
                    if (buf.remaining() < headerBuf.remaining()) {
                        flushBuffer(buf, target);
                    }
                    if (headerBuf.remaining() < COMPRESSION_HEADER_LENGTH) {
                        tooShort = true;
                    }
                    pos = headerBuf.remaining();
                    buf.put(headerBuf);
                }

                // write this entry's data to the target channel using the buffer
                // pos may already have been increased by length of header
                for (int count; ; pos += count) {
                    count = src.read(buf);
                    if (!buf.hasRemaining()) {
                        flushBuffer(buf, target);
                    }
                    if (count == -1) {
                        break;
                    }
                }
                size = pos;

                // create Dir and Index Table Data
                if (!tooShort && DBPFPackager.isCompressed(headerBuf.array())) {
                    DirListData dataEntry = new DirListData(offset,
                            size, DBPFPackager.getDecompressedSize(headerBuf.array()), entry.getTGI());
                    dirData.add(dataEntry);
                    indexData.add(dataEntry);
                } else { // if not compressed
                    indexData.add(new WriteListData(offset, size, entry.getTGI()));
                }
            } finally {
                if (src != null) {
                    src.close();
                }
            }
            return size;
        }
        
        private static void flushBuffer(ByteBuffer buf, FileChannel target) throws IOException {
            buf.flip();
            while (buf.hasRemaining()) {
                target.write(buf);
            }
            buf.clear();
        }
        
        private static class WriteListData {
            final long offset;
            final long size;
            final DBPFTGI tgi;

            public WriteListData(long offset, long size, DBPFTGI tgi) {
                this.offset = offset;
                this.size = size;
                this.tgi = tgi;
            }
        }
        
        private static class DirListData extends WriteListData {
            final long decompressedSize;

            public DirListData(long offset, long size, long decompressedSize, DBPFTGI tgi) {
                super(offset, size, tgi);
                this.decompressedSize = decompressedSize;
            }
        }
    }
    
    /**
     * An entry within this DBPFFile with a link to its data on disk.
     * 
     * <dl><dt><b>Specified by:</b>
     * <dd>{@link DBPFEntry}.
     * </dl>
     * 
     * @author memo
     */
    public class DirectDBPFEntry extends DBPFEntry {

        // Global, this will read from DBPF File
        private final long offset;
        private final long size;
        private final long index;
        
        /**
         * Creates a DBPFEntry.
         *
         * @param tgi The TGI of this entry.
         * @param offset The byte offset of the actual data.
         * @param size The byte size of the actual data.
         * @param index The order of this entry in the file.
         */
        private DirectDBPFEntry(DBPFTGI tgi, long offset, long size, long index) {
            super(tgi);
            this.offset = offset;
            this.size = size;
            this.index = index;
        }

//        /**
//         * Compares another {@code DirectDBPFEntry} with this one for equality.
//         * The two entries are equal, if their TGIs are equal.
//         *
//         * @return TRUE, if the two TGIs are the same; FALSE, otherwise or if obj
//         * is not a DBPFEntry object.
//         */
//        @Override
//        public boolean equals(Object obj) {
//            if (obj instanceof DirectDBPFEntry) {
//                DirectDBPFEntry entry = (DirectDBPFEntry) obj;
//                    if (entry.getTGI().equals(this.tgi))
//                        return true;
//            }
//            return false;
//        }
//
//        /**
//         * The hashcode of this entry's TGI, in accordance to the custom
//         * {@link #equals equals} method.
//         * 
//         * @return The hashcode of this entry's TGI.
//         */
//        @Override
//        public int hashCode() {
//            return this.tgi.hashCode();
//        }

        /**
         * Returns a string with this entry's TGI and the label of the TGI.
         *
         * @return a string representation based on this entry's TGI.
         *
         * @see DBPFTGI#toString()
         * @see DBPFTGI#getLabel()
         */
        @Override
        public String toString() {
            return this.getTGI().toString() + " " + this.getTGI().getLabel();
        }
        
        /**
         * Retrieves the enclosing {@code DBPFFile} of this entry which
         * holds the data of this entry.
         * 
         * @return the enclosing {@code DBPFFile}.
         */
        public DBPFFile getEnclosingDBPFFile() {
            return DBPFFile.this;
        }

//        /**
//         * Returns a string with this entry's TGI, Offset and Size.
//         *
//         * @return The offset and size appended to {@link #toString()}.
//         */
//        public String toDetailString() {
//            StringBuilder sb = new StringBuilder(toString());
//            sb.append("\nOffset: ");
//            sb.append(DBPFUtil.toHex(offset, 8));
//            sb.append(" Size: ");
//            sb.append(size);
//            return sb.toString();
//        }

        /**
         * Creates a {@code DBPFType} from a given entry index.
         * Reads the data associated with the given entry and returns as a base
         * {@code DBPFType}.
         * <p>
         * Behaves exactly like {@link #createType(boolean) createType(false)}.
         *
         * @return The {@code DBPFType}.
         */
        public DBPFType createType() {
            return this.createType(false);
        }

        /**
         * Creates a {@code DBPFType} from a given entry index.
         * Reads the data associated with the given entry and returns as a base
         * {@code DBPFType}.
         * <p>
         * If {@code onlyRawType} is true, or if an appropriate type is not availabel,
         * this method will return {@code DBPFRaw} objects, - otherwise the returned
         * objects will be of the appropriate {@code  DBPFType}.
         *
         * @param onlyRawType
         *      TRUE, if only {@code  DBPFRaw} types with no decompression or decoding
         *      are to be returned
         * @return The {@code  DBPFType}.
         * 
         * @see DBPFFile#getEntries()
         */
        public DBPFType createType(boolean onlyRawType) {
            // System.out.println("Entry: "+entry.toString()+","+entry.getFile());
            // read rawdata from entry
            byte[] data = this.createData();
            DBPFTGI tgi = this.getTGI();
            DBPFPackager packager = new DBPFPackager();
            byte[] dData = packager.decompress(data);

            if (onlyRawType) {
                return new DBPFRaw(data, tgi, packager.isCompressed(), packager.getDecompressedSize());
            }

            DBPFType type = null;
            if (tgi.matches(DBPFTGI.EXEMPLAR)) {
                type = new DBPFExemplar(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.COHORT)) {
                type = new DBPFCohort(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.PNG)) {
                type = new DBPFPNG(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.WAV)) {
                // TODO not implemented yet, so use DBPFRaw
                type = null;
            } else if (tgi.matches(DBPFTGI.LTEXT)) {
                type = new DBPFLText(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.DIRECTORY)) {
                type = new DBPFDirectory();
                ((DBPFDirectory) type).setData(data);
            } else if (tgi.matches(DBPFTGI.LUA)) {
                type = new DBPFLUA(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.LUA_GEN)) {
                type = new DBPFLUA(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.RUL)) {
                type = new DBPFRUL(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.FSH)) {
                type = new DBPFFSH(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.S3D)) {
                // TODO check S3D type conversion
                type = new DBPFS3D(dData, tgi, packager.isCompressed());
            } else if (tgi.matches(DBPFTGI.SC4PATH)) {
                type = new DBPFSC4Path(dData, tgi, packager.isCompressed());
            }

            if (type == null) {
                type = new DBPFRaw(data, tgi, packager.isCompressed(), packager.getDecompressedSize());
            }
            return type;
        }
        
        /**
         * Create a Cohort file from this entry. If its TGI does not specify
         * a Cohort or the Cohort could not be created, this will return null.
         *
         * @return The Cohort or {@code null}.
         */
        public DBPFCohort createCohort() {
            if (this.getTGI().matches(DBPFTGI.COHORT)) {
                DBPFType type = createType();
                if (type instanceof DBPFCohort) {
                    return (DBPFCohort) type;
                }
            }
            return null;
        }
        
        /**
         * Create a Directory file from this entry. If its TGI does not specify
         * a Directory or the Directory could not be created, this will return null.
         *
         * @return The Directory or {@code null}.
         */
        public DBPFDirectory createDirectory() {
            if (this.getTGI().matches(DBPFTGI.DIRECTORY)) {
                DBPFType type = createType();
                if (type instanceof DBPFDirectory) {
                    return (DBPFDirectory) type;
                }
            }
            return null;
        }

        /**
         * Create an Exemplar file from this entry. If its TGI does not specify
         * an Exemplar or the Exemplar could not be created, this will return null.
         *
         * @return The Exemplar or {@code null}.
         */
        public DBPFExemplar createExemplar() {
            if (this.getTGI().matches(DBPFTGI.EXEMPLAR)) {
                DBPFType type = createType();
                if (type instanceof DBPFExemplar) {
                    return (DBPFExemplar) type;
                }
            }
            return null;
        }

        /**
         * Create an FSH file from this entry. If its TGI does not specify
         * an FSH or the FSH could not be created, this will return null.
         *
         * @return The FSH or {@code null}.
         */
        public DBPFFSH createFSH() {
            if (this.getTGI().matches(DBPFTGI.FSH)) {
                DBPFType type = createType();
                if (type instanceof DBPFFSH) {
                    return (DBPFFSH) type;
                }
            }
            return null;
        }

        /**
         * Create an LTEXT file from this entry. If its TGI does not specify
         * an LTEXT or the LTEXT could not be created, this will return null.
         *
         * @return The LTEXT or {@code null}.
         */
        public DBPFLText createLTEXT() {
            if (this.getTGI().matches(DBPFTGI.LTEXT)) {
                DBPFType type = createType();
                if (type instanceof DBPFLText) {
                    return (DBPFLText) type;
                }
            }
            return null;
        }
        
        /**
         * Create a LUA file from this entry. If its TGI does not specify
         * a LUA or the LUA could not be created, this will return null.
         *
         * @return The LUA or {@code null}.
         */
        public DBPFLUA createLUA() {
            if (this.getTGI().matches(DBPFTGI.LUA) ||
                    this.getTGI().matches(DBPFTGI.LUA_GEN)) {
                DBPFType type = createType();
                if (type instanceof DBPFLUA) {
                    return (DBPFLUA) type;
                }
            }
            return null;
        }

        /**
         * Create a PNG file from this entry. If its TGI does not specify
         * a PNG or the PNG could not be created, this will return null.
         *
         * @return The PNG or {@code null}.
         */
        public DBPFPNG createPNG() {
            if (this.getTGI().matches(DBPFTGI.PNG)) {
                DBPFType type = createType();
                if (type instanceof DBPFPNG) {
                    return (DBPFPNG) type;
                }
            }
            return null;
        }

        /**
         * Create a RUL file from this entry. If its TGI does not specify
         * a RUL or the RUL could not be created, this will return null.
         *
         * @return The RUL or {@code null}.
         */
        public DBPFRUL createRUL() {
            if (this.getTGI().matches(DBPFTGI.RUL)) {
                DBPFType type = createType();
                if (type instanceof DBPFRUL) {
                    return (DBPFRUL) type;
                }
            }
            return null;
        }
        
        /**
         * Create an S3D file from this entry. If its TGI does not specify
         * an S3D or the S3D could not be created, this will return null.
         *
         * @return The S3D or {@code null}.
         */
        public DBPFS3D createS3D() {
            if (this.getTGI().matches(DBPFTGI.S3D)) {
                DBPFType type = createType();
                if (type instanceof DBPFS3D) {
                    return (DBPFS3D) type;
                }
            }
            return null;
        }

        /**
         * Create an SC4Path file from this entry. If its TGI does not specify
         * an SC4Path or the SC4Path could not be created, this will return null.
         *
         * @return The SC4Path or {@code null}.
         */
        public DBPFSC4Path createSC4Path() {
            if (this.getTGI().matches(DBPFTGI.SC4PATH)) {
                DBPFType type = createType();
                if (type instanceof DBPFSC4Path) {
                    return (DBPFSC4Path) type;
                }
            }
            return null;
        }

        @Override
        public byte[] createData() {
            int len = (int) this.size;
            byte[] data = new byte[len];
            if (DBPFFile.this.isMapped()) {
                try {
                    mappedByteBuffer.position((int) this.offset);
                    mappedByteBuffer.get(data);
                } catch (BufferUnderflowException e) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] BufferUnderflowException for file: " + getFile() + ", entry index: " + this.index, e);
                    return null;
                }
            } else {
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(getFile(), "r");
                    raf.seek(this.offset);
                    raf.readFully(data);
                } catch (FileNotFoundException e) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] File not found: " + getFile(), e);
                    return null;
                } catch (IOException e) {
                    DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] IOException for file: " + getFile(), e);
                    return null;
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] IOException for file: " + getFile(), e);
                            return null;
                        }
                    }
                }
            }
            return data;
        }
        
        @Override
        public ReadableByteChannel createDataChannel() {
            try {
                return new DirectReadableByteChannel();
            } catch (FileNotFoundException e) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] File not found: " + getFile(), e);
                return null;
            } catch (IOException e) {
                DBPFUtil.LOGGER.log(Level.SEVERE, "[DBPFFile.Converter] IOException for file: " + getFile(), e);
                return null;
            }
        }
        
        /**
         * Reads directly from the file on the disk or from the mapped byte buffer.
         */
        private class DirectReadableByteChannel implements ReadableByteChannel {

            private boolean isClosed = false;
            private final boolean isMapped;
            
            private final long offset = DirectDBPFEntry.this.offset; 
            private final long size = DirectDBPFEntry.this.size;
            private long pos = 0;
            
            private ByteBuffer buf;
            private final RandomAccessFile raf;
            private final FileChannel fc;
            
            private DirectReadableByteChannel() throws FileNotFoundException, IOException {
                if (DBPFFile.this.isMapped()) {
                    buf = DBPFFile.this.mappedByteBuffer.asReadOnlyBuffer();
                    buf.position((int) offset);
                    isMapped = true;
                    raf = null;
                    fc = null;
                } else {
                    buf = null;
                    isMapped = false;
                    raf = new RandomAccessFile(DBPFFile.this.getFile(), "r");
                    fc = raf.getChannel();
                    fc.position(offset);
                }
            }
            
            @Override
            public void close() throws IOException {
                this.isClosed = true;
                buf = null;
                if (fc != null) {
                    fc.close();
                }
                if (raf != null) {
                    raf.close();
                }
            }

            @Override
            public boolean isOpen() {
                return !this.isClosed;
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                // exceptional cases
                if (this.isClosed) {
                    throw new ClosedChannelException();
                }
                if (this.pos == this.size) {
                    return -1;
                } else if (this.pos > this.size) {
                    throw new IOException("Too many bytes were read for entry: " +
                    		DirectDBPFEntry.this.toString());
                }
                if (!dst.hasArray()) {
                    throw new UnsupportedOperationException("The delivered ByteBuffer must have a backing array.");
                }
                
                // actual reading
                if (this.isMapped) {
                    int min = (int) Math.min(dst.remaining(), size - pos);
                    buf.get(dst.array(), dst.position(), min);
                    pos += min;
                    //System.out.printf("pos %d min %d size %d cap %d%n", pos, min, size, dst.capacity());
                    dst.position(dst.position() + min);
                    return min;
                } else {
                    int count = fc.read(dst);
                    pos += count;
                    //System.out.printf("pos %d count %d size %d cap %d position %d%n", pos, count, size, dst.capacity(), dst.position());
                    if (pos > size) { // overflow
                        dst.position(dst.position() - (int) (pos - size));
                        fc.position(offset + size);
                        count -= (int) (pos - size);
                        pos = size;
                    }
                    return count;
                }
            }
        }
    }
}
