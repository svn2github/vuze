/*
 * File    : EndGameModeChunk.java
 * Created : 4 déc. 2003}
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

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;

/**
 * @author Olivier
 *
 */
public class EndGameModeChunk {
    
  //private PEPiece piece;
  private int blockNumber;
  
  private int pieceNumber;
  private int length;
  private int offset;
  
  public EndGameModeChunk(PEPiece piece,int blockNumber) {
    //this.piece = piece;
    this.blockNumber = blockNumber;
    this.pieceNumber = piece.getPieceNumber();
    this.length = piece.getBlockSize(blockNumber);
    this.offset = DiskManager.BLOCK_SIZE * blockNumber;
  }
  
  public boolean compare(int pieceNumber,int offset) {
    return (   (this.pieceNumber == pieceNumber)
        		&& (this.offset == offset));
  }
  
  public int getPieceNumber() {
    return this.pieceNumber;
  }
  
  public int getLength() {
    return this.length;
  }
  
  public int getOffset() {
    return this.offset;
  }
  
  
  /**
   * @return Returns the blockNumber.
   */
  public int getBlockNumber() {
    return blockNumber;
  }

}
