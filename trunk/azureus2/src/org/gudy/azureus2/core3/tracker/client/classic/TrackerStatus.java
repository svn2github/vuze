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
import java.util.Iterator;
import java.util.Map;
import java.util.zip.*;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;

/**
 * @author Olivier
 * 
 */
public class TrackerStatus {
  private String scrapeURL = null;
 
  private HashMap 					hashes;
   private TRTrackerScraperImpl		scraper;
    

  public 
  TrackerStatus(
  	TRTrackerScraperImpl	_scraper,
  	String 					trackerUrl) 
  {    	
  	scraper		= _scraper;
    
    hashes = new HashMap();
    
    try {
      trackerUrl = trackerUrl.replaceAll(" ", "");
      int position = trackerUrl.lastIndexOf('/');
      if(	position >= 0 &&
      		trackerUrl.length() >= position+9 && 
      		trackerUrl.substring(position+1,position+9).equals("announce")){
      		
      		
        this.scrapeURL = trackerUrl.substring(0,position+1) + "scrape" + trackerUrl.substring(position+9);
        
        // System.out.println( "url = " + trackerUrl + ", scrape =" + scrapeURL );
     }else{
		LGLogger.log(0,0,LGLogger.INFORMATION,"can't scrape using '" + trackerUrl + "' as it doesn't end in '/announce'");		
     }
    } catch (Exception e) {
    	
      e.printStackTrace();
    } 
  }

  protected TRTrackerScraperResponseImpl 
  getHashData(HashWrapper hash) 
  {
  	synchronized( hashes ){
  		
  		return (TRTrackerScraperResponseImpl) hashes.get(hash);
  	}
  }



  protected synchronized void 
  updateSingleHash(
  	HashWrapper hash) 
  {      
    
    if(scrapeURL == null) return;
    
    synchronized( hashes ){
      TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl)hashes.get(hash);
      
      if(response == null){
        hashes.put(hash,new TRTrackerScraperResponseImpl(null,-1,-1,-1));
      }
      else if (response.getNextScrapeStartTime() >= System.currentTimeMillis()) {
        LGLogger.log(0,0,LGLogger.INFORMATION,"Skipping Scrape for hash "+hash.getHash());
        return;
      }
    }
          
    try {
      String info_hash = "?info_hash=";
      info_hash += URLEncoder.encode(new String(hash.getHash(), Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
      URL reqUrl = new URL(scrapeURL + info_hash);
      
      LGLogger.log(0,0,LGLogger.INFORMATION,"Accessing scrape interface using url : " + reqUrl);
 
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      
      long scrapeStartTime = System.currentTimeMillis();
      
      if ( reqUrl.getProtocol().equalsIgnoreCase( "udp" )){
      	
      	scrapeUDP( reqUrl, message, hash.getHash());
      	
      }else{
      	
      	scrapeHTTP( reqUrl, message );
      }
        
            
      LGLogger.log(0,0,LGLogger.INFORMATION,"Response from scrape interface : " + message);
      
      Map map = BDecoder.decode(message.toByteArray());
      
      Map mapFiles = (Map) map.get("files");
      
      //retrieve the scrape data for the relevent infohash
      Map scrapeMap = (Map)mapFiles.get(new String(hash.getHash(), Constants.BYTE_ENCODING));
      
      if ( scrapeMap == null ){
      	
      	// System.out.println("scrape: hash missing from reply");
      	
      }else{
      	
	      //retrive values
	      int seeds = ((Long)scrapeMap.get("complete")).intValue();
	      int peers = ((Long)scrapeMap.get("incomplete")).intValue();

	      //create the response
         TRTrackerScraperResponseImpl response = new TRTrackerScraperResponseImpl(hash.getHash(), seeds, peers, scrapeStartTime);
          
	      // decode additional flags - see http://anime-xtreme.com/tracker/blah.txt for example
	      Map mapFlags = (Map) map.get("flags");
	      if (mapFlags != null) {
	        int scrapeInterval = ((Long) mapFlags.get("min_request_interval")).intValue();
	        
           if (scrapeInterval < 10*59) scrapeInterval = 10*59;
           if (scrapeInterval > 60*60) scrapeInterval = 60*60;

           long nextScrapeTime = System.currentTimeMillis() + (scrapeInterval * 1000);
           response.setNextScrapeStartTime(nextScrapeTime);
           
           Debug.out("scrape min_request_interval = " +scrapeInterval);
	      }

	      //update the hash list
	      synchronized( hashes ) {
	        hashes.put(new HashWrapper(hash.getHash()), response);
	      }
	  
	      //notifiy listeners
	      scraper.scrapeReceived( response );
      }

    } catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
    } catch (Exception ignore) {
    }
  }
  
  protected void
  scrapeHTTP(
  	URL						reqUrl,
	ByteArrayOutputStream	message )
  
  	throws IOException
  {
  	//System.out.println( "trying " + scrape.toString());
  	
  	InputStream is = null;
  	
  	try{
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
	  }finally {
	  	if(is != null)
	  		try {
	  		is.close();
	  	} catch (IOException e1) {
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
	int lp = COConfigurationManager.getIntParameter("Low Port", 6881);
	
	PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( lp );
	
	InetSocketAddress destination = new InetSocketAddress(reqUrl.getHost(),reqUrl.getPort()==-1?80:reqUrl.getPort());
	
	for (int retry_loop=0;retry_loop<PRUDPPacket.DEFAULT_RETRY_COUNT;retry_loop++){
	
		try{
			PRUDPPacket connect_request = new PRUDPPacketRequestConnect(0);
			
			PRUDPPacket reply = handler.sendAndReceive( connect_request, destination );
			
			if ( reply.getAction() == PRUDPPacket.ACT_REPLY_CONNECT ){
				
				PRUDPPacketReplyConnect connect_reply = (PRUDPPacketReplyConnect)reply;
				
				long	my_connection = connect_reply.getConnectionId();
				
				PRUDPPacketRequestScrape scrape_request = new PRUDPPacketRequestScrape( my_connection, hash );
								
				reply = handler.sendAndReceive( scrape_request, destination );
				
				if ( reply.getAction() == PRUDPPacket.ACT_REPLY_SCRAPE ){
					
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
					
					LGLogger.log(LGLogger.ERROR, 
									"Response from scrape interface : " +
										((PRUDPPacketReplyError)reply).getMessage());
				}
			}else{
				
				LGLogger.log(LGLogger.ERROR, 
						"Response from scrape interface : " +
						((PRUDPPacketReplyError)reply).getMessage());
			}
		}catch( PRUDPPacketHandlerException e ){
			
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
  

  
  protected void 
  removeHash(
  	HashWrapper hash)
  {
  	synchronized( hashes ){
  		
  		hashes.remove( hash );
  	}
  }
  
  
  protected Map getHashes() {
    return hashes;
  }
}
