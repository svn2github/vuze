/*
 * Created on 8 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.File;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.launcher.Launcher;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.ui.swt.mainwindow.Initializer;

/**
 * @author Olivier
 * 
 */
public class 
Main 
{  
	private static final LogIDs LOGID = LogIDs.GUI;
  public static final String	PR_MULTI_INSTANCE	= "MULTI_INSTANCE";	// values "true" or "false"
	
  StartServer startServer;
  
  // This method is called by other Main classes via reflection - must be kept public.
  public Main(String args[])
	{
  	try{
  		// this should not be necessary, but since it's public let's play safe
		if(Launcher.checkAndLaunch(Main.class, args))
			return;
  		
  			// This *has* to be done first as it sets system properties that are read and cached by Java
  		
  		COConfigurationManager.preInitialise();
  		
	  	String	mi_str = System.getProperty( PR_MULTI_INSTANCE );
	  	
	  	boolean mi = mi_str != null && mi_str.equalsIgnoreCase("true");
	  	
	    startServer = new StartServer();

		    
	
	    boolean debugGUI = Boolean.getBoolean("debug");
	    
	    if( mi || debugGUI){
	    	
	    	// create a MainWindow regardless to the server state
	    	
		  AzureusCore		core = AzureusCoreFactory.create();

	      new Initializer(core,startServer,args);
	      
	      return;
	    }
	    
	    
	    if ( processParams(args, startServer) ){
	
	    	AzureusCore		core = AzureusCoreFactory.create();
	    	
	    	startServer.pollForConnections(core);
	
	    	new Initializer(core,startServer,args);
	      
	    }
	    
  	}catch( AzureusCoreException e ){
  		
  		Logger.log(new LogEvent(LOGID, "Start failed", e));
  	}
  }
  
  
  
  /**
	 * @param args
	 * @return whether to init the core
	 */
	public static boolean processParams(String[] args, StartServer startServer) {
    boolean	closedown	= false;
    
    boolean another_instance = startServer.getState() != StartServer.STATE_LISTENING;
    
    /*  if another instance is running then set the property which is checked during
     *	class instantiation by various stuff to to avoid pulling in too much state
     *	from the already running instance  
     */
    if(another_instance)
    	System.setProperty("transitory.startup", "1");
    
    	// WATCH OUT FOR LOGGING HERE - we don't want to use Logger if this is a secondary instance as
    	// it initialised TOO MUCH of AZ core
    
    for (int i=0;i<args.length;i++){

    	String	arg = args[i];
    	
    	if ( arg.equalsIgnoreCase( "--closedown" )){
    		
    		closedown	= true;
    		
    		break;
    	}
	        // Sometimes Windows use filename in 8.3 form and cannot
	        // match .torrent extension. To solve this, canonical path
	        // is used to get back the long form
	    	
        String filename = arg;
          
          if( 	filename.toUpperCase().startsWith( "HTTP:" ) || 
          		filename.toUpperCase().startsWith( "HTTPS:" ) || 
         		filename.toUpperCase().startsWith( "MAGNET:" ) ||
       			filename.toUpperCase().startsWith( "DHT:" ) ) {
        	  
        	  if ( !another_instance ){
        		  
        		Logger.log(new LogEvent(LOGID, "Main::main: args[" + i
        				+ "] handling as a URI: " + filename));
        	  }
        	  
            continue;  //URIs cannot be checked as a .torrent file
          }            
        
        try{
        	File	file = new File(filename);
        	
        	if ( !file.exists()){
        		
        		throw( new Exception("File '" + file + "' not found" ));
        	}
        	
        	args[i] = file.getCanonicalPath();
          	
        		// don't use logger if we're not the main instance as we don't want all
        		// the associated core initialisation + debug file moving...
        	
        	if ( (!another_instance) && Logger.isEnabled()){
        		
        		Logger.log(new LogEvent(LOGID, "Main::main: args[" + i
        				+ "] exists = " + new File(filename).exists()));
        	}
        }catch( Throwable e ){
        	
        	if ( another_instance ){
        		
        		e.printStackTrace();
        		
        	}else{
        		
	        	Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
							"Failed to access torrent file '" + filename
									+ "'. Ensure sufficient temporary "
									+ "file space available (check browser cache usage)."));
        	}
        }
    }
    
    
     if( another_instance ) {  //looks like there's already a process listening on 127.0.0.1:6880
    	//attempt to pass args to existing instance
    	StartSocket ss = new StartSocket(args);
    	
    	if( !ss.sendArgs() ) {  //arg passing attempt failed, so start core anyway
    		another_instance = false;
    		String msg = "There appears to be another program process already listening on socket [127.0.0.1: 6880].\nLoading of torrents via command line parameter will fail until this is fixed.";
    		System.out.println( msg );
    		Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_WARNING, msg));
    	}
    }
    
    if ( !another_instance ){
    	
    	if ( closedown ){
    			// closedown request and no instance running
    		return false;
    	}
    	
    	return true;
    }
    return false;
	}


	public static void main(String args[]) 
  {
		if(Launcher.checkAndLaunch(Main.class, args))
			return;
  	//Debug.dumpThreads("Entry threads");
 
  	//Debug.dumpSystemProperties();
  	
		if (System.getProperty("ui.temp") == null) {
			System.setProperty("ui.temp", "az2");
		}

		new Main(args);
  }
}
