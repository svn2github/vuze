/*
 * Created on Apr 8, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.updater;

import java.io.File;
import java.io.FileOutputStream;

import org.gudy.azureus2.core3.logging.LGLogger;

public class RestartUtil {
  
  private static final String restartScriptName = "restartScript";
  
  public static void restartAzureus(String mainClass,String[] parameters) {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      restartAzureus_OSX(mainClass,parameters);
    } else if(osName.equalsIgnoreCase("Linux")) {
      restartAzureus_Linux(mainClass,parameters);
    } else {
      restartAzureus_win32(mainClass,parameters);
    }
  }
  
  
  //Beware that for OSX no SPECIAL Java will be used with
  //This method.
  public static void exec(String command) {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      exec_OSX(command);
    } else if(osName.equalsIgnoreCase("Linux")) {
      exec_Linux(command);
    } else {
      exec_win32(command);
    }
  }
  
  private static void restartAzureus_win32(String mainClass,String[] parameters) {
    //Classic restart way using Runtime.exec directly on java(w)
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "javaw\" -classpath \"" + classPath
    + "\" " + mainClass;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    exec_win32(exec);
  }
  
  /**
   * @param command
   */
  private static void exec_win32(String command) {
    LGLogger.log("RestartUtil is about to execute (win32) : " + command);
    try {                
      Runtime.getRuntime().exec(command);
    } catch(Exception e) {
      LGLogger.log("Exception while restarting : " + e);
      e.printStackTrace();
    }
  }


  private static void restartAzureus_OSX(String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    exec_OSX(exec);
  }
  
  /**
   * @param command
   */
  private static void exec_OSX(String command) {
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    LGLogger.log("RestartUtil is about to execute (osx) : " + command);
    String fileName = userPath + "/" + restartScriptName;
    LGLogger.log("RestartUtil is about to create the file : " + fileName);
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(("#!/bin/bash\n" + command).getBytes());
	    fosUpdate.close();
	    LGLogger.log("RestartUtil has closed the file : " + fileName);
	    LGLogger.log("RestartUtil is changing the rights of file : " + fileName);
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    LGLogger.log("RestartUtil is running file : ./" + restartScriptName);
	    Process p = Runtime.getRuntime().exec(fileName);
    } catch(Exception e) {
      LGLogger.log("RestartUtil has encountered an exception : " + e);
      e.printStackTrace();
    }
  }


  private static void restartAzureus_Linux(String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    exec_Linux(exec);
  }


  /**
   * @param command
   */
  private static void exec_Linux(String command) {
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    LGLogger.log("RestartUtil is about to execute (linux) : " + command);
    String fileName = userPath + "/" + restartScriptName;
    LGLogger.log("RestartUtil is about to create the file : " + fileName);
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(("#!/bin/bash\n" + command).getBytes());
	    fosUpdate.close();
	    LGLogger.log("RestartUtil has closed the file : " + fileName);
	    LGLogger.log("RestartUtil is changing the rights of file : " + fileName);
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    LGLogger.log("RestartUtil is running file : ./" + restartScriptName);
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
      LGLogger.log("RestartUtil has encountered an exception : " + e);
      e.printStackTrace();
    }
  }
}
