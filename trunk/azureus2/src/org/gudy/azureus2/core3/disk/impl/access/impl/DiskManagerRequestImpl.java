/*
 * File    : DiskManagerRequestImpl.java
 * Created : 18-Oct-2003
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

package org.gudy.azureus2.core3.disk.impl.access.impl;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * 
 * This class represents a Bittorrent Request.
 * and a time stamp to know when it was created.
 * 
 * Request may expire after some time, which is used to determine who is snubbed.
 * 
 * @author Olivier
 *
 * 
 */
public class 
DiskManagerRequestImpl
	implements DiskManagerRequest
{
  //60 secs of expiration for any request.
  private static final int EXPIRATION_TIME = 1000 * 60;
  
  private int pieceNumber;
  private int offset;
  private int length;
  private long timeCreated;
  
  /**
   * Parameters correspond to bittorrent parameters
   * @param pieceNumber
   * @param offset
   * @param length
   */
  public DiskManagerRequestImpl(int pieceNumber,int offset,int length)
  {
    this.pieceNumber = pieceNumber;
    this.offset = offset;
    this.length = length;
    timeCreated = SystemTime.getCurrentTime();
  }
  
  /**
   * Method to determine if a Request has expired
   * @return true is the request is expired
   */
  public boolean isExpired()
  {
    return ((SystemTime.getCurrentTime() - timeCreated) > EXPIRATION_TIME);    
  }
  
  /**
   * Allow some more time to the request.
   * Typically used on peers that have just sent some data, we reset all
   * other requests to give them extra time.
   */
  public void reSetTime()
  {
      timeCreated = SystemTime.getCurrentTime();
  }
  
  //Getters  
  public int getPieceNumber()
  {
    return this.pieceNumber;
  }
  
  public int getOffset()
  {
    return this.offset;
  }
  
  public int getLength()
  {
    return this.length;
  }  
  
  /**
   * We override the equals method
   * 2 requests are equals if
   * all their bt fields (piece number, offset, length) are equal
   */
  public boolean equals(Object o)
  {
    if(! (o instanceof DiskManagerRequestImpl))
      return false;    
	DiskManagerRequestImpl otherRequest = (DiskManagerRequestImpl) o;
    if(otherRequest.pieceNumber != this.pieceNumber)
      return false;
    if(otherRequest.offset != this.offset)
      return false;
    if(otherRequest.length != this.length)
      return false;
      
    return true;
  }
  /**
   * @return
   */
  public long getTimeCreated() {
    return timeCreated;
  }

}
