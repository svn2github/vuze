/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core2;

import org.gudy.azureus2.core.PeerManager;

/**
 * @author Olivier
 * 
 */
public class Peer {

  public Peer(PeerManager manager, String ip, int port) {
    this.manager = manager;
    this.ip = ip;
    this.port = port;
    this.id = null;
  }

  public Peer(PeerManager manager, byte[] id, String ip, int port) {
    this(manager, ip, port);
    this.id = id;
  }

  PeerManager manager;
  byte[] id;
  String ip;
  int port;

  /**
   * @return
   */
  public byte[] getId() {
    return id;
  }

  /**
   * @return
   */
  public String getIp() {
    return ip;
  }

  /**
   * @return
   */
  public PeerManager getManager() {
    return manager;
  }

  /**
   * @return
   */
  public int getPort() {
    return port;
  }
  
  public boolean equals(Object o) {
      if (!(o instanceof Peer))
        return false;
      Peer p = (Peer) o;
      //At least the same instance is equal to itself :p
      if (this == p)
        return true;
      if (!(p.ip).equals(this.ip))
        return false;
      //same ip, we'll check peerId
      byte[] otherId = p.getId();
      if (otherId == null)
        return false;
      if (this.id == null)
        return false;
      for (int i = 0; i < otherId.length; i++) {
        if (otherId[i] != this.id[i])
          return false;
      }
      return true;
    }

}
