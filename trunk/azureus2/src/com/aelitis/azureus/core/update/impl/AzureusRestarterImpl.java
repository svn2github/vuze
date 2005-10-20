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
package com.aelitis.azureus.core.update.impl;

import java.io.*;
import java.util.Properties;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.update.AzureusRestarter;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.platform.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.LGLogger;

public class 
AzureusRestarterImpl 
	implements AzureusRestarter
{    
	private static final String MAIN_CLASS 		= "org.gudy.azureus2.update.Updater";
	private static final String UPDATER_JAR 	= "Updater.jar";
  
	public static final String		UPDATE_PROPERTIES	= "update.properties";

	protected static boolean	restarted		= false;
	
	private static String JAVA_EXEC_DIR = System.getProperty("java.home") +
	 																		  System.getProperty("file.separator") +
	 																		  "bin" +
	 																		  System.getProperty("file.separator");
	
	
	protected AzureusCore	azureus_core;
	protected String		classpath_prefix;
	
	public
	AzureusRestarterImpl(
		AzureusCore		_azureus_core )
	{
		azureus_core	= _azureus_core;
	}
	
	public void
	restart(
		boolean	update_only )
	{
		if ( restarted ){
			
			LGLogger.log( "AzureusRestarter: already restarted!!!!");
			
			return;
		}
		
		restarted	= true;
		
		PluginInterface pi = azureus_core.getPluginManager().getPluginInterfaceByID( "azupdater" );
		
		if ( pi == null ){
			
			LGLogger.logUnrepeatableAlert( LGLogger.AT_ERROR, "Can't restart, mandatory plugin 'azupdater' not found" );
			
			return;
		}
		
		String	updater_dir = pi.getPluginDirectoryName();
		
		classpath_prefix = updater_dir + File.separator + UPDATER_JAR;
		
 	 	String	app_path = SystemProperties.getApplicationPath();
	  	
	  	while( app_path.endsWith(File.separator)){
	  		
	  		app_path = app_path.substring(0,app_path.length()-1);
	  	}
	  	
	 	String	user_path = SystemProperties.getUserPath();
	  	
	  	while( user_path.endsWith(File.separator)){
	  		
	  		user_path = user_path.substring(0,user_path.length()-1);
	  	}
	  	
	  	String config_override = System.getProperty( SystemProperties.SYS_PROP_CONFIG_OVERRIDE );
	  	
	  	if ( config_override == null ){
	  		
	  		config_override = "";
	  	}
	  	
	  	String[]	parameters = {
	  			update_only?"updateonly":"restart",
	  			app_path,
	  			user_path,
				config_override,
	  	};
	  	
	  	FileOutputStream	fos	= null;
	  	
	  	try{
	  		Properties	restart_properties = new Properties();
	  	
	  		long	max_mem = Runtime.getRuntime().maxMemory();
	  			  			  			
	  		restart_properties.put( "max_mem", ""+max_mem );
	  		restart_properties.put( "app_name", SystemProperties.getApplicationName());
	  		restart_properties.put( "app_entry", SystemProperties.getApplicationEntryPoint());
	  		
	  		if ( System.getProperty( "azureus.nativelauncher" ) != null || isOSX() ){
	  			//NOTE: new 2306 osx bundle now sets azureus.nativelauncher=1, but older bundles dont
	  			
	  			try{
		  			String	cmd = PlatformManagerFactory.getPlatformManager().getApplicationCommandLine();
		  			
		  			if ( cmd != null ){
		  				
		  				restart_properties.put( "app_cmd", cmd );
		  			}
	  			}catch( Throwable e ){
	  				
	  				Debug.printStackTrace(e);
	  			}
	  		}	  		
	  		
	  		
	  		fos	= new FileOutputStream( new File( user_path, UPDATE_PROPERTIES ));
	  		
	  			// this handles unicode chars by writing \\u escapes
	  		
	  		restart_properties.store(fos, "Azureus restart properties" );
	  		
	  	}catch( Throwable e ){
	  		
	  		Debug.printStackTrace( e );
	  		
	  	}finally{
	  		
	  		if ( fos != null ){
	  			
	  			try{
	  				
	  				fos.close();
	  				
	  			}catch( Throwable e ){
	  				
	  				Debug.printStackTrace(e);
	  			}
	  		}
	  	}
	  	
	  	String[]	properties = { "-Duser.dir=\"" + app_path + "\"" };
	  	
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
	  	restartAzureus(
	  		new PrintWriter(os)
			{
	  			public void
				println(
					String	str )
				{
						// we intercept these logs and log immediately
					
	  				LGLogger.log( str );
	  			}
					
	  		},
			MAIN_CLASS,
			properties,
			parameters );
		
			// just check if any non-logged data exists
		
		byte[]	bytes = os.toByteArray();
		
		if ( bytes.length > 0 ){
			
			LGLogger.log( "AzureusRestarter: extra log - " + new String( bytes ));
		}
	}
  
	
	private String
	getClassPath()
	{
		String classPath = System.getProperty("java.class.path");
	    	
    	classPath = classpath_prefix + System.getProperty("path.separator") + classPath;
	    
	    return( "-classpath \"" + classPath + "\" " );
	}
	  
	
	private boolean
	win32NativeRestart(
		PrintWriter	log,
		String		exec )
	{
	    try{
	    		// we need to spawn without inheriting handles
	    	
	    	PlatformManager pm = PlatformManagerFactory.getPlatformManager();
	    	
	    	pm.createProcess( exec, false );
	    
	    	return( true );
	    	
	    }catch(Throwable e) {
	    	
	        e.printStackTrace(log);
	        
	        return( false );
	    }
	}
	
	private boolean
	isOSX()
	{
		return( Constants.isOSX );
	}
	
	private boolean
	isLinux()
	{
		return( Constants.isLinux );
	}
	
  
  // ****************** This code is copied into Restarter / Updater so make changes there too !!!
  
  


  public void 
  restartAzureus(
      PrintWriter log, 
    String    mainClass,
    String[]  properties,
    String[]  parameters ) 
  {
    if(isOSX()){
    	
    	restartAzureus_OSX(log,mainClass,properties,parameters);
    	
    }else if( isLinux() ){
    	
    	restartAzureus_Linux(log,mainClass,properties,parameters);
      
    }else{
    	
    	restartAzureus_win32(log,mainClass,properties,parameters);
    }
  }
  
  private void 
  restartAzureus_win32(
      PrintWriter log,
    String    mainClass,
    String[]  properties,
    String[]  parameters) 
  {
    
    //Classic restart way using Runtime.exec directly on java(w)    
    String exec = "\"" + JAVA_EXEC_DIR + "javaw\" "+ getClassPath() + getLibraryPath();
    
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
    
    if ( !win32NativeRestart( log, exec )){
      
      // hmm, try java method - this WILL inherit handles but might work :)
          
        try{
        	log.println( "Using java spawn" );

  		  	//NOTE: no logging done here, as we need the method to return right away, before the external process completes
        	Process p = Runtime.getRuntime().exec( exec );
          
        	log.println("    -> " + p );
        	
        }catch(Throwable f){
          
          f.printStackTrace( log );
        }
    }
  }
  
  
  
  private void 
  restartAzureus_OSX(
      PrintWriter log,
    String mainClass,
    String[]  properties,
    String[] parameters) 
  {

     String exec = "\"" + JAVA_EXEC_DIR + "java\" " + getClassPath() + getLibraryPath();
  	 
     for (int i=0;i<properties.length;i++){
    	 exec += properties[i] + " ";
     }
    
     exec += mainClass ;
    
     for(int i = 0 ; i < parameters.length ; i++) {
    	 exec += " \"" + parameters[i] + "\"";
     }

     runExternalCommandViaUnixShell( log, exec );
  }
  
  
  
  private void 
  restartAzureus_Linux(
    PrintWriter log,
  String    mainClass,
  String[]  properties,
  String[]  parameters) 
  {
    
    String exec = "\"" + JAVA_EXEC_DIR + "java\" " + getClassPath() +	getLibraryPath();
    
    for (int i=0;i<properties.length;i++){
      exec += properties[i] + " ";
    }
    
    exec += mainClass ;
    
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    runExternalCommandViaUnixShell( log, exec );
  }
  
  
  
  private String
  getLibraryPath()
  {
    String libraryPath = System.getProperty("java.library.path");
    
    if ( libraryPath == null ){
    	
      libraryPath = "";
      
    }else{
    	
    		// remove any quotes from the damn thing
    	
    	String	temp = "";
    	
    	for (int i=0;i<libraryPath.length();i++){
    		
    		char	c = libraryPath.charAt(i);
    		
    		if ( c != '"' ){
    			
    			temp += c;
    		}
    	}
    	
    	libraryPath	= temp;
    	
    		// remove trailing separator chars if they exist as they stuff up
    		// the following "
    	
    	while( libraryPath.endsWith(File.separator)){
    	
    		libraryPath = libraryPath.substring( 0, libraryPath.length()-1 );
    	}
    	
    	if ( libraryPath.length() > 0 ){
  
    		libraryPath = "-Djava.library.path=\"" + libraryPath + "\" ";
    	}
    }
    
    return( libraryPath );
  }
  
  /*
  private void logStream(String message,InputStream stream,PrintWriter log) {
    BufferedReader br = new BufferedReader (new InputStreamReader(stream));
    String line = null;
    boolean first = true;
    
    try {
      while((line = br.readLine()) != null) {
      	if( first ) {
      		log.println(message);
      		first = false;
      	}
      	
        log.println(line);
      }
    } catch(Exception e) {
       log.println(e);
       e.printStackTrace(log);
    }
  }
  
  
  private void chMod(String fileName,String rights,PrintWriter log) {
    String[] execStr = new String[3];
    execStr[0] = "chmod";
    execStr[1] = rights;
    execStr[2] = fileName;
    
    runExternalCommandsLogged( log, execStr );
  }
  
  
  private Process runExternalCommandLogged( PrintWriter log, String command ) {  //NOTE: will not return until external command process has completed
  	log.println("About to execute: R:[" +command+ "]" );
  	
  	try {
  		Process runner = Runtime.getRuntime().exec( command );
  		runner.waitFor();		
  		logStream( "runtime.exec() output:", runner.getInputStream(), log);
      logStream( "runtime.exec() error:", runner.getErrorStream(), log);
      return runner;
  	}
  	catch( Throwable t ) {
  		log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  		log.println( t );
  		t.printStackTrace( log );
  		return null;
  	}
  }
  
  private Process runExternalCommandsLogged( PrintWriter log, String[] commands ) {  //NOTE: will not return until external command process has completed
  	String cmd = "About to execute: R:[";
  	for( int i=0; i < commands.length; i++ ) {
  		cmd += commands[i];
  		if( i < commands.length -1 )  cmd += " ";
  	}
  	cmd += "]";
  	
  	log.println( cmd );
  	
  	try {
  		Process runner = Runtime.getRuntime().exec( commands );
  		runner.waitFor();		
  		logStream( "runtime.exec() output:", runner.getInputStream(), log);
      logStream( "runtime.exec() error:", runner.getErrorStream(), log);
      return runner;
  	}
  	catch( Throwable t ) {
  		log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  		log.println( t );
  		t.printStackTrace( log );
  		return null;
  	}
  }
  */
  
  
  private void runExternalCommandViaUnixShell( PrintWriter log, String command ) {
  	String[] to_run = new String[3];
  	to_run[0] = "/bin/sh";
  	to_run[1] = "-c";
  	to_run[2] = command;
   	 
  	if( log != null )  log.println("Executing: R:[" +to_run[0]+ " " +to_run[1]+ " " +to_run[2]+ "]" );

  	try {
  		//NOTE: no logging done here, as we need the method to return right away, before the external process completes
  		Runtime.getRuntime().exec( to_run );	
  	}
  	catch(Throwable t) {
  		if( log != null )  {
  			log.println( t.getMessage() != null ? t.getMessage() : "<null>" );
  			log.println( t );
  			t.printStackTrace( log );
  		}
  		else {
  			t.printStackTrace();
  		}
  	}
  }
  
  
}