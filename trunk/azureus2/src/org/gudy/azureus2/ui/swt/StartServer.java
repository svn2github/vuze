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
import java.util.*;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

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

  private boolean bContinue;
  public static final int STATE_FAULTY = 0;
  public static final int STATE_LISTENING = 1;

  protected List		queued_torrents = new ArrayList();
  protected boolean		core_started	= false;
  protected AEMonitor	this_mon		= new AEMonitor( "StartServer" );
  
  public 
  StartServer(
  	AzureusCore		_azureus_core )
   {
    try {
    	azureus_core	= _azureus_core;
        
        socket = new ServerSocket(6880, 50, InetAddress.getByName("127.0.0.1")); //NOLAR: only bind to localhost
        
        state = STATE_LISTENING;    
        
        azureus_core.addLifecycleListener(
		    	new AzureusCoreLifecycleAdapter()
				{
		    		public void
					componentCreated(
						AzureusCore					core,
						AzureusCoreComponent		comp )
		    		{
		    		}
		    		
		    		public void
					started(
						AzureusCore		core )
		    		{
		    			openQueuedTorrents();
		    		}
				});
        
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
	    		runSupport()
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
            showMainWindow();
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
              	                  
                  openTorrent(args);
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

  
  protected void 
  openTorrent(
   	String 		args[]) 
  {
    if(args.length != 0) {
      if(args[0].equals("args")) {
        if(args.length > 1)
        {
        	for ( int i=1;i<args.length;i++){
        		
	          LGLogger.log( "Main::useParam: open '" + args[i] + "'");
	
	          try{
	          	this_mon.enter();
	          	
	          	if ( !core_started ){
	          		
	          		queued_torrents.add( args[i] );
	          		
	          		return;
	          	}
	          }finally{
	          	
	          	this_mon.exit();
	          }
	          
	          try{
	          	
	          	TorrentOpener.openTorrent(azureus_core, args[i]);
	          	
	          }catch( Throwable e ){
	        		
	        	Debug.printStackTrace(e);
	          }
        	}
        }
      }
    }
  }
  
  protected void
  openQueuedTorrents()
  {
    try{
      	this_mon.enter();
      	
      	core_started	= true;
      	
    }finally{
      	
      	this_mon.exit();
    }
     	
    for (int i=0;i<queued_torrents.size();i++){
    	
    	try{
    		TorrentOpener.openTorrent(azureus_core, (String)queued_torrents.get(i));
    		
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    }
  }
  
  protected void 
  showMainWindow() 
  {
    if(MainWindow.getWindow() != null) {
      MainWindow.getWindow().getDisplay().asyncExec(new AERunnable(){
        public void runSupport() {
          if (!COConfigurationManager.getBooleanParameter("Password enabled",false) || MainWindow.getWindow().isVisible())          
            MainWindow.getWindow().setVisible(true);
          else
            PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
        }
      });
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
