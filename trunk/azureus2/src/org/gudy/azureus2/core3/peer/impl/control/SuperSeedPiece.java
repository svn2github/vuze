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
  private int timeToReach25Percent;
  
  
  public SuperSeedPiece(PEPeerControl manager,int pieceNumber) {
    this.manager = manager;
    this.pieceNumber = pieceNumber;
    level = 0;
  }
  
  public synchronized void peerHasPiece(PEPeer peer) {
    if(firstReceiver == null) {
      firstReceiver = peer;
      timeFirstDistributed = System.currentTimeMillis();
      numberOfPeersWhenFirstReceived = manager.getNbPeers();
    }
    int availLevel = manager.getAvailability(pieceNumber);
    if(availLevel == 10) {
      timeToReach25Percent = (int) (System.currentTimeMillis() - timeFirstDistributed);
      if(firstReceiver != null) {
        firstReceiver.setUploadHint(timeToReach25Percent);
      }
    }
    //Ok, the idea is to go from 0 to 2, from 1 to 2, from 2 to 4, from 3 to 4 ...
    level = 2 * (level / 2) + 2;
  }
  
  public int getLevel() {
    return level;
  }
  
  public synchronized void pieceRevealedToPeer() {
    //0->1 , 1->1 , 2->3 , 3->3
    level = 2 * (level / 2) + 1;
  }
  /**
   * @return Returns the pieceNumber.
   */
  public int getPieceNumber() {
    return pieceNumber;
  }
  
  public void peerLeft() {
    level = 2 * (level / 2);
  }

}
