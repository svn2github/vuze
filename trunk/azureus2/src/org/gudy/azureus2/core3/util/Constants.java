/*
 * Created on 16 juin 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.util;

/**
 * 
 * Should be used for constants??? 
 * 
 * @author Olivier
 *
 */

public class Constants {
    
  public static final String DEFAULT_ENCODING 	= "UTF8";
  public static final String BYTE_ENCODING 		= "ISO-8859-1";
  
  public static final String INFINITY_STRING	= "\u221E"; // "oo";
  
  public static final String AZUREUS_NAME	    = "Azureus";
  public static final String AZUREUS_VERSION  = "2.0.4.3_CVS";
  public static final byte[] VERSION_ID       = ("-" + "AZ" + "2043" + "-").getBytes();  //MUST be 8 chars long!
}
