package org.cliffc.high_scale_lib;

/*
 * Written by Cliff Click and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */


import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

// Test NonBlockingHashSet via JUnit
public class NonBlockingHashSetTest extends TestCase {

  private NonBlockingHashSet<String> _nbhs;
  protected void setUp   () { _nbhs = new NonBlockingHashSet<String>(); }
  protected void tearDown() { _nbhs = null; }

  // Test some basic stuff; add a few keys, remove a few keys
  public void testBasic() {
    assertTrue ( _nbhs.isEmpty() );
    assertTrue ( _nbhs.add("k1") );
    checkSizes (1);
    assertTrue ( _nbhs.add("k2") );
    checkSizes (2);
    assertFalse( _nbhs.add("k1") );
    assertFalse( _nbhs.add("k2") );
    checkSizes (2);
    assertThat ( _nbhs.remove("k1"), is(true ) );
    checkSizes (1);
    assertThat ( _nbhs.remove("k1"), is(false) );
    assertTrue ( _nbhs.remove("k2") );
    checkSizes (0);
    assertFalse( _nbhs.remove("k2") );
    assertFalse( _nbhs.remove("k3") );
    assertTrue ( _nbhs.isEmpty() );
  }

  // Check all iterators for correct size counts
  private void checkSizes(int expectedSize) {
    assertEquals( "size()", _nbhs.size(), expectedSize );
    Iterator it = _nbhs.iterator();
    int result = 0;
    while (it.hasNext()) {
      result++;
      it.next();
    }
    assertEquals( "iterator missed", expectedSize, result );
  }


  public void testIteration() {
    assertTrue ( _nbhs.isEmpty() );
    assertTrue ( _nbhs.add("k1") );
    assertTrue ( _nbhs.add("k2") );

    StringBuffer buf = new StringBuffer();
    for( Iterator<String> i = _nbhs.iterator(); i.hasNext(); ) {
      String val = i.next();
      buf.append(val);
    }
    assertThat("found all vals",buf.toString(),anyOf(is("k1k2"),is("k2k1")));

    assertThat("toString works",_nbhs.toString(), anyOf(is("[k1, k2]"),is("[k2, k1]")));
  }

  public void testIterationBig() {
    for( int i=0; i<100; i++ )
      _nbhs.add("a"+i);
    assertThat( _nbhs.size(), is(100) );

    int sz =0;
    int sum = 0;
    for( String s : _nbhs ) {
      sz++;
      assertThat("",s.charAt(0),is('a'));
      int x = Integer.parseInt(s.substring(1));
      sum += x;
      assertTrue(x>=0 && x<=99);
    }
    assertThat("Found 100 ints",sz,is(100));
    assertThat("Found all integers in list",sum,is(100*99/2));

    assertThat( "can remove 3", _nbhs.remove("a3"), is(true) );
    assertThat( "can remove 4", _nbhs.remove("a4"), is(true) );
    sz =0;
    sum = 0;
    for( String s : _nbhs ) {
      sz++;
      assertThat("",s.charAt(0),is('a'));
      int x = Integer.parseInt(s.substring(1));
      sum += x;
      assertTrue(x>=0 && x<=99);
    }
    assertThat("Found 98 ints",sz,is(98));
    assertThat("Found all integers in list",sum,is(100*99/2 - (3+4)));

  }

  public void testSerial() {
    assertTrue ( _nbhs.isEmpty() );
    assertTrue ( _nbhs.add("k1") );
    assertTrue ( _nbhs.add("k2") );

    // Serialize it out
    try {
      FileOutputStream fos = new FileOutputStream("NBHS_test.txt");
      ObjectOutputStream out = new ObjectOutputStream(fos);
      out.writeObject(_nbhs);
      out.close();
    } catch(IOException ex) {
      ex.printStackTrace();
    }

    // Read it back
    try {
      File f = new File("NBHS_test.txt");
      FileInputStream fis = new FileInputStream(f);
      ObjectInputStream in = new ObjectInputStream(fis);
      NonBlockingHashSet nbhs = (NonBlockingHashSet)in.readObject();
      in.close();
      assertEquals(_nbhs.toString(),nbhs.toString());
      if( !f.delete() ) throw new IOException("delete failed");
    } catch(IOException ex) {
      ex.printStackTrace();
    } catch(ClassNotFoundException ex) {
      ex.printStackTrace();
    }

  }
}
