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
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

/**
 * 
 * This class handles communication with the tracker, but doesn't analyse responses.
 * 
 * @author Olivier
 *
 */
public class 
TRTrackerClientClassicImpl
	implements TRTrackerClient 
{
	private TOTorrent		torrent;
	
  private int timeout = 20000; // default timeout 20 seconds
  private Thread httpConnecter = null;
  private boolean httpConnected = false;

  private List trackerUrlLists;
    
  private int listIndex;
  private int firstIndexUsed;
  private int inListIndex;
  
  private String lastUsedUrl;
  
  private String trackerUrlListString;
  
  //private String trackerUrl;
  private String info_hash = "?info_hash=";
  private byte[] peerId;
  private String peer_id = "&peer_id=";
  private String port;

  //  private long uploaded;
  //  private long downloaded;
  //  private long remaining;

  private PEPeerManager manager;

  public final static int componentID = 2;
  public final static int evtLifeCycle = 0;
  public final static int evtFullTrace = 1;
  public final static int evtErrors = 2;

  private static final byte[] azureus = "Azureus".getBytes();

  public 
  TRTrackerClientClassicImpl(
  	TOTorrent	_torrent,
  	int 		_port ) 
  	
  	throws TRTrackerClientException
  {
  	torrent		= _torrent;

	//Get the Tracker url
	constructTrackerUrlLists();     

	listIndex = 0;
	inListIndex = 0;

	//Create a peerId
	peerId = new byte[20];
	for (int i = 12; i < 20; i++) {
	  peerId[i] = (byte) (Math.random() * 254);
	}
	for (int i = 5; i < 12; i++) {
	  peerId[i] = azureus[i - 5];
	}
	for (int i = 4; i >= 0; i--) {
	  peerId[i] = (byte) 0;
	}

	try {
	  //1.4 Version
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
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client Created using url : " + trackerUrlListString);
  }

  public TRTrackerResponse start() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a start Request");
	return update("started");
  }

  public TRTrackerResponse completed() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a completed Request");
	return update("completed");
  }

  public TRTrackerResponse stop() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a stopped Request");
	int oldTimeout = timeout;
	if(timeout > 5000 || timeout == 0)
	  timeout = 5000;
	TRTrackerResponse response = update("stopped");
	timeout = oldTimeout;
	return response;
  }

  public TRTrackerResponse update() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending an update Request");
	return update("");
  }
  
  private TRTrackerResponse 
  update(String evt) 
  {
	byte[] result = null;
	boolean failed = false;
	this.firstIndexUsed = this.listIndex;
	try {
	  for(int i = 0 ; i < trackerUrlLists.size() ; i++) {
		List urls = (List) trackerUrlLists.get(i);
		for(int j = 0 ; j < urls.size() ; j++) {
		  String url = (String) urls.get(j);
		  lastUsedUrl = url; 
		  URL reqUrl = new URL(constructUrl(evt,url));
		  result = updateOld(reqUrl,evt);
		  //We have a result, move everything in top of list
		  if(result != null && !result.equals("")) {
			urls.remove(j);
			urls.add(0,url);
			trackerUrlLists.remove(i);
			trackerUrlLists.add(0,urls);            
			//and return the result
			return(decodeTrackerResponse(result));
		  }
		}
	  }  
	} catch(MalformedURLException e) {
	  e.printStackTrace();
	}
	return( decodeTrackerResponse( null ));
  }

  private byte[] 
  updateOld(URL reqUrl,String evt) {
	try {      
	  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, "Tracker Client is Requesting : " + reqUrl);
	  final HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
	  final ByteArrayOutputStream message = new ByteArrayOutputStream();

	  if(httpConnecter != null && httpConnecter.isAlive() && !httpConnecter.isInterrupted()) {
		httpConnecter.interrupt();
	  }
	  httpConnected = false;
	  httpConnecter = new Thread("Tracker HTTP Connect") {
		public void run() {
		  try {
			con.connect();
			httpConnected = true;
		  } catch (Exception ignore) {
		  }
		}
	  };
	  httpConnecter.setDaemon(true);
	  httpConnecter.setPriority(Thread.MIN_PRIORITY);
	  httpConnecter.start();

	  try {
		httpConnecter.join(timeout);
	  } catch (InterruptedException ignore) {
	   // if somebody interrupts us he knows what he is doing
	  }
	  if (httpConnecter.isAlive()) {
		httpConnecter.interrupt();
	  }
	  httpConnecter = null;
	  if(httpConnected) {
		InputStream is = null;
		try {
		  is = con.getInputStream();
		  //      int length = con.getContentLength();
		  //      System.out.println(length);
		  byte[] data = new byte[1024];
		  int nbRead = 0;
		  while (nbRead >= 0) {
			try {
			  nbRead = is.read(data);
			  if (nbRead >= 0)
				message.write(data, 0, nbRead);
			  Thread.sleep(20);
			} catch (Exception e) {
			  LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Exception while Requesting Tracker : " + e);
			  LGLogger.log(componentID, evtFullTrace, LGLogger.ERROR, "Message Received was : " + message);
			  nbRead = -1;
			}
		  }
		  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, "Tracker Client has received : " + message);
		} catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
		} catch (Exception ignore) {
		} finally {
		  if (is != null) {
			try {
			  is.close();
			} catch (Exception e) {
			}
			is = null;
		  }
		}
		return( message.toByteArray());
	  }
	} catch (Exception e) {
	  LGLogger.log(componentID, evtErrors, LGLogger.ERROR, "Exception while creating the Tracker Request : " + e);
	}
	return null;
  }
  
  public String constructUrl(String evt,String url) {
	StringBuffer request = new StringBuffer(url);
	request.append(info_hash);
	request.append(peer_id);
	request.append(port);
	request.append("&uploaded=").append(manager.uploaded());
	request.append("&downloaded=").append(manager.downloaded());
	request.append("&left=").append(manager.getRemaining());
	if (evt.length() != 0)
	  request.append("&event=").append(evt);
	if (evt.equals("stopped"))
	  request.append("&num_peers=0");
	else
	  request.append("&num_peers=50");
	String ip = COConfigurationManager.getStringParameter("Override Ip", "");
	if (ip.length() != 0)
	  request.append("&ip=").append(ip);

	return request.toString();
  }

  public byte[] getPeerId() {
	return peerId;
  }

  public void setManager(PEPeerManager manager) {
	this.manager = manager;
  }

	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
  public String getTrackerUrl() {
	return lastUsedUrl;
  }

  /**
   * @param timeout maximum time in ms to wait for con.connect()
   */
  public void setTimeout(int timeout) {
	if(timeout >= 0)
	  this.timeout = timeout;
  }

  /**
   * @param trackerUrl
   */
  public void setTrackerUrl(String trackerUrl) {
	List list = new ArrayList(1);
	list.add(trackerUrl.replaceAll(" ", ""));
	trackerUrlLists.add(list);
  }
  
  private void 
  constructTrackerUrlLists()
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
					
				int pos = (int)(Math.random() *  stringUrls.size());
				
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
  		if ( data != null ){
  			
	 		try{
					   //parse the metadata
					   
				Map metaData = BDecoder.decode(data); //$NON-NLS-1$
	
					// decode could fail if the tracker's returned, say, an HTTP response
					// indicating server overload
					
				if ( metaData != null ){
				
					long	time_to_wait;
					String	failure_reason = null;
					
					boolean		tracker_ok = false;
					
					try{
						// * In fact we use 2/3 of what tracker is asking us to wait, in order not to be considered as timed-out by it.
						
						time_to_wait = (2 * ((Long) metaData.get("interval")).intValue()) / 3; //$NON-NLS-1$
										
				   	}catch( Exception e ){
				   	
						byte[]	failure_reason_bytes = (byte[]) metaData.get("failure reason");
						
						if ( failure_reason_bytes == null ){
							
							System.out.println("Problems with Tracker, will retry in 1 minute");
													
							time_to_wait = 60;
	
							return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, time_to_wait ));
	
						}else{
							
							failure_reason = new String( failure_reason_bytes, Constants.DEFAULT_ENCODING);
	
							time_to_wait = 120;
						
							return( new TRTrackerResponseImpl( TRTrackerResponse.ST_REPORTED_ERROR, time_to_wait, failure_reason ));
						}
				 	}
						
						//build the list of peers
						
					List meta_peers = (List) metaData.get("peers"); //$NON-NLS-1$
					 
					TRTrackerResponsePeer[] peers = new TRTrackerResponsePeer[ meta_peers.size()];
					 
						//for every peer
						
					for (int i = 0; i < peers.length; i++) {
						 	
						Map peer = (Map) meta_peers.get(i);
						   
						  //build a dictionary object
						 	         
						byte[] peerId = (byte[]) peer.get("peer id"); //$NON-NLS-1$ //$NON-NLS-2$
						   
						 	//get the peer id
						   	
						String ip = new String((byte[]) peer.get("ip"), Constants.DEFAULT_ENCODING); //$NON-NLS-1$ //$NON-NLS-2$
						   
						  	//get the peer ip address
						   	
						int port = ((Long) peer.get("port")).intValue(); //$NON-NLS-1$
						   
						   	//get the peer port number
						
						peers[i] = new TRTrackerResponsePeerImpl( peerId, ip, port );
					} 
					
					return( new TRTrackerResponseImpl( TRTrackerResponse.ST_ONLINE, time_to_wait, peers ));  

				}			
			}catch( Exception e ){
				
				e.printStackTrace();
			}
  		}

		return( new TRTrackerResponseImpl( TRTrackerResponse.ST_OFFLINE, 60 ));
  	}
}