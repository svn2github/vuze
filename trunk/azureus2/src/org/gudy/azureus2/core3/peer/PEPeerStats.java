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
  public long getDownloadAverage();

  public long getReception();

  public long getUploadAverage();
   
  public long getTotalAverage();
   
  public long getTotalDiscarded();
 
  public long getTotalSent();
  
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