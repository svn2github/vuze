/*
 * Created on May 16, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.update;

import java.io.File;
import java.io.FileOutputStream;

import org.gudy.azureus2.core3.logging.LGLogger;

public class Restarter {
  
  private static final String classpath = "Updater.jar";
  private static final String mainClass = "org.gudy.azureus2.update.Updater";
  
  
  private static final String restartScriptName = "restartScript";
  
  public static void restartForUpgrade() {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      restartForUpgrade_OSX();
    } else if(osName.equalsIgnoreCase("Linux")) {
      restartForUpgrade_Linux();
    } else {
      restartForUpgrade_win32();
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
  
  private static void restartForUpgrade_win32() {
    //Classic restart way using Runtime.exec directly on java(w)
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "javaw\" -classpath \"" + classPath
    + "\" " + mainClass;
    
    /*
    //Parameters if needed.
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    */
    
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


  private static void restartForUpgrade_OSX() {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    
    /*
    //Parameters if needed.
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    */
    
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


  private static void restartForUpgrade_Linux() {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    
    /*
    //Parameters if needed.
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    */
    
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

