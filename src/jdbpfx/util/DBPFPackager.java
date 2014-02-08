package jdbpfx.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Jon
 */
public class DBPFPackager {

    private long compressedSize = 0;
    private long decompressedSize = 0;
    private boolean compressed = false;
    public static boolean debug = false;

    /**
     * Constructor.<br>
     */
    public DBPFPackager() {
    }

    /**
     * @return the compressedSize
     */
    public long getCompressedSize() {
        return compressedSize;
    }

    /**
     * @return the decompressedSize
     */
    public long getDecompressedSize() {
        return decompressedSize;
    }

    /**
     * @return the compressed
     */
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * Check, if data is compressed.<br>
     *
     * @param data
     *            The data to check
     * @return TRUE, if compressed; FALSE, otherwise
     */
    public static boolean isCompressed(byte[] data) {
        if (data.length > 6) {
            int signature = (int) DBPFUtil.getUint(data, 0x04, 2);
            if (signature == DBPFUtil.MAGICNUMBER_QFS) {
                // there is an s3d file in SC1.dat which would otherwise
                // return true on uncompressed data,
                // this workaround is not failproof
                String fileType = DBPFUtil.getChars(data, 0x00, 4);
                if (fileType.equals(DBPFUtil.MAGICNUMBER_3DMD)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static long getDecompressedSize(byte[] data) {
        long decompressedSize = -1L;
        if (data.length >= 9) {
//            long compressedSize = DBPFUtil.getUint(data, 0x00, 4);
            int signature = (int) DBPFUtil.getUint(data, 0x04, 2);
            if (signature == DBPFUtil.MAGICNUMBER_QFS) {
                //decompressed size is stored big endian in contrast to everything else stored little endian
                decompressedSize = DBPFUtil.getUint(data, 0x06, 1) * 0x10000
                                   + DBPFUtil.getUint(data, 0x07, 1) * 0x100
                                   + DBPFUtil.getUint(data, 0x08, 1);
            }
        }
        return decompressedSize;
    }

    /**
     * Copies data from source to destination array.<br>
     * The copy is byte by byte from srcPos to destPos and given length.
     *
     * @param src
     *            The source array
     * @param srcPos
     *            The source position
     * @param dest
     *            The destination array
     * @param destPos
     *            The destination position
     * @param length
     *            The length
     */
    private void arrayCopy2(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
        // This shouldn't occur, but to prevent errors
        if (dest.length < destPos + length) {
            if (debug) {
                System.err
                        .println("ATTENTION!"
                                + "\nBy arrayCopy2 the destination array is not big enough!"
                                + "\nWill make it bigger and make a System.arraycopy.");
            }
            byte[] destExt = new byte[destPos + length];
            System.arraycopy(dest, 0, destExt, 0, dest.length);
            dest = destExt;
        }

        for (int i = 0; i < length; i++) {
            dest[destPos + i] = src[srcPos + i];
        }
    }

    /**
     * Copies data from array at destPos-srcPos to array at destPos.<br>
     *
     * @param array
     *            The array
     * @param srcPos
     *            The position to copy from (reverse from end of array!)
     * @param destPos
     *            The position to copy to
     * @param length
     *            The length of data to copy
     */
    private void offsetCopy(byte[] array, int srcPos, int destPos, int length) {
        srcPos = destPos - srcPos;
        // This shouldn't occur, but to prevent errors
        if (array.length < destPos + length) {
            if (debug) {
                System.err
                        .println("ATTENTION!"
                                + "\nBy offsetCopy the destination array is not big enough!"
                                + "\nWill make it bigger and make a System.arraycopy.");
            }
            byte[] arrayNew = new byte[destPos + length];
            System.arraycopy(array, 0, arrayNew, 0, array.length);
            array = arrayNew;
        }

        for (int i = 0; i < length; i++) {
            array[destPos + i] = array[srcPos + i];
        }
    }

    /**
     * Compress the decompressed data.<br>
     *
     * @param dData
     *            The decompressed data
     * @return The compressed data
     */
    public byte[] compress(byte[] dData) {
        // if data is big enough for compress
        if (dData.length > 6) {
            // check, if data already compressed
            int signature = (int) DBPFUtil.getUint(dData, 0x04, 2);
            if (signature != DBPFUtil.MAGICNUMBER_QFS) {

                // some Compression Data
                final int MAX_OFFSET = 0x20000;
                final int MAX_COPY_COUNT = 0x404;
                // used to finetune the lookup (small values increase the
                // compression for Big Files)
                final int QFS_MAXITER = 0x80;

                // contains the latest offset for a combination of two
                // characters
                HashMap<Integer, ArrayList<Integer>> cmpmap2 = new HashMap<Integer, ArrayList<Integer>>();

                // will contain the compressed data (maximal size =
                // uncompressedSize+MAX_COPY_COUNT)
                byte[] cData = new byte[dData.length + MAX_COPY_COUNT];

                // init some vars
                int writeIndex = 9; // leave 9 bytes for the header
                int lastReadIndex = 0;
                ArrayList<Integer> indexList = null;
                int copyOffset = 0;
                int copyCount = 0;
                int index = -1;
                boolean end = false;

                // begin main compression loop
                while (index < dData.length - 3) {
                    // get all Compression Candidates (list of offsets for all
                    // occurances of the current 3 bytes)
                    do {
                        index++;
                        if (index >= dData.length - 2) {
                            end = true;
                            break;
                        }
                        int mapindex = (dData[index] & 0xFF)
                                        | ((dData[index + 1] & 0xFF) << 8)
                                        | ((dData[index + 2] & 0xFF) << 16);

                        indexList = cmpmap2.get(mapindex);
                        if (indexList == null) {
                            indexList = new ArrayList<Integer>();
                            cmpmap2.put(mapindex, indexList);
                        }
                        indexList.add(index);
                    } while (index < lastReadIndex);
                    if (end) {
                        break;
                    }

                    // find the longest repeating byte sequence in the index
                    // List (for offset copy)
                    int offsetCopyCount = 0;
                    int loopcount = 1;
                    while ((loopcount < indexList.size()) && (loopcount < QFS_MAXITER)) {
                        int foundindex = indexList.get((indexList.size() - 1) - loopcount);
                        if ((index - foundindex) >= MAX_OFFSET) {
                            break;
                        }
                        loopcount++;
                        copyCount = 3;
                        while ((dData.length > index + copyCount)
                                && (dData[index + copyCount] == dData[foundindex + copyCount])
                                && (copyCount < MAX_COPY_COUNT)) {
                            copyCount++;
                        }
                        if (copyCount > offsetCopyCount) {
                            offsetCopyCount = copyCount;
                            copyOffset = index - foundindex;
                        }
                    }

                    // check if we can compress this
                    // In FSH Tool stand additionally this:
                    if (offsetCopyCount > dData.length - index) {
                        offsetCopyCount = index - dData.length;
                    }
                    if (offsetCopyCount <= 2) {
                        offsetCopyCount = 0;
                    } else if ((offsetCopyCount == 3) && (copyOffset > 0x400)) { // 1024
                        offsetCopyCount = 0;
                    } else if ((offsetCopyCount == 4) && (copyOffset > 0x4000)) { // 16384
                        offsetCopyCount = 0;
                    }

                    // this is offset-compressable? so do the compression
                    if (offsetCopyCount > 0) {
                        // plaincopy

                        // In FSH Tool stand this (A):
                        while (index - lastReadIndex >= 4) {
                            copyCount = (index - lastReadIndex) / 4 - 1;
                            if (copyCount > 0x1B) {
                                copyCount = 0x1B;
                            }
                            cData[writeIndex++] = (byte) (0xE0 + copyCount);
                            copyCount = 4 * copyCount + 4;

                            arrayCopy2(dData, lastReadIndex, cData, writeIndex, copyCount);
                            lastReadIndex += copyCount;
                            writeIndex += copyCount;
                        }
                        // while ((index - lastReadIndex) > 3) {
                        // copyCount = (index - lastReadIndex);
                        // while (copyCount > 0x71) {
                        // copyCount -= 0x71;
                        // }
                        // copyCount = copyCount & 0xfc;
                        // int realCopyCount = (copyCount >> 2);
                        // cData[writeIndex++] = (short) (0xdf + realCopyCount);
                        // arrayCopy2(dData, lastReadIndex, cData, writeIndex,
                        // copyCount);
                        // writeIndex += copyCount;
                        // lastReadIndex += copyCount;
                        // }

                        // offsetcopy
                        copyCount = index - lastReadIndex;
                        copyOffset--;
                        if ((offsetCopyCount <= 0x0A) && (copyOffset < 0x400)) {
                            cData[writeIndex++] = (byte) (((copyOffset >> 8) << 5) + ((offsetCopyCount - 3) << 2) + copyCount);
                            cData[writeIndex++] = (byte) (copyOffset & 0xff);
                        } else if ((offsetCopyCount <= 0x43) && (copyOffset < 0x4000)) {
                            cData[writeIndex++] = (byte) (0x80 + (offsetCopyCount - 4));
                            cData[writeIndex++] = (byte) ((copyCount << 6) + (copyOffset >> 8));
                            cData[writeIndex++] = (byte) (copyOffset & 0xff);
                        } else if ((offsetCopyCount <= MAX_COPY_COUNT) && (copyOffset < MAX_OFFSET)) {
                            cData[writeIndex++] = (byte) (0xc0 + ((copyOffset >> 16) << 4) + (((offsetCopyCount - 5) >> 8) << 2) + copyCount);
                            cData[writeIndex++] = (byte) ((copyOffset >> 8) & 0xff);
                            cData[writeIndex++] = (byte) (copyOffset & 0xff);
                            cData[writeIndex++] = (byte) ((offsetCopyCount - 5) & 0xff);
                        }
                        // else {
                        // copyCount = 0;
                        // offsetCopyCount = 0;
                        // }

                        // do the offset copy
                        arrayCopy2(dData, lastReadIndex, cData, writeIndex, copyCount);
                        writeIndex += copyCount;
                        lastReadIndex += copyCount;
                        lastReadIndex += offsetCopyCount;

                    }
                }

                // add the End Record
                index = dData.length;
                // in FSH Tool stand the same as above (A)
                while (index - lastReadIndex >= 4) {
                    copyCount = (index - lastReadIndex) / 4 - 1;
                    if (copyCount > 0x1B) {
                        copyCount = 0x1B;
                    }
                    cData[writeIndex++] = (byte) (0xE0 + copyCount);
                    copyCount = 4 * copyCount + 4;

                    arrayCopy2(dData, lastReadIndex, cData, writeIndex, copyCount);
                    lastReadIndex += copyCount;
                    writeIndex += copyCount;
                }

                // lastReadIndex = Math.min(index, lastReadIndex);
                // while ((index - lastReadIndex) > 3) {
                // copyCount = (index - lastReadIndex);
                // while (copyCount > 0x71) {
                // copyCount -= 0x71;
                // }
                // copyCount = copyCount & 0xfc;
                // int realCopyCount = (copyCount >> 2);
                // cData[writeIndex++] = (short) (0xdf + realCopyCount);
                // arrayCopy2(dData, lastReadIndex, cData, writeIndex,
                // copyCount);
                // writeIndex += copyCount;
                // lastReadIndex += copyCount;
                // }
                copyCount = index - lastReadIndex;
                cData[writeIndex++] = (byte) (0xfc + copyCount);
                arrayCopy2(dData, lastReadIndex, cData, writeIndex, copyCount);
                writeIndex += copyCount;
                lastReadIndex += copyCount;

                // write the header for the compressed data
                // set the compressed size
                DBPFUtil.setUint(writeIndex, cData, 0x00, 4);
                this.compressedSize = writeIndex;
                // set the MAGICNUMBER
                DBPFUtil.setUint(DBPFUtil.MAGICNUMBER_QFS, cData, 0x04, 2);
                // set the decompressed size
                byte[] revData = new byte[3];
                DBPFUtil.setUint(dData.length, revData, 0x00, 3);
                for (int j = 0; j < revData.length; j++) {
                    cData[j + 6] = revData[2 - j];
                }
                this.decompressedSize = dData.length;
                compressed = false;
                if (compressedSize < decompressedSize) {
                    compressed = true;
                }
                // get the compressed data
                byte[] retData = new byte[writeIndex];
                System.arraycopy(cData, 0, retData, 0, writeIndex);
                return retData;
            }
        }
        return dData;
    }

    /**
     * Decompress the compressed data.<br>
     *
     * If the data are not compressed, this will return the same data.
     *
     * @param cData
     *            The compressed data
     * @return The decompressed data
     */
    public byte[] decompress(byte[] cData) {
        compressed = false;
        if (cData.length > 6) {
            // HEADER
            compressedSize = DBPFUtil.getUint(cData, 0x00, 4);
            int signature = (int) DBPFUtil.getUint(cData, 0x04, 2);
            // if not compressed
            decompressedSize = compressedSize;

            if (signature == DBPFUtil.MAGICNUMBER_QFS
                    // see static isCompressed method for explanation
                    && !DBPFUtil.getChars(cData, 0x00, 4).equals(DBPFUtil.MAGICNUMBER_3DMD)) {
                //decompressed size is stored big endian in contrast to everything else stored little endian
                decompressedSize = DBPFUtil.getUint(cData, 0x06, 1) * 0x10000
                                   + DBPFUtil.getUint(cData, 0x07, 1) * 0x100
                                   + DBPFUtil.getUint(cData, 0x08, 1);

                // There seems sometimes that given compressedSize is
                // not exactly the read data size.
                // Don't know why but take real data size for decompress
                if (debug) {
                    System.err.println("RawData-Size: " + cData.length
                            + " Found in RawData: " + compressedSize
                            + " DecompressedSize: " + decompressedSize);
                }

                byte[] dData = new byte[(int) decompressedSize];
                int dpos = 0;
                // COMPRESSED DATA
                compressed = true;
                int pos = 9;
                int control1 = 0;
                while (control1 < 0xFC && pos < cData.length) {
                    control1 = cData[pos] & 0xFF;
                    // System.out.println(">>> Position: " + pos +
                    // " ## Control: "
                    // + Integer.toHexString((int)
                    // control1)+" ## Rest: "+(compressedSize-pos));
                    pos++;

                    if (control1 >= 0 && control1 <= 127) {
                        // 0x00 - 0x7F
                        int control2 = cData[pos] & 0xFF;
                        pos++;
                        int numberOfPlainText = (control1 & 0x03);
                        arrayCopy2(cData, pos, dData, dpos, numberOfPlainText);
                        dpos += numberOfPlainText;
                        pos += numberOfPlainText;

                        int offset = ((control1 & 0x60) << 3) + (control2) + 1;
                        int numberToCopyFromOffset = ((control1 & 0x1C) >> 2) + 3;
                        offsetCopy(dData, offset, dpos, numberToCopyFromOffset);
                        dpos += numberToCopyFromOffset;

                    } else if (control1 >= 128 && control1 <= 191) {
                        // 0x80 - 0xBF
                        int control2 = cData[pos] & 0xFF;
                        pos++;
                        int control3 = cData[pos] & 0xFF;
                        pos++;

                        int numberOfPlainText = (control2 >> 6) & 0x03;
                        arrayCopy2(cData, pos, dData, dpos, numberOfPlainText);
                        dpos += numberOfPlainText;
                        pos += numberOfPlainText;

                        int offset = ((control2 & 0x3F) << 8) + (control3) + 1;
                        int numberToCopyFromOffset = (control1 & 0x3F) + 4;
                        offsetCopy(dData, offset, dpos, numberToCopyFromOffset);
                        dpos += numberToCopyFromOffset;
                    } else if (control1 >= 192 && control1 <= 223) {
                        // 0xC0 - 0xDF
                        int numberOfPlainText = (control1 & 0x03);
                        int control2 = cData[pos] & 0xFF;
                        pos++;
                        int control3 = cData[pos] & 0xFF;
                        pos++;
                        int control4 = cData[pos] & 0xFF;
                        pos++;
                        arrayCopy2(cData, pos, dData, dpos, numberOfPlainText);
                        dpos += numberOfPlainText;
                        pos += numberOfPlainText;

                        int offset = ((control1 & 0x10) << 12) + (control2 << 8) + (control3) + 1;
                        int numberToCopyFromOffset = ((control1 & 0x0C) << 6) + (control4) + 5;
                        offsetCopy(dData, offset, dpos, numberToCopyFromOffset);
                        dpos += numberToCopyFromOffset;
                    } else if (control1 >= 224 && control1 <= 251) {
                        // 0xE0 - 0xFB
                        int numberOfPlainText = ((control1 & 0x1F) << 2) + 4;
                        arrayCopy2(cData, pos, dData, dpos, numberOfPlainText);
                        dpos += numberOfPlainText;
                        pos += numberOfPlainText;
                    } else {
                        int numberOfPlainText = (control1 & 0x03);
                        arrayCopy2(cData, pos, dData, dpos, numberOfPlainText);
                        dpos += numberOfPlainText;
                        pos += numberOfPlainText;
                    }
                }
                return dData;
            }
        }
        // no data to decompress
        compressed = false;
        return cData;
    }
}
