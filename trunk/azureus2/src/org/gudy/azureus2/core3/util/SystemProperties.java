/*
 * Created on Feb 27, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.core3.util;

import java.io.*;


/**
 * Utility class to manage system-dependant information.
 */
public class SystemProperties {
  
  public static final String SEPARATOR = System.getProperty("file.separator");
  private static final String USER_DIR_LINUX = ".azureus";
  private static final String USER_DIR = "azureus";
  
  /**
   * Returns the full path to the user's home azureus directory.
   * Under unix, this is usually ~/.azureus/
   * Under Windows, this is usually .../Documents and Settings/username/.azureus/
   */
  public static String getUserPath() {
    String path;
    if ( System.getProperty("os.name").equals("Linux")) {
    	path = System.getProperty("user.home") + SEPARATOR + USER_DIR_LINUX + SEPARATOR;
    }
    else {
      path = System.getProperty("user.home") + SEPARATOR + USER_DIR + SEPARATOR;
    }
    
    //if the directory doesn't already exist, create it
    File dir = new File( path );
    if (!dir.exists()) {
      dir.mkdirs();
    }
    
    return path;
  }
  
  
  /**
   * Returns the full path to the directory where Azureus is installed
   * and running from.
   */
  public static String getApplicationPath() {
    return System.getProperty("user.dir") + SEPARATOR;
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

}
