/*
 * File    : PEPeerManagerStatsImpl.java
 * Created : 05-Nov-2003
 * By      : parg
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

public class 
PEPeerManagerStatsImpl 
	implements PEPeerManagerStats
{
	private int pieceLength;

	private long totalReceived;
	private long totalSent;
	  
	private long totalDiscarded;
	private long totalHave;

	private Average receptionSpeed;
	private Average sendingSpeed;
	private Average overallSpeed;



	public PEPeerManagerStatsImpl(int pieceLength) {
//		timeCreated = System.currentTimeMillis() / 100;

	  this.pieceLength = pieceLength;

	  //average over 10s, update every 2000ms.
	  receptionSpeed = Average.getInstance(2000, 10);

	  //average over 5s, update every 100ms.
	  sendingSpeed = Average.getInstance(1000, 5);

	  //average over 100s, update every 5s
	  overallSpeed = Average.getInstance(5000, 100);
	}
  
	public void discarded(int length) {
	  this.totalDiscarded += length;
	}

	public void received(int length) {
	  totalReceived += length;
	  receptionSpeed.addValue(length);
	}

	public void sent(int length) {
	  totalSent += length;
	  sendingSpeed.addValue(length);
	}

	public void haveNewPiece() {
	  totalHave += pieceLength;
	  overallSpeed.addValue(pieceLength);
	}

	public int getDownloadAverage() { 
	  return( receptionSpeed.getAverage());
	}

	public int getUploadAverage() {
	  return( sendingSpeed.getAverage());
	}
  
	public long getTotalDiscarded() {
	  return( totalDiscarded );
	}
  
	public void setTotalDiscarded(long total) {
	  this.totalDiscarded = total;
	}

	public long getTotalSent() {
	  return totalSent;
	}
  
	public long getTotalReceived() {
	  return totalReceived;
	}
    
	public int 
	getTotalAverage() 
	{
	  return( overallSpeed.getAverage());
	}
}
