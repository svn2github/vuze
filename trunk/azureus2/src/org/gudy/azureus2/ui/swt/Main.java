/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.mainwindow.Initializer;
import org.gudy.azureus2.ui.swt.updater.UpdateSWTWindow;

/**
 * @author Olivier
 * 
 */
public class Main implements ILocaleUtilChooser {  
  
  public static final String	PR_MULTI_INSTANCE	= "MULTI_INSTANCE";	// values "true" or "false"
	
  StartServer startServer;
  
  public static class StartSocket {
    public StartSocket(String args[]) {
//      if(args.length == 0)
//        return;

      Socket sck = null;
      PrintWriter pw = null;
      try {  
      	LGLogger.log( "Main::startSocket: starting connect to 6880");
        System.out.println("StartSocket: passing startup args to already-running process.");
      	
        sck = new Socket("127.0.0.1", 6880);
        
        pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream(),Constants.DEFAULT_ENCODING));
        
        StringBuffer buffer = new StringBuffer(StartServer.ACCESS_STRING + ";args;");
        
        for(int i = 0 ; i < args.length ; i++) {
          String arg = args[i].replaceAll("&","&&").replaceAll(";","&;");
          buffer.append(arg);
          buffer.append(';');
        }
        
     	LGLogger.log( "Main::startSocket: sending '" + buffer.toString() + "'");
     	 
        pw.println(buffer.toString());
        pw.flush();
      } catch(Exception e) {
        e.printStackTrace();
      } finally {
        try {
          if (pw != null)
            pw.close();
        } catch (Exception e) {
        }
        try {
          if (sck != null)
            sck.close();
        } catch (Exception e) {
        }
      }
    }
  }
  
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
      new Initializer(startServer);
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
    	
      if(!checkForSWT()) {      
        
        startServer.stopIt();
        
        UpdateSWTWindow usWindow = new UpdateSWTWindow();  
        
        if (!usWindow.bIgnored)
          return;
        startServer = new StartServer(this);
      }

      startServer.pollForConnections();

      new Initializer(startServer);
      
      if (args.length != 0) {

        TorrentOpener.openTorrent( args[0]);
      }
      

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

  
  public boolean checkForSWT() {    
    return (SWT.getVersion() >= Constants.MINIMAL_SWT_VERSION) ||
           System.getProperty("azureus.skipSWTcheck") != null;
  }
}
