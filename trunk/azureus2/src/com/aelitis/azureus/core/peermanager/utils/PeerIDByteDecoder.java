/*
 * Created on Nov 12, 2003
 * Created by Alon Rohter
 * Copyright (C) 2003-2004 Alon Rohter, All Rights Reserved.
 * 
 */
package com.aelitis.azureus.core.peermanager.utils;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import java.io.*;

/**
 * Used for identifying clients by their peerID.
 */
public class PeerIDByteDecoder {
	
  /**
   * Decodes the given peerID, returning an identification string.
   */  
  protected static String decode(byte[] peer_id) {
    String decoded = null;
    byte[] peerID = new byte[peer_id.length];
    System.arraycopy(peer_id, 0, peerID, 0, peer_id.length);
         
    final boolean LOG_UNKNOWN = System.getProperty("log.unknown.peerids").equals( "1" );
    FileWriter log = null;
    File logFile = FileUtil.getUserFile("identification.log");
    
    int iFirstNonZeroPos = 0;
    try {
      if( (decoded = decodeAzStyle( peerID, "AZ", "Azureus" )) != null ) return decoded;
      if( (decoded = decodeAzStyle( peerID, "LT", "libtorrent" )) != null ) return decoded;
      if( (decoded = decodeAzStyle( peerID, "TS", "TorrentStorm" )) != null ) return decoded;
      if( (decoded = decodeAzStyle( peerID, "MT", "MoonlightTorrent" )) != null ) return decoded;
      if( (decoded = decodeAzStyle( peerID, "XT", "XanTorrent" )) != null ) return decoded;
      if( (decoded = decodeAzStyle( peerID, "bk", "BitKitten (libtorrent)" )) != null ) return decoded;
      if( (decoded = decodeTornadoStyle( peerID, "T", "BitTornado" )) != null ) return decoded;
      if( (decoded = decodeTornadoStyle( peerID, "A", "ABC" )) != null ) return decoded;
      if( (decoded = decodeMainlineStyle( peerID, "M", "Mainline" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "martini", "Martini Man" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "oernu", "BTugaXP" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "BTDWV-", "Deadman Walking" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "PRC.P---", "BitTorrent Plus! II" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "P87.P---", "BitTorrent Plus!" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "S587Plus", "BitTorrent Plus!" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 5, "Azureus", "Azureus 2.0.3.2" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "-G3", "G3 Torrent" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 4, "btfans", "SimpleBT" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "btuga", "BTugaXP" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "DansClient", "XanTorrent" )) != null ) return decoded;
      if( (decoded = decodeSimpleStyle( peerID, 0, "Deadman Walking-", "Deadman" )) != null ) return decoded;


      String burst = new String(peerID, 0, 5, Constants.BYTE_ENCODING);
      if( burst.equals( "Mbrst" ) ) {
        String major = new String(peerID, 5, 1, Constants.BYTE_ENCODING);
        String minor = new String(peerID, 7, 1, Constants.BYTE_ENCODING);
        String sub   = new String(peerID, 9, 1, Constants.BYTE_ENCODING);
        return "Burst! " + major + "." + minor + "." + sub;
      }
            
      String turbobt = new String(peerID, 0, 7, Constants.BYTE_ENCODING);
      if (turbobt.equals("turbobt")) {
        return "TurboBT " + new String(peerID, 7, 5, Constants.BYTE_ENCODING);
      }
      
      //not 100% sure on this one
      String plus = new String(peerID, 0, 4, Constants.BYTE_ENCODING);
      if( plus.equals( "Plus" ) ) {
        String v1 = new String(peerID, 4, 1, Constants.BYTE_ENCODING);
        String v2 = new String(peerID, 5, 1, Constants.BYTE_ENCODING);
        String v3 = new String(peerID, 6, 1, Constants.BYTE_ENCODING);
        return "Plus! " + v1 + "." + v2 + "." + v3;
      }
      
      String xbt = new String(peerID, 0, 3, Constants.BYTE_ENCODING);
      if( xbt.equals( "XBT" ) ) {
        String v1 = new String(peerID, 3, 1, Constants.BYTE_ENCODING);
        String v2 = new String(peerID, 4, 1, Constants.BYTE_ENCODING);
        String v3 = new String(peerID, 5, 1, Constants.BYTE_ENCODING);
        return "XBT " + v1 + "." + v2 + "." + v3;
      }
      
      String shadow = new String(peerID, 0, 1, Constants.BYTE_ENCODING);
      if (shadow.equals("S")) {
        
        if ( (peerID[6] == (byte)45) && (peerID[7] == (byte)45) && (peerID[8] == (byte)45) ) {
          String name = "Shad0w ";
          for (int i = 1; i < 3; i++) {
            String v = new String(peerID, i, 1, Constants.BYTE_ENCODING);
            name = name.concat( Integer.parseInt(v, 16) + "." );
          }
          String v = new String(peerID, 3, 1, Constants.BYTE_ENCODING);
          name = name.concat( "" + Integer.parseInt(v, 16) );
          return name;
        }
        
        if (peerID[8] == (byte)0) {
          String name = "Shad0w ";
          for (int i = 1; i < 3; i++) {
            name = name.concat(String.valueOf(peerID[i]) + ".");
          }
          name = name + String.valueOf(peerID[3]);
          return name;
        }
      }
      
     
      String bitspirit = new String(peerID, 2, 2, Constants.BYTE_ENCODING);
      if (bitspirit.equals("BS")) {
        if (peerID[1] == (byte)0)  return "BitSpirit v1";
        if (peerID[1] == (byte)2)  return "BitSpirit v2";
      }
            
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
      
      String bitcomet = new String(peerID, 0, 4, Constants.BYTE_ENCODING);
      if (bitcomet.equals("exbc")) {
        String name = "BitComet ";
        name = name.concat(String.valueOf(peerID[4]) + ".");
        name = name.concat(String.valueOf(peerID[5]/10));
        name = name.concat(String.valueOf(peerID[5]%10));
        return name;
      }
      
      // Shareaza check
      boolean bShareaza = true;
      for (int i = 0; i < 16; i++) {
        if (peerID[i] == (byte)0) {
          bShareaza = false;
          break;
        }
      }
      if (bShareaza) {
        for (int i = 16; i < 20; i++) {
      		if (peerID[i] != (peerID[i % 16] ^ peerID[15 - (i % 16)])) {
            bShareaza = false;
      			break;
      		}
     		}
     		return "Shareaza";
      }
      
      iFirstNonZeroPos = 20;
      for (int i = 0; i < 20; i++) {
        if (peerID[i] != (byte)0) { iFirstNonZeroPos = i; break; }
      }
      
      byte three = (byte)3;
      if ((iFirstNonZeroPos == 9)
          && (peerID[9] == three)
          && (peerID[10] == three)
          && (peerID[11] == three)) {
        return "Snark";
      }
      
      if ((iFirstNonZeroPos == 12) && (peerID[12] == (byte)97) && (peerID[13] == (byte)97)) {
        return "Experimental 3.2.1b2";
      }
      if ((iFirstNonZeroPos == 12) && (peerID[12] == (byte)0) && (peerID[13] == (byte)0)) {
        return "Experimental 3.1";
      }
      if (iFirstNonZeroPos == 12) return "Mainline";
      
    }
    catch (Exception e) { Debug.out(e.toString()); }
    
    if (LOG_UNKNOWN) {
      try {
        log = new FileWriter( logFile, true );

        String text = new String(peerID, 0, 20, Constants.BYTE_ENCODING);
        text = text.replace((char)12, (char)32);
        text = text.replace((char)10, (char)32);
        
        log.write("[" + text + "] ");
        
        for (int i=0; i < 20; i++) {
          log.write(i+"=" + peerID[i] + " ");
        }
        log.write("\n");
        
      }
      catch (Exception e) {
        Debug.out(e.toString());
      }
      finally {
        try {
          if (log != null) log.close();
        }
        catch (IOException ignore) {/*ignore*/}
      }
      
    }

    String sPeerID = getPrintablePeerID( peerID, iFirstNonZeroPos );

    
    return MessageText.getString("PeerSocket.unknown") + " " + iFirstNonZeroPos +"[" + sPeerID + "]";
}
  
  
  
  private static String decodeAzStyle( byte[] id, String ident, String name ) {
    try {
      if( (id[0] == (byte)45) && (id[7] == (byte)45) ) {
        String decoded = new String( id, 1, 2, Constants.BYTE_ENCODING );
        if( decoded.equals( ident ) ) {
          String v1 = new String( id, 3, 1, Constants.BYTE_ENCODING );
          String v2 = new String( id, 4, 1, Constants.BYTE_ENCODING );
          String v3 = new String( id, 5, 1, Constants.BYTE_ENCODING );
          String v4 = new String( id, 6, 1, Constants.BYTE_ENCODING );
          return name + " " + v1 + "." + v2 + "." + v3 + "." + v4;
        }
      }
    }
    catch( Exception e ) {  return null;  }
    return null;
  }
  
  
  private static String decodeTornadoStyle( byte[] id, String ident, String name ) {
    try {
      if( (id[4] == (byte)45) && (id[5] == (byte)45) && (id[6] == (byte)45)
          && (id[7] == (byte)45) && (id[8] == (byte)45)) {
        String decoded = new String( id, 0, 1, Constants.BYTE_ENCODING );
        if( decoded.equals( ident ) ) {
          int v1 = Integer.parseInt( new String( id, 1, 1, Constants.BYTE_ENCODING ), 16 );
          int v2 = Integer.parseInt( new String( id, 2, 1, Constants.BYTE_ENCODING ), 16 );
          int v3 = Integer.parseInt( new String( id, 3, 1, Constants.BYTE_ENCODING ), 16 );
          return name + " " + v1 + "." + v2 + "." + v3;
        }
      }
    }
    catch( Exception e ) {  return null;  }
    return null;
  }
  
  
  private static String decodeSimpleStyle( byte[] id, int start_pos, String ident, String name ) {
    try {
      String decoded = new String( id, start_pos, ident.length(), Constants.BYTE_ENCODING );
      if( decoded.equals( ident ) ) return name;
    }
    catch( Exception e ) {  return null;  }
    return null;
  }
  
  
  private static String decodeMainlineStyle( byte[] id, String ident, String name ) {
    try {
      if ( (id[2] == (byte)45) && (id[4] == (byte)45) && (id[6] == (byte)45) && (id[7] == (byte)45) ) {
        String decoded = new String( id, 0, 1, Constants.BYTE_ENCODING );
        if( decoded.equals( ident ) ) {
          String v1 = new String( id, 1, 1, Constants.BYTE_ENCODING );
          String v2 = new String( id, 3, 1, Constants.BYTE_ENCODING );
          String v3 = new String( id, 5, 1, Constants.BYTE_ENCODING );
          return name + " " + v1 + "." + v2 + "." + v3;
        }
      }
    }
    catch( Exception e ) {  return null;  }
    return null;
  }
  
  

  protected static String
  getPrintablePeerID(
  	byte[]		peerID,
  	int iStartAtPos )
  {
  	String	sPeerID = "";
  	
    try {
    	for (int i = iStartAtPos; i < peerID.length; i++) {
    	  int b = (0xFF & peerID[i]);
    		if (b < 32 || b > 127)
    			peerID[i] = '-';
    	}
    	sPeerID = new String(peerID, iStartAtPos, peerID.length - iStartAtPos, 
    	                     Constants.BYTE_ENCODING);
    }
    catch (UnsupportedEncodingException ignore) {}
    catch (Exception e) {}
    
    return( sPeerID );
  }
}
