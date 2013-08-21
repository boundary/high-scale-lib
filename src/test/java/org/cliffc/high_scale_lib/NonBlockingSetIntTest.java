package org.cliffc.high_scale_lib;

import junit.framework.TestCase;

import java.io.*;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NonBlockingSetIntTest extends TestCase {

  public void testDerp() {
    NonBlockingSetInt a = new NonBlockingSetInt();
    NonBlockingSetInt b = new NonBlockingSetInt();
    
    a.add(1213446);
    NonBlockingSetInt c = a.union(b);
  }

  public void testSetOperations() {
    // add values to the following bitsets
    // 'a' will get all values divisible by 63
    // 'b' will get the rest
    // this ensures that one NBSI will have the largest bit array, while the other
    // will have the largest recursive bitset used to store values for every 64th bit
    // this exercises that the constructor building a blank NBSI to hold bitwise operations
    // on two others is sized appropriately initially
    NonBlockingSetInt a = new NonBlockingSetInt();
    NonBlockingSetInt b = new NonBlockingSetInt();
    NonBlockingSetInt empty = new NonBlockingSetInt();
    int max = 20000000;

    for(int i = 0; i < max; i++) {
      NonBlockingSetInt t = (i&63) == 63 ? a : b;
      t.add(i);
      assertTrue(t.contains(i));
    }

    // c should contain the empty set since a and b are disjoint
    NonBlockingSetInt c = a.intersect(b);
    NonBlockingSetInt d = b.intersect(a);
    for(int i = 0; i < max; i++) {
      assertFalse(c.contains(i));
      assertFalse(d.contains(i));
    }

    c = a.union(b);
    d = b.union(a);
    for(int i = 0; i < max; i++) {
      assertTrue(c.contains(i));
      assertTrue(d.contains(i));
    }

    c = a.union(empty);
    d = empty.union(a);

    IntIterator itr = a.intIterator();
    while(itr.hasNext()) {
      int next = itr.next();
      assertTrue(c.contains(next));
      assertTrue(d.contains(next));
    }

    itr = b.intIterator();
    while(itr.hasNext()) {
      int next = itr.next();
      assertFalse(c.contains(next));
      assertFalse(d.contains(next));
    }

    c = b.union(empty);
    d = empty.union(b);

    itr = b.intIterator();
    while(itr.hasNext()) {
      int next = itr.next();
      assertTrue(c.contains(next));
      assertTrue(d.contains(next));
    }

    itr = a.intIterator();
    while(itr.hasNext()) {
      int next = itr.next();
      assertFalse(c.contains(next));
      assertFalse(d.contains(next));
    }

    // just make sure the bitset is usable after building out an instance with an ample internal buffer
    for(int i = 0; i < max; i++) {
      c.add(i);
      assertTrue(c.contains(i));
      d.add(i);
      assertTrue(d.contains(i));
    }

    c.intersect(new NonBlockingSetInt());
    new NonBlockingSetInt().intersect(c);
  }

  private NonBlockingSetInt _nbsi;
  protected void setUp   () { _nbsi = new NonBlockingSetInt(); }
  protected void tearDown() { _nbsi = null; }

  // Test some basic stuff; add a few keys, remove a few keys
  public void testBasic() {
    assertTrue ( _nbsi.isEmpty() );
    assertTrue ( _nbsi.add(1) );
    checkSizes (1);
    assertTrue ( _nbsi.add(2) );
    checkSizes (2);
    assertFalse( _nbsi.add(1) );
    assertFalse( _nbsi.add(2) );
    checkSizes (2);
    assertThat ( _nbsi.remove(1), is(true ) );
    checkSizes (1);
    assertThat ( _nbsi.remove(1), is(false) );
    assertTrue ( _nbsi.remove(2) );
    checkSizes (0);
    assertFalse( _nbsi.remove(2) );
    assertFalse( _nbsi.remove(3) );
    assertTrue ( _nbsi.isEmpty() );
    assertTrue ( _nbsi.add(63) );
    checkSizes (1);
    assertTrue ( _nbsi.remove(63) );
    assertFalse( _nbsi.remove(63) );


    assertTrue ( _nbsi.isEmpty() );
    assertTrue ( _nbsi.add(10000) );
    checkSizes (1);
    assertTrue ( _nbsi.add(20000) );
    checkSizes (2);
    assertFalse( _nbsi.add(10000) );
    assertFalse( _nbsi.add(20000) );
    checkSizes (2);
    assertThat ( _nbsi.remove(10000), is(true ) );
    checkSizes (1);
    assertThat ( _nbsi.remove(10000), is(false) );
    assertTrue ( _nbsi.remove(20000) );
    checkSizes (0);
    assertFalse( _nbsi.remove(20000) );
  }

  // Check all iterators for correct size counts
  private void checkSizes(int expectedSize) {
    assertEquals( "size()", _nbsi.size(), expectedSize );
    Iterator it = _nbsi.iterator();
    int result = 0;
    while (it.hasNext()) {
      result++;
      it.next();
    }
    assertEquals( "iterator missed", expectedSize, result );
  }


  public void testIteration() {
    assertTrue ( _nbsi.isEmpty() );
    assertTrue ( _nbsi.add(1) );
    assertTrue ( _nbsi.add(2) );

    StringBuffer buf = new StringBuffer();
    for( Iterator<Integer> i = _nbsi.iterator(); i.hasNext(); ) {
      Integer val = i.next();
      buf.append(val);
    }
    assertThat("found all vals",buf.toString(),anyOf(is("12"),is("21")));

    assertThat("toString works",_nbsi.toString(), anyOf(is("[1, 2]"),is("[2, 1]")));
  }

  public void testIterationBig() {
    for( int i=0; i<100; i++ )
      _nbsi.add(i);
    assertThat( _nbsi.size(), is(100) );

    int sz =0;
    int sum = 0;
    for( Integer x : _nbsi ) {
      sz++;
      sum += x;
      assertTrue(x>=0 && x<=99);
    }
    assertThat("Found 100 ints",sz,is(100));
    assertThat("Found all integers in list",sum,is(100*99/2));

    assertThat( "can remove 3", _nbsi.remove(3), is(true) );
    assertThat( "can remove 4", _nbsi.remove(4), is(true) );
    sz =0;
    sum = 0;
    for( Integer x : _nbsi ) {
      sz++;
      sum += x;
      assertTrue(x>=0 && x<=99);
    }
    assertThat("Found 98 ints",sz,is(98));
    assertThat("Found all integers in list",sum,is(100*99/2 - (3+4)));

  }

  public void testSerial() {
    assertTrue ( _nbsi.isEmpty() );
    assertTrue ( _nbsi.add(1) );
    assertTrue ( _nbsi.add(2) );

    // Serialize it out
    try {
      FileOutputStream fos = new FileOutputStream("NBSI_test.txt");
      ObjectOutputStream out = new ObjectOutputStream(fos);
      out.writeObject(_nbsi);
      out.close();
    } catch(IOException ex) {
      ex.printStackTrace();
    }

    // Read it back
    try {
      File f = new File("NBSI_test.txt");
      FileInputStream fis = new FileInputStream(f);
      ObjectInputStream in = new ObjectInputStream(fis);
      NonBlockingSetInt nbsi = (NonBlockingSetInt)in.readObject();
      in.close();
      assertEquals(_nbsi.toString(),nbsi.toString());
      if( !f.delete() ) throw new IOException("delete failed");
    } catch(IOException ex) {
      ex.printStackTrace();
    } catch(ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  // Do some simple concurrent testing
  public void testConcurrentSimple() throws InterruptedException {
    final NonBlockingSetInt nbsi = new NonBlockingSetInt();

    // In 2 threads, add & remove even & odd elements concurrently
    Thread t1 = new Thread() { public void run() { work_helper(nbsi,"T1",1); } };
    t1.start();
    work_helper(nbsi,"T0",1);
    t1.join();

    // In the end, all members should be removed
    StringBuffer buf = new StringBuffer();
    buf.append("Should be emptyset but has these elements: {");
    boolean found = false;
    for( Integer x : nbsi ) {
      buf.append(" ").append(x);
      found = true;
    }
    if( found ) System.out.println(buf);
    assertThat( "concurrent size=0", nbsi.size(), is(0) );
    for( Integer x : nbsi ) {
      assertTrue("No elements so never get here",false);
    }

  }

  void work_helper(NonBlockingSetInt nbsi, String thrd, int d) {
    final int ITERS = 100000;
    for( int j=0; j<10; j++ ) {
      long start = System.nanoTime();
      for( int i=d; i<ITERS; i+=2 )
        nbsi.add(i);
      for( int i=d; i<ITERS; i+=2 )
        nbsi.remove(i);
      double delta_nanos = System.nanoTime()-start;
      double delta_secs = delta_nanos/1000000000.0;
      double ops = ITERS*2;
      //System.out.println("Thrd"+thrd+" "+(ops/delta_secs)+" ops/sec size="+nbsi.size());
    }
  }

  public void testRetainAllNonBlocking() {
    NonBlockingSetInt nonBlockingSetInt = new NonBlockingSetInt();
    nonBlockingSetInt.add(1);
    nonBlockingSetInt.add(2);
    nonBlockingSetInt.add(3);
    NonBlockingSetInt toRetain = new NonBlockingSetInt();
    toRetain.add(1);
    toRetain.add(3);
    assertTrue(nonBlockingSetInt.retainAll(toRetain));
    assertEquals(nonBlockingSetInt, toRetain);
  }
}
