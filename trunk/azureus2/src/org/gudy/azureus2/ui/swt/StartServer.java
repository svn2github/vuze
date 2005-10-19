/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.BufferedReader;
import java.io.File;
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

  private boolean bContinue;
  public static final int STATE_FAULTY = 0;
  public static final int STATE_LISTENING = 1;

  protected List		queued_torrents = new ArrayList();
  protected boolean		core_started	= false;
  protected AEMonitor	this_mon		= new AEMonitor( "StartServer" );
  
  public 
  StartServer()
    {
    try {
        socket = new ServerSocket(6880, 50, InetAddress.getByName("127.0.0.1")); //NOLAR: only bind to localhost
        
        state = STATE_LISTENING;    
        
        LGLogger.log( "StartServer: listening on 127.0.0.1:6880 for passed torrent info");
    
    }catch (Throwable t) {
      state = STATE_FAULTY;
      String reason = t.getMessage() == null ? "<>" : t.getMessage();
      LGLogger.log( "StartServer ERROR: unable to bind to 127.0.0.1:6880 listening for passed torrent info: " +reason );
    }
  }

    public void
    pollForConnections(
   	 	final AzureusCore		azureus_core )

	{
        azureus_core.addLifecycleListener(
		    	new AzureusCoreLifecycleAdapter()
				{
		    		public void
					started(
						AzureusCore		core )
		    		{
		    			openQueuedTorrents( azureus_core );
		    		}
				});
        
	    Thread t = 
	    	new AEThread("Start Server")
			{
	    		public void 
	    		runSupport()
				{
	    			pollForConnectionsSupport( azureus_core );
	    		}
			};
	    
		t.setDaemon(true);
			
		t.start();     
	}
    
  private void 
  pollForConnectionsSupport(
	AzureusCore		azureus_core ) 
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
                  
            		if( !COConfigurationManager.getBooleanParameter( "add_torrents_silently" ) ) {
            			showMainWindow();
                  }
              	                  
                  processArgs(azureus_core,args);
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
  processArgs(
	AzureusCore		azureus_core,
   	String 			args[]) 
  {
    if (args.length < 1 || !args[0].equals( "args" )){
    	
    	return;
    }
           	
    for (int i = 1; i < args.length; i++) {

    	String	arg = args[i];
        	  
  	    if ( arg.equalsIgnoreCase( "--closedown" )){

  	    	MainWindow.getWindow().destroyRequest();
  	    	
  	    	return;
  	    }
  	    
        String file_name = arg;
        
        if( file_name.toUpperCase().startsWith( "HTTP:" ) || file_name.toUpperCase().startsWith( "MAGNET:" ) ) {
        	
          LGLogger.log( "StartServer: args[" + i + "] handling as a URI: " +file_name );
        
        }else{

            try {
              File file = new File(file_name);

              if (!file.exists()) {

                throw (new Exception("File not found"));
              }

              file_name = file.getCanonicalPath();

              LGLogger.log("StartServer: file = " + file_name);

            } catch (Throwable e) {

              LGLogger
                  .logRepeatableAlert(
                      LGLogger.AT_ERROR,
                      "Failed to access torrent file '"
                          + file_name
                          + "'. Ensure sufficient temporary file space available (check browser cache usage).");
            }
        }
        
        boolean	queued = false;
        
        try {
          this_mon.enter();

          if (!core_started) {

            queued_torrents.add(file_name);

            queued = true;
          }
        } finally {

          this_mon.exit();
        }

        if ( !queued ){
            try {

              TorrentOpener.openTorrent(azureus_core, file_name);

            } catch (Throwable e) {

              Debug.printStackTrace(e);
            }
        }
      }
  }
  
  protected void
  openQueuedTorrents(
	AzureusCore		azureus_core )
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
