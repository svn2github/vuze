/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

/**
 * @author Olivier
 * 
 */
public class 
StartServer
{
  public static final String ACCESS_STRING = "Azureus Start Server Access";
  private ServerSocket socket;
  private int state;
  private AzureusCore	azureus_core;
  private Main 			main;

  private boolean bContinue;
  public static final int STATE_FAULTY = 0;
  public static final int STATE_LISTENING = 1;

  public 
  StartServer(
  	AzureusCore		_azureus_core,
  	Main 			main ) 
  {
    try {
    	azureus_core	= _azureus_core;
        this.main = main;
        
        socket = new ServerSocket(6880, 50, InetAddress.getByName("127.0.0.1")); //NOLAR: only bind to localhost
        
        state = STATE_LISTENING;
        
        LGLogger.log( "StartServer: listening on 127.0.0.1:6880 for passed torrent info");
    
    }catch (Exception e) {
        state = STATE_FAULTY;
        LGLogger.log( "StartServer ERROR: unable to bind to 127.0.0.1:6880 for passed torrent info");
    }
  }

    public void
    pollForConnections()
	{
	    Thread t = 
	    	new AEThread("Start Server")
			{
	    		public void 
				run()
				{
	    			pollForConnectionsSupport();
	    		}
			};
	    
		t.setDaemon(true);
			
		t.start();     
	}
    
  private void 
  pollForConnectionsSupport() 
  {
    bContinue = true;
    while (bContinue) {
      BufferedReader br = null;
      try {
        Socket sck = socket.accept();
        String address = sck.getInetAddress().getHostAddress();
        if (address.equals("localhost") || address.equals("127.0.0.1")) {
          br = new BufferedReader(new InputStreamReader(sck.getInputStream(),Constants.DEFAULT_ENCODING));
          String line = br.readLine();
          //System.out.println("received : " + line);
          
      	  LGLogger.log( "Main::startServer: received '" + line + "'");
      	 
          if (line != null) {
            main.showMainWindow();
            StringTokenizer st = new StringTokenizer(line, ";");           
            int i = 0;
            if(st.countTokens() > 1) {
              String args[] = new String[st.countTokens() - 1];
              String checker = st.nextToken();
                if(checker.equals(ACCESS_STRING)) {
                	
                  String debug_str = "";
                  	
                  while (st.hasMoreElements()) {              
                    String bit = st.nextToken().replaceAll("&;", ";").replaceAll("&&", "&");
                    
                    debug_str += (debug_str.length()==0?"":" ; ") + bit;
                    
                    args[i++] = bit;
                  }
                  
              	  LGLogger.log( "Main::startServer: decoded to '" + debug_str + "'");
              	                  
                  main.useParam(azureus_core,args);
              }
            }
          }
        }
        sck.close();

      }
      catch (Exception e) {
        if(!(e instanceof SocketException))
        	Debug.printStackTrace( e );      
        //bContinue = false;
      } finally {
        try {
          if (br != null)
            br.close();
        } catch (Exception e) { /*ignore */
        }
      }
    }
  }

  public void stopIt() {
    bContinue = false;
    try {
      socket.close();
    }
    catch (Exception e) {/*ignore */}
  }
  /**
   * @return
   */
  public int getState() {
    return state;
  }

}
