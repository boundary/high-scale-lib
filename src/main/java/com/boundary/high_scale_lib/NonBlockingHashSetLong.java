package com.boundary.high_scale_lib;

import org.cliffc.high_scale_lib.LongIterator;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple wrapper around {@link NonBlockingHashMapLong} making it implement the
 * {@link Set} interface.  All operations are Non-Blocking and multi-thread safe.
 */
public class NonBlockingHashSetLong extends AbstractSet<Long> implements Serializable {
  private static final Object V = "";

  private final NonBlockingHashMapLong<Object> _map;

  /** Make a new empty {@link NonBlockingHashSetLong}.  */
  public NonBlockingHashSetLong() {
    super();
    _map = new NonBlockingHashMapLong<Object>();
  }

  @Override
  public boolean addAll(Collection<? extends Long> c) {
    if (!NonBlockingHashSetLong.class.equals(c.getClass())) {
      return super.addAll(c);
    }
    boolean modified = false;
    for (final LongIterator it = ((NonBlockingHashSetLong)c).longIterator(); it.hasNext(); ) {
      modified |= add(it.nextLong());
    }
    return modified;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (!NonBlockingHashSetLong.class.equals(c.getClass())) {
      return super.removeAll(c);
    }
    boolean modified = false;
    for (final LongIterator it = ((NonBlockingHashSetLong)c).longIterator(); it.hasNext(); ) {
      modified |= remove(it.nextLong());
    }
    return modified;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    if (!NonBlockingHashSetLong.class.equals(c.getClass())) {
      return super.containsAll(c);
    }
    for (final LongIterator it = ((NonBlockingHashSetLong)c).longIterator(); it.hasNext(); ) {
      if (!contains(it.nextLong())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    if (!NonBlockingHashSetLong.class.equals(c.getClass())) {
      return super.retainAll(c);
    }
    boolean modified = false;
    final NonBlockingHashSetLong nonBlockingHashSetLong = (NonBlockingHashSetLong) c;
    for (final LongIterator it = longIterator(); it.hasNext(); ) {
      if (!nonBlockingHashSetLong.contains(it.nextLong())) {
        it.remove();
        modified = true;
      }
    }
    return modified;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (final LongIterator it = longIterator(); it.hasNext(); ) {
      final long value = it.nextLong();
      hashCode += (int)(value ^ (value >>> 32));
    }
    return hashCode;
  }

  /** Add {@code o} to the set.
   * @return <tt>true</tt> if {@code o} was added to the set, <tt>false</tt>
   * if {@code o} was already in the set.
   */
  public boolean add(final long o) {
    return _map.putIfAbsent(o,V) != V;
  }
  
  /** 
   * To support AbstractCollection.addAll
   */
  @Override
  public boolean add(final Long o) {
    return _map.putIfAbsent(o.longValue(),V) != V;
  }

  /** 
   * @return <tt>true</tt> if {@code o} is in the set.
   */
  public boolean contains(final long o) { return _map.containsKey(o); }

  @Override
  public boolean contains(Object o) {
    return o instanceof Long && contains(((Long) o).longValue());
  }

  /** Remove {@code o} from the set.
   * @return <tt>true</tt> if {@code o} was removed to the set, <tt>false</tt>
   * if {@code o} was not in the set.
   */
  public boolean remove(final long o) { return _map.remove(o) == V; }

  @Override
  public boolean remove(final Object o) { return o instanceof Long && remove(((Long) o).longValue()); }
  /** 
   * Current count of elements in the set.  Due to concurrent racing updates,
   * the size is only ever approximate.  Updates due to the calling thread are
   * immediately visible to calling thread.
   * @return count of elements.
   */
  @Override
  public int size() { return _map.size(); }
  /** Empty the set. */
  @Override
  public void clear() { _map.clear(); }

  @Override
  public String toString() {
    // Overloaded to avoid auto-boxing
    final LongIterator it = longIterator();
    if (!it.hasNext()) {
      return "[]";
    }
    final StringBuilder sb = new StringBuilder().append('[');
    for (;;) {
      sb.append(it.next());
      if (!it.hasNext()) {
        return sb.append(']').toString();
      }
      sb.append(", ");
    }
  }

  @Override
  public Iterator<Long>iterator() { return _map.keySet().iterator(); }

  public LongIterator longIterator() {
    return (LongIterator) _map.keySet().iterator();
  }

  // ---

  /**
   * Atomically make the set immutable.  Future calls to mutate will throw an
   * IllegalStateException.  Existing mutator calls in other threads racing
   * with this thread and will either throw IllegalStateException or their
   * update will be visible to this thread.  This implies that a simple flag
   * cannot make the Set immutable, because a late-arriving update in another
   * thread might see immutable flag not set yet, then mutate the Set after
   * the {@link #readOnly} call returns.  This call can be called concurrently
   * (and indeed until the operation completes, all calls on the Set from any
   * thread either complete normally or end up calling {@link #readOnly}
   * internally).
   *
   * <p> This call is useful in debugging multi-threaded programs where the
   * Set is constructed in parallel, but construction completes after some
   * time; and after construction the Set is only read.  Making the Set
   * read-only will cause updates arriving after construction is supposedly
   * complete to throw an {@link IllegalStateException}.
   */

  // (1) call _map's immutable() call
  // (2) get snapshot
  // (3) CAS down a local map, power-of-2 larger than _map.size()+1/8th
  // (4) start @ random, visit all snapshot, insert live keys
  // (5) CAS _map to null, needs happens-after (4)
  // (6) if Set call sees _map is null, needs happens-after (4) for readers
  public void readOnly() {
    throw new RuntimeException("Unimplemented");
  }
}
