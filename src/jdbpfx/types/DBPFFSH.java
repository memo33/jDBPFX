package jdbpfx.types;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import jdbpfx.DBPFTGI;
import jdbpfx.DBPFType;
import jdbpfx.util.DBPFUtil;

//import gr.zdimensions.jsquish.Squish;

/**
 * @author Jon
 * @author memo
 */
public class DBPFFSH extends DBPFType {

    private long numEntries;
    private String id;

    private ArrayList<ImageIndex> imageIndex;

    /**
     * Constructor.<br>
     */
    public DBPFFSH(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.decompressedSize = data.length;

        String fileType = DBPFUtil.getChars(data, 0x00, 4);
        if (fileType.equals(DBPFUtil.MAGICNUMBER_SHPI)) {
            @SuppressWarnings("unused")
            long fileSize = DBPFUtil.getUint(data, 0x04, 4);
            numEntries = DBPFUtil.getUint(data, 0x08, 4);
            imageIndex = new ArrayList<ImageIndex>((int)(numEntries / 0.75));
            id = DBPFUtil.getChars(data, 0x0C, 4);

            int pos = 0x10;
            for(int i = 0;i<numEntries;i++) {
                ImageIndex entry = new ImageIndex();
                entry.name = DBPFUtil.getChars(data, pos, 4);
                entry.offset = DBPFUtil.getUint(data, pos + 4, 4);
                imageIndex.add(entry);
                pos += 8;
            }

            for(ImageIndex entry : imageIndex) {
                int offset = (int)entry.offset;
                entry.type = (short)DBPFUtil.getUint(data, offset, 1);
                entry.size = DBPFUtil.getUint(data, offset + 1, 3);
                entry.width = (int)DBPFUtil.getUint(data, offset + 4, 2);
                entry.height = (int)DBPFUtil.getUint(data, offset + 6, 2);
                entry.xCenter = (int)DBPFUtil.getUint(data, offset + 8, 2);
                entry.yCenter = (int)DBPFUtil.getUint(data, offset + 10, 2);
                entry.xOffset = (int)DBPFUtil.getUint(data, offset + 12, 2) & 0xFFF;
                entry.yOffset = (int)DBPFUtil.getUint(data, offset + 14, 2) & 0xFFF;
                entry.mipCount = ((int)DBPFUtil.getUint(data, offset + 14, 2) & 0xF000) >>> 12;
                //readImageData(entry, data);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", RawData-Size: ");
        sb.append(rawData.length);
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public long getNumEntries() {
        return numEntries;
    }

    public int getNumMips(int index) {
        if(index < numEntries)
            return imageIndex.get(index).mipCount;

        return -1;
    }

    public BufferedImage getImage() {
        return getImage(0, 0);
    }

    public BufferedImage getImage(int index) {
        return getImage(index, 0);
    }

    public BufferedImage getImage(int index, int mip) {
        if(index < numEntries) {
            if(imageIndex.get(index).images.isEmpty())
                readImageData(imageIndex.get(index), rawData);

            if(mip < imageIndex.get(index).images.size())
                return imageIndex.get(index).images.get(mip);

        }
        return null;
    }

    public List<BufferedImage> getImages(int index) {
        if(index < numEntries) {
            if(imageIndex.get(index).images.isEmpty())
                readImageData(imageIndex.get(index), rawData);

            return new ArrayList<BufferedImage>(imageIndex.get(index).images);
        }

        return null;
    }

    /**
     * Returns the data of the raw type.<br>
     * This data is equivalent to the rawData.
     *
     * @return The data
     */
    @Override
    public byte[] getRawData() {
        return rawData;
    }

    @Override
    public Type getType() {
        return DBPFType.Type.FSH;
    }

    private void readImageData(ImageIndex entry, byte[] data) {
        int dataEnd = 0;
        int offset = (int)entry.offset + 0x10;

        for(int i=0;i<=entry.mipCount;i++) {
            int width = (entry.width / (1 << i)), height = (entry.height / (1 << i));
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            if((entry.type & 0x7F) == 0x60) { //DXT1
                int left = 0, top = 0;
                for(;; offset += 8) {
                    int color0 = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8),
                        color1 = (data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8),
                        red0 = (int)((color0 & 0x1F) * 33.0 / 4), red1 = (int)((color1 & 0x1F) * 33.0 / 4),
                        green0 = (int)(((color0 >> 5) & 0x3F) * 65.0 / 16), green1 = (int)(((color1 >> 5) & 0x3F) * 65.0 / 16),
                        blue0 = (int)(((color0 >> 11) & 0x1F) * 33.0 / 4), blue1 = (int)(((color1 >> 11) & 0x1F) * 33.0 / 4);

                    if(color0 > color1) {
                        int yStop = (height - top > 4) ? 4 : height - top;
                        for(int y=0;y<yStop;y++) {
                            int bits = (data[offset + 4 + y] & 0xFF);
                            int xStop = (width - left > 4) ? 4 : width - left;
                            for(int x=0;x<xStop;x++) {
                                int code = (bits >> (x*2)) & 0x0003;
                                int pixel = 0;
                                switch (code) {
                                    case 0:
                                        pixel = red0 |
                                                        (green0 << 8) |
                                                        (blue0 << 16) |
                                                        0xFF000000;
                                        break;
                                    case 1:
                                        pixel = red1 |
                                                        (green1 << 8) |
                                                        (blue1 << 16) |
                                                        0xFF000000;
                                        break;
                                    case 2:
                                        pixel = ((2 * red0 + red1) / 3) |
                                                        (((2 * green0 + green1) / 3) << 8) |
                                                        (((2 * blue0 + blue1) / 3) << 16) |
                                                        0xFF000000;
                                        break;
                                    case 3:
                                        pixel = ((red0 + 2 * red1) / 3) |
                                                        (((green0 + 2 * green1) / 3) << 8) |
                                                        (((blue0 + 2 * blue1) / 3) << 16) |
                                                        0xFF000000;
                                        break;
                                }
                                image.setRGB(left + x, top + y, pixel);
                            }
                        }
                    } else {
                        int yStop = (height - top > 4) ? 4 : height - top;
                        for(int y=0;y<yStop;y++) {
                            int bits = (data[offset + 4 + y] & 0xFF);
                            int xStop = (width - left > 4) ? 4 : width - left;
                            for(int x=0;x<xStop;x++) {
                                int code = (bits >> (x*2)) & 0x0003;
                                int pixel = 0;
                                switch (code) {
                                    case 0:
                                        pixel = red0 |
                                                (green0 << 8) |
                                                (blue0 << 16) |
                                                0xFF000000;
                                        break;
                                    case 1:
                                        pixel = red1 |
                                                (green1 << 8) |
                                                (blue1 << 16) |
                                                0xFF000000;
                                        break;
                                    case 2:
                                        pixel = ((red0 + red1) / 2) |
                                                (((green0 + green1) / 2) << 8) |
                                                (((blue0 + blue1) / 2) << 16) |
                                                0xFF000000;
                                        break;
                                    case 3:
                                        pixel = 0;
                                        break;
                                }
                                image.setRGB(left + x, top + y, pixel);
                            }
                        }
                    }
                    left += 4;
                    if(left >= width) {
                        left = 0;
                        top += 4;
                        if(top >= height) {
                            break;
                        }
                    }
                }
                // mipmap offset fix
                offset += 8;
            } else if((entry.type & 0x7F) == 0x61) { //DXT3
                int left = 0, top = 0;
                for(;; offset += 16) {
                    int[] pixels = new int[16];

                    for(int y=0;y<4;y++) {
                        int bits = (data[offset + 2 * y] & 0xFF);
                        pixels[4*y] = ((bits & 0x000F) * 17) << 24;
                        pixels[4*y + 1] = (((bits & 0x00F0) >> 4) * 17) << 24;

                        bits = (data[offset + 2 * y + 1] & 0xFF);
                        pixels[4*y + 2] = ((bits & 0x000F) * 17) << 24;
                        pixels[4*y + 3] = (((bits & 0x00F0) >> 4) * 17) << 24;
                    }
                    int color0 = (data[offset + 8] & 0xFF) | ((data[offset + 9] & 0xFF) << 8),
                        color1 = (data[offset + 10] & 0xFF) | ((data[offset + 11] & 0xFF) << 8),
                        red0 = (int)((color0 & 0x1F) * 33.0 / 4), red1 = (int)((color1 & 0x1F) * 33.0 / 4),
                        green0 = (int)(((color0 >> 5) & 0x3F) * 65.0 / 16), green1 = (int)(((color1 >> 5) & 0x3F) * 65.0 / 16),
                        blue0 = (int)(((color0 >> 11) & 0x1F) * 33.0 / 4), blue1 = (int)(((color1 >> 11) & 0x1F) * 33.0 / 4);

                    int yStop = (height - top > 4) ? 4 : height - top;
                    for(int y=0;y<yStop;y++) {
                        int bits = (data[offset + 12 + y] & 0xFF);
                        int xStop = (width - left > 4) ? 4 : width - left;
                        for(int x=0;x<xStop;x++) {
                            int code = (bits >> (x*2)) & 0x0003;
                            switch (code) {
                                    case 0:
                                        pixels[4*y+x] |= red0 |
                                                        (green0 << 8) |
                                                        (blue0 << 16);
                                        break;
                                    case 1:
                                        pixels[4*y+x] |= red1 |
                                                        (green1 << 8) |
                                                        (blue1 << 16);
                                        break;
                                    case 2:
                                        pixels[4*y+x] |= ((2 * red0 + red1) / 3) |
                                                        (((2 * green0 + green1) / 3) << 8) |
                                                        (((2 * blue0 + blue1) / 3) << 16);
                                        break;
                                    case 3:
                                        pixels[4*y+x] |= ((red0 + 2 * red1) / 3) |
                                                        (((green0 + 2 * green1) / 3) << 8) |
                                                        (((blue0 + 2 * blue1) / 3) << 16);
                                        break;
                            }
                            image.setRGB(left + x, top + y, pixels[4*y+x]);
                        }
                    }
                    left += 4;
                    if(left >= width) {
                        left = 0;
                        top += 4;
                        if(top >= height) {
                            break;
                        }
                    }
                }
                // mipmap offset fix
                offset += 16;
            } else if((entry.type & 0x7F) == 0x7D) { //ARGB 32bit 8x8x8x8
                dataEnd = offset + (width * height * 4);
                int[] pixels = new int[(dataEnd-offset)/4];
                int pos = 0;
                for(;offset < dataEnd; offset += 4) {
                    pixels[pos++] = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) |
                                    ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
                }
                image.setRGB(0, 0, width, height, pixels, 0, width);
            } else if((entry.type & 0x7F) == 0x7F) { //RGB 24bit 8x8x8
                dataEnd = offset + (width * height * 3);
                int[] pixels = new int[(dataEnd-offset)/3];
                int pos = 0;
                for(;offset < dataEnd; offset += 3) {
                    pixels[pos++] = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) |
                                    ((data[offset + 2] & 0xFF) << 16) | 0xFF000000;
                }
                image.setRGB(0, 0, width, height, pixels, 0, width);
            } else if((entry.type & 0x7F) == 0x7E) { //ARGB 16bit 1x5x5x5
                dataEnd = offset + (width * height * 2);
                int[] pixels = new int[(dataEnd-offset)/2];
                int pos = 0;
                for(;offset < dataEnd; offset += 2) {
                    pixels[pos++] = ((data[offset] & 0x1F) << 3) | ((data[offset] & 0xE0) << 6) |
                                    ((data[offset + 1] & 0x03) << 14) | ((data[offset + 1] & 0x7C) << 17) |
                                    ((((data[offset + 1] & 0x80) >> 7) * 255) << 24);
                }
                image.setRGB(0, 0, width, height, pixels, 0, width);
            } else if((entry.type & 0x7F) == 0x78) { //RGB 16bit 5x6x5
                dataEnd = offset + (width * height * 2);
                int[] pixels = new int[(dataEnd-offset)/2];
                int pos = 0;
                for(;offset < dataEnd; offset += 2) {
                    pixels[pos++] = ((data[offset] & 0x1F) << 3) | ((data[offset] & 0xE0) << 5) |
                                    ((data[offset + 1] & 0x07) << 13) | ((data[offset + 1] & 0xF8) << 16) |
                                    0xFF000000;
                }
                image.setRGB(0, 0, width, height, pixels, 0, width);
                dataEnd = offset + (width * height * 2);
            } else if((entry.type & 0x7F) == 0x6D) { //ARGB 16bit 4x4x4x4
                int[] pixels = new int[(dataEnd-offset)/2];
                int pos = 0;
                for(;offset < dataEnd; offset += 2) {
                    pixels[pos++] = ((data[offset] & 0x0F) * 17) | (((data[offset] & 0xF0) << 8) * 17) |
                                    (((data[offset + 1] & 0x0F) * 17) << 20) |
                                    ((((data[offset + 1] & 0xF0) >>> 4) * 17) << 24);
                }
                image.setRGB(0, 0, width, height, pixels, 0, width);
            }
            entry.images.add(image);
        }
    }

    private class ImageIndex {
        String name;
        long offset;
        short type;
        long size;
        int width;
        int height;
        int xCenter;
        int yCenter;
        int xOffset;
        int yOffset;
        int mipCount;
        ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    }
}
