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
    String generic = MessageText.getString("PeerSocket.generic");
    
    try {

      String azureus     = new String(peerID, 1, 2, Constants.BYTE_ENCODING);
      String old_azureus = new String(peerID, 5, 7, Constants.BYTE_ENCODING);
      String shadow      = new String(peerID, 0, 1, Constants.BYTE_ENCODING);
    
      if (shadow.equals("S")) {
        return "Shadow";
      }
      
      if (old_azureus.equals("Azureus")) return "Azureus";
      
      if (azureus.equals("AZ")) {
        String version = new String(peerID, 3, 4, Constants.BYTE_ENCODING);
        String name = "Azureus ";
        
        for (int i = 0; i < 3; i++) {
          name = name.concat(version.charAt(i) + ".");
        }
        name = name + version.charAt(3);
        
        return name;
      }
      
    }
    catch (Exception ignore) {/*ignore*/}
    
    return generic;  
  }

}
