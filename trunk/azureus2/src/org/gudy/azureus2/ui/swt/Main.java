/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.ILocaleUtilChooser;
import org.gudy.azureus2.core.LocaleUtil;

/**
 * @author Olivier
 * 
 */
public class Main implements ILocaleUtilChooser {  
  
  StartServer startServer;
  MainWindow mainWindow;
  GlobalManager gm;
  
  public static class StartSocket {
    public StartSocket(String args[]) {
//      if(args.length == 0)
//        return;

      Socket sck = null;
      PrintWriter pw = null;
      try {      
        sck = new Socket("localhost",6880);
        pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream()));
        StringBuffer buffer = new StringBuffer("args;");
        for(int i = 0 ; i < args.length ; i++) {
          String arg = args[i].replaceAll("&","&&").replaceAll(";","&;");
          buffer.append(arg);
          buffer.append(';');
        }
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
    LocaleUtil.setLocaleUtilChooser(this);
    startServer = new StartServer(this);
    if (startServer.getState() == StartServer.STATE_LISTENING) {
      startServer.start();
      gm = new GlobalManager();
      mainWindow = new MainWindow(gm, startServer);
      if (args.length != 0) {
        // Sometimes Windows use filename in 8.3 form and cannot
        // match .torrent extension. To solve this, canonical path
        // is used to get back the long form
        String filename = args[0];
        try {
          filename = new java.io.File(args[0]).getCanonicalPath();
        } catch (java.io.IOException ioe) {
        }
        mainWindow.openTorrent(filename);
      }
      mainWindow.waitForClose();
    } else {
      new StartSocket(args);
    }
  }
  
  public LocaleUtil getProperLocaleUtil(Object lastEncoding) {
    return new LocaleUtilSWT(lastEncoding);
  }
  
  
  public void useParam(String args[]) {
    if(args.length != 0) {
      if(args[0].equals("args")) {
        if(args.length > 1)
        {
          mainWindow.openTorrent(args[1]);
        }
      }
    }
  }
  
  public static void main(String args[]) {
    new Main(args);
  }

  public void showMainWindow() {
    if(mainWindow != null) {
      mainWindow.getDisplay().asyncExec(new Runnable() {
        public void run() {
          if (!ConfigurationManager.getInstance().getBooleanParameter("Password enabled",false) || mainWindow.isVisible())          
            mainWindow.setVisible(true);
          else
            PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
        }
      });
    }
  }

}
