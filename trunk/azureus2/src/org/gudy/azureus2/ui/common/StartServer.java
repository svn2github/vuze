/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * @author Olivier
 * 
 */
public class StartServer extends Thread {

  public static final String ACCESS_STRING = "Azureus Start Server Access";

  private ServerSocket socket;
  private int state;
  private Main main;

  private boolean bContinue;
  public static final int STATE_FAULTY = 0;
  public static final int STATE_LISTENING = 1;

  public StartServer() {
    super("Start Server");
    try {
      socket = new ServerSocket(6880, 50, InetAddress.getByName("localhost")); //NOLAR: only bind to localhost
      state = STATE_LISTENING;
      Logger.getLogger("azureus2").info("StartServer: listening on localhost:6880 for passed torrent info");
    } catch (Exception e) {
      state = STATE_FAULTY;
      Logger.getLogger("azureus2").error("StartServer ERROR: unable to bind to localhost:6880 for passed torrent info");
    }
  }

  public void run() {
    bContinue = true;
    while (bContinue) {
      BufferedReader br = null;
      try {
        Socket sck = socket.accept();
        String address = sck.getInetAddress().getHostAddress();
        if (address.equals("localhost") || address.equals("127.0.0.1")) {
          br = new BufferedReader(new InputStreamReader(sck.getInputStream()));
          String line = br.readLine();
          //System.out.println("received : " + line);
          if (line != null) {
            //            main.showMainWindow();
            StringTokenizer st = new StringTokenizer(line, ";");
            if (st.countTokens() > 1) {
              String args[] = new String[st.countTokens()];
              String checker = st.nextToken();
              if (checker.equals(ACCESS_STRING)) {
                int i = 0;
                while (st.hasMoreElements()) {
                  args[i++] = st.nextToken().replaceAll("&;", ";").replaceAll("&&", "&");
                }
                if (args[0].equals("args")) {
                  String newargs[] = new String[args.length - 1];
                  if (args.length > 1) {
                    for (int j = 1; j < args.length; j++)
                      newargs[j - 1] = args[j];
                  }
                  Main.processArgs(newargs, false, null);
                } else {
                  Logger.getLogger("azureus2").error("Something strange was sent to the StartServer: " + line);
                }
              } else {
				Logger.getLogger("azureus2").error("StartServer: Wrong access token.");
              }
            }
          }
        }
        sck.close();

      } catch (Exception e) {
        if (!(e instanceof SocketException))
          e.printStackTrace();
        bContinue = false;
      } finally {
        try {
          if (br != null)
            br.close();
        } catch (Exception e) {
        }
      }
    }
  }

  public void stopIt() {
    bContinue = false;
    try {
      socket.close();
    } catch (Exception e) {
    }
  }
  /**
   * @return
   */
  public int getState() {
    return state;
  }

}
