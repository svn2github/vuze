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

import javax.net.ssl.*;

import java.util.zip.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.*;

/**
 * 
 * This class handles communication with the tracker
 * 
 * @author Olivier
 *
 */
public class 
TRTrackerClientClassicImpl
	implements TRTrackerClient 
{
	private static final int REFRESH_ERROR_SECS			= 120;
	private static final int OVERRIDE_PERIOD			= 10*1000;
	 
	private static Timer	tracker_timer = new Timer( "Tracker Timer", 32);
	
	private TOTorrent				torrent;
	private TimerEvent				current_timer_event;
	private TimerEventPerformer		timer_event_action;
	
	private int					tracker_state 			= TS_INITIALISED;
	private String				tracker_status_str		= "";
	private TRTrackerResponse	last_response			= new TRTrackerResponseImpl(TRTrackerResponse.ST_OFFLINE, TRTrackerClient.REFRESH_MINIMUM_SECS );
	private int					last_update_time_secs;
	private int					current_time_to_wait_secs;
	
	private boolean			stopped;
	private boolean			completed;
	private boolean			complete_reported	= false;
	
	private boolean			update_in_progress	= false;
	
	private long			rd_last_override;
	private boolean			rd_override_use_minimum;
	private int				rd_override_percentage	= 100;

  	private List trackerUrlLists;
     
  	private String lastUsedUrl;
  
  	private String trackerUrlListString;
  
	private String info_hash = "?info_hash=";
	private byte[] peerId;
	private String peer_id = "&peer_id=";
	private String port;
	private String ip_override;
  
	private TrackerClientAnnounceDataProvider 	announce_data_provider;

  
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
  public 
  TRTrackerClientClassicImpl(
  	TOTorrent	_torrent,
  	int 		_port ) 
  	
  	throws TRTrackerClientException
  {
  	torrent		= _torrent;

		//Get the Tracker url
		
	constructTrackerUrlLists( true );     

		//Create our unique peerId
		
	peerId = new byte[20];
	
    byte[] version = Constants.VERSION_ID;
    
	for (int i = 0; i < 8; i++) {
	  peerId[i] = version[i];
    }
    
   String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	for (int i = 8; i < 20; i++) {
	  int pos = (int) ( Math.random() * chars.length());
     peerId[i] = (byte)chars.charAt(pos);
	}
	
	try {
	
	  this.info_hash += URLEncoder.encode(new String(_torrent.getHash(), Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
	  
	  this.peer_id += URLEncoder.encode(new String(peerId, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
	  
	}catch (UnsupportedEncodingException e){
		
		LGLogger.log(componentID, evtLifeCycle,"URL encode fails", e );
	  
	  throw( new TRTrackerClientException( "TRTrackerClient: URL encode fails"));
	  
	}catch( TOTorrentException e ){
	
		LGLogger.log(componentID, evtLifeCycle,"Torrent hash retrieval fails", e );
		
		throw( new TRTrackerClientException( "TRTrackerClient: URL encode fails"));	
	}
	
	this.port = "&port=" + _port;
	   
	timer_event_action =  
		new TimerEventPerformer()
		{
			public void
			perform(
				TimerEvent	this_event )
			{
				int	secs_to_wait = REFRESH_MINIMUM_SECS;
							
				try{
															
					secs_to_wait = requestUpdateSupport();
								
				}finally{
						
					current_time_to_wait_secs	= secs_to_wait;
							
					if ( tracker_state == TS_STOPPED ){
						
						// System.out.println( "\tperform: stopped so no more events");
						
					}else{
						
					
						synchronized( TRTrackerClientClassicImpl.this ){
						
								// it is possible that the current event was being processed
								// when another thread cancelled it and created a further timer
								// event. if this is the case we don't want to go ahead and
								// create another timer as one already exists 
								
							if ( this_event.isCancelled()){
								
								// System.out.println( "\tperform: cancelled so no new event");
								
							}else{
								
								
								secs_to_wait = getAdjustedSecsToWait();
								
								long target_time = System.currentTimeMillis() + (secs_to_wait*1000);
								
								if ( current_timer_event != null && !current_timer_event.isCancelled()){
									
									if ( 	current_timer_event != this_event &&
											current_timer_event.getWhen() < target_time ){
									
											// existing event is earlier then current target, use it
												
										return;
									}
									
									current_timer_event.cancel();
								}
								
								current_timer_event = 
									tracker_timer.addEvent( target_time, this );
							}
						}
					}
				}
			}
		};
			
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client Created using url : " + trackerUrlListString);
  }
	
	protected int
	getAdjustedSecsToWait()
	{
  		int		secs_to_wait = current_time_to_wait_secs;
													
  		if ( rd_override_use_minimum ){
								
	  		secs_to_wait = REFRESH_MINIMUM_SECS;
										
  		}else{
							
	  		secs_to_wait = (secs_to_wait*rd_override_percentage)/100;
									
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
		boolean	use_minimum,
		int		percentage )
	{
		if ( percentage > 100 ){
			
			percentage = 100;
			
		}else if ( percentage <= 0 ){
			
			percentage	= 1;
		}
		
		long	now = System.currentTimeMillis();
		
		if ( now - rd_last_override > OVERRIDE_PERIOD &&
				(	rd_override_use_minimum != use_minimum ||
					rd_override_percentage != percentage )){
		
			synchronized(this){
			
				// System.out.println( "TRTrackerClient::setRefreshDelayOverrides(" + use_minimum + "/" + percentage + ")");
				
				rd_last_override	= now;
				
				rd_override_use_minimum	= use_minimum;
				rd_override_percentage	= percentage;
				
				if ( current_timer_event != null && !current_timer_event.isCancelled()){
					
					long	start 	= current_timer_event.getCreatedTime();
					long	expiry	= current_timer_event.getWhen();
					
					int	secs_to_wait = getAdjustedSecsToWait();
								
					long target_time = start + (secs_to_wait*1000);

					if ( target_time != expiry ){
						
						current_timer_event.cancel();
						
						current_timer_event = 
							tracker_timer.addEvent( 
								start,
								target_time,
								timer_event_action );					
					}			
				}
			}
		}
	}
	
	public synchronized int
	getTimeUntilNextUpdate()
	{
		if ( current_timer_event == null ){
			
			return( REFRESH_MINIMUM_SECS );
		}
				
		int rem = (int)((current_timer_event.getWhen() - System.currentTimeMillis())/1000);
				
		return( rem );
	}

	public int
	getLastUpdateTime()
	{
		return( last_update_time_secs );
	}

	public void
	update(
		boolean		force )
	{
		long time = System.currentTimeMillis() / 1000;
		
		if  ( 	force ||
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
		
		requestUpdate();
	}
	
	protected synchronized void
	requestUpdate()
	{
		if ( current_timer_event != null ){
			
			current_timer_event.cancel();
		}
		
		current_timer_event = 
			tracker_timer.addEvent( 
				System.currentTimeMillis(),
				timer_event_action );
	}
	
	protected int
	requestUpdateSupport()
	{
		boolean	clear_progress = true;
		
		try{
			synchronized( this ){

				if ( update_in_progress ){
					
					clear_progress = false;
					
					return( REFRESH_MINIMUM_SECS );
				}
				
				update_in_progress = true;
			}
	
			last_update_time_secs	= (int)(System.currentTimeMillis()/1000);
			
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
				
			}else if ( tracker_state == TS_INITIALISED ){
								
				response = startSupport();
					
				if ( response.getStatus() == TRTrackerResponse.ST_ONLINE ){
						
					tracker_state = TS_DOWNLOADING;
				}
			}else{
				
				response = updateSupport();
			}
						
			if ( response != null ){

				int	rs = response.getStatus();
				
				if ( rs == TRTrackerResponse.ST_OFFLINE ){
      
					tracker_status_str = MessageText.getString("PeerManager.status.offline"); //set the status to offline       //$NON-NLS-1$
      		      
					String	reason = response.getFailureReason();
      		
					if ( reason != null ){
      			
						tracker_status_str += " (" + reason + ")";		
					}
				}else if ( rs == TRTrackerResponse.ST_REPORTED_ERROR ){

					tracker_status_str = response.getFailureReason();
			
				}else{
	    	       	        	
					tracker_status_str = MessageText.getString("PeerManager.status.ok"); //set the status      //$NON-NLS-1$
				}
				
				last_response = response;
				
				listeners.dispatch( LDT_TRACKER_RESPONSE, response );
				
				return((int) response.getTimeToWait());
			}else{
				
				tracker_status_str = "";
				
				return( REFRESH_MINIMUM_SECS );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			return( REFRESH_MINIMUM_SECS );
			
		}finally{
			
			synchronized( this ){
			
				if ( clear_progress ){
					
					update_in_progress = false;
				}
			}
		}
	}
	
	protected TRTrackerResponse 
  	startSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a start Request");
	
		// System.out.println( "started");
		
		return(update("started"));
  	}

  	protected TRTrackerResponse 
  	completeSupport() 
  	{	
		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a completed Request");
		
		// System.out.println( "complete");
		
		return(update("completed"));
  	}

  	protected TRTrackerResponse 
  	stopSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a stopped Request");

		// System.out.println( "stop");		
	
		return( update("stopped"));
  	}

  	protected TRTrackerResponse 
  	updateSupport() 
  	{
		LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending an update Request");
	
		// System.out.println( "update");
		
		return update("");
  	}
  
  private TRTrackerResponse 
  update(String evt) 
  {
	String	last_failure_reason = "";
		
	for (int i = 0 ; i < trackerUrlLists.size() ; i++) {
	  	
		List urls = (List) trackerUrlLists.get(i);
		
		for (int j = 0 ; j < urls.size() ; j++) {
			
		  String url = (String) urls.get(j);
		  
		  lastUsedUrl = url;
		   
		  String	this_url_string = null;
		  
		  try{
		  
			  this_url_string = constructUrl(evt,url);
			  					  	  
			  URL reqUrl = new URL(this_url_string);
			  
			  TRTrackerResponse resp = decodeTrackerResponse( updateOld(reqUrl,evt));
			  
		      if ( resp.getStatus() == TRTrackerResponse.ST_ONLINE ){
					
	            	urls.remove(j);
	            	
	            	urls.add(0,url);
	            	
	            	trackerUrlLists.remove(i);
	            	
	            	trackerUrlLists.add(0,urls);            
	            
	            	informURLChange( this_url_string, false );
	            	
	            		//and return the result
	            		
	            	return( resp );
			  }else{
			  	
			  	String	this_reason = resp.getFailureReason();
			  	
			  	if ( this_reason != null ){
			  		
			  		last_failure_reason = this_reason;	
			  	}
			  }
		  }catch( MalformedURLException e ){
		  	
		  	e.printStackTrace();
		  	
		  	last_failure_reason = "malformed URL '" + this_url_string + "'";
		  	
		  }catch( Exception e ){
		  	
		  	//e.printStackTrace();
		  	
		  	last_failure_reason = e.getMessage();
		  }
		}
	  } 
	   
	  return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, REFRESH_MINIMUM_SECS, last_failure_reason ));
  }

 	private byte[] 
 	updateOld(
 		URL 		reqUrl,
		String 		evt)
  
  		throws Exception
	{
 		
 			// loop to possibly retry update on SSL certificate install
 		
 		for (int i=0;i<2;i++){	
 		
	  		String	failure_reason = null;
	  	
			try{      
		  		LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, "Tracker Client is Requesting : " + reqUrl);
		  
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
		  		
		  		ByteArrayOutputStream message = new ByteArrayOutputStream();
		  	  
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
						
				  		int content_length = con.getContentLength();
				  		
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
				  			  		
				  		LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, "Tracker Client has received : " + message);
				  
				
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
  exceptionToString(
  	Exception 	e )
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
	request.append(info_hash);
	request.append(peer_id);
	request.append(port);
	request.append("&uploaded=").append(announce_data_provider.getTotalSent());
	request.append("&downloaded=").append(announce_data_provider.getTotalReceived());
	request.append("&left=").append(announce_data_provider.getRemaining());
	if (evt.length() != 0)
	  request.append("&event=").append(evt);
	if (evt.equals("stopped")){
	
	  request.append("&num_peers=0");
	}else{
	
	  request.append("&num_peers=50");
	}
	
	String ip = ip_override==null?COConfigurationManager.getStringParameter("Override Ip", ""):ip_override;
	
	if (ip.length() != 0){
	
	  request.append("&ip=").append(ip);
	}
	
	return request.toString();
  }

  	public byte[] 
  	getPeerId() 
  	{
  		return peerId;
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
	try {
	  trackerUrlLists = new ArrayList();
	  trackerUrlListString = "";
	  
	  	//This entry is present on multi-tracker torrents
	  
	  TOTorrentAnnounceURLSet[]	announce_sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
	       
	  if( announce_sets.length == 0 ) {
	  	
				//If not present, we use the default specification
				
			String url = torrent.getAnnounceURL().toString();
			       
			trackerUrlListString = "{ " + url + " }"; 
			
				//We then contruct a list of one element, containing this url, and put this list
				//into the list of lists of urls.
				
			List list = new ArrayList();
			
			list.add(url);
			
			trackerUrlLists.add(list);
	  }else{
	  	
		String separatorList = "";
		
			//Ok we have a multi-tracker torrent
		
		for(int i = 0 ; i < announce_sets.length ; i++){
			
		  	//Each list contains a list of urls
		  
			URL[]	urls = announce_sets[i].getAnnounceURLs();
			
		 	List stringUrls = new ArrayList(urls.length);
		 	
		  	String separatorUrl = "";
		  	
		  	trackerUrlListString += separatorList + " { ";
		  	
		  	for(int j = 0 ; j < urls.length; j++){
		  		
				//System.out.println(urls.get(j).getClass());
				      
				String url = urls[j].toString();
				            
				trackerUrlListString += separatorUrl + url;
		
					//Shuffle
					
				int pos = shuffle?(int)(Math.random() *  stringUrls.size()):j;
				
				stringUrls.add(pos,url);
			
				separatorUrl = ", ";
			
				lastUsedUrl = url;
		  	}
		  	
		  	separatorList = " ; ";
		  	
		  	trackerUrlListString += " } ";
		  	         
		  		//Add this list to the list
		  		
		  	trackerUrlLists.add(stringUrls);
			}
	  	}      
	} catch(Exception e) {
	  e.printStackTrace();
	}
  }
  
  	protected TRTrackerResponse
  	decodeTrackerResponse(
  		byte[]		data )
  	{
  		String	failure_reason;
  		
  		if ( data == null ){
  			
  			failure_reason = "no response";
  			
  		}else{
  		
	 		try{
					   //parse the metadata
					   
				Map metaData = BDecoder.decode(data); //$NON-NLS-1$
	
					// decode could fail if the tracker's returned, say, an HTTP response
					// indicating server overload
					
				if ( metaData == null ){
					
					String	trace_data = new String(data);
					
					if ( trace_data.length() > 20 ){
						
						trace_data = trace_data.substring(0,20) + "...";
					}
					
					failure_reason = "invalid reply:" + trace_data;
				
				}else{
					
					long	time_to_wait;
										
					boolean		tracker_ok = false;
					
					try{
						// * In fact we use 2/3 of what tracker is asking us to wait, in order not to be considered as timed-out by it.
						
						time_to_wait = (2 * ((Long) metaData.get("interval")).intValue()) / 3; //$NON-NLS-1$
										
				   	}catch( Exception e ){
				   	
						byte[]	failure_reason_bytes = (byte[]) metaData.get("failure reason");
						
						if ( failure_reason_bytes == null ){
							
							System.out.println("Problems with Tracker, will retry in 1 minute");
													
							time_to_wait = REFRESH_MINIMUM_SECS;
	
							return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, time_to_wait ));
	
						}else{
							
							failure_reason = new String( failure_reason_bytes, Constants.DEFAULT_ENCODING);
	
							time_to_wait = REFRESH_ERROR_SECS;
						
							return( new TRTrackerResponseImpl( TRTrackerResponse.ST_REPORTED_ERROR, time_to_wait, failure_reason ));
						}
				 	}
						
						//build the list of peers
						
					List meta_peers = (List) metaData.get("peers"); //$NON-NLS-1$
					 					 
					List valid_meta_peers = new ArrayList();
					
						//for every peer
					int peers_length = meta_peers.size();
						
					for (int i = 0; i < peers_length; i++) {
						 	
						Map peer = (Map) meta_peers.get(i);
						   
						  //build a dictionary object
						Object s_peerid=peer.get("peer id"); //$NON-NLS-1$
						Object s_ip=peer.get("ip"); //$NON-NLS-1$
						Object s_port=peer.get("port"); //$NON-NLS-1$
												
							// Assert that all ip and port are available
						
						if ( s_ip != null && s_port != null ){
				
								//get the peer ip address
							
							String ip = new String((byte[]) s_ip, Constants.DEFAULT_ENCODING); 
							
								//get the peer port number
							
							int port = ((Long) s_port).intValue(); 
							
							byte[] peerId;
							
							// extension - if peer id is missing then the tracker isn't sending
							// peer ids to save on bandwidth. However, we need something "unique" to 
							// work on internally so make an ID up from the ip and port
							
							if ( s_peerid == null ){
								
								peerId = new byte[20];
								
									// unique initial two bytes to identify this as fake (See Identification.java)
								
								peerId[0] = '[';
								peerId[1] = ']';
								
								byte[]	ip_bytes 	= (byte[])s_ip;
								int		ip_len		= ip_bytes.length;
								
								if ( ip_len > 18 ){
									
									ip_len = 18;
								}
								
								System.arraycopy( ip_bytes, 0, peerId, 2, ip_len );
																
								int	port_copy = port;
								
								for (int j=2+ip_len;j<20;j++){
									
									peerId[j] = (byte)(port_copy&0xff);
									
									port_copy >>= 8;
								}
								
								// System.out.println("generated peer id" + new String(peerId) + "/" + ByteFormatter.nicePrint( peerId, true ));
							}else{
							
								peerId = (byte[])s_peerid ; 
							}
							
							valid_meta_peers.add(new TRTrackerResponsePeerImpl( peerId, ip, port ));
							
						} 
					} 
					
					TRTrackerResponsePeer[] peers=new TRTrackerResponsePeer[valid_meta_peers.size()];
					peers=(TRTrackerResponsePeer[]) valid_meta_peers.toArray(peers);
					
					TRTrackerResponseImpl resp = new TRTrackerResponseImpl( TRTrackerResponse.ST_ONLINE, time_to_wait, peers );
					
					resp.setExtensions((Map)metaData.get( "extensions" ));
					
					return( resp );  

				}			
			}catch( Exception e ){
				
				e.printStackTrace();
				
				failure_reason = "error: " + e.getMessage();
			}
  		}

		return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, REFRESH_MINIMUM_SECS, failure_reason ));
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
		TRTrackerClientFactoryImpl.destroy( this );
	}
}