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
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.*;

public class 
DownloadManagerStatsImpl 
	implements DownloadManagerStats
{
	protected DownloadManagerImpl	download_manager;
	
	protected int maxUploads = 4;
	
		//Completed (used for auto-starting purposes)
		
	protected int completed;
	
		// saved downloaded and uploaded
		
	protected long saved_downloaded;
	protected long saved_uploaded;
  
	protected long saved_discarded = 0;
	protected long saved_hashfails = 0;
  

	protected
	DownloadManagerStatsImpl(
		DownloadManagerImpl	dm )
	{
		download_manager = dm;
	}
	
	public void 
	received(
		int length )
	{
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   		
			gm.getStats().received(length);
	   	}
	}
  
	public void 
	discarded(int length) 
	{
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   		
			gm.getStats().discarded(length);
	   	}
	}

	 public void 
	 sent(int length) 
	 {
		GlobalManager	gm = download_manager.getGlobalManager();
		
	   	if (length > 0 && gm != null){
	   	
			gm.getStats().sent(length);
	   	}
	 }

	public int 
	getDownloadAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDownloadAverage();
	  	}
	  	
	  	return 0;
	}
  
	public int 
	getUploadAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getUploadAverage();
	  	}
	  	
	  	return 0;
	}
	
	public long 
	getETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null){
	  
			return pm.getETA();
	  }
	  
	  return -1;   //return exactly -1 if ETA is unknown
	}

	public int 
	getCompleted() 
	{
		DiskManager	dm = download_manager.getDiskManager();
		
	  if (dm == null)
		return completed;
	  if (dm.getState() == DiskManager.ALLOCATING || dm.getState() == DiskManager.CHECKING || dm.getState() == DiskManager.INITIALIZING)
		return dm.getPercentDone();
	  else {
		long total = dm.getTotalLength();
		return total == 0 ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);
	  }
	}

	/**
	 * @return
	 */
	public int getMaxUploads() {
	  return maxUploads;
	}

	/**
	 * @param i
	 */
	public void setMaxUploads(int i) {
	  maxUploads = i;
	}
	public String 
	getElapsedTime() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null)
		return pm.getElapsedTime();
	  return ""; //$NON-NLS-1$
	}
	
	public long getTimeStarted()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null)
		  return pm.getTimeStarted();
		return -1;
	}

	public long getDownloaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null) {
		PEPeerManagerStats ps = pm.getStats();
		return saved_downloaded + ps.getTotalReceived();
	  }
	  return(saved_downloaded);
	}

	public long getUploaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return saved_uploaded + pm.getStats().getTotalSent();
	  return( saved_uploaded );
	}
	
	public long getDiscarded(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalDiscarded();
	  return( saved_discarded );
	}
  
   public void setSavedDiscarded() {
     PEPeerManager  pm = download_manager.getPeerManager();
     if (pm == null) System.out.println("setDiscarded:: PeerManager null");
     else pm.getStats().setTotalDiscarded(saved_discarded);
   }
   
   public void saveDiscarded(long discarded) {
     this.saved_discarded = discarded;
   }

	public long getHashFails(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getNbHashFails();
	  return( saved_hashfails );
	}
  
	public void setSavedHashFails() {
	  PEPeerManager  pm = download_manager.getPeerManager();
	  if (pm == null) System.out.println("setSavedHashFails:: PeerManager null");
	  else pm.setNbHashFails((int)saved_hashfails);
	}
  
  public void saveHashFails(long fails) {
    this.saved_hashfails = fails;
  }
  

	public int getTotalAverage() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalAverage();
	  return( 0 );
	}
      
	public void setCompleted(int completed) {
	  this.completed = completed;
	}
  
	public int getShareRatio() {
	  long downloaded,uploaded;
	  PEPeerManager	pm = download_manager.getPeerManager();
	  if(pm != null) {
		downloaded = saved_downloaded + pm.getStats().getTotalReceived();
		uploaded = saved_uploaded + pm.getStats().getTotalSent();
	  } else {
		downloaded = this.saved_downloaded;
		uploaded = this.saved_uploaded;
	  }
        
	  if(downloaded == 0) {
		return -1;
	  }
	  else {
		return (int) ((1000 * uploaded) / downloaded);
	  }
	}
	
	
	public void 
	setSavedDownloadedUploaded(
		long 	downloaded,
		long 	uploaded ) 
	{
	  saved_downloaded = downloaded;
	  saved_uploaded = uploaded;
	}
  

	protected long
	getSavedDownloaded()
	{
		return( saved_downloaded );
	}
		
	protected long
	getSavedUploaded()
	{
		return( saved_uploaded );
	}
}
