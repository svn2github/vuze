/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;


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
  
  public Main(String args[]) {
  	
  	String	mi_str = System.getProperty( PR_MULTI_INSTANCE );
  	
  	boolean mi = mi_str != null && mi_str.equalsIgnoreCase("true");
  	
    COConfigurationManager.checkConfiguration();
    
 	LGLogger.initialise();

    LocaleUtil.setLocaleUtilChooser(this);
    startServer = new StartServer(this);

    boolean debugGUI = Boolean.getBoolean("debug");
    if( mi || debugGUI) {
      // create a MainWindow regardless to the server state
      new Initializer(startServer,args);
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

      new Initializer(startServer,args);
      
    }else{
    	
      new StartSocket(args);
      
      try{
      	Thread.sleep(2500);
      }catch( Throwable e ){
      	
      }
    }
  }
  
  public LocaleUtil getProperLocaleUtil() {
    return new LocaleUtilSWT();
  }
  
  
  public void useParam(String args[]) {
    if(args.length != 0) {
      if(args[0].equals("args")) {
        if(args.length > 1)
        {
          LGLogger.log( "Main::useParam: open '" + args[1] + "'");

          TorrentOpener.openTorrent(args[1]);
        }
      }
    }
  }
  
  public static void main(String args[]) {
  	
  	COConfigurationManager.setSystemProperties();
  	
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
