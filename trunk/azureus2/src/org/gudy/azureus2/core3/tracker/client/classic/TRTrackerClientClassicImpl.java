/*
 * Created on 04-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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

  public String start() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a start Request");
	return update("started");
  }

  public String completed() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a completed Request");
	return update("completed");
  }

  public String stop() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending a stopped Request");
	int oldTimeout = timeout;
	if(timeout > 5000 || timeout == 0)
	  timeout = 5000;
	String response = update("stopped");
	timeout = oldTimeout;
	return response;
  }

  public String update() {
	LGLogger.log(componentID, evtLifeCycle, LGLogger.INFORMATION, "Tracker Client is sending an update Request");
	return update("");
  }
  
  private String update(String evt) {
	String result = null;
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
			return result;
		  }
		}
	  }  
	} catch(MalformedURLException e) {
	  e.printStackTrace();
	}
	return null;
  }

  private String updateOld(URL reqUrl,String evt) {
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
		return new String(message.toByteArray(), Constants.BYTE_ENCODING);
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
}