/*
 * File    : PEPeerStatsImpl.java
 * Created : 15-Oct-2003
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

public class 
PEPeerStatsImpl 
	implements PEPeerStats
{
	  private int pieceLength;

	  private long totalReceived;
	  private long totalSent;
	  
	  private long totalDiscarded;
	  private long totalHave;

	  private Average receptionSpeed;
	  private Average chokingReceptionSpeed;
	  private Average sendingSpeed;
	  private Average overallSpeed;
	  private Average statisticSentSpeed;



	  public PEPeerStatsImpl(int pieceLength) {
//		  timeCreated = SystemTime.getCurrentTime() / 100;

		this.pieceLength = pieceLength;

		//average over 10s, update every 2000ms.
		receptionSpeed = Average.getInstance(2000, 10);

		//average over 5s, update every 100ms.
		sendingSpeed = Average.getInstance(1000, 5);

		//average over 20s, update every 1s.
		chokingReceptionSpeed = Average.getInstance(1000, 20);

		//average over 100s, update every 5s
		overallSpeed = Average.getInstance(5000, 100);

		//average over 60s, update every 3s
		statisticSentSpeed = Average.getInstance(3000, 60);

	  }
  
	  public void discarded(int length) {
		this.totalDiscarded += length;
	  }

	  public void received(int length) {
		totalReceived += length;
		receptionSpeed.addValue(length);
		chokingReceptionSpeed.addValue(length);
	  }

	  public void sent(int length) {
		totalSent += length;
		sendingSpeed.addValue(length);
	  }

	  public void haveNewPiece() {
		totalHave += pieceLength;
		overallSpeed.addValue(pieceLength);
	  }

	  public void statisticSent(int length) {
		statisticSentSpeed.addValue(length);
	  }

	  public long getDownloadAverage() { 
		return( receptionSpeed.getAverage());
	  }

	  public long getReception() {
		return chokingReceptionSpeed.getAverage();
	  }

	  public long getUploadAverage() {
		return( sendingSpeed.getAverage());
	  }
  
	  public long getTotalDiscarded() {
		return( totalDiscarded );
	  }  

	  public long getTotalSent() {
		return totalSent;
	  }
  
	  public long getTotalReceived() {
		return totalReceived;
	  }
    
	  public long 
	  getTotalAverage() 
	  {
	  	return( overallSpeed.getAverage());
	  }

	  public long getStatisticSentAverage() {
		return statisticSentSpeed.getAverage();
	  }
}
