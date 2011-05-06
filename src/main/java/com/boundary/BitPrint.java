package com.boundary;

/**
 * utility for printing bit patterns
 */
public class BitPrint {
  public static String fmt(long bits) {
    StringBuilder sb = new StringBuilder();
    long mask = 1L<<63;
    for(int i = 1; i <= 64; i++) {
      if((mask & bits) == mask)
        sb.append("1");
      else
        sb.append("0");
      if(i%8 == 0)
        sb.append("|");
      mask >>>= 1;
    }
    return sb.toString();
  }
  public static String fmt(long ... buffer) {
    StringBuilder sb = new StringBuilder();
    for(long bits : buffer) {
      sb.append(fmt(bits)).append("\n");
    }
    return sb.toString();
  }
}
