package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
  private int timeout = 20000; // default timeout 20 seconds
  private Thread httpConnecter = null;
  private boolean httpConnected = false;

  private String trackerUrl;
  private String info_hash = "?info_hash=";
  private byte[] peerId;
  private String peer_id = "&peer_id=";
  private String port;

  //  private long uploaded;
  //  private long downloaded;
  //  private long remaining;

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
      //1.3 Version
      //request += "?info_hash=" + URLEncoder.encode(new String(hash,"ISO-8859-1"));
      //request += "&peer_id=" + URLEncoder.encode(new String(peerId,"ISO-8859-1"));
  
      //1.4 Version
      this.info_hash += URLEncoder.encode(new String(hash, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
      this.peer_id += URLEncoder.encode(new String(peerId, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING).replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
    }
    this.port = "&port=" + port;

    //    uploaded = 0;
    //    downloaded = 0;
    //    remaining = 0; 

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
    try {
      URL reqUrl = new URL(constructURL(evt));
      Logger.getLogger().log(componentID, evtFullTrace, Logger.INFORMATION, "Tracker is Requesting : " + reqUrl);
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
              Logger.getLogger().log(componentID, evtErrors, Logger.ERROR, "Exception while Requesting Tracker : " + e);
              Logger.getLogger().log(componentID, evtFullTrace, Logger.ERROR, "Message Received was : " + message);
              nbRead = -1;
            }
          }
          Logger.getLogger().log(componentID, evtFullTrace, Logger.INFORMATION, "Tracker Connection has received : " + message);
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
      Logger.getLogger().log(componentID, evtErrors, Logger.ERROR, "Exception while creating the Tracker Request : " + e);
    }
    return null;
  }

  public String constructURL(String evt) {
    StringBuffer request = new StringBuffer(trackerUrl);
    request.append(info_hash);
    request.append(peer_id);
    request.append(port);
    request.append("&uploaded=").append(manager.uploaded());
    request.append("&downloaded=").append(manager.downloaded());
    request.append("&left=").append(manager.getRemaining());
    if (evt.length() != 0)
      request.append("&event=").append(evt);
    if (evt.equals("stopped"))
      request.append("&numpeers=0");
    String ip = ConfigurationManager.getInstance().getStringParameter("Override Ip", "");
    if (ip.length() != 0)
      request.append("&ip=").append(ip);

    return request.toString();
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

  /**
   * @param timeout maximum time in ms to wait for con.connect()
   */
  public void setTimeout(int timeout) {
    if(timeout >= 0)
      this.timeout = timeout;
  }

}