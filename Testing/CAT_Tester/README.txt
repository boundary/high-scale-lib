
A testing harness for scalable counters.

There are some counter implementations in the local java/util which
must be in the bootclasspath (they use Unsafe).

The main counter is ConcurrentAutoTable.java, which is kept in the
top-level java/util directory, so I end up with 2 java/util
directories: one for the testing harness & strawman counter
implementations, one for the main implementation.


