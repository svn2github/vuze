/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core2;

import java.util.Arrays;
import java.util.Vector;

import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;

/**
 * @author Olivier
 * 
 */
public class PeerConnection extends Peer {

  /**
   * @param manager
   * @param ip
   * @param port
   */
  public PeerConnection(PEPeerManager manager, String ip, int port) {
    super(manager, ip, port);
  }

  /**
   * @param manager
   * @param id
   * @param ip
   * @param port
   */
  public PeerConnection(PEPeerManager manager, byte[] id, String ip, int port) {
    super(manager, id, ip, port);
  }

  protected void allocateAll() {
    seed = false;

    choked = true;
    interested = false;
    requested = new Vector();

    choking = true;
    interesting = false;
    requesting = new Vector();

    available = new boolean[manager.getPiecesNumber()];
    Arrays.fill(available, false);

    stats = manager.createPeerStats();
  }

  protected PEPeerStats stats;

  protected boolean choked;
  protected boolean interested;
  protected Vector requested;

  protected boolean choking;
  protected boolean interesting;
  protected Vector requesting;

  protected boolean snubbed;

  protected boolean[] available;
  protected boolean seed;

  /**
   * @return
   */
  public boolean[] getAvailable() {
    return available;
  }

  /**
   * @return
   */
  public boolean isChoked() {
    return choked;
  }

  /**
   * @return
   */
  public boolean isChoking() {
    return choking;
  }

  /**
   * @return
   */
  public boolean isInterested() {
    return interested;
  }

  /**
   * @return
   */
  public boolean isInteresting() {
    return interesting;
  }

  /**
   * @return
   */
  public Vector getRequested() {
    return requested;
  }

  /**
   * @return
   */
  public Vector getRequesting() {
    return requesting;
  }

  /**
   * @return
   */
  public boolean isSeed() {
    return seed;
  }

  /**
   * @return
   */
  public boolean isSnubbed() {
    return snubbed;
  }

  /**
   * @return
   */
  public PEPeerStats getStats() {
    return stats;
  }

  /**
   * @param b
   */
  public void setChoked(boolean b) {
    choked = b;
  }

  /**
   * @param b
   */
  public void setChoking(boolean b) {
    choking = b;
  }

  /**
   * @param b
   */
  public void setInterested(boolean b) {
    interested = b;
  }

  /**
   * @param b
   */
  public void setInteresting(boolean b) {
    interesting = b;
  }

  /**
   * @param b
   */
  public void setSeed(boolean b) {
    seed = b;
  }

  /**
   * @param b
   */
  public void setSnubbed(boolean b) {
    snubbed = b;
  }

}
