/*
 * File    : GlobalManagerStatsImpl.java
 * Created : 21-Oct-2003
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

package org.gudy.azureus2.core3.global.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.util.*;


public class 
GlobalManagerStatsImpl
	implements GlobalManagerStats
{

	  private long totalReceived;
	  private long totalDiscarded;
	  private long totalSent;

	  private Average receptionSpeed;
	  private Average sendingSpeed;



	  protected 
	  GlobalManagerStatsImpl()
	  {

		//average over 10s, update every 2000ms.
		receptionSpeed = Average.getInstance(2000, 10);

		//average over 5s, update every 100ms.
		sendingSpeed = Average.getInstance(1000, 5);
	  }
  
  			// update methods
  			
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

 
	  public int getDownloadAverage() {
		return receptionSpeed.getAverage();
	  }
  
	  public int getUploadAverage() {
		  return sendingSpeed.getAverage();
	  }


	  public String getTotalSent() {
		return DisplayFormatters.formatByteCountToKBEtc(totalSent);
	  }

	  public String getTotalReceived() {
		return DisplayFormatters.formatByteCountToKBEtc(totalReceived);
	  }
    
	  public String getTotalDiscarded() {
		return DisplayFormatters.formatByteCountToKBEtc(totalDiscarded);
	  }  

	  public long getTotalSentRaw() {
		return totalSent;
	  }
  
	  public long getTotalReceivedRaw() {
		return totalReceived;
	  }
    
	  public long getTotalDiscardedRaw() {
		  return totalDiscarded;
	  }
}