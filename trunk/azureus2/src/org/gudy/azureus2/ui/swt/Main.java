/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.File;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Initializer;

/**
 * @author Olivier
 * 
 */
public class 
Main 
{  
  public static final String	PR_MULTI_INSTANCE	= "MULTI_INSTANCE";	// values "true" or "false"
	
  StartServer startServer;
  
  public 
  Main(
  	String args[]) 
  {
  	try{
	  	String	mi_str = System.getProperty( PR_MULTI_INSTANCE );
	  	
	  	boolean mi = mi_str != null && mi_str.equalsIgnoreCase("true");
	  	
	  	AzureusCore		core = AzureusCoreFactory.create();
	
	  		// initialise the SWT locale util
	  	
	    new LocaleUtilSWT( core );
	    
	    startServer = new StartServer(core);
	
	    boolean debugGUI = Boolean.getBoolean("debug");
	    
	    if( mi || debugGUI) {
	      // create a MainWindow regardless to the server state
	      new Initializer(core,startServer,args);
	      return;
	      
	    }
	    
	    for (int i=0;i<args.length;i++){
	
		        // Sometimes Windows use filename in 8.3 form and cannot
		        // match .torrent extension. To solve this, canonical path
		        // is used to get back the long form
		    	
	        String filename = args[i];
	        
	        try{
	          args[i] = new File(filename).getCanonicalPath();
	          
	          LGLogger.log( "Main::main: args[" + i + "] exists = " + new File(filename).exists());
	          
	        }catch(java.io.IOException ioe){
	        }
	    }
	    
	    if (startServer.getState() == StartServer.STATE_LISTENING) {
	
	      startServer.pollForConnections();
	
	      new Initializer(core,startServer,args);
	      
	    }else{
	    	
	      new StartSocket(args);
	      
	      try{
	      	Thread.sleep(2500);
	      }catch( Throwable e ){
	      	
	      }
	    }
  	}catch( AzureusCoreException e ){
  		
   		
  		LGLogger.log( LGLogger.ERROR, "Start failed" );
	
  		Debug.printStackTrace( e );	
  	}
  }
  
  public static void main(String args[]) 
  { 	
  	//Debug.dumpThreads("Entry threads");
 
  	//Debug.dumpSystemProperties();
  	
    new Main(args);
  }
}
