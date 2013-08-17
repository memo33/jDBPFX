package jdpbfx.types;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import jdpbfx.DBPFTGI;
import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 */
public class DBPFS3D extends DBPFType {

    private final static String MAGICNUMBER_HEAD = "HEAD";
    private final static String MAGICNUMBER_VERT = "VERT";
    private final static String MAGICNUMBER_INDX = "INDX";
    private final static String MAGICNUMBER_PRIM = "PRIM";
    private final static String MAGICNUMBER_MATS = "MATS";
    private final static String MAGICNUMBER_ANIM = "ANIM";
    private final static String MAGICNUMBER_PROP = "PROP";
    private final static String MAGICNUMBER_REGP = "REGP";

    public boolean decoded;

    public long majorVersion;
    public long minorVersion;

    public long numVertGroups;
    public VertGroup[] vertGroups;

    public long numIndxGroups;
    public IndxGroup[] indxGroups;

    public long numPrimGroups;
    public PrimGroup[] primGroups;

    public long numMatsGroups;
    public MatsGroup[] matsGroups;

    public long numFrames;
    public long frameRate;
    public long playMode;
    public long flags;
    public float displacement;
    public long numAnimGroups;
    public AnimGroup[] animGroups;
    /**
     * Constructor.<br>
     */
    public DBPFS3D(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(tgi);
        this.rawData = data;
        this.compressed = compressed;
        this.decompressedSize = data.length;

        int offset = 0;

        String fileAsString = new String(data, Charset.forName("US-ASCII"));

        String fileType = DBPFUtil.getChars(data, 0x00, 4);
        if (!fileType.equals(DBPFUtil.MAGICNUMBER_3DMD)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: 3DMD Header: {0}", tgi.toString());
            decoded = false;
            return;
        }
        @SuppressWarnings("unused")
        long fileSize = DBPFUtil.getUint(data, 0x04, 4);
        String head = DBPFUtil.getChars(data, 0x08, 4);
        if(!head.equals(MAGICNUMBER_HEAD)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: HEAD Section: {0}", tgi.toString());
            decoded = false;
            return;
        }
        @SuppressWarnings("unused")
        long headSize = DBPFUtil.getUint(data, 0x0C, 4);
        majorVersion = DBPFUtil.getUint(data, 0x10, 2);
        minorVersion = DBPFUtil.getUint(data, 0x12, 2);
        //offset = 0x14;
        //String vert = DBPFUtil.getChars(data, offset, 4);
        //if(!vert.equals(MAGICNUMBER_VERT)) {
            offset = fileAsString.indexOf(MAGICNUMBER_VERT);
        //}
        //long vertSize = DBPFUtil.getUint(data, offset + 4, 4);
        if(!decodeVert(data, offset)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: VERT Section: {0}", tgi.toString());
            decoded = false;
            return;
        }

        //offset += vertSize;
        //String indx = DBPFUtil.getChars(data, offset, 4);
        //if(!indx.equals(MAGICNUMBER_INDX)) {
            offset = fileAsString.indexOf(MAGICNUMBER_INDX);
        //}
        //long indxSize = DBPFUtil.getUint(data, offset + 4, 4);
        if(!decodeIndx(data, offset)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: INDX Section: {0}", tgi.toString());
            decoded = false;
            return;
        }

        //offset += indxSize;
        //String prim = DBPFUtil.getChars(data, offset, 4);
        //if(!prim.equals(MAGICNUMBER_PRIM)) {
            offset = fileAsString.indexOf(MAGICNUMBER_PRIM);
        //}
        //long primSize = DBPFUtil.getUint(data, offset + 4, 4);
        if(!decodePrim(data, offset)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: PRIM Section: {0}", tgi.toString());
            decoded = false;
            return;
        }

        //offset += primSize;
        //String mats = DBPFUtil.getChars(data, offset, 4);
        //if(!mats.equals(MAGICNUMBER_MATS)) {
            offset = fileAsString.indexOf(MAGICNUMBER_MATS);
        //}
        //long matsSize = DBPFUtil.getUint(data, offset + 4, 4);
        if(!decodeMats(data, offset)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: MATS Section: {0}", tgi.toString());
            decoded = false;
            return;
        }

        //offset += matsSize;
        //String anim = DBPFUtil.getChars(data, offset, 4);
        //if(!anim.equals(MAGICNUMBER_ANIM)) {
            offset = fileAsString.indexOf(MAGICNUMBER_ANIM);
        //}
        //long animSize = DBPFUtil.getUint(data, offset + 4, 4);
        if(!decodeAnim(data, offset)) {
            DBPFUtil.LOGGER.log(Level.SEVERE, "DBPFS3D decode failed: ANIM Section: {0}", tgi.toString());
            decoded = false;
            return;
        }

        decoded = true;
    }

    private boolean decodeVert(byte[] data, int vertOffset) {
        int offset = vertOffset + 8;
        this.numVertGroups = DBPFUtil.getUint(data, offset, 4);
        this.vertGroups = new VertGroup[(int)numVertGroups];

        offset += 4;
        for(int x=0;x<numVertGroups;x++) {
            vertGroups[x] = new VertGroup();
            @SuppressWarnings("unused")
            long unknown = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            vertGroups[x].numVerts = DBPFUtil.getUint(data, offset, 2);
            vertGroups[x].verts = new Vert[(int)vertGroups[x].numVerts];
            offset += 2;
            vertGroups[x].format = DBPFUtil.getUint(data, offset, 4);
            if(vertGroups[x].format != 0x80004001L)
                return false;
            offset += 4;
            for(int y=0;y<vertGroups[x].numVerts;y++) {
                vertGroups[x].verts[y] = new Vert();
                vertGroups[x].verts[y].x = DBPFUtil.getFloat32(data, offset, 4);
                offset += 4;
                vertGroups[x].verts[y].y = DBPFUtil.getFloat32(data, offset, 4);
                offset += 4;
                vertGroups[x].verts[y].z = DBPFUtil.getFloat32(data, offset, 4);
                offset += 4;
                vertGroups[x].verts[y].u = DBPFUtil.getFloat32(data, offset, 4);
                offset += 4;
                vertGroups[x].verts[y].v = DBPFUtil.getFloat32(data, offset, 4);
                offset += 4;
            }
        }
        return true;
    }

    private boolean decodeIndx(byte[] data, int indxOffset) {
        int offset = indxOffset + 8;
        this.numIndxGroups = DBPFUtil.getUint(data, offset, 4);
        this.indxGroups = new IndxGroup[(int)numIndxGroups];

        offset += 4;
        for(int x=0;x<numIndxGroups;x++) {
            indxGroups[x] = new IndxGroup();
            @SuppressWarnings("unused")
            long unknown = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            indxGroups[x].format = DBPFUtil.getUint(data, offset, 2);
            if(indxGroups[x].format != 2)
                return false;
            offset += 2;
            indxGroups[x].numIndxs = DBPFUtil.getUint(data, offset, 2);
            indxGroups[x].indxs = new Indx[(int)indxGroups[x].numIndxs / 3];
            offset += 2;
            for(int y=0;y<indxGroups[x].numIndxs / 3;y++) {
                indxGroups[x].indxs[y] = new Indx();
                indxGroups[x].indxs[y].a = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
                indxGroups[x].indxs[y].b = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
                indxGroups[x].indxs[y].c = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
            }
        }
        return true;
    }

    private boolean decodePrim(byte[] data, int primOffset) {
        int offset = primOffset + 8;
        this.numPrimGroups = DBPFUtil.getUint(data, offset, 4);
        this.primGroups = new PrimGroup[(int)numPrimGroups];

        offset += 4;
        for(int x=0;x<numPrimGroups;x++) {
            primGroups[x] = new PrimGroup();
            long format = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            if(format != 1) {
                DBPFUtil.LOGGER.log(Level.WARNING, "Number of Primitives: {0}", format);
                return false;
            }
            @SuppressWarnings("unused")
            long unknown = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
            @SuppressWarnings("unused")
            long firstVert = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
            primGroups[x].numVerts = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
        }
        return true;
    }

    private boolean decodeMats(byte[] data, int matsOffset) {
        int offset = matsOffset + 8;
        this.numMatsGroups = DBPFUtil.getUint(data, offset, 4);
        this.matsGroups = new MatsGroup[(int)numMatsGroups];

        offset += 4;
        for(int x=0;x<numMatsGroups;x++) {
            matsGroups[x] = new MatsGroup();
            matsGroups[x].flags = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
            matsGroups[x].alphaFunc = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].depthFunc = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].sourceBlend = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].destBlend = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].alphaThreshold = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            matsGroups[x].matClass = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
            matsGroups[x].reserved = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].textureCount = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].IID = DBPFUtil.getUint(data, offset, 4);
            offset += 4;
            matsGroups[x].wrapU = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].wrapV = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            if((majorVersion == 1 && minorVersion >= 5) || majorVersion > 1) {
                matsGroups[x].magnificationFilter = DBPFUtil.getUint(data, offset, 1);
                offset += 1;
                matsGroups[x].minificationFilter = DBPFUtil.getUint(data, offset, 1);
                offset += 1;
            }
            matsGroups[x].animRate = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            matsGroups[x].animMode = DBPFUtil.getUint(data, offset, 2);
            offset += 2;
            long nameLen = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            matsGroups[x].name = DBPFUtil.getChars(data, offset, (int)nameLen - 1);
            offset += (int)nameLen;
        }
        return true;
    }

    private boolean decodeAnim(byte[] data, int animOffset) {
        int offset = animOffset + 8;

        numFrames = DBPFUtil.getUint(data, offset, 2);
        offset += 2;
        frameRate = DBPFUtil.getUint(data, offset, 2);
        offset += 2;
        playMode = DBPFUtil.getUint(data, offset, 2);
        offset += 2;
        flags = DBPFUtil.getUint(data, offset, 4);
        offset += 4;
        displacement = DBPFUtil.getFloat32(data, offset, 4);
        offset += 4;
        numAnimGroups = DBPFUtil.getUint(data, offset, 2);
        this.animGroups = new AnimGroup[(int)numAnimGroups];
        offset += 2;
        for(int x=0;x<numAnimGroups;x++) {
            animGroups[x] = new AnimGroup();
            long nameLen = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            animGroups[x].flags = DBPFUtil.getUint(data, offset, 1);
            offset += 1;
            animGroups[x].name = DBPFUtil.getChars(data, offset, (int)nameLen - 1);
            offset += nameLen;
            animGroups[x].vertBlock = new long[(int)numFrames];
            animGroups[x].indxBlock = new long[(int)numFrames];
            animGroups[x].primBlock = new long[(int)numFrames];
            animGroups[x].matsBlock = new long[(int)numFrames];
            for(int y=0;y<numFrames;y++) {
                animGroups[x].vertBlock[y] = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
                animGroups[x].indxBlock[y] = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
                animGroups[x].primBlock[y] = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
                animGroups[x].matsBlock[y] = DBPFUtil.getUint(data, offset, 2);
                offset += 2;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", RawData-Size: ");
        sb.append(rawData.length);
        return sb.toString();
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
        return DBPFType.Type.S3D;
    }
    
    public List<Vert> getPolys(long frame, long group) {
        if(!decoded)
            return null;
        LinkedList<Vert> verts = new LinkedList<Vert>();
        IndxGroup indxG = indxGroups[(int)animGroups[(int)group].indxBlock[(int)frame]];
        VertGroup vertG = vertGroups[(int)animGroups[(int)group].vertBlock[(int)frame]];
        for(int x=0;x<indxG.numIndxs / 3;x++) {
            verts.add(vertG.verts[(int)indxG.indxs[x].a]);
            verts.add(vertG.verts[(int)indxG.indxs[x].b]);
            verts.add(vertG.verts[(int)indxG.indxs[x].c]);
        }
        return verts;
    }
    
    public List<Vert> getPolys(long frame) {
        if(!decoded)
            return null;
        LinkedList<Vert> verts = new LinkedList<Vert>();
        for(int x=0;x<numAnimGroups;x++) {
            verts.addAll(getPolys(frame, x));
        }
        return verts;
    }

    public static class VertGroup {
        public long numVerts;
        public long format;
        public Vert[] verts;
    }

    public static class Vert {
        public float x, y, z, u, v;
    }

    public static class IndxGroup {
        public long numIndxs;
        public long format;
        public Indx[] indxs;
    }

    public static class Indx {
        public long a, b, c;
    }

    public static class PrimGroup {
        public long numVerts;
    }

    public static class MatsGroup {
        public long flags;
        public long alphaFunc;
        public long depthFunc;
        public long sourceBlend;
        public long destBlend;
        public long alphaThreshold;
        public long matClass;
        public long reserved;
        public long textureCount;
        public long IID;
        public long wrapU;
        public long wrapV;
        public long magnificationFilter;
        public long minificationFilter;
        public long animRate;
        public long animMode;
        public String name;
    }

    public static class AnimGroup {
        public String name;
        public long flags;
        public long vertBlock[], indxBlock[], primBlock[], matsBlock[];
    }
}
