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
    
    LGLogger.log("RestartUtil is about to execute (win32) : " + exec);
    try {                
      Runtime.getRuntime().exec(exec);
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
    
    String exec = "#!/bin/bash\n\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    LGLogger.log("RestartUtil is about to execute (osx) : " + exec);
    String fileName = userPath + "/" + restartScriptName;
    LGLogger.log("RestartUtil is about to create the file : " + fileName);
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
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
  
  private static void restartAzureus_Linux(String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    LGLogger.log("RestartUtil is about to execute (linux) : " + exec);
    String fileName = userPath + "/" + restartScriptName;
    LGLogger.log("RestartUtil is about to create the file : " + fileName);
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
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
