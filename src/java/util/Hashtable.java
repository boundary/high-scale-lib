/*
 * Written by Cliff Click and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package java.util;
import  java.io.*;
import  java.util.Map;
import  org.cliffc.high_scale_lib.NonBlockingHashtable;

/**
 * A plug-in replacement for JDK1.5 {@link java.util.Hashtable}.  This version
 * is based on {@link org.cliffc.high_scale_lib.NonBlockingHashMap}.  The
 * undocumented iteration order is different from Hashtable, as is the
 * undocumented (lack of) synchronization.  <em>Programs that rely on this
 * undocumented behavior may break</em>.  Otherwise this solution should be
 * completely compatible, including the serialized forms.
 *
 * This version is <strong>not</strong> synchronized, and correctly operates
 * as a thread-safe Hashtable.  It does <strong>not</strong> provide the
 * same ordering guarantees as calling synchronized methods will.  The old
 * Hashtable's methods were synchronized and would provide ordering.  This
 * behavior is not part of Hashtable's spec.  This version's methods are not
 * synchronized and will not force the same Java Memory Model orderings.
 * 
 *
 * @since 1.5
 * @author Cliff Click
 * @param <TypeK> the type of keys maintained by this map
 * @param <TypeV> the type of mapped values
 */
public class Hashtable<K, V> extends NonBlockingHashtable<K, V> {
  /** use serialVersionUID from JDK 1.0.2 for interoperability */
  private static final long serialVersionUID = 1421746759512286392L;
  // Field included strictly to pass the serialization JCK tests
  private final float loadFactor = 0.75f;
  private int threshold = (int)(loadFactor*4.0f);

  public Hashtable() { super(); }
  public Hashtable(int initialCapacity) { super(initialCapacity); }
  public Hashtable(int initialCapacity, float loadFactor) { 
    super(initialCapacity);  
    if (!(loadFactor > 0) )
      throw new IllegalArgumentException();
    threshold = (int)(initialCapacity * loadFactor);
  }
  public Hashtable(Map<? extends K, ? extends V> t) {
    super();
    putAll(t);
  }

  // Serialize.  This format is painful in several ways; it requires the count
  // of K/V pairs ahead of time - but the Hashtable is undergoing rapid
  // concurrent modification, so we painfully clone the entire table to get a
  // stable local version.  Another way to do this would be to write-lock the
  // table somehow until the serizalition is done, or copy-on-write.
  // Another useless thing is that the threshold, loadfactor and current table
  // size are all meaningless to the underlying NBHM.
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    // Clone, to guard against concurrent mod during the write messing with
    // the element count.
    Hashtable<K,V> t = (Hashtable<K,V>)this.clone();
    // Write out the threshold, loadfactor
    s.defaultWriteObject();
    // Write out length, count of elements and then the key/value objects
    s.writeInt((int)(t.size()/loadFactor));
    s.writeInt(t.size());
    for( Object K : keySet() ) {
      final Object V = get(K);  // Do an official 'get'
      s.writeObject(K);         // Write the <TypeK,TypeV> pair
      s.writeObject(V);
    }
  }

  // Reconstitute the Hashtable from a stream (i.e., deserialize it).
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    // Read in the threshold, and loadfactor
    s.defaultReadObject();
    initialize();               // Setup the NBHM

    // Read the original length of the array and number of elements
    int origlength = s.readInt();
    int elements = s.readInt();

    // Read the number of elements and then all the key/value objects
    for( int i=0; i<elements; i++ ) {
      K key   = (K)s.readObject();
      V value = (V)s.readObject();
      put(key,value);
    }
  }
}
