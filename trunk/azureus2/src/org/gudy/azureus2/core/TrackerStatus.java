/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Olivier
 * 
 */
public class TrackerStatus {
  private URL scrapeURL;
  byte[] data;

  private HashMap hashes;

  public TrackerStatus(String trackerUrl) {    
    this.hashes = new HashMap();
    try {
      if(trackerUrl.endsWith("/announce"))
        this.scrapeURL = new URL(trackerUrl.substring(0,trackerUrl.length()-9)  + "/scrape");
      else
        this.scrapeURL = new URL(trackerUrl + "/scrape");
    } catch (Exception e) {
      e.printStackTrace();
    } 
    data = new byte[1024];
  }

  public HashData getHashData(Hash hash) {
    return (HashData) hashes.get(hash);
  }

  public synchronized void update() {
    InputStream is = null;
    try {
      HttpURLConnection con = (HttpURLConnection) scrapeURL.openConnection();
      con.connect();
      is = con.getInputStream();
      String message = "";
      int nbRead = 0;
      while (nbRead >= 0) {
        try {
          nbRead = is.read(data);
          if (nbRead >= 0)
            message += new String(data, 0, nbRead, "ISO-8859-1");
          Thread.sleep(20);
        }
        catch (Exception e) {
          nbRead = -1;
          message = null;
          return;
          //e.printStackTrace();
        }
      }
      Map map = BDecoder.decode(message.getBytes("ISO-8859-1"));
      map = (Map) map.get("files");
      Iterator iter = map.keySet().iterator();
      while(iter.hasNext()) {
        String strKey = (String) iter.next();
        byte[] key = (strKey).getBytes("ISO-8859-1");
        Map hashMap = (Map) map.get(strKey);
        //System.out.println(ByteFormater.nicePrint(key) + " :: " + hashMap);
        int seeds = ((Long)hashMap.get("complete")).intValue();
        int peers = ((Long)hashMap.get("incomplete")).intValue();
        hashes.put(new Hash(key),new HashData(seeds,peers));        
      }
    }
    catch (Exception e) {
      //e.printStackTrace();
    } finally {
      if(is != null)
        try {
          is.close();
        } catch (IOException e1) {
        }
    }
  }

}
