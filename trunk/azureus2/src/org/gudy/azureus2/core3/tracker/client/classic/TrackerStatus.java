/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.*;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

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

  protected void 
  asyncUpdate(
  	final HashWrapper hash ) 
  {
  	synchronized( hashes ){
  		
	    if(hashes.get(hash) == null){
	    	
	      hashes.put(hash,new TRTrackerScraperResponseImpl(null,-1,-1));
	    }
  	}
    
    Thread t = new Thread("Tracker Checker - Scrape interface") {
       public void run() 
       {
       	updateSingleHash(hash);
      }
    };
    
    t.setDaemon(true);
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  private synchronized void 
  updateSingleHash(
  	HashWrapper hash) 
  {        
    if(scrapeURL == null){
     
    	return;
    }
      
    InputStream is = null;
    
    try {
      String info_hash = "?info_hash=";
      info_hash += URLEncoder.encode(new String(hash.getHash(), Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
      URL reqUrl = new URL(scrapeURL + info_hash);
      
      LGLogger.log(0,0,LGLogger.INFORMATION,"Accessing scrape interface using url : " + reqUrl);
      
      //System.out.println( "trying " + scrape.toString());
      
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
      
      ByteArrayOutputStream message = new ByteArrayOutputStream();
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
      
      LGLogger.log(0,0,LGLogger.INFORMATION,"Response from scrape interface : " + message);
      
      Map map = BDecoder.decode(message.toByteArray());
      Map mapFiles = (Map) map.get("files");
      Iterator iter = mapFiles.keySet().iterator();
      while(iter.hasNext()) {
        String strKey = (String)iter.next();
        
        byte[] key = (strKey).getBytes(Constants.BYTE_ENCODING);
        
        Map hashMap = (Map)mapFiles.get(strKey);
        
        // System.out.println(ByteFormatter.nicePrint(hash.getHash()) + " -> " + ByteFormatter.nicePrint(key));
        int seeds = ((Long)hashMap.get("complete")).intValue();
        int peers = ((Long)hashMap.get("incomplete")).intValue();
        
        TRTrackerScraperResponseImpl	response = new TRTrackerScraperResponseImpl(key,seeds,peers);
        
        synchronized( hashes ){
        	hashes.put(new HashWrapper(key),response);
        }
        
        scraper.scrapeReceived( response );
      }
      
      // decode additional flags - see http://anime-xtreme.com/tracker/blah.txt for example
      Map mapFlags = (Map) map.get("flags");
      
      if (mapFlags != null) {
      	int scrapeInterval = ((Long) mapFlags.get("min_request_interval")).intValue();
      	//Debug.out("scrape min_request_interval = " +scrapeInterval);
      }
      
    } catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
    } catch (Exception ignore) {
    } finally {
      if(is != null)
        try {
          is.close();
        } catch (IOException e1) {
        }
    }
  }
  
  protected void
  asyncUpdate()
  {
  	synchronized( hashes ){
  		
  		Iterator iterHashes = hashes.keySet().iterator();
  	
  		while(iterHashes.hasNext()) {
  		
  			HashWrapper hash = (HashWrapper) iterHashes.next();
  		
  			asyncUpdate(hash);
  		}
  	}     
  }

  
  protected void 
  removeHash(
  	HashWrapper hash)
  {
  	synchronized( hashes ){
  		
  		hashes.remove( hash );
  	}
  }  
}
