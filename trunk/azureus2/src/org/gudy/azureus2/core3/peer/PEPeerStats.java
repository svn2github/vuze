/*
 * File    : PEPeerStats
 * Created : 15-Oct-2003
 * By      : stuff
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
 
 package org.gudy.azureus2.core3.peer;

/**
 * Provides Statistic upon a peer.
 * It uses Average to compute its different averages. 
 * 
 * @author Olivier
 *
 */

public interface 
PEPeerStats 
{   
  
  /**
   * Get the average speed at which this peer is uploading to us.
   * @return average in bytes per second
   */  
  public long getDownloadAverage();

  /**
   * Get the longer-term average speed at which this peer is uploading to us.
   * @return average in bytes per second
   */  
  public long getReception();

  /**
   * Get the average speed at which we are uploading data to this peer.
   * @return average in bytes per second
   */
  public long getUploadAverage();
   
  
  public long getTotalAverage();
   
  public long getTotalDiscarded();
 
  
  
  /**
   * Get the total number of data bytes uploaded to the peer this session.
   * @return total bytes sent
   */
  public long getTotalSent();
  
  
  /**
   * Get the total number of data bytes downloaded from the peer this session.
   * @return total bytes received
   */
  public long getTotalReceived();
 
  
  
  
  public long getStatisticSentAverage();
  
  /** Bytes the peer downloaded while we have been connected to it (not necessarily from us */
	public long getBytesDone();

  public void
  received(
  	int		bytes );
  
  public void
  discarded(
  	int		bytes );
}