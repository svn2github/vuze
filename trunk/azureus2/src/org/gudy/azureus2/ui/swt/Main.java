/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.mainwindow.Initializer;

/**
 * @author Olivier
 * 
 */
public class Main implements ILocaleUtilChooser {  
  
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
	
	    core.setLocaleChooser( this );
	    
	    startServer = new StartServer(core,this);
	
	    boolean debugGUI = Boolean.getBoolean("debug");
	    
	    if( mi || debugGUI) {
	      // create a MainWindow regardless to the server state
	      new Initializer(core,startServer,args);
	      return;
	      
	    }
	    
	    if (args.length != 0) {
	        // Sometimes Windows use filename in 8.3 form and cannot
	        // match .torrent extension. To solve this, canonical path
	        // is used to get back the long form
	        String filename = args[0];
	        try {
	          args[0] = new java.io.File(filename).getCanonicalPath();
	          
	          LGLogger.log( "Main::main: args[0] exists = " + new java.io.File(filename).exists());
	          
	        } catch (java.io.IOException ioe) {
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
	
  		e.printStackTrace(); 		
  	}
  }
  
  public LocaleUtil getProperLocaleUtil() {
    return new LocaleUtilSWT();
  }
  
  
  public void 
  useParam(
  	AzureusCore	azureus_core,
  	String 		args[]) 
  {
    if(args.length != 0) {
      if(args[0].equals("args")) {
        if(args.length > 1)
        {
          LGLogger.log( "Main::useParam: open '" + args[1] + "'");

          TorrentOpener.openTorrent(azureus_core, args[1]);
        }
      }
    }
  }
  
  public static void main(String args[]) 
  { 	
  	//Debug.dumpThreads("Entry threads");
 
  	//Debug.dumpSystemProperties();
  	
    new Main(args);
  }

  public void showMainWindow() {
    if(MainWindow.getWindow() != null) {
      MainWindow.getWindow().getDisplay().asyncExec(new Runnable() {
        public void run() {
          if (!COConfigurationManager.getBooleanParameter("Password enabled",false) || MainWindow.getWindow().isVisible())          
            MainWindow.getWindow().setVisible(true);
          else
            PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
        }
      });
    }
  }

}
