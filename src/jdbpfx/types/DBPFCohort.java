package jdbpfx.types;

import jdbpfx.DBPFTGI;
import jdbpfx.DBPFType;
import jdbpfx.util.DBPFUtil;

/**
 * @author Jon
 * @author memo
 */
public class DBPFCohort extends AbstractCohortType {

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
    public DBPFCohort(byte[] data, DBPFTGI tgi, boolean compressed) {
        super(DBPFUtil.MAGICNUMBER_CQZ, data, tgi, compressed);
    }

    public DBPFCohort(DBPFTGI tgi, boolean compressed, short format, DBPFTGI parentCohortTGI) {
        super(DBPFUtil.MAGICNUMBER_CQZ, tgi, compressed, format, parentCohortTGI);
    }

    public DBPFCohort(DBPFTGI tgi, boolean compressed, short format) {
        this(tgi, compressed, format, DBPFTGI.BLANKTGI);
    }

    @Override
    public Type getType() {
        return DBPFType.Type.COHORT;
    }
}
