/*
 * Created on Feb 27, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util;

import java.io.*;
import java.util.Properties;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.platform.*;

/**
 * Utility class to manage system-dependant information.
 */
public class SystemProperties {
  
		// note this is also used in the restart code....
	
	public static final String SYS_PROP_CONFIG_OVERRIDE = "azureus.config.path";
  /**
   * Path separator charactor.
   */
  public static final String SEP = System.getProperty("file.separator");
  
  private static final String AZ_DIR = "Azureus";
  private static final String WIN_DEFAULT = "Application Data";
  private static final String OSX_DEFAULT = "Library" + SEP + "Application Support";
  
  private static String user_path = null;
  
  
  /**
   * Returns the full path to the user's home azureus directory.
   * Under unix, this is usually ~/.Azureus/
   * Under Windows, this is usually .../Documents and Settings/username/Application Data/Azureus/
   * Under OSX, this is usually /Users/username/Library/Application Support/Azureus/
   */
  public static String getUserPath() {
    
    if ( user_path != null ) {
      return user_path;
    }
    
    // Super Override -- no AZ_DIR or xxx_DEFAULT added at all.
    user_path = System.getProperty( SYS_PROP_CONFIG_OVERRIDE );
    if (user_path != null) {
      if (!user_path.endsWith(SEP))
        user_path += SEP;
      File dir = new File( user_path );
      if (!dir.exists()) {
        dir.mkdirs();
      }
      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Custom): user_path = " + user_path );
      return user_path;
    }
    
    String userhome = System.getProperty("user.home");
        
    if ( Constants.isWindows ) {   	
      try { 
        user_path = PlatformManagerFactory.getPlatformManager().getUserDataDirectory();
        LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from registry: " + user_path  );
      }
      catch ( Throwable e ){
        LGLogger.log( LGLogger.CORE_SYSTEM, "Unable to retrieve user config path from registry. Make sure aereg.dll is present." );
        
        user_path = getEnvironmentalVariable( "APPDATA" );
        
        if ( user_path != null && user_path.length() > 0 ) {
          LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from APPDATA env var instead: " + user_path  );
        }
        else {
          user_path = userhome + SEP + WIN_DEFAULT;
          LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from java user.home var instead: " + user_path  );
        }
      }
    	
      user_path = user_path + SEP + AZ_DIR + SEP;
      
      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Win): user_path = " + user_path );
      
    }else if ( Constants.isOSX ) {
    	
      user_path = userhome + SEP + OSX_DEFAULT + SEP + AZ_DIR + SEP;
      
      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Mac): user_path = " + user_path );
    
    }else{
    	
      user_path = userhome + SEP + "." + AZ_DIR + SEP;
      
      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Unix): user_path = " + user_path );
    }
    
    //if the directory doesn't already exist, create it
    File dir = new File( user_path );
    if (!dir.exists()) {
      dir.mkdirs();
    }
    
    return user_path;
  }
  
  
  /**
   * Returns the full path to the directory where Azureus is installed
   * and running from.
   */
  public static String getApplicationPath() {
    String sDir = System.getProperty("user.dir");
    if (sDir.endsWith(SEP))
      return sDir;

    return sDir + SEP;
  }
  
  
  /**
   * Returns whether or not this running instance was started via
   * Java's Web Start system.
   */
  public static boolean isJavaWebStartInstance() {
    try {
      String java_ws_prop = System.getProperty("azureus.javaws");
      return ( java_ws_prop != null && java_ws_prop.equals( "true" ) );
    }
    catch (Throwable e) {
      //we can get here if running in an applet, as we have no access to system props
      return false;
    }
  }
  
  
  
  /**
   * Will attempt to retrieve an OS-specific environmental var.
   */
  
  public static String 
  getEnvironmentalVariable( 
  		final String _var ) 
  {
  	Process p = null;
  	Properties envVars = new Properties();
  	Runtime r = Runtime.getRuntime();
    BufferedReader br = null;

    	// this approach doesn't work at all on Windows 95/98/ME - it just hangs
    	// so get the hell outta here!
    
    if ( Constants.isWindows9598ME ){
    	
    	return( "" );
    }
    
    try {
    	if ( Constants.isWindows ) {
    		p = r.exec( "cmd.exe /c set" );
    	}
    	else { //we assume unix
    		p = r.exec( "env" );
    	}
    
    	String system_encoding = LocaleUtil.getSystemEncoding();
    	
    	LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getEnvironmentalVariable - " + _var + ", system encoding = " + system_encoding );

    	br = new BufferedReader( new InputStreamReader( p.getInputStream(), system_encoding), 8192);
    	String line;
    	while( (line = br.readLine()) != null ) {
    		int idx = line.indexOf( '=' );
    		if (idx >= 0) {
      		String key = line.substring( 0, idx );
      		String value = line.substring( idx+1 );
      		envVars.setProperty( key, value );
      	}
    	}
      br.close();
    }
    catch (Throwable t) {
      if (br != null) try {  br.close();  } catch (Exception ingore) {}
    }
    
    return envVars.getProperty( _var, "" );
  }


}
