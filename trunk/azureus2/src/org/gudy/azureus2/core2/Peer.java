/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core2;

import java.util.Arrays;

import org.gudy.azureus2.core3.peer.PEPeerManager;

/**
 * @author Olivier
 * 
 */
public class Peer {

  public Peer(PEPeerManager manager, String ip, int port) {
    this.manager = manager;
    this.ip = ip;
    this.port = port;
    this.id = null;
  }

  public Peer(PEPeerManager manager, byte[] id, String ip, int port) {
    this(manager, ip, port);
    this.id = id;
  }

  PEPeerManager manager;
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
  public PEPeerManager getManager() {
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
      byte[] otherId;
      if (this.id == null || (otherId = p.getId()) == null)
        return false;
        
      return Arrays.equals(this.id, otherId);
    }

}
