/*
 * Written by Cliff Click and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package java.util.concurrent;
import  java.util.Map;
import  org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * A plug-in replacement for JDK1.5 {@link java.util.concurrent.ConcurrentHashMap}.  
 * This version is based on {@link * org.cliffc.high_scale_lib.NonBlockingHashMap}.  
 * This solution should be completely compatible, including the serialized
 * forms and all multi-threaded ordering guarantees.
 *
 * @since 1.5
 * @author Cliff Click
 * @param <TypeK> the type of keys maintained by this map
 * @param <TypeV> the type of mapped values
 */
public class ConcurrentHashMap<K, V> extends NonBlockingHashMap<K, V> {
  private static final long serialVersionUID = 7249069246763182397L;
  public ConcurrentHashMap() { super(); }
  public ConcurrentHashMap(int initialCapacity) { super(initialCapacity); }
  public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) { 
    super(initialCapacity);  
    if (!(loadFactor > 0) || concurrencyLevel <= 0)
      throw new IllegalArgumentException();
  }
  public ConcurrentHashMap(Map<? extends K, ? extends V> t) {
    super();
    putAll(t);
  }
}
