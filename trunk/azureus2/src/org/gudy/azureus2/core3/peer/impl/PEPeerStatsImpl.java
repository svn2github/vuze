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
  
    private long total_data_bytes_received = 0;
    private long total_protocol_bytes_received = 0;

    private final Average data_receive_speed = Average.getInstance( 1000, 10 );  //update every 1s, average over 10s
    private final Average protocol_receive_speed = Average.getInstance( 1000, 10 );
    
    private long total_data_bytes_sent = 0;
    private long total_protocol_bytes_sent = 0;
    
    private final Average data_send_speed = Average.getInstance( 1000, 5 );   //update every 1s, average over 5s
    private final Average protocol_send_speed = Average.getInstance( 1000, 5 );
    
    private final Average receive_speed_for_choking = Average.getInstance( 1000, 20 );  //update every 1s, average over 20s
    private final Average estimated_download_speed = Average.getInstance( 5000, 100 );  //update every 5s, average over 100s
    private final Average estimated_upload_speed = Average.getInstance( 3000, 60 );  //update every 3s, average over 60s
    
    private long total_bytes_discarded = 0;
    private long total_bytes_downloaded = 0;


	  public PEPeerStatsImpl() {
	    /* nothing */
	  }
  

    public void dataBytesSent( int num_bytes ) {
      total_data_bytes_sent += num_bytes;
      data_send_speed.addValue( num_bytes );
    }
    
    public void protocolBytesSent( int num_bytes ) {
      total_protocol_bytes_sent += num_bytes;
      protocol_send_speed.addValue( num_bytes );
    }
    
    public void dataBytesReceived( int num_bytes ) {
      total_data_bytes_received += num_bytes;
      data_receive_speed.addValue( num_bytes );
      receive_speed_for_choking.addValue( num_bytes );
    }
    
    public void protocolBytesReceived( int num_bytes ) {
      total_protocol_bytes_received += num_bytes;
      protocol_receive_speed.addValue( num_bytes );
      //dont count protocol overhead towards a peer's choke/unchoke value, only piece data
    }
    
    public void bytesDiscarded( int num_bytes ) {
      total_bytes_discarded += num_bytes;
    }

    public void hasNewPiece( int piece_size ) {
      total_bytes_downloaded += piece_size;
      estimated_download_speed.addValue( piece_size );
    }
    
    public void statisticalSentPiece( int piece_size ) {
      estimated_upload_speed.addValue( piece_size );
    }
    

    public long getDataReceiveRate() {  return data_receive_speed.getAverage();  }
    public long getProtocolReceiveRate() {  return protocol_receive_speed.getAverage();  }

    public long getDataSendRate() {  return data_send_speed.getAverage();  }
    public long getProtocolSendRate() {  return protocol_send_speed.getAverage();  }

    public long getSmoothDataReceiveRate() {  return receive_speed_for_choking.getAverage();  }

    public long getTotalBytesDiscarded() {  return total_bytes_discarded;  }
    
    public long getTotalBytesDownloadedByPeer() {  return total_bytes_downloaded;  }

    public long getEstimatedDownloadRateOfPeer() {  return estimated_download_speed.getAverage();  }
    public long getEstimatedUploadRateOfPeer() {  return estimated_upload_speed.getAverage();  }

    public long getTotalDataBytesReceived() {  return total_data_bytes_received;  }
    public long getTotalProtocolBytesReceived() {  return total_protocol_bytes_received;  }
    
    public long getTotalDataBytesSent() {  return total_data_bytes_sent;  }
    public long getTotalProtocolBytesSent() {  return total_protocol_bytes_sent;  }
    

}
