/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

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
  private String trackerUrl;
  private String trackerStatus;
  private HashMap hashes;

  public TrackerStatus(String trackerUrl) {    
    this.trackerUrl = trackerUrl;
    this.hashes = new HashMap();
    if(this.trackerUrl.endsWith("/announce")) {
      this.trackerUrl = trackerUrl.substring(0,trackerUrl.length()-9);
    }
  }

  public HashData getHashData(Hash hash) {
    return (HashData) hashes.get(hash);
  }

  public void update() {
    try {
      URL reqUrl = new URL(trackerUrl + "/scrape");
      HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
      con.connect();
      InputStream is = con.getInputStream();
      byte[] data = new byte[1024];
      String message = "";
      int nbRead = 0;
      while (nbRead != -1) {
        try {
          nbRead = is.read(data);
          if (nbRead != -1)
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
      Map map = (Map) BDecoder.decode(message.getBytes("ISO-8859-1"));
      map = (Map) map.get("files");
      Iterator iter = map.keySet().iterator();
      while(iter.hasNext()) {
        String strKey = (String) iter.next();
        byte[] key = (strKey).getBytes("ISO-8859-1");
        Map hashMap = (Map) map.get(strKey);
        System.out.println(ByteFormater.nicePrint(key) + " :: " + hashMap);
        int seeds = ((Long)hashMap.get("complete")).intValue();
        int peers = ((Long)hashMap.get("incomplete")).intValue();
        hashes.put(new Hash(key),new HashData(seeds,peers));        
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
