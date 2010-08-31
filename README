
IF YOU ARE LOOKING for the drop-in replacement for java.util.Hashtable, it's
in the lib directory, lib/java_util_hashtable.jar.  It needs to be in your
bootclasspath.  Example:

  java -Xbootclasspath/p:lib/java_util_hashtable.jar my_java_app_goes_here


---

A collection of Concurrent and Highly Scalable Utilities.  These are intended
as direct replacements for the java.util.* or java.util.concurrent.*
collections but with better performance when many CPUs are using the
collection concurrently.  Single-threaded performance may be slightly lower.

The direct replacements match the API - but not all behaviors are covered by
the API, and so they may not work for your program.  In particular, the
replacement for java.util.Hashtable is NOT synchronized (that is the point!),
although it is multi-threaded safe.  If you rely on the undocumented
synchronization behavior of the JDK Hashtable, your program may not work.
Similarly, the iteration order is different between this version and the JDK
version (this exact issue broke the SpecJBB benchmark when the iteration order
was changed slightly (via using a slightly different hash function) between
JDK rev's).

If you want to drop-in the non-blocking versions of Hashtable, HashMap or
ConcurrentHashMap, you'll need to alter your bootclasspath - these classes
come directly from your JDK and so are found via the System loader before any
class-path hacks can be done.  

To replace the JDK implementation of Hashtable with a non-blocking version of
Hashtable, add java_util_hashtable.jar to your java launch line:

  java -Xbootclasspath/p:lib/java_util_hashtable.jar my_app_goes_here

Similarly for ConcurrentHashMap, add java_util_concurrent_chm.jar:

  java -Xbootclasspath/p:lib/java_util_concurrent_chm.jar my_app_goes_here


The other utilities do not have direct JDK replacements; you need to call them
out directly and place high_scale_lib.jar in your classpath:

- NonBlockingHashMap - Fast, concurrent, lock-free HashMap.  Linear scaling to 768 CPUs.
- NonBlockingHashMapLong - Same as above, but using primitive 'long' keys
- NonBlockingHashSet - A Set version of NBHM
- NonBlockingSetInt - A fast fully concurrent BitVector
- Counter - A simple counter that scales linearly even when extremely hot.
  Most simple counters are either unsynchronized (hence drop counts, generally
  really badly beyond 2 cpus), or are normally lock'd (hence bottleneck in the
  5-10 cpu range), or might use Atomic's (hence bottleneck in the 25-50 cpu
  range).  This version scales linearly to 768 CPUs.
  


Cliff Click

