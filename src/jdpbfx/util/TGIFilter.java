package jdpbfx.util;

import jdpbfx.DBPFTGI;

/**
 * A filter to be used for filtering {@link DBPFTGI} objects.
 *
 * @author memo
 */
public abstract class TGIFilter {
    // TODO make TGIFilter serializable

    /**
     * This method is used for filtering.
     *
     * @param tgi a TGI to be tested.
     * @return {@code true}, iff the TGI is accepted by the filter.
     */
    public abstract boolean accepts(DBPFTGI tgi);

    /**
     * Returns a {@code TGIFilter} that accepts all TGIs that match any of the
     * masks.
     *
     * @param tgiMasks the TGI masks.
     * @return the filter.
     */
    public static TGIFilter accept(final DBPFTGI... tgiMasks) {
        return new TGIFilter() {
            @Override
            public boolean accepts(DBPFTGI tgi) {
                for (DBPFTGI mask : tgiMasks) {
                    if (tgi.matches(mask)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Returns a {@code TGIFilter} that rejects all TGIs that match any of the
     * masks.
     *
     * @param tgiMasks the TGI masks.
     * @return the filter.
     */
    public static TGIFilter reject(final DBPFTGI... tgiMasks) {
        return new TGIFilter() {
            @Override
            public boolean accepts(DBPFTGI tgi) {
                for (DBPFTGI mask : tgiMasks) {
                    if (tgi.matches(mask)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
