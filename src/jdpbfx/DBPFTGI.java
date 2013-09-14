package jdpbfx;

import java.util.ArrayDeque;
import java.util.Queue;

import jdpbfx.util.DBPFUtil;

/**
 * The {@code DBPFTGI} class encapsulates a type, group and instance identifier.
 * TGI objects are immutable and can be obtained by a call to the static factory
 * method {@link #valueOf}. Each component is limited to 32 bits or the null
 * value -1 (used as a mask in certain functions).
 * <p>
 * The method {@link DBPFTGI#matches(DBPFTGI)} may be used to determine
 * whether a TGI matches a particular format, that is, a known file type.
 * <p>
 * Masks for the common file types are declared as constants in this class and
 * may be used with this method.
 * 
 * @author jondor
 * @author memo
 */
public class DBPFTGI {

    private final long type;
    private final long group;
    private final long instance;
    
    /**
     * Static factory method that returns a {@code DBPFTGI} object. Components
     * are stored as longs to avoid sign problems. -1 can be used as {@code null}
     * value.
     * <p>
     * Whether two TGIs {@code tgi1} and {@code tgi2} returned by this method that
     * satisfy {@code tgi1.equals(tgi2)} will also satisfy {@code tgi1 == tgi2}
     * is unspecified.
     * 
     * @param type 32-bit type identifier.
     * @param group 32-bit group identifier.
     * @param instance 32-bit instance identifier.
     * @return a TGI object.
     */
    public static DBPFTGI valueOf(long type, long group, long instance) {
        return new DBPFTGI(type, group, instance);
    }

    /**
     * Creates a TGI. Components are stored as longs to avoid sign problems.
     * -1 can be used as {@code null} value.
     * 
     * @param type 32-bit type identifier.
     * @param group 32-bit group identifier.
     * @param instance 32-bit instance identifier.
     */
    private DBPFTGI(long type, long group, long instance) {
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
        for (Mask mask : Mask.values) {
            if (this.matches(mask)) {
                return mask.label;
            }
        }
        // cannot occur as NULLTGI always matches
        throw new RuntimeException("Compilation problem: NULLTGI has not been added to Mask.values.");
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
     * Check if this TGI matches a given masked TGI.
     * <p>
     * {@code DBPFTGI} constants are useful here.
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
     * @return a DBPFTGI object with the modified fields.
     */
    public DBPFTGI modifyTGI(DBPFTGI modifier) {
        if (modifier == null) {
            return this;
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
     * @return a DBPFTGI object with the modified fields.
     */
    public DBPFTGI modifyTGI(long t, long g, long i) {
        return DBPFTGI.valueOf(t == -1L ? this.type : t,
                               g == -1L ? this.group : g,
                               i == -1L ? this.instance : i);
    }

    /** BLANKTGI <p> (0, 0, 0) */
    public static final DBPFTGI BLANKTGI;

    /** Directory file <p> (0xe86b1eef, 0xe86b1eef, 0x286b1f03) */
    public static final DBPFTGI DIRECTORY;

    /** LD file <p> (0x6be74c60, 0x6be74c60, -1) */
    public static final DBPFTGI LD;

    /** Exemplar file: LotInfo, LotConfig <p> (0x6534284a, -1, -1) */
    public static final DBPFTGI EXEMPLAR;

    /** Cohort file <p> (0x05342861, -1, -1) */
    public static final DBPFTGI COHORT;

    /** PNG file: Menu building icons, bridges, overlays <p> (0x856ddbac, 0x6a386d26, -1) */
    public static final DBPFTGI PNG_ICON;

    /** PNG file (image, icon) <p> (0x856ddbac, -1, -1) */
    public static final DBPFTGI PNG;

    /** FSH file: Transit Textures/Buildings/Bridges/Misc <p> (0x7ab50e44, 0x1abe787d, -1) */
    public static final DBPFTGI FSH_TRANSIT;

    /** FSH file: Base and Overlay Lot Textures <p> (0x7ab50e44, 0x0986135e, -1) */
    public static final DBPFTGI FSH_BASE_OVERLAY;

    /** FSH file: Transit Network Shadows (Masks) <p> (0x7ab50e44, 0x2BC2759a, -1) */
    public static final DBPFTGI FSH_SHADOW;

    /** FSH file: Animation Sprites (Props) <p> (0x7ab50e44, 0x2a2458f9, -1) */
    public static final DBPFTGI FSH_ANIM_PROPS;

    /** FSH file: Animation Sprites (Non Props) <p> (0x7ab50e44, 0x49a593e7, -1) */
    public static final DBPFTGI FSH_ANIM_NONPROPS;

    /** FSH file: Terrain And Foundations <p> (0x7ab50e44, 0x891b0e1a, -1) */
    public static final DBPFTGI FSH_TERRAIN_FOUNDATION;

    /** FSH file: User Interface Images <p> (0x7ab50e44, 0x46a006b0, -1) */
    public static final DBPFTGI FSH_UI;

    /** FSH file: Textures <p> (0x7ab50e44, -1, -1) */
    public static final DBPFTGI FSH;
    
    /** S3D file: Models <p> (0x5ad0e817, -1, -1) */
    public static final DBPFTGI S3D;

    /** SC4PATH (2D) <p> (0x296678f7, 0x69668828, -1) */
    public static final DBPFTGI SC4PATH_2D;

    /** SC4PATH (3D) <p> (0x296678f7, 0xa966883f, -1) */
    public static final DBPFTGI SC4PATH_3D;

    /** SC4PATH file <p> (0x296678f7, -1, -1) */
    public static final DBPFTGI SC4PATH;

    /** LUA file: Missions, Advisors, Tutorials and Packaging files <p> (0xca63e2a3, 0x4a5e8ef6, -1) */
    public static final DBPFTGI LUA;

    /** LUA file: Generators, Attractors, Repulsors and System LUA <p> (0xca63e2a3, 0x4a5e8f3f, -1) */
    public static final DBPFTGI LUA_GEN;

    /** RUL file: Network rules <p> (0x0a5bcf4b, 0xaa5bcf57, -1) */
    public static final DBPFTGI RUL;

    /** WAV file <p> (0x2026960b, 0xaa4d1933, -1) */
    public static final DBPFTGI WAV;

    /** LTEXT or WAV file <p> (0x2026960b, -1, -1) */
    public static final DBPFTGI LTEXT;

    /** Effect Directory file <p> (0xea5118b0, -1, -1) */
    public static final DBPFTGI EFFDIR;

    /** Font Table INI <p> (0, 0x4a87bfe8, 0x2a87bffc) */
    public static final DBPFTGI INI_FONT;

    /** Network INI: Remapping, Bridge Exemplars <p> (0, 0x8a5971c5, 0x8a5993b9) */
    public static final DBPFTGI INI_NETWORK;

    /** INI file <p> (0, 0x8a5971c5, -1) */
    public static final DBPFTGI INI;

    /** NULLTGI <p> (-1, -1, -1) */
    public static final DBPFTGI NULLTGI;

    static {
        /*
         * Masks need to be ordered "bottom-up", that is, specialized entries
         * need to be inserted first, more general ones later.
         * 
         * @see DBPFTGI#getLabel()
         */
        BLANKTGI                = new Mask(0L, 0L, 0L, "-");
        DIRECTORY               = new Mask(0xe86b1eefL, 0xe86b1eefL, 0x286b1f03L, "DIR");
        LD                      = new Mask(0x6be74c60L, 0x6be74c60L, -1L, "LD");
        EXEMPLAR                = new Mask(0x6534284aL, -1L, -1L, "EXEMPLAR");
        COHORT                  = new Mask(0x05342861L, -1L, -1L, "COHORT");
        S3D                     = new Mask(0x5ad0e817L, -1L, -1L, "S3D");
        
        FSH_TRANSIT             = new Mask(0x7ab50e44L, 0x1abe787dL, -1L, "FSH (Transit Texture)");
        FSH_BASE_OVERLAY        = new Mask(0x7ab50e44L, 0x0986135eL, -1L, "FSH (Base/Overlay Texture)");
        FSH_SHADOW              = new Mask(0x7ab50e44L, 0x2BC2759aL, -1L, "FSH (Shadow Mask)");
        FSH_ANIM_PROPS          = new Mask(0x7ab50e44L, 0x2a2458f9L, -1L, "FSH (Animation Sprites (Props))");
        FSH_ANIM_NONPROPS       = new Mask(0x7ab50e44L, 0x49a593e7L, -1L, "FSH (Animation Sprites (Non Props))");
        FSH_TERRAIN_FOUNDATION  = new Mask(0x7ab50e44L, 0x891b0e1aL, -1L, "FSH (Terrain/Foundation)");
        FSH_UI                  = new Mask(0x7ab50e44L, 0x46a006b0L, -1L, "FSH (UI Image)");
        FSH                     = new Mask(0x7ab50e44L, -1L, -1L, "FSH");
        
        SC4PATH_2D              = new Mask(0x296678f7L, 0x69668828L, -1L, "SC4PATH (2D)");
        SC4PATH_3D              = new Mask(0x296678f7L, 0xa966883fL, -1L, "SC4PATH (3D)");
        SC4PATH                 = new Mask(0x296678f7L, -1L, -1L, "SC4PATH");
        
        PNG_ICON                = new Mask(0x856ddbacL, 0x6a386d26L, -1L, "PNG (Icon)");
        PNG                     = new Mask(0x856ddbacL, -1L, -1L, "PNG");
        LUA                     = new Mask(0xca63e2a3L, 0x4a5e8ef6L, -1L, "LUA");
        LUA_GEN                 = new Mask(0xca63e2a3L, 0x4a5e8f3fL, -1L, "LUA (Generators)");
        WAV                     = new Mask(0x2026960bL, 0xaa4d1933L, -1L, "WAV");
        LTEXT                   = new Mask(0x2026960bL, -1L, -1L, "LTEXT");
        INI_FONT                = new Mask(0L, 0x4a87bfe8L, 0x2a87bffcL, "INI (Font Table)");
        INI_NETWORK             = new Mask(0L, 0x8a5971c5L, 0x8a5993b9L, "INI (Networks)");
        INI                     = new Mask(0L, 0x8a5971c5L, -1L, "INI");
        
        RUL                     = new Mask(0x0a5bcf4bL, 0xaa5bcf57L, -1L, "RUL");
        EFFDIR                  = new Mask(0xea5118b0L, -1L, -1L, "EFFDIR");
        NULLTGI                 = new Mask(-1L, -1L, -1L, "UNKNOWN"); // any TGI matches this last one
    }

    private static class Mask extends DBPFTGI {
        
        private static final Queue<Mask> values = new ArrayDeque<Mask>(); // for ordered iteration

        private final String label;

        private Mask(long type, long group, long instance, String label) {
            super(type, group, instance);
            this.label = label;
            values.add(this);
        }
    }
}
