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

import java.io.*;

import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.logging.LGLogger;

public class 
Restarter 
{  
  private static final String mainClass = "org.gudy.azureus2.update.Updater";
  
  
  public static void 
  restartForUpgrade() 
  {
 	 	String	app_path = SystemProperties.getApplicationPath();
	  	
	  	if ( app_path.endsWith(File.separator)){
	  		
	  		app_path = app_path.substring(0,app_path.length()-1);
	  	}
	  	
	 	String	user_path = SystemProperties.getUserPath();
	  	
	  	if ( user_path.endsWith(File.separator)){
	  		
	  		user_path = user_path.substring(0,user_path.length()-1);
	  	}
	  	
	  	String[]	parameters = {
	  			"restart",
	  			app_path,
	  			user_path,
				System.getProperty( SystemProperties.SYS_PROP_CONFIG_OVERRIDE ),
	  	};
	  	
	  	restartAzureus(
	  		new PrintWriter(new ByteArrayOutputStream())
			{
	  			public void
				println(
					String	str )
				{
	  				LGLogger.log( str );
	  			}
					
	  		},
			mainClass,
			new String[0],
			parameters );
  }
  
  
  
  // ******************************** This code is copied into Updater so make changes there too !!!
  
  //Beware that for OSX no SPECIAL Java will be used with
  //This method.
  
  private static final String restartScriptName = "restartScript";
  
  public static void 
  restartAzureus(
  		PrintWriter log, 
		String 		mainClass,
		String[]	properties,
		String[] 	parameters ) 
  {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      restartAzureus_OSX(log,mainClass,properties,parameters);
    } else if(osName.equalsIgnoreCase("Linux")) {
      restartAzureus_Linux(log,mainClass,properties,parameters);
    } else {
      restartAzureus_win32(log,mainClass,properties,parameters);
    }
  }
  
  private static void 
  restartAzureus_win32(
  		PrintWriter log,
		String 		mainClass,
		String[]	properties,
		String[] 	parameters) 
  {
    //Classic restart way using Runtime.exec directly on java(w)
    String classPath = System.getProperty("java.class.path");
        
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "javaw\" -classpath \"" + classPath
						+ "\" " + getLibraryPath();
    
    for (int i=0;i<properties.length;i++){
    	exec += properties[i] + " ";
    }
    
    exec += mainClass;
    
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    try {                
      Runtime.getRuntime().exec(exec);
    } catch(Exception e) {
        e.printStackTrace(log);
   }
  }
  
  private static void 
  restartAzureus_OSX(
  		PrintWriter log,
		String mainClass,
		String[]	properties,
		String[] parameters) 
  {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" " + getLibraryPath();
    
    for (int i=0;i<properties.length;i++){
    	exec += properties[i] + " ";
    }
    
    exec += mainClass ;
    
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
      e.printStackTrace(log);
    }
  }
  
  private static void 
  restartAzureus_Linux(
  	PrintWriter log,
	String 		mainClass,
	String[]	properties,
	String[] 	parameters) 
  {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" " + getLibraryPath();
    
    for (int i=0;i<properties.length;i++){
    	exec += properties[i] + " ";
    }
    
    exec += mainClass ;
    
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
        e.printStackTrace(log);
    }
  }
  
  private static String
  getLibraryPath()
  {
    String libraryPath = System.getProperty("java.library.path");
    
    if ( libraryPath == null ){
    	libraryPath	= "";
    }else if ( libraryPath.length() > 0 ){
    	libraryPath = "-Djava.library.path=\"" + libraryPath + "\" ";
    }
    
    return( libraryPath );
  }
}

