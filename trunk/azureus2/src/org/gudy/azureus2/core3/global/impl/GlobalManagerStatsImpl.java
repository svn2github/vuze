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
	private GlobalManagerImpl		manager;
	
	private long total_data_bytes_received;
    private long total_protocol_bytes_received;
    
	private long totalDiscarded;
    
    private long total_data_bytes_sent;
    private long total_protocol_bytes_sent;

	private Average data_receive_speed = Average.getInstance(1000, 10);  //average over 10s, update every 1000ms
    private Average protocol_receive_speed = Average.getInstance(1000, 10);  //average over 10s, update every 1000ms

	private Average data_send_speed = Average.getInstance(1000, 10);  //average over 10s, update every 1000ms
    private Average protocol_send_speed = Average.getInstance(1000, 10);  //average over 10s, update every 1000ms


	protected 
	GlobalManagerStatsImpl(
		GlobalManagerImpl	_manager )
	{
		manager = _manager;
	}
  
    
  			// update methods
  			
	  public void discarded(int length) {
	    this.totalDiscarded += length;
	  }

	  public void dataBytesReceived(int length) {
	    total_data_bytes_received += length;
	    data_receive_speed.addValue(length);
	  }
    
    
    public void protocolBytesReceived(int length) {
      total_protocol_bytes_received += length;
      protocol_receive_speed.addValue(length);
    }
    
    

	  public void dataBytesSent(int length) {
	    total_data_bytes_sent += length;
	    data_send_speed.addValue(length);
	  }
    
    
    public void protocolBytesSent(int length) {
      total_protocol_bytes_sent += length;
      protocol_send_speed.addValue(length);
    }
    

 
	  public int getDataReceiveRate() {
	    return (int)data_receive_speed.getAverage();
	  }
    
    public int getProtocolReceiveRate() {
      return (int)protocol_receive_speed.getAverage();
    }
    
    
  
	  public int getDataSendRate() {
		  return (int)data_send_speed.getAverage();
	  }
    
    public int getProtocolSendRate() {
      return (int)protocol_send_speed.getAverage();
    }
    

    
	  public long getTotalDataBytesSent() {
	    return total_data_bytes_sent;
	  }
    
    public long getTotalProtocolBytesSent() {
      return total_protocol_bytes_sent;
    }
    
  
	  public long getTotalDataBytesReceived() {
	    return total_data_bytes_received;
	  }
    
    public long getTotalProtocolBytesReceived() {
      return total_protocol_bytes_received;
    }
    
    
	  public long getTotalDiscardedRaw() {
		  return totalDiscarded;
	  }
	  
	  public long getTotalSwarmsPeerRate(boolean downloading, boolean seeding )
	  {
		  return( manager.getTotalSwarmsPeerRate(downloading,seeding));
	  }

}