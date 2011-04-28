package com.boundary;

import junit.framework.TestCase;

public class BitPrintTest extends TestCase {
  public void testBitPrinting() {
    long[] buf = new long[256];
    for(int i = 0; i < buf.length; i++) {
      buf[i] = i;
    }
    System.out.println(BitPrint.fmt(buf));
  }
}
