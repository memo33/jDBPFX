package jdpbfx;

import jdpbfx.util.DBPFUtil;

/**
 * The {@code DBPFTGI} class encapsulates a type, group and instance identifier.
 * TGI objects are immutable.  Each component is limited to 32 bits or the null
 * value -1 (used as a mask in certain functions).
 * <p>
 * The method {@link DBPFTGI#matches(Mask)} may be used to determine
 * whether a TGI matches a particular format, that is, a known file type.
 * <p>
 * Only the unique TGIs {@link #BLANKTGI}, {@link #NULLTGI} and {@link #DIRECTORY} are
 * declared as constants in this class. However, masks for the common file types
 * can be found in {@link DBPFTGI.Mask}. 
 * 
 * @author jondor
 * @author memo
 *
 * @see DBPFTGI.Mask
 */
public final class DBPFTGI {

    private final long type;
    private final long group;
    private final long instance;

    /**
     * Creates a TGI. Components are stored as longs to avoid sign problems.
     * 
     * @param type 32-bit type identifier.
     * @param group 32-bit group identifier.
     * @param instance 32-bit instance identifier.
     */
    public DBPFTGI(long type, long group, long instance) {
        if(type >= -1L && type <= 0xFFFFFFFFL)
            this.type = type;
        else
            this.type = -1L;

        if(group >= -1L && group <= 0xFFFFFFFFL)
            this.group = group;
        else
            this.group = -1L;

        if(instance >= -1L && instance <= 0xFFFFFFFFL)
            this.instance = instance;
        else
            this.instance = -1L;
    }

    /**
     * @return The type identifier.
     */
    public long getType() {
        return this.type;
    }

    /**
     * @return The group identifier.
     */
    public long getGroup() {
        return this.group;
    }

    /**
     * @return The instance identifier.
     */
    public long getInstance() {
        return this.instance;
    }
    
    /**
     * Returns a string representing the type of this TGI.
     *
     * @return The representative string or UNKNOWN, if TGI is not recognized.
     */
    public String getLabel() {
        for (Mask key : Mask.values()) {
            if (this.matches(key)) {
                return key.label;
            }
        }
        return Mask.NULLTGI.label;
    }
    
    /**
     * @return TRUE if the type identifier of this TGI is null (-1L).
     */
    public boolean isTypeNull() {
        return this.type == -1L;
    }
    
    /**
     * @return TRUE if the group identifier of this TGI is null (-1L).
     */
    public boolean isGroupNull() {
        return this.group == -1L;
    }
    
    /**
     * @return TRUE if the instance identifier of this TGI is null (-1L).
     */
    public boolean isInstanceNull() {
        return this.instance == -1L;
    }
    
    /**
     * @return TRUE if any component of this TGI is null (-1L).
     */
    public boolean hasNullID() {
        return isTypeNull() || isGroupNull() || isInstanceNull();
    }

    /**
     * Compares two TGI objects for exact equality between their components.
     * 
     * @param obj The TGI to compare with this one.
     * 
     * @return True if both TGIs have identical components, false if not or if
     *         obj is not a TGI.
     */
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DBPFTGI) {
            DBPFTGI tgiObj = (DBPFTGI) obj;
            if(this.type == tgiObj.getType() && this.group == tgiObj.getGroup() && this.instance == tgiObj.getInstance())
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long combined = this.type ^ this.group ^ this.instance;
        if(combined > Integer.MAX_VALUE)
            combined += 2 * Integer.MIN_VALUE;
        return Long.valueOf(combined).intValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("T:");
        sb.append(DBPFUtil.toHex(type, 8));
        sb.append(", G:");
        sb.append(DBPFUtil.toHex(group, 8));
        sb.append(", I:");
        sb.append(DBPFUtil.toHex(instance, 8));
        return sb.toString();
    }

    /**
     * Check if this TGI matches a given {@link DBPFTGI.Mask}.
     * 
     * @param tgiMask the TGI Mask to check against.
     * @return TRUE, if the check passes; FALSE, otherwise.
     * 
     * @see #matches(DBPFTGI)
     */
    public boolean matches(Mask tgiMask) {
        return this.matches(tgiMask.tgi);
    }
    
    /**
     * Check if this TGI matches a given masked TGI.
     * <p>
     * Whenever possible, the method {@link #matches(Mask)} should be preferred
     * over this method.
     * <p>
     * If any component of tgiMask is null (-1L), it will be skipped.
     * Compare with {@link #equals(java.lang.Object)} which explicitly checks each component.
     * Unlike equals, this method is not reflexive.
     * Only the tgiMask parameter is checked for null (-1L) components.
     *
     * @param tgiMask The TGI to check against.
     *
     * @return TRUE, if the check passes; FALSE, otherwise.
     */
    public boolean matches(DBPFTGI tgiMask) {
        boolean tidOK = (tgiMask.isTypeNull()) || (type == tgiMask.getType());
        boolean gidOK = (tgiMask.isGroupNull()) || (group == tgiMask.getGroup());
        boolean iidOK = (tgiMask.isInstanceNull()) || (instance == tgiMask.getInstance());
        if (tidOK && gidOK && iidOK) {
            return true;
        }
        return false;
    }

    /**
     * Modifies and returns a new TGI using the fields of this and another TGI.
     * <p>
     * Each component is replaced by the corresponding component of modifier.
     * If any component of modifier is -1, the corresponding component of this TGI
     * will be used.
     *
     * @param modifier The TGI to use to modify this one
     * 
     * @return a new DBPFTGI instance with the modified fields.
     */
    public DBPFTGI modifyTGI(DBPFTGI modifier) {
        if (modifier == null) {
            return new DBPFTGI(this.type, this.group, this.instance);
        } else {
            return this.modifyTGI(modifier.getType(),
                                  modifier.getGroup(),
                                  modifier.getInstance());
        }
    }
    
    /**
     * Modifies and returns a new TGI using the fields of this and another TGI.
     * <p>
     * Each id is replaced by the corresponding t, g, or i.
     * If any of t, g, or i is -1, it will be skipped
     *
     * @param t The type id to change to or -1 as a mask
     * @param g The group id to change to or -1 as a mask
     * @param i The instance id to change to or -1 as a mask
     * 
     * @return a new DBPFTGI instance with the modified fields.
     */
    public DBPFTGI modifyTGI(long t, long g, long i) {
        return new DBPFTGI(t == -1L ? this.type : t,
                           g == -1L ? this.group : g,
                           i == -1L ? this.instance : i);
    }

    /** 
     * NULLTGI (-1, -1, -1)
     */
    public static final DBPFTGI NULLTGI = new DBPFTGI(-1L, -1L, -1L);

    /**
     * BLANKTGI (0, 0, 0)
     */
    public static final DBPFTGI BLANKTGI = new DBPFTGI(0L, 0L, 0L);

    /**
     *  Directory file
     */
    public static final DBPFTGI DIRECTORY = new DBPFTGI(0xe86b1eefL, 0xe86b1eefL, 0x286b1f03L);

    /**
     * An Enumeration of a number of common TGI Masks of file types that are
     * particularly suited for the {@link DBPFTGI#matches(Mask)} test.
     * 
     * @author memo
     */
    public static enum Mask {
        /*
         * Masks need to be ordered "bottom-up", that is, specialized entries
         * need to be inserted first, more general ones later.
         * 
         * @see DBPFTGI#getLabel()
         */
        
        /** BLANKTGI (0, 0, 0) */
        BLANKTGI(DBPFTGI.BLANKTGI, "-"),

        /** Directory file */
        DIRECTORY(DBPFTGI.DIRECTORY, "DIR"),

        /** LD file */
        LD(0x6be74c60L, 0x6be74c60L, -1L, "LD"),

        /** Exemplar file: LotInfo, LotConfig */
        EXEMPLAR(0x6534284aL, -1L, -1L, "EXEMPLAR"),

        /** Cohort file */
        COHORT(0x05342861L, -1L, -1L, "COHORT"),

        /** PNG file: Menu building icons, bridges, overlays */
        PNG_ICON(0x856ddbacL, 0x6a386d26L, -1L, "PNG (Icon)"),

        /** PNG file (image, icon) */
        PNG(0x856ddbacL, -1L, -1L, "PNG"),

        /** FSH file: Textures */
        FSH(0x7ab50e44L, -1L, -1L, "FSH"),

        /** S3D file: Models */
        S3D(0x5ad0e817L, -1L, -1L, "S3D"),

        /** SC4PATH (2D) */
        SC4PATH_2D(0x296678f7L, 0x69668828L, -1L, "SC4PATH (2D)"),

        /** SC4PATH (3D) */
        SC4PATH_3D(0x296678f7L, 0xa966883fL, -1L, "SC4PATH (3D)"),

        /** SC4PATH file */
        SC4PATH(0x296678f7L, -1L, -1L, "SC4PATH"),

        /** LUA file: Missions, Advisors, Tutorials and Packaging files */
        LUA(0xca63e2a3L, 0x4a5e8ef6L, -1L, "LUA"),

        /** LUA file: Generators, Attractors, Repulsors and System LUA */
        LUA_GEN(0xca63e2a3L, 0x4a5e8f3fL, -1L, "LUA (Generators)"),

        /** RUL file: Network rules */
        RUL(0x0a5bcf4bL, 0xaa5bcf57L, -1L, "RUL"),

        /** WAV file */
        WAV(0x2026960bL, 0xaa4d1933L, -1L, "WAV"),

        /** LTEXT or WAV file */
        LTEXT(0x2026960bL, -1L, -1L, "LTEXT"),
        
        /** Effect Directory file */
        EFFDIR(0xea5118b0L, -1L, -1L, "EFFDIR"),
        
        /** Font Table INI */
        INI_FONT(0L, 0x4a87bfe8L, 0x2a87bffcL, "INI (Font Table)"),

        /** Network INI: Remapping, Bridge Exemplars */
        INI_NETW(0L, 0x8a5971c5L, 0x8a5993b9L, "INI (Networks)"),
        
        /** INI file */
        INI(0L, 0x8a5971c5L, -1L, "INI"),
        
        /** NULLTGI (-1, -1, -1) */
        NULLTGI(DBPFTGI.NULLTGI, "UNKNOWN"); // any TGI matches this last one

        private final DBPFTGI tgi;
        private final String label;

        private Mask(DBPFTGI tgi, String label) {
            this.tgi = tgi;
            this.label = label;
        }
        
        private Mask(long type, long group, long instance, String label) {
            this(new DBPFTGI(type, group, instance), label);
        }
        
        @Override
        public String toString() {
            return this.tgi.toString() + " " + this.label;
        }
    }
}
