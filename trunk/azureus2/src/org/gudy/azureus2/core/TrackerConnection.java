package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 
 * This class handles communication with the tracker, but doesn't analyse responses.
 * 
 * @author Olivier
 *
 */
public class TrackerConnection {
  private String trackerUrl;
  private byte[] hash;
  private byte[] peerId;
  //  private long uploaded;
  //  private long downloaded;
  //  private long remaining;
  private int port;

  private PeerManager manager;

  public final static int componentID = 2;
  public final static int evtLifeCycle = 0;
  public final static int evtFullTrace = 1;
  public final static int evtErrors = 2;

  private static final byte[] azureus = "Azureus".getBytes();

  public TrackerConnection(Map metainfo, byte[] hash, int port) {
    //Get the Tracker url
    try {
      trackerUrl = new String((byte[]) metainfo.get("announce"), Constants.DEFAULT_ENCODING);
    } catch (Exception e) {
      e.printStackTrace();
    }

    //Copy the hash
    this.hash = new byte[20];
    for (int i = this.hash.length - 1; i >= 0; i--) {
      this.hash[i] = hash[i];
    }

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

    //    uploaded = 0;
    //    downloaded = 0;
    //    remaining = 0; 

    this.port = port;
    Logger.getLogger().log(componentID, evtLifeCycle, Logger.INFORMATION, "Tracker Connection Created using url : " + trackerUrl);
    //Logger.getLogger().log(componentID,evtFullTrace,Logger.INFORMATION,"PeerId Generated : " + Main.nicePrint(peerId));
  }

  public String start() {
    Logger.getLogger().log(componentID, evtLifeCycle, Logger.INFORMATION, "Tracker Connection is sending a start Request");
    return update("started");
  }

  public String completed() {
    Logger.getLogger().log(componentID, evtLifeCycle, Logger.INFORMATION, "Tracker Connection is sending a completed Request");
    return update("completed");
  }

  public String stop() {
    Logger.getLogger().log(componentID, evtLifeCycle, Logger.INFORMATION, "Tracker Connection is sending a stopped Request");
    return update("stopped");
  }

  public String update() {
    Logger.getLogger().log(componentID, evtLifeCycle, Logger.INFORMATION, "Tracker Connection is sending an update Request");
    return update("");
  }

  private String update(String evt) {
    InputStream is = null;
    try {
      URL reqUrl = new URL(constructURL(evt));
      Logger.getLogger().log(componentID, evtFullTrace, Logger.INFORMATION, "Tracker is Requesting : " + reqUrl);
      HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
      con.connect();
      is = con.getInputStream();
      //      int length = con.getContentLength();
      //      System.out.println(length);
      byte[] data = new byte[1024];
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      int nbRead = 0;
      while (nbRead >= 0) {
        try {
          nbRead = is.read(data);
          if (nbRead >= 0)
            message.write(data, 0, nbRead);
          Thread.sleep(10);
        } catch (Exception e) {
          Logger.getLogger().log(componentID, evtErrors, Logger.ERROR, "Exception while Requesting Tracker : " + e);
          Logger.getLogger().log(componentID, evtFullTrace, Logger.ERROR, "Message Received was : " + message);
          nbRead = -1;
          message = null;
        }
      }

      Logger.getLogger().log(componentID, evtFullTrace, Logger.INFORMATION, "Tracker Connection has received : " + message);
      return new String(message.toByteArray(), Constants.BYTE_ENCODING);
    } catch (Exception e) {
      Logger.getLogger().log(componentID, evtErrors, Logger.ERROR, "Exception while creating the Tracker Request : " + e);
      return null;
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Exception e) {
      }
    }
  }

  public String constructURL(String evt) {
    String request = trackerUrl;
    try {
      //1.3 Version
      //request += "?info_hash=" + URLEncoder.encode(new String(hash,"ISO-8859-1"));
      //request += "&peer_id=" + URLEncoder.encode(new String(peerId,"ISO-8859-1"));

      //1.4 Version
      request += "?info_hash=" + URLEncoder.encode(new String(hash, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
      request += "&peer_id=" + URLEncoder.encode(new String(peerId, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
    } catch (Exception e) {
      e.printStackTrace();
    }
    request += "&port=" + port;
    request += "&uploaded=" + manager.uploaded();
    request += "&downloaded=" + manager.downloaded();
    request += "&left=" + manager.getRemaining();
    if (evt.length() != 0)
      request += "&event=" + evt;
    if (evt.equals("stopped"))
      request += "&numpeers=0";
    ConfigurationManager config = ConfigurationManager.getInstance();
    String ip = config.getStringParameter("Override Ip", "");
    if (ip.length() != 0)
      request += "&ip=" + ip;

    return request;
  }

  public byte[] getPeerId() {
    return peerId;
  }

  public void setManager(PeerManager manager) {
    this.manager = manager;
  }

  public String getTrackerUrl() {
    return trackerUrl;
  }

}