package org.cliffc.high_scale_lib;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * HighScaleLibTestSuite
 * @author Dietrich Featherston
 */
public class HighScaleLibTestSuite extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new TestSuite(NonBlockingSetIntTest.class));
    suite.addTest(new TestSuite(NonBlockingHashSetTest.class));
    suite.addTest(new TestSuite(NonBlockingHashMapTest.class));
    suite.addTest(new TestSuite(NonBlockingIdentityHashMapTest.class));
    suite.addTest(new TestSuite(NonBlockingHashMapLongTest.class));
    return suite;
  }
}
