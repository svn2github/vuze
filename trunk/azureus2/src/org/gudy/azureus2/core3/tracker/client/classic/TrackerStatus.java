/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.security.*;

/**
 * @author Olivier
 * 
 */
 
/** One TrackerStatus object handles scrape functionality for all torrents
 * on one tracker.
 */
public class TrackerStatus {
  private final static int FAULTY_SCRAPE_RETRY_INTERVAL = 60 * 10 * 1000;
  private String scrapeURL = null;
 
  /** key = Torrent hash.  values = TRTrackerScraperResponseImpl */
  private HashMap 					hashes;
  /** only needed to notify listeners */ 
  private TRTrackerScraperImpl		scraper;
  
  private boolean bSingleHashScrapes = false;
    

  public TrackerStatus(TRTrackerScraperImpl	_scraper, String trackerUrl) {    	
  	scraper		= _scraper;
    
    hashes = new HashMap();
    
    try {
      trackerUrl = trackerUrl.replaceAll(" ", "");
      int position = trackerUrl.lastIndexOf('/');
      if(	position >= 0 &&
      		trackerUrl.length() >= position+9 && 
      		trackerUrl.substring(position+1,position+9).equals("announce")) {

        this.scrapeURL = trackerUrl.substring(0,position+1) + "scrape" + trackerUrl.substring(position+9);
        // System.out.println( "url = " + trackerUrl + ", scrape =" + scrapeURL );
        
      }else if ( trackerUrl.toLowerCase().startsWith("udp:")){
      		// UDP scrapes aren't based on URL rewriting, just carry on
      	
      	scrapeURL = trackerUrl;
      	
       } else {
        LGLogger.log(0, 0, LGLogger.INFORMATION,
                     "can't scrape using '" + trackerUrl + "' as it doesn't end in '/announce'");		
       }
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }

  protected TRTrackerScraperResponseImpl getHashData(HashWrapper hash) {
  	synchronized( hashes ){
  		return (TRTrackerScraperResponseImpl) hashes.get(hash.getHash());
  	}
  }

  protected TRTrackerScraperResponseImpl getHashData(byte[] hash) {
  	synchronized( hashes ){
  		return (TRTrackerScraperResponseImpl) hashes.get(hash);
  	}
  }


  protected synchronized void updateSingleHash(HashWrapper hash, boolean force) {
    updateSingleHash(hash.getHash(), force, true);
  }

  protected synchronized void updateSingleHash(byte[] hash, boolean force) {
    updateSingleHash(hash, force, true);
  }

  protected synchronized void updateSingleHash(byte[] hash, boolean force, boolean async) {      
    //System.out.println("updateSingleHash:" + force + " " + scrapeURL + " : " + ByteFormatter.nicePrint(hash, true));
    if (scrapeURL == null)  {
      return;
    }
    
    ArrayList responsesToUpdate = new ArrayList();

    TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)hashes.get(hash);
    if (response == null) {
      response = addHash(hash);
    }

    long lMainNextScrapeStartTime = response.getNextScrapeStartTime();
    
    if (!SystemTime.isErrorLast1min() && !force && lMainNextScrapeStartTime >= SystemTime.getCurrentTime()) {
      return;
    }
    response.setStatus(TRTrackerScraperResponse.ST_SCRAPING);
    response.setStatusString(MessageText.getString("Scrape.status.scraping"));

    responsesToUpdate.add(response);
    
    // TODO: Go through hashes and pick out other scrapes that are "close to"
    //       wanting a new scrape.
    if (!bSingleHashScrapes) {
      Iterator iterHashes = hashes.values().iterator();
      while( iterHashes.hasNext() ) {
        TRTrackerScraperResponseImpl r = (TRTrackerScraperResponseImpl)iterHashes.next();
        if (!r.getHash().equals(hash)) {
          long lTimeDiff = Math.abs(lMainNextScrapeStartTime - r.getNextScrapeStartTime());
          if (lTimeDiff <= 30000 && r.getStatus() != TRTrackerScraperResponse.ST_SCRAPING) {
            r.setStatus(TRTrackerScraperResponse.ST_SCRAPING);
            r.setStatusString(MessageText.getString("Scrape.status.scraping"));
            responsesToUpdate.add(r);
          }
        }
      }
    }
    
    new ThreadedScrapeRunner(responsesToUpdate,  force, async);
  }
  
  /** Does the scrape and decoding asynchronously.
    *
    * TODO: Allow handling of multiple TRTrackerScraperResponseImpl objects
    *       on one URL
    */
  private class ThreadedScrapeRunner extends Thread {
    boolean force;
    ArrayList responses;

    public ThreadedScrapeRunner(ArrayList _responses, boolean _force, boolean async) {
      super("ThreadedScrapeRunner");
      force = _force;
      responses = _responses;

      if (async) {
        setDaemon(true);
        start();
      } else {
        run();
      }
    }

    public void run() {
      if (scrapeURL == null)  {
        return;
      }
            
      try {
        String info_hash = "";
        for (int i = 0; i < responses.size(); i++) {
          byte[] hash = ((TRTrackerScraperResponseImpl)responses.get(i)).getHash();
          info_hash += ((i > 0) ? "&" : "?") + "info_hash=";
          info_hash += URLEncoder.encode(new String(hash, Constants.BYTE_ENCODING), 
                                         Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
        }

        URL reqUrl = new URL(scrapeURL + info_hash);
        
        LGLogger.log(0,0,LGLogger.SENT,"Accessing scrape interface using url : " + reqUrl);
   
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        
        long scrapeStartTime = SystemTime.getCurrentTime();
        
        if ( reqUrl.getProtocol().equalsIgnoreCase( "udp" )){
          // TODO: support multi hash scrapes on UDP
        	scrapeUDP( reqUrl, message, ((TRTrackerScraperResponseImpl)responses.get(0)).getHash());
        	bSingleHashScrapes = true;
        }else{
        	scrapeHTTP( reqUrl, message );
        }
          
              
        LGLogger.log(0, 0, LGLogger.RECEIVED, "Response from scrape interface : " + message);
        
        Map map = BDecoder.decode(message.toByteArray());
        
        Map mapFiles = (Map) map.get("files");
        if (!bSingleHashScrapes && responses.size() > 1 && mapFiles.size() == 1) {
          bSingleHashScrapes = true;
          LGLogger.log(scrapeURL + " only returned " + mapFiles.size() + " hash scrape(s), but we asked for " + responses.size());
        }
        
        for (int i = 0; i < responses.size(); i++) {
          TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)responses.get(i);
          //retrieve the scrape data for the relevent infohash
          Map scrapeMap = (Map)mapFiles.get(new String(response.getHash(), Constants.BYTE_ENCODING));
        
          if ( scrapeMap == null ){
            // some trackers that return only 1 hash return a random one!
            if (responses.size() == 1 || mapFiles.size() != 1) {
              response.setNextScrapeStartTime(SystemTime.getCurrentTime() + 
                                              FAULTY_SCRAPE_RETRY_INTERVAL);
              response.setStatus(TRTrackerScraperResponse.ST_ERROR);
              response.setStatusString(MessageText.getString("Scrape.status.error") + 
                                       MessageText.getString("Scrape.status.error.nohash"));
            } else {
              // This tracker doesn't support multiple hash requests.
              // revert status to what it was
              response.revertStatus();
              if (response.getStatus() == TRTrackerScraperResponse.ST_SCRAPING) {
                System.out.println("Hash " + ByteFormatter.nicePrint(response.getHash(), true) + " mysteriously reverted to ST_SCRAPING!");
                response.setStatus(TRTrackerScraperResponse.ST_ONLINE);
              }
              // if this was the first scrape request in the list, TrackerChecker
              // will attempt to scrape again because we didn't reset the 
              // nextscrapestarttime.  But the next time, bSingleHashScrapes
              // will be false, and only 1 has will be requested, so there
              // will be infinite looping
            }
          	// System.out.println("scrape: hash missing from reply");
          } else {
    	      //retrieve values
    	      int seeds = ((Long)scrapeMap.get("complete")).intValue();
    	      int peers = ((Long)scrapeMap.get("incomplete")).intValue();
              
            //make sure we dont use invalid replies
            if ( seeds < 0 || peers < 0 ) {
            	response.setNextScrapeStartTime(SystemTime.getCurrentTime() + FAULTY_SCRAPE_RETRY_INTERVAL);
            	response.setStatus(TRTrackerScraperResponse.ST_ERROR);
              response.setStatusString(MessageText.getString("Scrape.status.error") + MessageText.getString("Scrape.status.error.invalid"));
              scraper.scrapeReceived( response );
              return;
            }

    	      // decode additional flags - see http://anime-xtreme.com/tracker/blah.txt for example
    	      int scrapeInterval = 10 * 60;
    	      Map mapFlags = (Map) map.get("flags");
    	      if (mapFlags != null) {
              scrapeInterval = ((Long)mapFlags.get("min_request_interval")).intValue();
  
              if (scrapeInterval < 10*59) scrapeInterval = 10*59;
              if (scrapeInterval > 60*60) scrapeInterval = 60*60;
    
              //Debug.out("scrape min_request_interval = " +scrapeInterval);
    	      }
  
            long nextScrapeTime = SystemTime.getCurrentTime() + (scrapeInterval * 1000);
            response.setNextScrapeStartTime(nextScrapeTime);

    	      //create the response
    	      response.setScrapeStartTime(scrapeStartTime);
    	      response.seeds = seeds;
    	      response.peers = peers; 
            response.setStatus(TRTrackerScraperResponse.ST_ONLINE);

            String s = MessageText.getString("Scrape.status.ok");
            if (!s.endsWith(".")) {
              s += ".  ";
            } else if (!s.equals("")) {
              s += "  ";
            }
            String[] params = { DisplayFormatters.formatTime(response.getNextScrapeStartTime()) };
            response.setStatusString(s + 
                                     MessageText.getString("Scrape.status.nextScrapeAt", 
                                                           params));
            //notifiy listeners
            scraper.scrapeReceived( response );
          }
        } // for responses
  
      } catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
        for (int i = 0; i < responses.size(); i++) {
          TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)responses.get(i);
          response.setNextScrapeStartTime(SystemTime.getCurrentTime() + 
                                          FAULTY_SCRAPE_RETRY_INTERVAL);
          response.setStatus(TRTrackerScraperResponse.ST_ERROR);
          //notifiy listeners
          scraper.scrapeReceived( response );
        }
      } catch (Exception e) {
        LGLogger.log(LGLogger.ERROR, 
  									"Response from scrape interface " + scrapeURL + " : " + e);
   
        for (int i = 0; i < responses.size(); i++) {
          TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)responses.get(i);
          response.setNextScrapeStartTime(SystemTime.getCurrentTime() + 
                                          FAULTY_SCRAPE_RETRY_INTERVAL);
          response.setStatus(TRTrackerScraperResponse.ST_ERROR);
          String s = MessageText.getString("Scrape.status.error") + e.getMessage();
          if (!s.endsWith(".")) {
            s += ".  ";
          } else if (!s.equals("")) {
            s += "  ";
          }
          String[] params = { DisplayFormatters.formatTime(response.getNextScrapeStartTime()) };
          response.setStatusString(s + 
                                   MessageText.getString("Scrape.status.nextScrapeAt", 
                                                         params));
          //notifiy listeners
          scraper.scrapeReceived( response );
        }
      }
    }
  }
  
  protected void scrapeHTTP(URL reqUrl, ByteArrayOutputStream message)
  	throws IOException
  {
  	TRTrackerClientUtilsImpl.checkForBlacklistedURLs( reqUrl );
  	
    reqUrl = TRTrackerClientUtilsImpl.adjustURLForHosting( reqUrl );

  	//System.out.println( "trying " + scrape.toString());
  	
  	InputStream is = null;
  	
  	try{
	  	HttpURLConnection con = null;

	  	if ( reqUrl.getProtocol().equalsIgnoreCase("https")){
	  		// see ConfigurationChecker for SSL client defaults
	  		HttpsURLConnection ssl_con = (HttpsURLConnection)reqUrl.openConnection();
	  		
	  		// allow for certs that contain IP addresses rather than dns names
	  		ssl_con.setHostnameVerifier(
	  				new HostnameVerifier() {
	  					public boolean verify(String host, SSLSession session) {
	  						return( true );
	  					}
	  				});
	  		
	  		con = ssl_con;
	  		
	  	} else {
	  		con = (HttpURLConnection) reqUrl.openConnection();
	  	}

	  	con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);
    	// some trackers support gzip encoding of replies
	    con.addRequestProperty("Accept-Encoding","gzip");
	  	con.connect();

	  	is = con.getInputStream();
	
	  	String encoding = con.getHeaderField( "content-encoding");
	  	
	  	boolean	gzip = encoding != null && encoding.equalsIgnoreCase("gzip");
	  	
	  	// System.out.println( "encoding = " + encoding );
	  	
	  	if ( gzip ){
	  		is = new GZIPInputStream( is );
	  	}
	  	
	  	byte[]	data = new byte[1024];
	  	
	  	int nbRead = 0;
	  	
	  	while (nbRead >= 0) {
	  		try {
	  			nbRead = is.read(data);
	  			if (nbRead >= 0)
	  				message.write(data, 0, nbRead);
	  			Thread.sleep(20);
	  		} catch (Exception e) {
	  			// nbRead = -1;
	  			// message = null;
	  			// e.printStackTrace();
	  			return;
	  		}
	  	}
	  } finally {
	  	if (is != null) {
        try {
	  		  is.close();
  	  	} catch (IOException e1) { }
  	  }
	  }
  }
  
  protected void
  scrapeUDP(
  	URL						reqUrl,
	ByteArrayOutputStream	message,
	byte[]					hash )
  
  		throws Exception
  {
	reqUrl = TRTrackerClientUtilsImpl.adjustURLForHosting( reqUrl );

	PasswordAuthentication	auth = null;
			
	if ( reqUrl.getQuery().toLowerCase().indexOf("auth") != -1 ){
				
		auth = SESecurityManager.getPasswordAuthentication( "UDP Tracker", reqUrl );
	}		

	int port = COConfigurationManager.getIntParameter("TCP.Listen.Port", 6881);
	
	PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( port );
	
	InetSocketAddress destination = new InetSocketAddress(reqUrl.getHost(),reqUrl.getPort()==-1?80:reqUrl.getPort());
	
	for (int retry_loop=0;retry_loop<PRUDPPacket.DEFAULT_RETRY_COUNT;retry_loop++){
	
		try{
			PRUDPPacket connect_request = new PRUDPPacketRequestConnect();
			
			PRUDPPacket reply = handler.sendAndReceive( auth, connect_request, destination );
			
			if ( reply.getAction() == PRUDPPacket.ACT_REPLY_CONNECT ){
				
				PRUDPPacketReplyConnect connect_reply = (PRUDPPacketReplyConnect)reply;
				
				long	my_connection = connect_reply.getConnectionId();
				
				PRUDPPacketRequestScrape scrape_request = new PRUDPPacketRequestScrape( my_connection, hash );
								
				reply = handler.sendAndReceive( auth, scrape_request, destination );
				
				if ( reply.getAction() == PRUDPPacket.ACT_REPLY_SCRAPE ){
					
 					if ( auth != null ){
 						
 						SESecurityManager.setPasswordAuthenticationOutcome( TRTrackerClientClassicImpl.UDP_REALM, reqUrl, true );
 					}

					if ( PRUDPPacket.VERSION == 1 ){
						PRUDPPacketReplyScrape	scrape_reply = (PRUDPPacketReplyScrape)reply;
						
						Map	map = new HashMap();
						
						/*
						int	interval = scrape_reply.getInterval();
						
						if ( interval != 0 ){
							
							map.put( "interval", new Long(interval ));
						}
						*/
						
						byte[][]	hashes 		= scrape_reply.getHashes();
						int[]		complete 	= scrape_reply.getComplete();
						int[]		downloaded 	= scrape_reply.getComplete();
						int[]		incomplete 	= scrape_reply.getComplete();
						
						Map	files = new ByteEncodedKeyHashMap();
						
						map.put( "files", files );
						
						for (int i=0;i<hashes.length;i++){
							
							Map	file = new HashMap();
							
							byte[]	resp_hash = hashes[i];
							
							// System.out.println("got hash:" + ByteFormatter.nicePrint( resp_hash, true ));
						
							files.put( new String(resp_hash, Constants.BYTE_ENCODING), file );
							
							file.put( "complete", new Long(complete[i]));
							file.put( "downloaded", new Long(downloaded[i]));
							file.put( "incomplete", new Long(incomplete[i]));
						}
						
						byte[] data = BEncoder.encode( map );
						
						message.write( data );
						
						return;
					}else{
						PRUDPPacketReplyScrape2	scrape_reply = (PRUDPPacketReplyScrape2)reply;
						
						Map	map = new HashMap();
						
						/*
						int	interval = scrape_reply.getInterval();
						
						if ( interval != 0 ){
							
							map.put( "interval", new Long(interval ));
						}
						*/
						
						int[]		complete 	= scrape_reply.getComplete();
						int[]		downloaded 	= scrape_reply.getComplete();
						int[]		incomplete 	= scrape_reply.getComplete();
						
						Map	files = new ByteEncodedKeyHashMap();
						
						map.put( "files", files );
													
						Map	file = new HashMap();
							
						byte[]	resp_hash = hash;
						
						// System.out.println("got hash:" + ByteFormatter.nicePrint( resp_hash, true ));
					
						files.put( new String(resp_hash, Constants.BYTE_ENCODING), file );
						
						file.put( "complete", new Long(complete[0]));
						file.put( "downloaded", new Long(downloaded[0]));
						file.put( "incomplete", new Long(incomplete[0]));
						
						byte[] data = BEncoder.encode( map );
						
						message.write( data );
						
						return;
					}
				}else{
					
					LGLogger.log(LGLogger.ERROR, 
									"Response from scrape interface : " +
										((PRUDPPacketReplyError)reply).getMessage());
				}
			}else{
				
				LGLogger.log(LGLogger.ERROR, 
						"Response from scrape interface : " +
						((PRUDPPacketReplyError)reply).getMessage());
			}
			
			if ( auth != null ){
						
				SESecurityManager.setPasswordAuthenticationOutcome( TRTrackerClientClassicImpl.UDP_REALM, reqUrl, false );
			}

		}catch( PRUDPPacketHandlerException e ){
			
			if ( auth != null ){
						
				SESecurityManager.setPasswordAuthenticationOutcome( TRTrackerClientClassicImpl.UDP_REALM, reqUrl, false );
			}

			if ( e.getMessage() == null || e.getMessage().indexOf("timed out") == -1 ){
				
				throw( e );
			}
		}
	}
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
  

  protected TRTrackerScraperResponseImpl addHash(byte[] hash) {
    TRTrackerScraperResponseImpl response = new TRTrackerScraperResponseImpl(this, hash);
    if (scrapeURL == null)  {
      response.setStatus(TRTrackerScraperResponse.ST_ERROR);
      response.setStatusString(MessageText.getString("Scrape.status.error") + 
                               MessageText.getString("Scrape.status.error.badURL"));
    } else {
      response.setStatusString(MessageText.getString("Scrape.status.initializing"));
    }
  	synchronized( hashes ){
      hashes.put(hash, response);
  	}

    //notifiy listeners
    scraper.scrapeReceived( response );

  	return response;
  }
  
  protected void removeHash(HashWrapper hash) {
  	synchronized( hashes ){
  		hashes.remove( hash.getHash() );
  	}
  }
  
  
  protected Map getHashes() {
    return hashes;
  }

	protected void scrapeReceived(TRTrackerScraperResponse response) {
	  scraper.scrapeReceived(response);
	}
}
