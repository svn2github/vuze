/*
 * Created on Nov 12, 2003
 * Created by nolar
 *
 */
package org.gudy.azureus2.core3.util;

import org.gudy.azureus2.core3.internat.MessageText;

/**
 * Used for identifying clients by their peerID.
 * 
 * @author Nolar
 */
public class Identification {
  
  
  /**
   * Decodes the given peerID, returning an identification string.
   */  
  public static String decode(byte[] peerID) {
    final boolean DEBUG_ALL = false;
    final boolean DEBUG_UNKNOWN = false;
    
    try {
      if (DEBUG_ALL) System.out.println(new String(peerID, 0, 20, Constants.BYTE_ENCODING));

      String shadow = new String(peerID, 0, 1, Constants.BYTE_ENCODING);
      if (shadow.equals("S")) {
        if ((peerID[8] == (byte)0) && (peerID[11] == (byte)0)) return "Shadow";
        if (peerID[8] == (byte)45) {
          String version = new String(peerID, 1, 3, Constants.BYTE_ENCODING);
          String name = "Shadow ";
          for (int i = 0; i < 2; i++) {
            name = name.concat(version.charAt(i) + ".");
          }
          name = name + version.charAt(2);
          return name;
        }
      }
      
      
      String azureus = new String(peerID, 1, 2, Constants.BYTE_ENCODING);
      if (azureus.equals("AZ")) {
        String version = new String(peerID, 3, 4, Constants.BYTE_ENCODING);
        String name = "Azureus ";
        for (int i = 0; i < 3; i++) {
          name = name.concat(version.charAt(i) + ".");
        }
        name = name + version.charAt(3);
        return name;
      }
      
      
      String old_azureus = new String(peerID, 5, 7, Constants.BYTE_ENCODING);
      if (old_azureus.equals("Azureus")) return "Azureus";
      
      
      String upnp = new String(peerID, 0, 1, Constants.BYTE_ENCODING);
      if (upnp.equals("U")) {
        if (peerID[8] == (byte)45) {
          String version = new String(peerID, 1, 3, Constants.BYTE_ENCODING);
          String name = "UPnP ";
          for (int i = 0; i < 2; i++) {
            name = name.concat(version.charAt(i) + ".");
          }
          name = name + version.charAt(2);
          return name;
        }  
      }
      
      
      String xantorrent = new String(peerID, 0, 10, Constants.BYTE_ENCODING);
      if (xantorrent.equals("DansClient")) return "Xantorrent";
      
      
      String btfans = new String(peerID, 4, 6, Constants.BYTE_ENCODING);
      if (btfans.equals("btfans")) return "btFans";
      
      
      //check for generic client
      boolean allZero = true;
      for (int i = 0; i < 12; i++) {
        if (peerID[i] != (byte)0) { allZero = false; break; }
      }
      if (allZero) return MessageText.getString("PeerSocket.generic");
      
    }
    catch (Exception ignore) {/*ignore*/}
    
    if (DEBUG_UNKNOWN && !DEBUG_ALL) {
      try {
        System.out.println(new String(peerID, 0, 20, Constants.BYTE_ENCODING));
      } catch (Exception ignore) {/*ignore*/} 
    }
    
    return MessageText.getString("PeerSocket.unknown");
  }

}
