/*
 * BeDecoder.java
 *
 * Created on May 30, 2003, 2:44 PM
 */

package org.gudy.azureus2.core3.util;

import java.util.*;
import java.io.*;

/**
 * A set of utility methods to decode a bencoded array of byte into a Map.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 * 
 * @author TdC_VgA
 *
 */
public class BDecoder {
  /** Creates a new instance of BeDecoder */
  private BDecoder() {
  }

  public static Map decode(byte[] data) {
    return BDecoder.decode(new ByteArrayInputStream(data));
  }

  public static Map decode(ByteArrayInputStream data) {
    try {
      return (Map) BDecoder.decodeInputStream(data);
    } catch (IOException e) {
      //e.printStackTrace();
      return null;
    }
  }

  public static Map decode(BufferedInputStream data) {
    try {
      return (Map) BDecoder.decodeInputStream(data);
    } catch (IOException e) {
      //e.printStackTrace(); 
      // TODO new or null like above?           
      return new HashMap();
    }
  }

  public static Object decodeInputStream(InputStream bais) throws IOException {
    if (!bais.markSupported()) {
      throw new IOException("InputStream must support the mark() method");
    }

    //set a mark
    bais.mark(Integer.MAX_VALUE);

    //read a byte
    int tempByte = bais.read();

    //decide what to do
    switch (tempByte) {
      case 'd' :
        //create a new dictionary object
        Map tempMap = new HashMap();

        //get the key   
        byte[] tempByteArray = null;
        while ((tempByteArray = (byte[]) BDecoder.decodeInputStream(bais)) != null) {
          //decode some more
          Object value = BDecoder.decodeInputStream(bais);
          //add the value to the map
          tempMap.put(new String(tempByteArray,"ISO-8859-1"), value);
        }

        //return the map
        return tempMap;

      case 'l' :
        //create the list
        List tempList = new ArrayList();

        //create the key
        Object tempElement = null;
        while ((tempElement = BDecoder.decodeInputStream(bais)) != null) {
          //add the element
          tempList.add(tempElement);
        }
        //return the list
        return tempList;

      case 'e' :
      case -1 :
        return null;

      case 'i' :
        return new Long(BDecoder.getNumberFromStream(bais, 'e'));

      case '0' :
      case '1' :
      case '2' :
      case '3' :
      case '4' :
      case '5' :
      case '6' :
      case '7' :
      case '8' :
      case '9' :
        //move back one
        bais.reset();
        //get the string
        return BDecoder.getByteArrayFromStream(bais);

      default :
        throw new IOException("UNKNOWN COMMAND");
    }
  }

  private static long getNumberFromStream(InputStream bais, char parseChar) throws IOException {
    int length = 0;

    //place a mark
    bais.mark(Integer.MAX_VALUE);

    int tempByte = bais.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
      tempByte = bais.read();
      length++;
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    //reset the mark
    bais.reset();

    //get the length
    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;

    //get the string
    while (count != length && (len = bais.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    //jump ahead in the stream to compensate for the :
    bais.skip(1);

    //return the value
    return Long.parseLong(new String(tempArray));
  }

  private static byte[] getByteArrayFromStream(InputStream bais) throws IOException {
    int length = (int) BDecoder.getNumberFromStream(bais, ':');

    if (length < 0) {
      return null;
    }

    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;
    //get the string
    while (count != length && (len = bais.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    return tempArray;
  }
}
