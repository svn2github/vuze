/*
 * File    : SuperSeedPiece.java
 * Created : 13 déc. 2003}
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.core3.peer.impl.control;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;

/**
 * @author Olivier
 *
 */
public class SuperSeedPiece {
  
  
  private PEPeerControl manager;
  private int pieceNumber;
  
  private int level;
  private long timeFirstDistributed;
  private PEPeer firstReceiver;
  private int numberOfPeersWhenFirstReceived;
  private int timeToReachAnotherPeer;
  
  
  public SuperSeedPiece(PEPeerControl manager,int pieceNumber) {
    this.manager = manager;
    this.pieceNumber = pieceNumber;
    level = 0;
  }
  
  public synchronized void peerHasPiece(PEPeer peer) {
    if(level < 2) {
      firstReceiver = peer;
      timeFirstDistributed = System.currentTimeMillis();
      numberOfPeersWhenFirstReceived = manager.getNbPeers();
    } else {
      if(peer != null && firstReceiver != null) {
        timeToReachAnotherPeer = (int) (System.currentTimeMillis() - timeFirstDistributed);
        firstReceiver.setUploadHint(timeToReachAnotherPeer);
      }
    }
    level = 2;
  }
  
  public int getLevel() {
    return level;
  }
  
  public synchronized void pieceRevealedToPeer() {
    level = 1;
  }
  /**
   * @return Returns the pieceNumber.
   */
  public int getPieceNumber() {
    return pieceNumber;
  }
  
  public void peerLeft() {
    if(level == 1)
      level = 0;
  }
  
  public void updateTime() {
    if(level < 2)
      return;
    if(timeToReachAnotherPeer > 0)
      return;
    if(firstReceiver == null)
      return;
    int timeToSend = (int) (System.currentTimeMillis() - timeFirstDistributed);
    if(timeToSend > firstReceiver.getUploadHint())
      firstReceiver.setUploadHint(timeToSend);
  }

}
