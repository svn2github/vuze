/*
 * File    : TRTrackerClientClassicImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.client.classic;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import javax.net.ssl.*;

import java.util.zip.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.peer.PEPeerServer;
import org.gudy.azureus2.core3.peer.PEPeerServerListener;

import org.gudy.azureus2.core3.tracker.protocol.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;
import org.gudy.azureus2.core3.tracker.util.impl.*;


/**
 * 
 * This class handles communication with the tracker
 * 
 * @author Olivier
 *
 */
public class 
TRTrackerClientClassicImpl
	implements TRTrackerClient, PEPeerServerListener, ParameterListener
{
	
		
	private static final int OVERRIDE_PERIOD			= 10*1000;
	 
	private static Timer	tracker_timer = new Timer( "Tracker Timer", 32);
	
	public static String 	UDP_REALM = "UDP Tracker";
	
	private TOTorrent				torrent;
	private PEPeerServer			peer_server;
	
	private TimerEvent				current_timer_event;
	private TimerEventPerformer		timer_event_action;
	
	private int					tracker_state 			= TS_INITIALISED;
	private String				tracker_status_str		= "";
	private TRTrackerResponse	last_response			= null;
	private long				last_update_time_secs;
	private long				current_time_to_wait_secs;
  
	private int  failure_added_time = 0;
    private long failure_time_last_updated = 0;
	
	private boolean			stopped;
	private boolean			completed;
	private boolean			complete_reported	= false;
	
	private boolean			update_in_progress	= false;
	
	private long			rd_last_override = 0;
	private int				rd_override_percentage	= 100;

  	private List trackerUrlLists;
     
  	private String lastUsedUrl;
  
  	private String trackerUrlListString;
  
  	private byte[] torrent_hash;
	private String info_hash = "info_hash=";
	private byte[] my_peer_id;
	private String my_peer_id_str = "&peer_id=";
	
	private String 					key_id			= "";
	private static final int	   	key_id_length	= 8;
	private int						key_udp;
	
	private String port;
	private String ip_override;

	private String	last_warning_message	= "";
	
	private TrackerClientAnnounceDataProvider 	announce_data_provider;
	
	private Map	tracker_peer_cache		= new LinkedHashMap();	// insertion order - most recent at end
	private AEMonitor tracker_peer_cache_mon 	= new AEMonitor( "TRTrackerClientClassic:PC" );
	
	protected AEMonitor this_mon 	= new AEMonitor( "TRTrackerClientClassic" );

	private static final boolean	socks_peer_inform;

	static{
	 	socks_peer_inform	= 	
	  		COConfigurationManager.getBooleanParameter("Proxy.Data.Enable", false)&&
	 		COConfigurationManager.getBooleanParameter("Proxy.Data.SOCKS.inform", true );
	 }
	
	public final static int componentID = 2;
	public final static int evtLifeCycle = 0;
	public final static int evtFullTrace = 1;
	public final static int evtErrors = 2;

	// 	listener
	
	private static final int LDT_TRACKER_RESPONSE		= 1;
	private static final int LDT_URL_CHANGED			= 2;
	private static final int LDT_URL_REFRESH			= 3;
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"TrackerClient:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					TRTrackerClientListener	listener = (TRTrackerClientListener)_listener;
					
					if ( type == LDT_TRACKER_RESPONSE ){
						
						listener.receivedTrackerResponse((TRTrackerResponse)value);
						
					}else if ( type == LDT_URL_CHANGED ){
						
						Object[]	x = (Object[])value;
						
						String		url 	= (String)x[0];
						boolean	explicit	= ((Boolean)x[1]).booleanValue();
						
						listener.urlChanged(url, explicit );
						
					}else{
						
						listener.urlRefresh();
					}
				}
			});

	static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static byte[]
	createPeerID()
	{
		byte[] peerId = new byte[20];
	
		byte[] version = Constants.VERSION_ID;
    
		for (int i = 0; i < 8; i++) {
			peerId[i] = version[i];
		}
    
	 	for (int i = 8; i < 20; i++) {
		  int pos = (int) ( Math.random() * chars.length());
	     peerId[i] = (byte)chars.charAt(pos);
		}
	 	
		// System.out.println( "generated new peer id:" + ByteFormatter.nicePrint(peerId));

	 	return( peerId );
	}

	public static String
	createKeyID()
	{
		String	key_id = "";
		
		for (int i = 0; i < key_id_length; i++) {
			int pos = (int) ( Math.random() * chars.length());
		    key_id +=  chars.charAt(pos);
		}
		
		return( key_id );
	}
	
  public 
  TRTrackerClientClassicImpl(
  	TOTorrent		_torrent,
  	PEPeerServer 	_peer_server ) 
  	
  	throws TRTrackerClientException
  {
  	torrent		= _torrent;
  	peer_server	= _peer_server;
  	
		//Get the Tracker url
		
	constructTrackerUrlLists( true );
    
    addConfigListeners();  
   
		//Create our unique peerId
	
    my_peer_id = createPeerID();

    key_id	= createKeyID();
    
	key_udp	= (int)(Math.random() *  (double)0xFFFFFFFFL );
	
	try {
	
		torrent_hash = _torrent.getHash();
		
		this.info_hash += URLEncoder.encode(new String(torrent_hash, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
	  
		this.my_peer_id_str += URLEncoder.encode(new String(my_peer_id, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
	  
	}catch (UnsupportedEncodingException e){
		
		LGLogger.log(componentID, evtLifeCycle,"URL encode fails", e );
	  
	  throw( new TRTrackerClientException( "TRTrackerClient: URL encode fails"));
	  
	}catch( TOTorrentException e ){
	
		LGLogger.log(componentID, evtLifeCycle,"Torrent hash retrieval fails", e );
		
		throw( new TRTrackerClientException( "TRTrackerClient: URL encode fails"));	
	}
	
	peer_server.addListener( this );
	
	setPort();
	   
	timer_event_action =  
		new TimerEventPerformer()
		{
			public void
			perform(
				TimerEvent	this_event )
			{
				long	secs_to_wait = getErrorRetryInterval();
							
				try{
															
					secs_to_wait = requestUpdateSupport();
								
				}finally{
						
					current_time_to_wait_secs	= secs_to_wait;
							
					if ( tracker_state == TS_STOPPED ){
						
						// System.out.println( "\tperform: stopped so no more events");
						
					}else{
						
					
						try{
							this_mon.enter();
						
								// it is possible that the current event was being processed
								// when another thread cancelled it and created a further timer
								// event. if this is the case we don't want to go ahead and
								// create another timer as one already exists 
								
							if ( this_event.isCancelled()){
								
								// System.out.println( "\tperform: cancelled so no new event");
								
							}else{
								
								
								secs_to_wait = getAdjustedSecsToWait();
								
								long target_time = SystemTime.getCurrentTime() + (secs_to_wait*1000);
								
								if ( current_timer_event != null && !current_timer_event.isCancelled()){
									
									if ( 	current_timer_event != this_event &&
											current_timer_event.getWhen() < target_time ){
									
											// existing event is earlier then current target, use it
												
										return;
									}
									
									current_timer_event.cancel();
								}
								
                
                String msg = "perform():: New tracker update event scheduled for " +target_time+ " [+" +(target_time - SystemTime.getCurrentTime())+ "ms], old time=" + (current_timer_event == null ? -1 : current_timer_event.getWhen());
                LGLogger.log( msg );
                
								current_timer_event = 
									tracker_timer.addEvent( target_time, this );
							}
						}finally{
							
							this_mon.exit();
						}
					}
				}
			}
		};
    
		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client Created using url : " + trackerUrlListString);
  }
	
	public void
	portChanged(
		int		new_port )
	{
		setPort();
		
		update( true );
	}
	
  	protected void
	setPort()
  	{
  			// we currently don't support incoming connections when SOCKs proxying
  		
  		int	port_num;
  		
  		if ( socks_peer_inform ){
  			
  			port_num	= 0;
  			
  		}else{
  		
  			port_num	= peer_server.getPort();
  		}
  		
		port = "&port=" + port_num;

			// BitComet extension for no incoming connections
		
		if ( port_num == 0 ){
			
			port += "&hide=1";
		}
  	}
  	
	protected long
	getAdjustedSecsToWait()
	{

	  long		secs_to_wait = current_time_to_wait_secs;
													
	  if( last_response != null && last_response.getStatus() != TRTrackerResponse.ST_ONLINE ) {
      
	    secs_to_wait = getErrorRetryInterval();
							
	  }
    else{
        
      if( rd_override_percentage == 0 )  return REFRESH_MINIMUM_SECS;
							
      secs_to_wait = (secs_to_wait * rd_override_percentage) /100;
									
      if ( secs_to_wait < REFRESH_MINIMUM_SECS ){
	  			
        secs_to_wait = REFRESH_MINIMUM_SECS;
      }
    }
  		
	  return( secs_to_wait );
	}
  
	
	public int
  	getStatus()
  	{
  		return( tracker_state );
  	}
	
  	public String
  	getStatusString()
  	{
  		return( tracker_status_str );
  	}
  	
	public void
	setRefreshDelayOverrides(
		int		percentage )
	{
		if ( percentage > 100 ){
			
			percentage = 100;
			
		}else if ( percentage < 0 ){
			
			percentage	= 0;
		}
		
		long	now = SystemTime.getCurrentTime();
    //only start overriding once the tracker announce update has been called
    boolean override_allowed = rd_last_override > 0 && now - rd_last_override > OVERRIDE_PERIOD;
    
		if ( ( SystemTime.isErrorLast10sec() || override_allowed ) && rd_override_percentage != percentage ){
		
			try{
				this_mon.enter();

				rd_last_override	= now;
				
				rd_override_percentage	= percentage;
				
				if ( current_timer_event != null && !current_timer_event.isCancelled()){
					
					long	start 	= current_timer_event.getCreatedTime();
					long	expiry	= current_timer_event.getWhen();
					
					long	secs_to_wait = getAdjustedSecsToWait();
								
					long target_time = start + (secs_to_wait*1000);

					if ( target_time != expiry ){
						
						current_timer_event.cancel();
						
            String msg = "setRefreshDelayOverrides():: New tracker update event scheduled for " +target_time+ " [+" +(target_time - SystemTime.getCurrentTime())+ "ms], old time=" + (current_timer_event == null ? -1 : current_timer_event.getWhen());
            LGLogger.log( msg );
            
						current_timer_event = 
							tracker_timer.addEvent( 
								start,
								target_time,
								timer_event_action );					
					}			
				}
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	public int
	getTimeUntilNextUpdate()
	{
		try{
			this_mon.enter();
		
			if ( current_timer_event == null ){
				
				return( getErrorRetryInterval() );
			}
					
			int rem = (int)((current_timer_event.getWhen() - SystemTime.getCurrentTime())/1000);
					
			return( rem );
			
		}finally{
			
			this_mon.exit();
		}
	}

	public int
	getLastUpdateTime()
	{
		return( (int)last_update_time_secs );
	}

	public void
	update(
		boolean		force )
	{
		long time = SystemTime.getCurrentTime() / 1000;

		if  ( SystemTime.isErrorLast1min() || force ||
			 	( time - last_update_time_secs >= REFRESH_MINIMUM_SECS )){
    		
			requestUpdate();
		}
	}
	
	public void
	complete(
		boolean	already_reported )
	{
		complete_reported	= (complete_reported || already_reported );
		
		completed			= true;
		
		requestUpdate();
	}
	
	public void
	stop()
	{
		stopped	= true;
        
    removeConfigListeners();
		
		requestUpdate();
	}
	
	protected void
	requestUpdate()
	{
		try{
			this_mon.enter();
			
			if ( current_timer_event != null ){
				
				current_timer_event.cancel();
			}

      String msg = "requestUpdate():: New tracker update event scheduled for " +SystemTime.getCurrentTime()+ " [+" +(SystemTime.getCurrentTime() - SystemTime.getCurrentTime())+ "ms], old time=" + (current_timer_event == null ? -1 : current_timer_event.getWhen());
      LGLogger.log( msg );
      
      rd_last_override = SystemTime.getCurrentTime();  //"pause" overrides for 10s
      
			current_timer_event = 
				tracker_timer.addEvent( 
					SystemTime.getCurrentTime(),
					timer_event_action );
		}
    finally{
			this_mon.exit();
		}
	}
	
	protected long
	requestUpdateSupport()
	{
    
    LGLogger.log( "requestUpdateSupport() started" );
    
		boolean	clear_progress = true;
		
		try{
			try{
				this_mon.enter();

				if ( update_in_progress ){
					
          LGLogger.log( "requestUpdateSupport():: update_in_progress" );
          
					clear_progress = false;
					
					return( getErrorRetryInterval() );
				}
				
				update_in_progress = true;
				
			}finally{
				
				this_mon.exit();
			}
	
			last_update_time_secs	= SystemTime.getCurrentTime()/1000;
			
			tracker_status_str = MessageText.getString("PeerManager.status.checking") + "..."; //$NON-NLS-1$ //$NON-NLS-2$      
		
			TRTrackerResponse	response = null;
			
			if ( stopped ){
				
				if ( tracker_state == TS_INITIALISED ){
					
						// never started
					
					tracker_state = TS_STOPPED;
					
				}else if ( tracker_state != TS_STOPPED ){
			
					response = stopSupport();
					
					if ( response.getStatus() == TRTrackerResponse.ST_ONLINE ){
												
						tracker_state = TS_STOPPED;
						
					}else{
						
							// just have one go at sending a stop event as we don't want to sit here
							// forever trying to send stop to a stuffed tracker
							
						tracker_state = TS_STOPPED;
					}
				}	
			}else if ( tracker_state == TS_INITIALISED ){
							
					// always go through the "start" phase, even if we're already complete
					// as some trackers insist on the initial "start"
				
				response = startSupport();
					
				if ( response.getStatus() == TRTrackerResponse.ST_ONLINE ){
						
					tracker_state = TS_DOWNLOADING;
				}
			}else if ( completed ){
				
				if ( !complete_reported ){
					
					response = completeSupport();
					
					if ( response.getStatus() == TRTrackerResponse.ST_ONLINE ){
						
						complete_reported	= true;
				
						tracker_state = TS_COMPLETED;
					}
				}else{
					tracker_state = TS_COMPLETED;
					
					response = updateSupport();
				}
				
			}else{
				
				response = updateSupport();
			}
						
			if ( response != null ){

				int	rs = response.getStatus();
				
				if ( rs == TRTrackerResponse.ST_OFFLINE ){
      
					tracker_status_str = MessageText.getString("PeerManager.status.offline"); 
      		      
					String	reason = response.getFailureReason();
      		
					if ( reason != null ){
      			
						tracker_status_str += " (" + reason + ")";		
					}
				}else if ( rs == TRTrackerResponse.ST_REPORTED_ERROR ){

					tracker_status_str = MessageText.getString("PeerManager.status.error"); 
	      		      
					String	reason = response.getFailureReason();
      		
					if ( reason != null ){
      			
						tracker_status_str += " (" + reason + ")";		
					}
			
						// move state back to initialised to next time around a "started"
						// event it resent. Required for trackers like 123torrents.com that
						// will fail peers that don't start with a "started" event after a 
						// tracker restart
					
					tracker_state	= TS_INITIALISED;
					
				}else{
	    	       	        	
					tracker_status_str = MessageText.getString("PeerManager.status.ok"); //set the status      //$NON-NLS-1$
				}
				
				last_response = response;
				
				listeners.dispatch( LDT_TRACKER_RESPONSE, response );
				
				return( response.getTimeToWait());
			}else{
				
				tracker_status_str = "";
				
				return( getErrorRetryInterval() );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( getErrorRetryInterval() );
			
		}finally{
			
			try{
				this_mon.enter();
			
				if ( clear_progress ){
					
					update_in_progress = false;
				}
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
	protected TRTrackerResponse 
  	startSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Tracker Client is sending a start Request");
	
		// System.out.println( "started");
		
		return(update("started"));
  	}

  	protected TRTrackerResponse 
  	completeSupport() 
  	{	
		LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Tracker Client is sending a completed Request");
		
		// System.out.println( "complete");
		
		return(update("completed"));
  	}

  	protected TRTrackerResponse 
  	stopSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Tracker Client is sending a stopped Request");

		// System.out.println( "stop");		
	
		return( update("stopped"));
  	}

  	protected TRTrackerResponse 
  	updateSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.SENT, "Tracker Client is sending an update Request");
	
		// System.out.println( "update");
		
		return update("");
  	}
  
  private TRTrackerResponse 
  update(String evt) 
  {
  	TRTrackerResponseImpl	last_failure_resp = null;
		
	for (int i = 0 ; i < trackerUrlLists.size() ; i++) {
	  	
		List urls = (List) trackerUrlLists.get(i);
		
		for (int j = 0 ; j < urls.size() ; j++) {
			
		  String url = (String) urls.get(j);
		  
		  lastUsedUrl = url;
		   
		  String	this_url_string = null;
		  
		  try{
		  
			  this_url_string = constructUrl(evt,url);
			  					  	  
			  URL reqUrl = new URL(this_url_string);
			  
			  TRTrackerResponseImpl resp = decodeTrackerResponse( updateOld(reqUrl));
			  
		      if ( resp.getStatus() == TRTrackerResponse.ST_ONLINE ){
					
	            	urls.remove(j);
	            	
	            	urls.add(0,url);
	            	
	            	trackerUrlLists.remove(i);
	            	
	            	trackerUrlLists.add(0,urls);            
	            
	            	informURLChange( this_url_string, false );
	            	
	            		//and return the result
	            		
	            	return( resp );
			  }else{
			  				  		
			  	last_failure_resp = resp;
			  }
		  }catch( MalformedURLException e ){
		  	
		  	Debug.printStackTrace( e );
		  	
		  	last_failure_resp = 
		  		new TRTrackerResponseImpl( 
		  				TRTrackerResponse.ST_OFFLINE, 
						getErrorRetryInterval(), 
						"malformed URL '" + this_url_string + "'" );
		  	
		  }catch( Exception e ){
		  	
		  	//e.printStackTrace();
		  	
		  	last_failure_resp = 
		  		new TRTrackerResponseImpl( 
		  				TRTrackerResponse.ST_OFFLINE, 
						getErrorRetryInterval(), 
						e.getMessage());
		  }
		}
	  } 
	   
		// things no good here
	
		if ( last_failure_resp == null ){
			
		  	last_failure_resp = 
		  		new TRTrackerResponseImpl( 
		  				TRTrackerResponse.ST_OFFLINE, 
						getErrorRetryInterval(), 
						"Reason Unknown" );
		
		}
     
      TRTrackerResponsePeer[]	cached_peers = getPeersFromCache();
      
      if ( cached_peers.length > 0 ){

      	// System.out.println( "cached peers used:" + cached_peers.length );
      	
      	last_failure_resp.setPeers( cached_peers );
      }
      
      return( last_failure_resp );
  }

 	private byte[] 
 	updateOld(
 		URL 		reqUrl )
  
  		throws Exception
	{
 		
   		// set context in case authentication dialog is required
    	
    	TorrentUtils.setTLSTorrentHash( torrent_hash );
    	
 			// loop to possibly retry update on SSL certificate install
 		
 		for (int i=0;i<2;i++){	
 		
	  		String	failure_reason = null;
	  	
			try{  
				String	protocol = reqUrl.getProtocol();
				
		  		LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, "Tracker Client is Requesting : " + reqUrl);
		  
		  		ByteArrayOutputStream message = new ByteArrayOutputStream();
		  				
		  		if ( protocol.equalsIgnoreCase("udp")){
		  			
		  			failure_reason = announceUDP( reqUrl, message );
		  			
		  		}else{
		  			
		  			failure_reason = announceHTTP( reqUrl, message );
		  			
		  		}
					// if we've got some kind of response then return it
				
				if ( message.size() > 0 ){
			
					return( message.toByteArray());
					
				}else{
					
					if ( failure_reason == null ){
					
						failure_reason = "No data received from tracker";
					}
				}
	
			}catch( SSLHandshakeException e ){
				
				// e.printStackTrace();
								
				if ( i == 0 && e.getMessage().indexOf("No trusted certificate found") != -1 ){
					
					if ( SESecurityManager.installServerCertificates( reqUrl )){
						
							// certificate has been installed
						
						continue;	// retry with new certificate
						
					}else{
						
						failure_reason = exceptionToString( e );
					}
				}
			}catch (Exception e){
		  
		  		// e.printStackTrace();
		  
		  		failure_reason = exceptionToString( e );
			}
				
			if ( failure_reason.indexOf("401" ) != -1 ){
					
				failure_reason = "Tracker authentication failed";
			}
		
			LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Exception while processing the Tracker Request : " + failure_reason);
			
			throw( new Exception( failure_reason));
 		}
 		
 			// should never get here as second iteration of loop will always cause an exit
 		
 		throw( new Exception( "Internal Error: should never get here" ));
  	}
  
 	
 	protected String
 	announceHTTP(
 		URL						reqUrl,
 		ByteArrayOutputStream	message )
 	
 		throws IOException
 	{
 		TRTrackerUtilsImpl.checkForBlacklistedURLs( reqUrl );
 		
 		reqUrl = TRTrackerUtilsImpl.adjustURLForHosting( reqUrl );
 		
 		String	failure_reason = null;
 		
 		HttpURLConnection con;
 		
 		if ( reqUrl.getProtocol().equalsIgnoreCase("https")){
 			
 			// see ConfigurationChecker for SSL client defaults
 			
 			HttpsURLConnection ssl_con = (HttpsURLConnection)reqUrl.openConnection();
 			
 			// allow for certs that contain IP addresses rather than dns names
 			
 			ssl_con.setHostnameVerifier(
 					new HostnameVerifier()
 					{
 						public boolean
 						verify(
 								String		host,
								SSLSession	session )
 						{
 							return( true );
 						}
 					});
 			
 			
 			con = ssl_con;
 			
 		}else{
 			
 			con = (HttpURLConnection) reqUrl.openConnection();
 		}
 		
 		con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);
 		
 		con.setRequestProperty("Connection", "close" );
 		
 		// some trackers support gzip encoding of replies
 		
 		con.addRequestProperty("Accept-Encoding","gzip");
 		
 		try{
 			
 			con.connect();
 			
 			InputStream is = null;
 			
 			try{
 				
 				is = con.getInputStream();
 				
 				String encoding = con.getHeaderField( "content-encoding");
 				
 				boolean	gzip = encoding != null && encoding.equalsIgnoreCase("gzip");
 				
 				// System.out.println( "encoding = " + encoding );
 				
 				if ( gzip ){
 					
 					is = new GZIPInputStream( is );
 				}
 				
 					// there are some trackers out there that don't set content length correctly
 					// so we can't reliably use it :(
 				
 				int content_length = -1; //con.getContentLength();
 				
 				//      System.out.println(length);
 				
 				byte[] data = new byte[1024];
 				
 				int	num_read = 0;
 				
 				// some trackers don't return content-length
 				
 				while ( content_length <= 0 || num_read < content_length ){
 					
 					try{
 						int	len = is.read(data);
 						
 						if ( len > 0 ){
 							
 							message.write(data, 0, len);
 							
 							num_read += len;
 							
 						}else if ( len == 0 ){
 							
 							Thread.sleep(20);
 							
 						}else{
 							
 							break;
 						}
 						
 					}catch (Exception e){
 						
 						LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Exception while Requesting Tracker : " + e);
 						LGLogger.log(componentID, evtFullTrace, LGLogger.ERROR, "Message Received was : " + message);
 						
 						failure_reason = exceptionToString( e );
 						
 						break;
 					}
 				}
 				
 				LGLogger.log(componentID, evtFullTrace, LGLogger.RECEIVED, "Tracker Client ["+lastUsedUrl+"] has received : " + message);
 				
 				
 			}catch (Exception e){
 				
 				// e.printStackTrace();
 				
 				failure_reason = exceptionToString( e );
 				
 			}finally{
 				
 				if (is != null) {
 					
 					try {
 						is.close();
 						
 					}catch (Exception e) {
 					}
 					
 					is = null;
 				}
 			}
 		}finally{
 			con.disconnect();
 		}
 		
 		return( failure_reason );
 	}
 	
 	protected String
 	announceUDP(
 		URL						reqUrl,
		ByteArrayOutputStream	message )
 	
 		throws IOException
 	{
 		reqUrl = TRTrackerUtilsImpl.adjustURLForHosting( reqUrl );

 		String	failure_reason = null;
		
 		PasswordAuthentication	auth = null;	
 		
 		try{
 			if ( reqUrl.getQuery().toLowerCase().indexOf("auth") != -1 ){
 				
 				 auth = SESecurityManager.getPasswordAuthentication( UDP_REALM, reqUrl );
 			}
 						
 			PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( peer_server.getPort());
 			
 			InetSocketAddress destination = new InetSocketAddress(reqUrl.getHost(),reqUrl.getPort()==-1?80:reqUrl.getPort());
 			
 			for (int retry_loop=0;retry_loop<PRUDPPacket.DEFAULT_RETRY_COUNT;retry_loop++){
 				
 				try{
 			
		 			PRUDPPacket connect_request = new PRUDPPacketRequestConnect();
		 			
		 			PRUDPPacket reply = handler.sendAndReceive( auth, connect_request, destination );
		 			
		 			if ( reply.getAction() == PRUDPPacket.ACT_REPLY_CONNECT ){
		 			
		 				PRUDPPacketReplyConnect connect_reply = (PRUDPPacketReplyConnect)reply;
		 				
		 				long	my_connection = connect_reply.getConnectionId();
		 			
		 				PRUDPPacketRequest request;
		 				
		 				if ( PRUDPPacket.VERSION == 1 ){
		 					
		 					PRUDPPacketRequestAnnounce announce_request = new PRUDPPacketRequestAnnounce( my_connection );
		 		
		 					request = announce_request;
		 					
			 					// bit of a hack this...
			 				
			 				String	url_str = reqUrl.toString();
			 				
			 				int		p_pos = url_str.indexOf("?");
			 				
			 				url_str	= url_str.substring(p_pos+1);
			 				
			 				String event_str = getURLParam( url_str, "event" );
			 				
			 				int	event = PRUDPPacketRequestAnnounce.EV_UPDATE;
			 				
			 				if ( event_str != null ){
			 					
			 					if ( event_str.equals( "started" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_STARTED;
			 						
			 					}else if ( event_str.equals( "stopped" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_STOPPED;
			 						
			 					}else if ( event_str.equals( "completed" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_COMPLETED;
			 					}
			 				}
			 				
			 				String	ip_str = getURLParam( url_str, "ip" );
			 				
			 				int	ip = 0;
			 				
			 				if ( ip_str != null ){
			 					
			 					ip = PRHelpers.addressToInt( ip_str);
			 				}
			 				
			 				announce_request.setDetails(
			 					torrent_hash,
			 					my_peer_id,
								getLongURLParam( url_str, "downloaded" ), 
								event,
								ip,
								(int)getLongURLParam( url_str, "numwant" ), 
								getLongURLParam( url_str, "left" ), 
								(short)getLongURLParam( url_str, "port" ),
								getLongURLParam( url_str, "uploaded" ));
		 				
		 				}else{
		 					PRUDPPacketRequestAnnounce2 announce_request = new PRUDPPacketRequestAnnounce2( my_connection );
		 					
		 					request = announce_request;
		 					
		 					// bit of a hack this...
			 				
			 				String	url_str = reqUrl.toString();
			 				
			 				int		p_pos = url_str.indexOf("?");
			 				
			 				url_str	= url_str.substring(p_pos+1);
			 				
			 				String event_str = getURLParam( url_str, "event" );
			 				
			 				int	event = PRUDPPacketRequestAnnounce.EV_UPDATE;
			 				
			 				if ( event_str != null ){
			 					
			 					if ( event_str.equals( "started" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_STARTED;
			 						
			 					}else if ( event_str.equals( "stopped" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_STOPPED;
			 						
			 					}else if ( event_str.equals( "completed" )){
			 						
			 						event = PRUDPPacketRequestAnnounce.EV_COMPLETED;
			 					}
			 				}
			 				
			 				String	ip_str = getURLParam( url_str, "ip" );
			 				
			 				int	ip = 0;
			 				
			 				if ( ip_str != null ){
			 					
			 					ip = PRHelpers.addressToInt( ip_str);
			 				}
			 				
			 				announce_request.setDetails(
			 					torrent_hash,
			 					my_peer_id,
								getLongURLParam( url_str, "downloaded" ), 
								event,
								ip,
								key_udp,
								(int)getLongURLParam( url_str, "numwant" ), 
								getLongURLParam( url_str, "left" ), 
								(short)getLongURLParam( url_str, "port" ),
								getLongURLParam( url_str, "uploaded" ));	
		 				}
		 				
		 				reply = handler.sendAndReceive( auth, request, destination );
		 			
		 				if ( reply.getAction() == PRUDPPacket.ACT_REPLY_ANNOUNCE ){
		 					
		 					if ( auth != null ){
		 						
		 						SESecurityManager.setPasswordAuthenticationOutcome( UDP_REALM, reqUrl, true );
		 					}
		 					
		 					if ( PRUDPPacket.VERSION == 1 ){
			 					PRUDPPacketReplyAnnounce	announce_reply = (PRUDPPacketReplyAnnounce)reply;
			 					
			 					Map	map = new HashMap();
			 					
			 					map.put( "interval", new Long( announce_reply.getInterval()));
			 					
			 					int[]	addresses 	= announce_reply.getAddresses();
			 					short[]	ports		= announce_reply.getPorts();
			 					
			 					List	peers = new ArrayList();
			 					
			 					map.put( "peers", peers );
			 					
			 					for (int i=0;i<addresses.length;i++){
			 						
			 						Map	peer = new HashMap();
			 						
			 						peers.add( peer );
			 						
			 						peer.put( "ip", PRHelpers.intToAddress(addresses[i]).getBytes());
			 						peer.put( "port", new Long( ports[i]));
			 					}
			 					
			 					byte[] data = BEncoder.encode( map );
			 					
			 					message.write( data );
			 					
			 					return( null );
			 					
		 					}else{
		 					
			 					PRUDPPacketReplyAnnounce2	announce_reply = (PRUDPPacketReplyAnnounce2)reply;
			 					
			 					Map	map = new HashMap();
			 					
			 					map.put( "interval", new Long( announce_reply.getInterval()));
			 					
			 					int[]	addresses 	= announce_reply.getAddresses();
			 					short[]	ports		= announce_reply.getPorts();
			 					
			 					map.put( "complete", new Long(announce_reply.getSeeders()));
			 					map.put( "incomplete", new Long(announce_reply.getLeechers()));
			 					
			 					List	peers = new ArrayList();
			 					
			 					map.put( "peers", peers );
			 					
			 					for (int i=0;i<addresses.length;i++){
			 						
			 						Map	peer = new HashMap();
			 						
			 						peers.add( peer );
			 						
			 						peer.put( "ip", PRHelpers.intToAddress(addresses[i]).getBytes());
			 						peer.put( "port", new Long( ports[i]));
			 					}
			 					
			 					byte[] data = BEncoder.encode( map );
			 					
			 					message.write( data );
			 					
			 					return( null );
		 					}
		 				}else{
		 			
		 					failure_reason = ((PRUDPPacketReplyError)reply).getMessage();
		 				}
		 			}else{
		 				
		 				failure_reason = ((PRUDPPacketReplyError)reply).getMessage();
		 			}
		 		}catch( PRUDPPacketHandlerException e ){
		 			
		 			if ( e.getMessage() == null || e.getMessage().indexOf("timed out") == -1 ){
		 				
		 				throw( e );
		 			}
		 		}
 			}
 			
 		}catch( Throwable e ){
 		
 			failure_reason = exceptionToString(e);
 		}
 		
		if ( auth != null ){
					
			SESecurityManager.setPasswordAuthenticationOutcome( UDP_REALM, reqUrl, false );
		}

 		return( failure_reason );
 	}
 	
 	protected long
 	getLongURLParam(
 		String		url,
		String		param )
 	{
 		String	val = getURLParam( url, param );
 		
 		if( val == null ){
 			
 			return(0);
 		}
 		
 		return( Long.parseLong( val ));
 	}
 	
 	protected String
 	getURLParam(
 		String		url,
		String		param )
 	{
 		int	p1 = url.indexOf( param + "=" );
 		
 		if ( p1 == -1 ){
 			
 			return( null );
 		}
 		
 		int	p2 = url.indexOf( "&", p1 );
 		
 		if ( p2 == -1 ){
 			
 			return( url.substring(p1+param.length()+1));
 		}
 		
 		return( url.substring(p1+param.length()+1,p2));
 	}
 	
  protected String
  exceptionToString(
  	Throwable 	e )
  {
  	String class_name = e.getClass().getName();
  	
  	int	pos = class_name.lastIndexOf( '.' );
  	
  	if ( pos != -1 ){
  		
  		class_name = class_name.substring(pos+1);
  	}
  	
  	String str = class_name + ":" + e.getMessage();
  	
  	if ( str.indexOf( "timed out") != -1 ){
  		
  		str  = "timeout";
  	}
  	
  	return( str );
  }
  
  public String constructUrl(String evt,String url) {
  	StringBuffer request = new StringBuffer(url);
  	
  		// if url already has a query component then just append our parameters on the end
  	if ( url.indexOf('?') != -1 ){
  		request.append('&');
  	}else{
  		request.append('?');
  	}
  	
  	request.append(info_hash);
  	request.append(my_peer_id_str);
  	request.append(port);
  	request.append("&uploaded=").append(announce_data_provider.getTotalSent());
  	request.append("&downloaded=").append(announce_data_provider.getTotalReceived());
  	request.append("&left=").append(announce_data_provider.getRemaining());
	
    if (evt.length() != 0) {
    	request.append("&event=").append(evt);
    }
    
    if (evt.equals("stopped")){
    	request.append("&numwant=0");
    }else {
      //calculate how many peers we should ask for
    	
      int numwant = calculateNumWant();


      request.append("&numwant=" + numwant);
      
      //no_peer_id has been made obsolete by 'compact'
      // TODO: remove this 2.0.9.0 or beyond
      
      //request.append("&no_peer_id=1");
      
    	// latest space saving measure, a compact return type where peers are returned
    	// as 6 byte entries in a single byte[] (4 bytes ip, 2 byte port)
      request.append( "&compact=1" );
      
    }
	
    String ip = ip_override==null?COConfigurationManager.getStringParameter("Override Ip", ""):ip_override;
    
    if (ip.length() != 0) {
    	
    		// gotta try and use the non-dns version 
    	
    	String	ip2;
    	
    	try{
    		ip2 = PRHelpers.DNSToIPAddress( ip );
    		
    	}catch( UnknownHostException e){
    		
    		LGLogger.logAlert( "IP Override host resolution fails", e );
   		
    		ip2	= ip;
    	}
    	    	
    	request.append("&ip=").append(ip2);
    }
	
    if ( COConfigurationManager.getBooleanParameter("Tracker Key Enable", true )){
      	
      	request.append( "&key=" + key_id);
    }
    
    return request.toString();
  }

  protected int
  calculateNumWant()
  {
    int MAX_PEERS = 100;
    
    int maxAllowed = PeerUtils.numNewConnectionsAllowed( torrent_hash );
    
    if ( maxAllowed < 0 || maxAllowed > MAX_PEERS ) {
      maxAllowed = MAX_PEERS;
    }

    return maxAllowed;
  }
  
  public byte[] 
  getPeerId() 
  {
  	return my_peer_id;
  }

 	public void 
 	setAnnounceDataProvider(
 			TrackerClientAnnounceDataProvider _provider) 
 	{
 		announce_data_provider = _provider;
 	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public String 
	getTrackerUrl() 
	{
		return lastUsedUrl;
	} 
  
	public void 
	setTrackerUrl(
		String trackerUrl ) 
	{
		trackerUrl = trackerUrl.replaceAll(" ", "");
		
		List list = new ArrayList(1);
  	
		list.add( trackerUrl );
  	
		trackerUrlLists.clear();
  	
		trackerUrlLists.add( list );
	
		informURLChange( trackerUrl, true );       	
	}
  
	public void
	resetTrackerUrl(
			boolean		shuffle )
	{
		constructTrackerUrlLists(shuffle);
 	
		if ( trackerUrlLists.size() == 0 ){
		
			return;
		}
	
		String	first_url = (String)((List)trackerUrlLists.get(0)).get(0);
		
		informURLChange( first_url, true );       	
	}
	
	public void
	refreshListeners()
	{
		informURLRefresh();
	}

	public void
	setIPOverride(
		String		override )
	{
		ip_override = override;
	}
	
	public void
	clearIPOverride()
	{
		ip_override = null;
	}
		
	private void 
	constructTrackerUrlLists(
		boolean	shuffle )
	{
		try{
			trackerUrlLists = new ArrayList();
	  
				//This entry is present on multi-tracker torrents
	  
			TOTorrentAnnounceURLSet[]	announce_sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
	       
			if ( announce_sets.length == 0 ){
	  	
					//If not present, we use the default specification
					
				String url = torrent.getAnnounceURL().toString();
				       
					//We then contruct a list of one element, containing this url, and put this list
					//into the list of lists of urls.
					
				List list = new ArrayList();
				
				list.add(url);
				
				trackerUrlLists.add(list);
			}else{
	  			
					//Ok we have a multi-tracker torrent
				
				for(int i = 0 ; i < announce_sets.length ; i++){
					
				  	//Each list contains a list of urls
				  
					URL[]	urls = announce_sets[i].getAnnounceURLs();
					
				 	List stringUrls = new ArrayList();
				 	
				 	for(int j = 0 ; j < urls.length; j++){
				  		
						//System.out.println(urls.get(j).getClass());
						      
						String url = urls[j].toString();
						            		
							//Shuffle
							
						int pos = shuffle?(int)(Math.random() *  (stringUrls.size()+1)):j;
						
						stringUrls.add(pos,url);
				  	}
				  			  	         
				  		//Add this list to the list
				  		
				 	trackerUrlLists.add(stringUrls);
				}
			}      
		}catch(Exception e){
			
			Debug.printStackTrace( e );
		}
	
		trackerUrlListString = "[";
	
		for (int i=0;i<trackerUrlLists.size();i++){

			List	group = (List)trackerUrlLists.get(i);
			
			trackerUrlListString	+= (i==0?"":",") + "[";
			
			for (int j=0;j<group.size();j++){
				
				String	u = (String)group.get(j);
				
				trackerUrlListString	+= (j==0?"":",") + u;
			}
			
			trackerUrlListString	+= "]";
		}
		
		trackerUrlListString += "]";
		
		// System.out.println( trackerUrlListString );
	}
  
  	protected TRTrackerResponseImpl
  	decodeTrackerResponse(
  		byte[]		data )
  	{
  		String	failure_reason;
  		
  		if ( data == null ){
  			
  			failure_reason = "no response";
  			
  		}else{
  		
	 		try{
					   //parse the metadata
				
	 			try{
	 				Map metaData = BDecoder.decode(data); //$NON-NLS-1$
						
	 					// handle any user warnings in the response
	 				try{
	 					byte[]	b_warning_message = (byte[])metaData.get( "warning message" );
	 				
	 					if ( b_warning_message != null ){
	 						
	 						String	warning_message = new String(b_warning_message);
	 						
	 						if ( !warning_message.equals( last_warning_message )){
	 							
	 							last_warning_message	= warning_message;
	 							
	 							String	expanded_message = 
	 								MessageText.getString(
	 										"TrackerClient.announce.warningmessage",
											new String[]{
	 												announce_data_provider.getName(),
													warning_message });
	 									
	 							LGLogger.logAlert(
	 								LGLogger.AT_WARNING,
									expanded_message );
	 						}
	 					}
	 				}catch( Throwable e ){
	 					
	 					Debug.printStackTrace( e );
	 				}
	 				
					long	time_to_wait;
										
					try {
													
            if( announce_data_provider.getRemaining() == 0 ) { //is a seed
              time_to_wait = ((Long) metaData.get("interval")).longValue();
            }
            else { // slightly shorten the wait so we don't time out
              time_to_wait = (6 * ((Long) metaData.get("interval")).intValue()) / 7;
            }
						
							// guard against crazy return values
						
						if ( time_to_wait < 0 || time_to_wait > 0xffffffffL ){
							
							time_to_wait = 0xffffffffL;
						}
									
				   }catch( Exception e ){
				   	
				     byte[]	failure_reason_bytes = (byte[]) metaData.get("failure reason");
						
				     if ( failure_reason_bytes == null ){
							
				       System.out.println("Problems with Tracker, will retry in 1 minute");
											   			
				       return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, getErrorRetryInterval() ));
	
				     }else{
				     	
				     		// explicit failure from the tracker
				     	
				       failure_reason = new String( failure_reason_bytes, Constants.DEFAULT_ENCODING);
                            				
				       return( new TRTrackerResponseImpl( TRTrackerResponse.ST_REPORTED_ERROR, getErrorRetryInterval(), failure_reason ));
				     }
				   }
				   
				  //System.out.println("Response from Announce: " + new String(data));
          Long lIncomplete = null;
          Long lComplete = (Long)metaData.get("complete");
				  if (lComplete != null) {
            lIncomplete = (Long)metaData.get("incomplete");
            LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
                         "ANNOUNCE SCRAPE1: seeds=" +lComplete+ " peers=" +lIncomplete);
          }
						
						//build the list of peers
					List valid_meta_peers = new ArrayList();
						
				    Object	meta_peers_peek = metaData.get( "peers" );
				    
				    	// list for non-compact returns
				    
				    if ( meta_peers_peek instanceof List ){
				    	
				    	
						List meta_peers = (List)meta_peers_peek;
						 					 
						
							//for every peer
						int peers_length = meta_peers.size();
							
						for (int i = 0; i < peers_length; i++) {
							 	
							Map peer = (Map) meta_peers.get(i);
							   						
							Object s_peerid	= peer.get("peer id"); 
							Object s_ip		= peer.get("ip"); 
							Object s_port	= peer.get("port"); 
													
								// Assert that all ip and port are available
							
							if ( s_ip != null && s_port != null ){
					
									//get the peer ip address
								
								String ip = new String((byte[]) s_ip, Constants.DEFAULT_ENCODING); 
								
									//get the peer port number
								
								int peer_port = ((Long) s_port).intValue(); 
								
								byte[] peer_peer_id;
								
								// extension - if peer id is missing then the tracker isn't sending
								// peer ids to save on bandwidth. However, we need something "unique" to 
								// work on internally so make an ID up from the ip and port
								
								if ( s_peerid == null ){
	                
									// Debug.out(ip + ": tracker did not give peerID in reply");

									peer_peer_id = getAnonymousPeerId( ip, peer_port );
									
									// System.out.println("generated peer id" + new String(peerId) + "/" + ByteFormatter.nicePrint( peerId, true ));
								}else{
								
									peer_peer_id = (byte[])s_peerid ; 
								}
								
								if ( PeerUtils.ignorePeerPort( peer_port )){
								
			 			    		LGLogger.log(
						    				componentID, evtFullTrace, LGLogger.INFORMATION, 
						    				"Ignoring " + ip + ":" + peer_port + " as peer port is in ignore list" );

								}else{
									
									valid_meta_peers.add(new TRTrackerResponsePeerImpl( peer_peer_id, ip, peer_port ));
								}
							} 
						}
				    }else{
				    	
				    		// byte[] for compact returns
				    	
				    
				    	byte[]	meta_peers = (byte[])meta_peers_peek;
				    	
				    	for (int i=0;i<meta_peers.length;i+=6){
				    		
				    		int	ip1 = 0xFF & meta_peers[i];
				    		int	ip2 = 0xFF & meta_peers[i+1];
				    		int	ip3 = 0xFF & meta_peers[i+2];
				    		int	ip4 = 0xFF & meta_peers[i+3];
				    		int	po1 = 0xFF & meta_peers[i+4];
				    		int	po2 = 0xFF & meta_peers[i+5];
				    		
				    		String	ip 		= "" + ip1 + "." + ip2 + "." + ip3 + "." + ip4;
				    		int		peer_port 	= po1*256+po2;
				    		
				    		byte[]	peer_peer_id = getAnonymousPeerId( ip, peer_port );
							
				    		LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
				    				"COMPACT PEER: ip=" +ip+ " port=" +peer_port);
							
                			if ( PeerUtils.ignorePeerPort( peer_port )){
					
        			    		LGLogger.log(
        			    				componentID, evtFullTrace, LGLogger.INFORMATION, 
    				    				"    Ignoring as peer port is in ignore list" );
    	
                			}else{
                				valid_meta_peers.add(new TRTrackerResponsePeerImpl( peer_peer_id, ip, peer_port ));
                			}
				    	}
				    }
				    
					TRTrackerResponsePeer[] peers=new TRTrackerResponsePeer[valid_meta_peers.size()];
					
					valid_meta_peers.toArray(peers);
					
					addToTrackerCache( peers);
					
					TRTrackerResponseImpl resp = new TRTrackerResponseImpl( TRTrackerResponse.ST_ONLINE, time_to_wait, peers );
          
						//reset failure retry interval on successful connect
					
					failure_added_time = 0;
					
					Map extensions = (Map)metaData.get( "extensions" );
					
					resp.setExtensions(extensions);
					
					if (extensions != null && lComplete == null) {
			            lComplete = (Long)extensions.get("complete");
			  		
			            if (lComplete != null) {
			              lIncomplete = (Long)extensions.get("incomplete");
			              LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
			                           "ANNOUNCE SCRAPE2: seeds=" +lComplete+ " peers=" +lIncomplete);
			            }
					}

		          if (lComplete != null && lIncomplete != null) {
		            TRTrackerScraper scraper = TRTrackerScraperFactory.getSingleton();
		            if (scraper != null) {
		              TRTrackerScraperResponse scrapeResponse = scraper.scrape(this);
		              if (scrapeResponse != null) {
		                long lNextScrapeTime = scrapeResponse.getNextScrapeStartTime();
		                long lNewNextScrapeTime = SystemTime.getCurrentTime() + 10*60*1000;
		                if (lNextScrapeTime < lNewNextScrapeTime) {
		                  scrapeResponse.setNextScrapeStartTime(lNewNextScrapeTime);
		                }
		                scrapeResponse.setSeedsPeers(lComplete.intValue(), lIncomplete.intValue());
		              }
		            }
       
		            //resp.setScrapeDetails( lComplete.intValue(), lIncomplete.intValue());
		          }
            
		          return( resp );  

				}catch( IOException e ){
					
						// decode could fail if the tracker's returned, say, an HTTP response
						// indicating server overload
	 				 				
	 				String	trace_data = new String(data);
	 				
	 				LGLogger.log("TRTrackerClient::invalid reply: " + trace_data );
	 				
	 				if ( trace_data.length() > 150 ){
	 					
	 					trace_data = trace_data.substring(0,150) + "...";
	 				}
	 				
	 				failure_reason = "invalid reply: " + trace_data;
	 			}	 				
	 		}catch( Throwable e ){
				
	 			Debug.printStackTrace( e );
				
				failure_reason = "error: " + e.getMessage();
			}
  		}

		return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, getErrorRetryInterval(), failure_reason ));
  	}
  	
	protected void
	informURLChange(
		String	url,
		boolean	explicit  )
	{
		listeners.dispatch(	LDT_URL_CHANGED,
							new Object[]{url,new Boolean(explicit)});
	}
	
	protected void
	informURLRefresh()
	{
		listeners.dispatch( LDT_URL_REFRESH, null );		
	}
	
	public TRTrackerResponse
	getLastResponse()
	{
    if( last_response == null ) {
      return new TRTrackerResponseImpl(TRTrackerResponse.ST_OFFLINE, TRTrackerClient.REFRESH_MINIMUM_SECS );
    }
		return( last_response );
	}
	
  	public void
	addListener(
		TRTrackerClientListener	l )
	{
		listeners.addListener( l );
	}
		
	public void
	removeListener(
		TRTrackerClientListener	l )
	{
		listeners.removeListener(l);
	}
	
	public void
	destroy()
	{
		removeConfigListeners();
       
		peer_server.removeListener( this );
		
		TRTrackerClientFactoryImpl.destroy( this );
	}
    
  
  private void addConfigListeners() {
  }
  
  private void removeConfigListeners() {
  }
  
  public void parameterChanged(String parameterName) {
  }
  
  
  /**
   * Retrieve the retry interval to use on announce errors.
   */
  private int getErrorRetryInterval() {
    
    long currentTime = SystemTime.getCurrentTime() /1000;
        
    
    //use previously calculated interval if it's not time to update
    if ( !SystemTime.isErrorLast1min() &&
        ((currentTime - failure_time_last_updated) < failure_added_time)) {
      return failure_added_time;
    }

    //update previous change time
    failure_time_last_updated = currentTime;
    
    if( failure_added_time == 0 ) { //start
      failure_added_time = 10;
    }
    else if( failure_added_time < 30 ) {
      //three 10-sec retries
      failure_added_time += 10;
    }
    else if( failure_added_time < 60 ) {
      //two 15-sec retries
      failure_added_time += 15;
    }
    else if( failure_added_time < 120 ) {
      //two 30-sec retries
      failure_added_time += 30;
    }
    else if( failure_added_time < 600 ) {
      //eight 60-sec retries
      failure_added_time += 60;
    }
    else {
      //2-3min random retry 
      failure_added_time += 120 + new Random().nextInt( 60 );
    }

    boolean is_seed = (announce_data_provider == null) ? false : announce_data_provider.getRemaining() == 0;
    
    if( is_seed ) failure_added_time = failure_added_time * 2; //no need to retry as often
    
    //make sure we're not waiting longer than 20min
    if( !is_seed && failure_added_time > 1200) {
      failure_added_time = 1200;
    }
    else if ( is_seed && failure_added_time > 3600) { //or 60min if seed
      failure_added_time = 3600;
    }

    return failure_added_time;
  }
  
  	protected byte[]
	getAnonymousPeerId(
		String	my_ip,
		int		my_port )
	{
  		byte[] anon_peer_id = new byte[20];
	
  		// unique initial two bytes to identify this as fake

  		anon_peer_id[0] = (byte)'[';
  		anon_peer_id[1] = (byte)']';

  		try{
	  		byte[]	ip_bytes 	= my_ip.getBytes( Constants.DEFAULT_ENCODING );
	  		int		ip_len		= ip_bytes.length;
	
	  		if ( ip_len > 18 ){
		
	  			ip_len = 18;
	  		}
	
	  		System.arraycopy( ip_bytes, 0, anon_peer_id, 2, ip_len );
									
	  		int	port_copy = my_port;
		
	  		for (int j=2+ip_len;j<20;j++){
			
	  			anon_peer_id[j] = (byte)(port_copy&0xff);
			
	  			port_copy >>= 8;
	  		}
  		}catch( UnsupportedEncodingException e ){
  			
  			Debug.printStackTrace( e );
  		}
  		
  		return( anon_peer_id );
   }
	 	
  		// NOTE: tracker_cache is cleared out in DownloadManager when opening a torrent for the
  		// first time as a DOS prevention measure
  
	public Map
	getTrackerResponseCache()
	{				
		return( exportTrackerCache());
	}
	
	
	public void
	setTrackerResponseCache(
		Map		map )
	{
		int	num = importTrackerCache( map );
		
    LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
                 "TRTrackerClient: imported " + num + " cached peers" );
	}
	
	protected Map
	exportTrackerCache()
	{
		Map	res = new HashMap();
		
		List	peers = new ArrayList();
		
		res.put( "tracker_peers", peers );
		
		try{
			tracker_peer_cache_mon.enter();
			
			Iterator it = tracker_peer_cache.values().iterator();
			
			while( it.hasNext()){
				
				TRTrackerResponsePeer	peer = (TRTrackerResponsePeer)it.next();		

				Map	entry = new HashMap();
				
				entry.put( "ip", peer.getIPAddress().getBytes());
				entry.put( "port", new Long(peer.getPort()));
				
				peers.add( entry );
			}
		
			LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
			             "TRTrackerClient: exported " + tracker_peer_cache.size() + " cached peers" );
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
		
		return( res );
	}
	
	protected int
	importTrackerCache(
		Map		map )
	{
		try{
			if ( map == null ){
				
				return( 0 );
			}
			
			List	peers = (List)map.get( "tracker_peers" );
	
			if ( peers == null ){
				
				return( 0 );
			}
			
			try{
				tracker_peer_cache_mon.enter();
				
				for (int i=0;i<peers.size();i++){
					
					Map	peer = (Map)peers.get(i);
					
					String	peer_ip_address = new String((byte[])peer.get("ip"));
					int		peer_port		= ((Long)peer.get("port")).intValue();
					byte[]	peer_peer_id	= getAnonymousPeerId( peer_ip_address, peer_port );
						
					//System.out.println( "recovered " + ip_address + ":" + port );
					
					if ( PeerUtils.ignorePeerPort( peer_port )){
						
   			    		LGLogger.log(
			    				componentID, evtFullTrace, LGLogger.INFORMATION, 
			    				"Ignoring " + peer_ip_address + ":" + peer_port + " as peer port is in ignore list" );

					}else{
						tracker_peer_cache.put( 
							peer_ip_address, 
							new TRTrackerResponsePeerImpl(peer_peer_id, peer_ip_address, peer_port ));
					}
				}
				
				return( tracker_peer_cache.size());
				
			}finally{
				
				tracker_peer_cache_mon.exit();
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( tracker_peer_cache.size());
		}
	}
	
	protected void
	addToTrackerCache(
		TRTrackerResponsePeer[]		peers )
	{
		int	max = COConfigurationManager.getIntParameter( "File.save.peers.max", DEFAULT_PEERS_TO_CACHE );
		
		// System.out.println( "max peers= " + max );
		
		try{
			tracker_peer_cache_mon.enter();
			
			for (int i=0;i<peers.length;i++){
				
				TRTrackerResponsePeer	peer = peers[i];
				
					// remove and reinsert to maintain most recent last
				
				tracker_peer_cache.remove( peer.getIPAddress());
				
				tracker_peer_cache.put( peer.getIPAddress(), peer );
			}
			
			Iterator	it = tracker_peer_cache.keySet().iterator();
			
			if ( max > 0 ){
					
				while ( tracker_peer_cache.size() > max ){
						
					it.next();
					
					it.remove();
				}
			}
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
	}
	
	public static Map
	mergeResponseCache(
		Map		map1,
		Map		map2 )
	{
		if ( map1 == null & map2 == null ){
			return( new HashMap());
		}else if ( map1 == null ){
			return( map2 );
		}else if ( map2 == null ){
			return( map1 );
		}
		
		Map	res = new HashMap();
				
		List	peers = (List)map1.get( "tracker_peers" );
		
		if ( peers == null ){
			
			peers = new ArrayList();
		}
		
		List	p2 = (List)map2.get( "tracker_peers" );
		
		if ( p2 != null ){
			
      LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
			             "TRTrackerClient: merged peer sets: p1 = " + peers.size() + ", p2 = " + p2.size());
		
			for (int i=0;i<p2.size();i++){
				
				peers.add( p2.get( i ));
			}
		}
		
		res.put( "tracker_peers", peers );
		
		return( res );
	}
	
	protected TRTrackerResponsePeer[]
	getPeersFromCache()
	{
			// use double the num_want as no doubt a fair few connections will fail and
			// we want to get a decent reconnect rate
		
		int	num_want = calculateNumWant() * 2;
	
		try{
			tracker_peer_cache_mon.enter();

			if ( tracker_peer_cache.size() <= num_want ){
				
				TRTrackerResponsePeer[]	res = new TRTrackerResponsePeer[tracker_peer_cache.size()];
				
				tracker_peer_cache.values().toArray( res );
				
			    LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
		                   "TRTrackerClient: returned " + res.length + " cached peers" );
			    
				return( res );
			}
			
			TRTrackerResponsePeer[]	res = new TRTrackerResponsePeer[num_want];
			
			Iterator	it = tracker_peer_cache.keySet().iterator();
			
				// take 'em out and put them back in so we cycle through the peers
				// over time
			
			for (int i=0;i<num_want;i++){
				
				String	key = (String)it.next();
				
				res[i] = (TRTrackerResponsePeer)tracker_peer_cache.get(key);
				
				it.remove();
			}
			
			for (int i=0;i<num_want;i++){
				
				tracker_peer_cache.put( res[i].getIPAddress(), res[i] );
			}
			
		    LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
	                   "TRTrackerClient: returned " + res.length + " cached peers" );
		    
			return( res );
			
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
	} 
}