/*
 * Created on Feb 27, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util;

import java.io.*;
import java.util.Properties;


/**
 * Utility class to manage system-dependant information.
 */
public class SystemProperties {
  
  /**
   * Path separator charactor.
   */
  public static final String SEP = System.getProperty("file.separator");
  
  private static final String AZ_DIR = "Azureus";
  private static final String WIN_DEFAULT = "Application Data";
  private static final String OSX_DEFAULT = "Library";
  
  private static String user_dir_win = null;
  private static String user_path = null;
  
  
  /**
   * Returns the full path to the user's home azureus directory.
   * Under unix, this is usually ~/Azureus/
   * Under Windows, this is usually .../Documents and Settings/username/Application Data/Azureus/
   * Under OSX, this is usually /Users/username/Library/Azureus/
   */
  public static String getUserPath() {
    
    if ( user_path != null ) {
      return user_path;
    }
    
    	// override for testing purposes
    
    String 	userhome 		= System.getProperty("azureus.user.home");
    boolean home_overridden = false;
    
    if ( userhome != null ){
    	
    	home_overridden	= true;
    	
    }else{
    	
    	userhome = System.getProperty("user.home");
    }
    
    String OS = System.getProperty("os.name").toLowerCase();
    
    if ( OS.indexOf("windows") >= 0 ) {
      if ( user_dir_win == null && !home_overridden ) {
        user_dir_win = getEnvironmentalVariable( "APPDATA" );
      }
      
      if ( user_dir_win == null || user_dir_win.length() < 1 ) {  //couldn't find env var, use default
        user_dir_win = userhome + SEP + WIN_DEFAULT;
      }
      
      user_path = user_dir_win + SEP + AZ_DIR + SEP;
    }
    
    else if ( OS.indexOf("mac os x") >= 0 ) {
      user_path = userhome + SEP + OSX_DEFAULT + SEP + AZ_DIR + SEP;
    }
    
    else {
      user_path = userhome + SEP + AZ_DIR + SEP;
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
    return System.getProperty("user.dir") + SEP;
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
  private static String getEnvironmentalVariable( final String _var) {
  	Process p = null;
  	Properties envVars = new Properties();
  	Runtime r = Runtime.getRuntime();
    BufferedReader br = null;
    String OS = System.getProperty("os.name").toLowerCase();

    try {
    	if (OS.indexOf("windows 9") > -1) {
    		p = r.exec( "command.com /c set" );
    	}
    	else if ( (OS.indexOf("nt") > -1) || (OS.indexOf("windows") > -1) ) {
    		p = r.exec( "cmd.exe /c set" );
    	}
    	else { //we assume unix
    		p = r.exec( "env" );
    	}
    
    	br = new BufferedReader( new InputStreamReader( p.getInputStream()), 8192);
    	String line;
    	while( (line = br.readLine()) != null ) {
    		int idx = line.indexOf( '=' );
    		String key = line.substring( 0, idx );
    		String value = line.substring( idx+1 );
    		envVars.setProperty( key, value );
    	}
      br.close();
    }
    catch (Throwable t) {
      if (br != null) try {  br.close();  } catch (Exception ingore) {}
    }
    
    return envVars.getProperty( _var, "" );
  }


}
