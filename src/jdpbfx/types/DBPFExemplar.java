package jdpbfx.types;

import jdpbfx.DBPFTGI;
import jdpbfx.DBPFType;
import jdpbfx.util.DBPFUtil;

/**
 * @author Jon
 * @author memo
 */
public class DBPFExemplar extends AbstractCohortType {

    /**
     * Constructor.
     *
     * @param data
     *          the uncompressed byte data of the entry.
     * @param tgi
     *          the TGI.
     * @param compressed
     *          If {@code true}, the method {@link DBPFType#createData} will
     *          return the compressed byte data, else uncompressed.
     */
    public DBPFExemplar(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(DBPFUtil.MAGICNUMBER_EQZ, data, tgi, compressed);
    }

    public DBPFExemplar(DBPFTGI tgi, boolean compressed, short format, DBPFTGI parentCohortTGI) {
        super(DBPFUtil.MAGICNUMBER_EQZ, tgi, compressed, format, parentCohortTGI);
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
    public Type getType() {
        return DBPFType.Type.EXEMPLAR;
    }
}
