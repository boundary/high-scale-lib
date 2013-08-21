package org.cliffc.high_scale_lib;

/**
 * An iterator optimized for primitive collections which avoids auto-boxing on {@link #next()}.
 */
public interface IntIterator {
    /**
     * Identical to {@link java.util.Iterator#next()} but avoids auto-boxing.
     *
     * @return The next int in the collection.
     */
    int next();

    /**
     * Identical to {@link java.util.Iterator#hasNext()}.
     *
     * @return True if the iterator has more elements.
     */
    boolean hasNext();

    /**
     * Identical to {@link java.util.Iterator#remove()}.
     */
    void remove();
}
