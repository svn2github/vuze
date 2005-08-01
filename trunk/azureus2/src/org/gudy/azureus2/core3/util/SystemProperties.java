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
  
  private static 		String APPLICATION_NAME 		= "Azureus";
  private static 		String APPLICATION_ID 			= "az";
  	// TODO: fix for non-SWT entry points one day
  private static 		String APPLICATION_ENTRY_POINT 	= "org.gudy.azureus2.ui.swt.Main";
  
  private static final 	String WIN_DEFAULT = "Application Data";
  private static final 	String OSX_DEFAULT = "Library" + SEP + "Application Support";
  
  	private static String user_path;
  	private static String app_path;
  
	public static void
	setApplicationName(
		String		name )
	{
		if ( name != null && name.trim().length() > 0 ){
			
			name	= name.trim();
			
			if ( user_path != null ){
				
				if ( !name.equals( APPLICATION_NAME )){
					
					System.out.println( "**** SystemProperties::setApplicationName called too late! ****" );
				}
			}
			
			APPLICATION_NAME			= name;
		}
	}
	
	public static void
	setApplicationIdentifier(
		String		application_id )
	{
		if ( application_id != null && application_id.trim().length() > 0 ){
			
			APPLICATION_ID			= application_id.trim();
		}
	}
	
	public static void
	setApplicationEntryPoint(
		String		entry_point )
	{
		if ( entry_point != null && entry_point.trim().length() > 0 ){

			APPLICATION_ENTRY_POINT	= entry_point.trim();
		}
	}
	
	public static String
	getApplicationName()
	{
		return( APPLICATION_NAME );
	}
	
	public static String
	getApplicationIdentifier()
	{
		return( APPLICATION_ID );
	}	
	
	public static String
	getApplicationEntryPoint()
	{
		return( APPLICATION_ENTRY_POINT );
	}
	
  /**
   * Returns the full path to the user's home azureus directory.
   * Under unix, this is usually ~/.Azureus/
   * Under Windows, this is usually .../Documents and Settings/username/Application Data/Azureus/
   * Under OSX, this is usually /Users/username/Library/Application Support/Azureus/
   */
  public static String 
  getUserPath() 
  {  
    if ( user_path != null ) {
      return user_path;
    }
    
		// WATCH OUT!!!! possible recursion here if logging is changed so that it messes with
		// config initialisation - that's why we don't assign the user_path variable until it
		// is complete - an earlier bug resulted in us half-assigning it and using it due to 
		// recursion. At least with this approach we'll get (worst case) stack overflow if
		// a similar change is made, and we'll spot it!!!!
	
    	// Super Override -- no AZ_DIR or xxx_DEFAULT added at all.
	
    String	temp_user_path = System.getProperty( SYS_PROP_CONFIG_OVERRIDE );
	
	try{
	    if ( temp_user_path != null ){
			
	      if (!temp_user_path.endsWith(SEP)){
			  
	        temp_user_path += SEP;
	      }
		  
	      File dir = new File( temp_user_path );
		  
	      if (!dir.exists()) {
	        dir.mkdirs();
	      }
		  
	      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Custom): user_path = " + temp_user_path );
		  
	      return temp_user_path;
	    }
	    
	    String userhome = System.getProperty("user.home");
	        
	    if ( Constants.isWindows ) {   	
	      try { 
	        temp_user_path = PlatformManagerFactory.getPlatformManager().getUserDataDirectory();
	        LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from registry: " + temp_user_path  );
	      }
	      catch ( Throwable e ){
	        LGLogger.log( LGLogger.CORE_SYSTEM, "Unable to retrieve user config path from registry. Make sure aereg.dll is present." );
	        
	        temp_user_path = getEnvironmentalVariable( "APPDATA" );
	        
	        if ( temp_user_path != null && temp_user_path.length() > 0 ) {
	          LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from APPDATA env var instead: " + temp_user_path  );
	        }
	        else {
	          temp_user_path = userhome + SEP + WIN_DEFAULT;
	          LGLogger.log( LGLogger.CORE_SYSTEM, "Using user config path from java user.home var instead: " + temp_user_path  );
	        }
	      }
	    	
	      temp_user_path = temp_user_path + SEP + APPLICATION_NAME + SEP;
	      
	      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Win): user_path = " + temp_user_path );
	      
	    }else if ( Constants.isOSX ) {
	    	
	      temp_user_path = userhome + SEP + OSX_DEFAULT + SEP + APPLICATION_NAME + SEP;
	      
	      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Mac): user_path = " + temp_user_path );
	    
	    }else{
	    	
	      temp_user_path = userhome + SEP + "." + APPLICATION_NAME + SEP;
	      
	      LGLogger.log( LGLogger.CORE_SYSTEM, "SystemProperties::getUserPath(Unix): user_path = " + temp_user_path );
	    }
	    
	    //if the directory doesn't already exist, create it
	    File dir = new File( temp_user_path );
	    if (!dir.exists()) {
	      dir.mkdirs();
	    }
	    
	    return temp_user_path;
	}finally{
		
		user_path = temp_user_path;
	}
  }
  
  
  /**
   * Returns the full path to the directory where Azureus is installed
   * and running from.
   */
  public static String 
  getApplicationPath() 
  {
	  if ( app_path != null ){
		  
		  return( app_path );
	  }
	  
	  String temp_app_path = System.getProperty("azureus.install.path", System.getProperty("user.dir"));
    
	  if ( !temp_app_path.endsWith(SEP)){
		  
		  temp_app_path += SEP;
	  }

	  app_path = temp_app_path;
	  
	  return( app_path );
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
    
    	String system_encoding = LocaleUtil.getSingleton().getSystemEncoding();
    	
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
