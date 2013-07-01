/*
 * File    : DownloadManagerStatsImpl.java
 * Created : 24-Oct-2003
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
 
package org.gudy.azureus2.core3.download.impl;

/**
 * @author parg
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.disk.*;

public class 
DownloadManagerStatsImpl 
	implements DownloadManagerStats
{
	private DownloadManagerImpl	download_manager;
		
		//Completed (used for auto-starting purposes)
		
	private int completed;
	private int downloadCompleted;
	
		// saved downloaded and uploaded
	private long saved_data_bytes_downloaded;
	private long saved_protocol_bytes_downloaded;
  
	private long saved_data_bytes_uploaded;
	private long saved_protocol_bytes_uploaded;
  
	private long saved_discarded = 0;
	private long saved_hashfails = 0;
	
	private long saved_SecondsDownloading = 0;
	private long saved_SecondsOnlySeeding = 0;
	
	private int saved_SecondsSinceDownload	= 0;
	private int saved_SecondsSinceUpload	= 0;
	
	private long saved_peak_receive_rate	= 0;
	private long saved_peak_send_rate		= 0;
	
	private int max_upload_rate_bps = 0;  //0 for unlimited
	private int max_download_rate_bps = 0;  //0 for unlimited
  
	private static final int HISTORY_MAX_SECS = 30*60;
	private volatile boolean history_retention_required;
	private long[]	history;
	private int		history_pos;
	private boolean	history_wrapped;
	
	protected
	DownloadManagerStatsImpl(
		DownloadManagerImpl	dm )
	{
		download_manager = dm;
	}

	public long 
	getDataReceiveRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataReceiveRate();
	  	}
	  	
	  	return 0;
	}
  
  
  public long 
  getProtocolReceiveRate() 
  {
    PEPeerManager pm = download_manager.getPeerManager();
    
      if (pm != null){
      
        return pm.getStats().getProtocolReceiveRate();
      }
      
      return 0;
  }
  
  
  
	public long 
	getDataSendRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataSendRate();
	  	}
	  	
	  	return 0;
	}
  
	public long 
	getProtocolSendRate() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
	    
	    if (pm != null){
	      
	        return pm.getStats().getProtocolSendRate();
	    }
	      
	    return 0;
	}
  
	public long
	getPeakDataReceiveRate()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		long	result = saved_peak_receive_rate;
		
	  	if ( pm != null ){
	  	
			result = Math.max( result, pm.getStats().getPeakDataReceiveRate());
	  	}
	  	
	  	return( result );
	}
	
	public long
	getPeakDataSendRate()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		long	result = saved_peak_send_rate;
		
	  	if ( pm != null ){
	  	
			result = Math.max( result, pm.getStats().getPeakDataSendRate());
	  	}
	  	
	  	return( result );
	}
	
	public long
	getSmoothedDataReceiveRate()
	{
		PEPeerManager	pm = download_manager.getPeerManager();

		if ( pm != null ){
			
			return( pm.getStats().getSmoothedDataReceiveRate());
		}
		
		return( 0 );
	}
	
	public long
	getSmoothedDataSendRate()
	{
		PEPeerManager	pm = download_manager.getPeerManager();

		if ( pm != null ){
			
			return( pm.getStats().getSmoothedDataSendRate());
		}
		
		return( 0 );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.download.DownloadManagerStats#getETA()
	 */
	public long 
	getETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
	  
			return pm.getETA( false );
		}
	  
		return -1;   //return exactly -1 if ETA is unknown
	}

	public long 
	getSmoothedETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
	  
			return pm.getETA( true );
		}
	  
		return -1;   //return exactly -1 if ETA is unknown
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.download.DownloadManagerStats#getCompleted()
	 */
	public int 
	getCompleted() 
	{
	  DiskManager	dm = download_manager.getDiskManager();
		
	  if (dm == null) {
	    int state = download_manager.getState();
	    if (state == DownloadManager.STATE_ALLOCATING ||
	        state == DownloadManager.STATE_CHECKING ||
	        state == DownloadManager.STATE_INITIALIZING)
	      return completed;
	    else
	      return downloadCompleted;
	  }
	  if (dm.getState() == DiskManager.ALLOCATING || 
	      dm.getState() == DiskManager.CHECKING || 
	      dm.getState() == DiskManager.INITIALIZING)
      return dm.getPercentDone();
	  else {
      long total = dm.getTotalLength();
      return total == 0 ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);
	  }
	}

	public void setCompleted(int _completed) {
	  completed = _completed;
	}

	public int 
	getDownloadCompleted(
		boolean bLive ) 
	{
		DiskManager	dm = download_manager.getDiskManager();
		
			// no disk manager -> not running -> use stored value
		
		if ( dm == null ){
			
		   return downloadCompleted;
		}
		
	    int state = dm.getState();

	    boolean	transient_state = 
	    		state == DiskManager.INITIALIZING ||
	            state == DiskManager.ALLOCATING   ||
	            state == DiskManager.CHECKING;
	    
	    long total = dm.getTotalLength();
	    
	    int computed_completion = (total == 0) ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);

	    	// use non-transient values to update the record of download completion
	    
	    if ( !transient_state ){
	    	
	    	downloadCompleted = computed_completion;
	    }
	    
	    if ( bLive ){
	    
	    		// return the transient completion level
	    	
	    	return computed_completion;
	    	
	    }else{
	    	
	    		// return the non-transient one
	    	
	    	return( downloadCompleted );
	    }
	}
  
	public void setDownloadCompleted(int _completed) {
		downloadCompleted = _completed;
	}

	public String getElapsedTime() {
	  PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null){
		return pm.getElapsedTime();
	  }
	  
	  return "";
	}
	
	public long 
	getTimeStarted() 
	{
		return( getTimeStarted( false ));
	}
	
	private long 
	getTimeStarted(
		boolean mono ) 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if ( pm != null ){
			
			return pm.getTimeStarted( mono );
		}
		
		return -1;
	}
	
	public long 
	getTimeStartedSeeding() 
	{
		return( getTimeStartedSeeding( false ));
	}
	
	private long 
	getTimeStartedSeeding(
		boolean mono ) 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
		 
			return( pm.getTimeStartedSeeding( mono ));
		}
		
		return -1;
	}

	public long 
	getTotalDataBytesReceived() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null) {
			return saved_data_bytes_downloaded + pm.getStats().getTotalDataBytesReceived();
		}
		return(saved_data_bytes_downloaded);
	}	
  
  
	public long 
	getTotalGoodDataBytesReceived() 
	{
		long downloaded	= getTotalDataBytesReceived();
       
		downloaded -= ( getHashFailBytes() + getDiscarded());
		
		if ( downloaded < 0 ){
			
			downloaded = 0;
		}
		
		return( downloaded );
	}
	
	public long 
	getTotalProtocolBytesReceived() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
    
		if (pm != null) {
			return saved_protocol_bytes_downloaded + pm.getStats().getTotalProtocolBytesReceived();
		}
    
		return(saved_protocol_bytes_downloaded);
	} 
  
	public void 
	resetTotalBytesSentReceived(
		long 	new_sent,
		long	new_received )
	{
		boolean running = download_manager.getPeerManager() != null;
		
		if ( running ){
			
			download_manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
		}
		
		
		if ( new_sent >= 0 ){
			
			saved_data_bytes_uploaded		= new_sent;
			saved_protocol_bytes_uploaded	= 0;
		}
		
		if ( new_received >= 0 ){
			
			saved_data_bytes_downloaded			= new_received;
			saved_protocol_bytes_downloaded		= 0;
		}

		if ( running ){
			
			download_manager.setStateWaiting();
		}
	}
	
	public long 
	getTotalDataBytesSent() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
	
		if (pm != null) {
			return saved_data_bytes_uploaded + pm.getStats().getTotalDataBytesSent();
		}
	  
		return( saved_data_bytes_uploaded );
	}
  
  
	public long 
	getTotalProtocolBytesSent() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
		
		if (pm != null) {
			
			return saved_protocol_bytes_uploaded + pm.getStats().getTotalProtocolBytesSent();
		}
		
		return( saved_protocol_bytes_uploaded );
	}
 
	public void
	setRecentHistoryRetention(
		boolean		required )
	{
		synchronized( this ){
			
			if ( required ){
				
				if ( !history_retention_required ){
					
					history 	= new long[HISTORY_MAX_SECS];
					
					history_pos	= 0;
					
					history_retention_required = true;
				}
			}else{
				
				history = null;
				
				history_retention_required = false;
			}
		}
	}
	
	private static final int HISTORY_DIV = 64;
	
	public int[][]
	getRecentHistory()
	{
		synchronized( this ){

			if ( history == null ){
		
				return( new int[3][0] );
				
			}else{
			
				int	entries = history_wrapped?HISTORY_MAX_SECS:history_pos;
				int	start	= history_wrapped?history_pos:0;
				
				int[][] result = new int[3][entries];
				
				int	pos = start;
				
				for ( int i=0;i<entries;i++){
					
					if ( pos == HISTORY_MAX_SECS ){
						
						pos = 0;
					}
					
					long entry = history[pos++];
					
					int	send_rate 	= (int)((entry>>42)&0x001fffffL);
					int	recv_rate 	= (int)((entry>>21)&0x001fffffL);
					int	swarm_rate 	= (int)((entry)&0x001fffffL);
					
					result[0][i] = send_rate*HISTORY_DIV;
					result[1][i] = recv_rate*HISTORY_DIV;
					result[2][i] = swarm_rate*HISTORY_DIV;
				}
				
				return( result );
			}
		}
	}
	
	protected void
	timerTick()
	{
		if ( !history_retention_required ){
			
			return;
		}
		
		PEPeerManager pm = download_manager.getPeerManager();
		
		if ( pm == null ){
			
			return;
		}
		
		PEPeerManagerStats stats = pm.getStats();
		
		long send_rate 			= stats.getDataSendRate() + stats.getProtocolSendRate();
		long receive_rate 		= stats.getDataReceiveRate() + stats.getProtocolReceiveRate();
		long peer_swarm_average = getTotalAveragePerPeer();
		
		long	entry = 
			((((send_rate-1+HISTORY_DIV/2)/HISTORY_DIV)<<42) 	&  0x7ffffc0000000000L ) |
			((((receive_rate-1+HISTORY_DIV/2)/HISTORY_DIV)<<21)  & 0x000003ffffe00000L ) |
			((((peer_swarm_average-1+HISTORY_DIV/2)/HISTORY_DIV))& 0x00000000001fffffL );
			
		
		synchronized( this ){
			
			if ( history != null ){
				
				history[history_pos++] = entry;
				
				if ( history_pos == HISTORY_MAX_SECS ){
					
					history_pos 	= 0;
					history_wrapped	= true;
				}
			}
		}
	}
	
	public long 
	getRemaining()
	{
		DiskManager disk_manager = download_manager.getDiskManager();
		
	    if ( disk_manager == null ){
	    	
	    	return download_manager.getSize() - 
		             ((long)getDownloadCompleted(false) * download_manager.getSize() / 1000L);
		
	    }else{
		     
	    	return disk_manager.getRemainingExcludingDND();
		}
	}
	
	public long 
	getDiscarded()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
			
			return saved_discarded + pm.getStats().getTotalDiscarded();
		}
		
		return( saved_discarded );
	}
  

	public long 
	getHashFailCount()
	{
		TOTorrent	t = download_manager.getTorrent();
		
		if ( t == null ){
			
			return(0);
		}
		
		long	total 	= getHashFailBytes();
		
		long	res = total / t.getPieceLength();
		
		if ( res == 0 && total > 0 ){
			
			res = 1;
		}
		
		return( res );
	}
  
	public long
	getHashFailBytes()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
			
			return saved_hashfails + pm.getStats().getTotalHashFailBytes();
		}
		
		return( saved_hashfails );
	}

	public long 
	getTotalAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
	
		if (pm != null){
			
			return pm.getStats().getTotalAverage();
		}
		
		return( 0 );
	}
      
	public long
	getTotalAveragePerPeer()
	{
		int div = download_manager.getNbPeers() + (download_manager.isDownloadComplete(false) ? 0 : 1);  //since total speed includes our own speed when downloading
	    
	    long average = div < 1 ? 0 : getTotalAverage() / div;

	    return( average );
	}
	
	public int 
	getShareRatio() 
	{
		long downloaded	= getTotalGoodDataBytesReceived();
		long uploaded	= getTotalDataBytesSent();
        
		if ( downloaded <= 0 ){
		  
			return( -1 );
		}

		return (int) ((1000 * uploaded) / downloaded);
	}
	
  
	public long 
	getSecondsDownloading() 
	{
	  long lTimeStartedDL = getTimeStarted( true );
	  if (lTimeStartedDL >= 0) {
  	  long lTimeEndedDL = getTimeStartedSeeding( true );
  	  if (lTimeEndedDL == -1) {
  	    lTimeEndedDL = SystemTime.getMonotonousTime();
  	  }
  	  if (lTimeEndedDL > lTimeStartedDL) {
    	  return saved_SecondsDownloading + ((lTimeEndedDL - lTimeStartedDL) / 1000);
    	}
  	}
	  return saved_SecondsDownloading;
	}

	public long 
	getSecondsOnlySeeding() 
	{
	  long lTimeStarted = getTimeStartedSeeding( true );
	  if (lTimeStarted >= 0) {
	    return saved_SecondsOnlySeeding + 
	           ((SystemTime.getMonotonousTime() - lTimeStarted) / 1000);
	  }
	  return saved_SecondsOnlySeeding;
	}
	
	public float
	getAvailability()
	{
	    PEPeerManager  pm = download_manager.getPeerManager();

	    if ( pm == null ){
	    	
	    	return( -1 );
	    }
		
	    return( pm.getMinAvailability());
	}

	public long
	getBytesUnavailable()
	{
	    PEPeerManager  pm = download_manager.getPeerManager();

	    if ( pm == null ){
	    	
	    	return( -1 );
	    }
		
	    return( pm.getBytesUnavailable());
	}

  
	public int 
	getUploadRateLimitBytesPerSecond() 
	{  
		return max_upload_rate_bps;  
	}

	public void 
	setUploadRateLimitBytesPerSecond( 
		int max_rate_bps ) 
	{  
		max_upload_rate_bps = max_rate_bps;  
	}
  
	public int 
	getDownloadRateLimitBytesPerSecond() 
	{  
		return max_download_rate_bps;  
	}
  
	public void 
	setDownloadRateLimitBytesPerSecond( 
		int max_rate_bps ) 
	{  
		max_download_rate_bps = max_rate_bps;  
	}
    
	public int 
	getTimeSinceLastDataReceivedInSeconds()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		int	res = saved_SecondsSinceDownload;
		
		if ( pm != null ){
			
			int	current = pm.getStats().getTimeSinceLastDataReceivedInSeconds();
			
			if ( current >= 0 ){
			
					// activity this session, use this value
				
				res = current;
				
			}else{
				
					// no activity this session. If ever has been activity add in session
					// time
				
				if ( res >= 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long	elapsed = now - pm.getTimeStarted( false );
					
					if ( elapsed < 0 ){
						
						elapsed = 0;
					}
					
					res += elapsed/1000;
				}
			}
		}
		
		return( res );	
	}
	
	public int 
	getTimeSinceLastDataSentInSeconds()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		int	res = saved_SecondsSinceUpload;
		
		if ( pm != null ){
			
			int	current = pm.getStats().getTimeSinceLastDataSentInSeconds();
			
			if ( current >= 0 ){
			
					// activity this session, use this value
				
				res = current;
				
			}else{
				
					// no activity this session. If ever has been activity add in session
					// time
				
				if ( res >= 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long	elapsed = now - pm.getTimeStarted( false );
					
					if ( elapsed < 0 ){
						
						elapsed = 0;
					}
					
					res += elapsed/1000;
				}
			}
		}
		
		return( res );	
	}
	
	public long
	getAvailWentBadTime()
	{
		PEPeerManager	pm = download_manager.getPeerManager();

		if ( pm != null ){
			
			long	bad_time = pm.getAvailWentBadTime();
			
			if ( bad_time > 0 ){
				
					// valid last bad time
				
				return( bad_time );
			}
			
			if ( pm.getMinAvailability() >= 1.0 ){
				
					// we can believe the fact that it isn't bad (we want to ignore 0 results from
					// downloads that never get to a 1.0 availbility)
				
				return( 0 );
			}
		}
		
		DownloadManagerState state = download_manager.getDownloadState();

		return( state.getLongAttribute( DownloadManagerState.AT_AVAIL_BAD_TIME ));
	}
	
	protected void
	saveSessionTotals()
	{
		  	// re-base the totals from current totals and session totals
		  
		saved_data_bytes_downloaded 	= getTotalDataBytesReceived();
		saved_data_bytes_uploaded		= getTotalDataBytesSent();
	  
		saved_discarded				= getDiscarded();
		saved_hashfails				= getHashFailBytes();
	
		saved_SecondsDownloading 		= getSecondsDownloading();
		saved_SecondsOnlySeeding		= getSecondsOnlySeeding();
		
		saved_SecondsSinceDownload		= getTimeSinceLastDataReceivedInSeconds();
		saved_SecondsSinceUpload		= getTimeSinceLastDataSentInSeconds();
		
		saved_peak_receive_rate			= getPeakDataReceiveRate();
		saved_peak_send_rate			= getPeakDataSendRate();
		
		DownloadManagerState state = download_manager.getDownloadState();

		state.setIntAttribute( DownloadManagerState.AT_TIME_SINCE_DOWNLOAD, saved_SecondsSinceDownload );
		state.setIntAttribute( DownloadManagerState.AT_TIME_SINCE_UPLOAD, saved_SecondsSinceUpload );
		
		state.setLongAttribute( DownloadManagerState.AT_AVAIL_BAD_TIME, getAvailWentBadTime());
		
		state.setLongAttribute( DownloadManagerState.AT_PEAK_RECEIVE_RATE, saved_peak_receive_rate );
		state.setLongAttribute( DownloadManagerState.AT_PEAK_SEND_RATE, saved_peak_send_rate );
	}
	
 	protected void
  	setSavedDownloadedUploaded(
  		long	d,
  		long	u )
  	{
		saved_data_bytes_downloaded	= d;
		saved_data_bytes_uploaded	= u;		
  	}
 	
	public void
	restoreSessionTotals(
		long		_saved_data_bytes_downloaded,
		long		_saved_data_bytes_uploaded,
		long		_saved_discarded,
		long		_saved_hashfails,
		long		_saved_SecondsDownloading,
		long		_saved_SecondsOnlySeeding )
	{
		saved_data_bytes_downloaded	= _saved_data_bytes_downloaded;
		saved_data_bytes_uploaded	= _saved_data_bytes_uploaded;
		saved_discarded				= _saved_discarded;
		saved_hashfails				= _saved_hashfails;
		saved_SecondsDownloading	= _saved_SecondsDownloading;
		saved_SecondsOnlySeeding	= _saved_SecondsOnlySeeding;
		
		DownloadManagerState state = download_manager.getDownloadState();
		
		saved_SecondsSinceDownload	= state.getIntAttribute( DownloadManagerState.AT_TIME_SINCE_DOWNLOAD );
		saved_SecondsSinceUpload	= state.getIntAttribute( DownloadManagerState.AT_TIME_SINCE_UPLOAD );
		
		saved_peak_receive_rate		= state.getLongAttribute( DownloadManagerState.AT_PEAK_RECEIVE_RATE );
		saved_peak_send_rate		= state.getLongAttribute( DownloadManagerState.AT_PEAK_SEND_RATE );
	}	
	
	protected void 
	generateEvidence(
		IndentWriter writer) 
	{
		writer.println( "DownloadManagerStats" );
		
		try{
			writer.indent();
			
			writer.println( 
				"recv_d=" + getTotalDataBytesReceived() + ",recv_p=" + getTotalProtocolBytesReceived() + ",recv_g=" + getTotalGoodDataBytesReceived() + 
				",sent_d=" + getTotalDataBytesSent() + ",sent_p=" + getTotalProtocolBytesSent() + 
				",discard=" + getDiscarded() + ",hash_fails=" + getHashFailCount() + "/" + getHashFailBytes() +
				",comp=" + getCompleted() 
				+ "[live:" + getDownloadCompleted(true) + "/" + getDownloadCompleted( false) 
				+ "],dl_comp=" + downloadCompleted
				+ ",remaining=" + getRemaining());
	
			writer.println( "down_lim=" + getDownloadRateLimitBytesPerSecond() +
							",up_lim=" + getUploadRateLimitBytesPerSecond());
		}finally{
			
			writer.exdent();
		}
	}
}
