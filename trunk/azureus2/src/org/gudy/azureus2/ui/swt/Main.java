/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.gudy.azureus2.core.GlobalManager;

/**
 * @author Olivier
 * 
 */
public class Main {  
  
  StartServer startServer;
  MainWindow mainWindow;
  GlobalManager gm;
  
  public class StartSocket {
    public StartSocket(String args[]) {
      try {      
        Socket sck = new Socket("localhost",6880);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream()));
        StringBuffer buffer = new StringBuffer("args;");
        for(int i = 0 ; i < args.length ; i++) {
          String arg = args[i].replaceAll("&","&&");
          arg = arg.replaceAll(";","&;");          
          buffer.append(arg);
          buffer.append(";");
        }
        pw.println(buffer.toString());
        pw.flush();
        sck.close();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  public Main(String args[]) {
     startServer = new StartServer(this);
     if(startServer.getState() == StartServer.STATE_LISTENING) {
       startServer.start();
       gm = new GlobalManager();
       mainWindow = new MainWindow(gm,startServer);
       if(args.length != 0)
        mainWindow.openTorrent(args[0]);
       mainWindow.waitForClose();
     }
     else {
       new StartSocket(args);
     }
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
}
