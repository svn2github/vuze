/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.core;

/**
 * @author Olivier
 * 
 */
public class ByteFormater {
  public static String nicePrint(byte[] data) {
    if(data == null)
      return "";      
    String out = "";    
    for (int i = 0; i < data.length; i++) {
      out = out + nicePrint(data[i]);
      if (i % 4 == 3)
        out = out + " ";
    }
    return out;
  }
  

  public static String nicePrint(byte[] data, boolean tight) {
    if(data == null)
      return "";      
    String out = "";    
    for (int i = 0; i < data.length; i++) {
      out = out + nicePrint(data[i]);
      if (!tight && (i % 4 == 3))
        out = out + " ";
    }
    return out;
  }


  public static String nicePrint(byte b) {
    byte b1 = (byte) ((b >> 4) & 0x0000000F);
    byte b2 = (byte) (b & 0x0000000F);
    return nicePrint2(b1) + nicePrint2(b2);
  }


  public static String nicePrint2(byte b) {
    String out = "";
    switch (b) {
      case 0 :
        out = "0";
        break;
      case 1 :
        out = "1";
        break;
      case 2 :
        out = "2";
        break;
      case 3 :
        out = "3";
        break;
      case 4 :
        out = "4";
        break;
      case 5 :
        out = "5";
        break;
      case 6 :
        out = "6";
        break;
      case 7 :
        out = "7";
        break;
      case 8 :
        out = "8";
        break;
      case 9 :
        out = "9";
        break;
      case 10 :
        out = "A";
        break;
      case 11 :
        out = "B";
        break;
      case 12 :
        out = "C";
        break;
      case 13 :
        out = "D";
        break;
      case 14 :
        out = "E";
        break;
      case 15 :
        out = "F";
        break;
    }
    return out;
  }



}
