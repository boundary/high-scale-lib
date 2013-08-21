package org.cliffc.high_scale_lib;

import java.util.Iterator;

/**
 * An extension of the standard {@link Iterator} interface which provides the {@link #nextLong()} method to avoid
 * auto-boxing of results as they are returned.
 * */
public interface LongIterator extends Iterator<Long> {
    /**
     * Returns the next long value without auto-boxing. Using this is preferred to {@link #next()}.
     *
     * @return The next long value.
     */
    long nextLong();
}
