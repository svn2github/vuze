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
	
		//saved downloaded and uploaded
		
	protected long downloaded;
	protected long uploaded;

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
	
	public String 
	getETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null){
	  
			return pm.getETA();
	  }
	  
	  return ""; 
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

	public long getDownloaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null) {
		PEPeerStats ps = pm.getStats();
		return ps.getTotalReceived();
	  }
	  return(0);
	}

	public long getUploaded() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalSent();
	  return( 0 );
	}
	
	public long getDiscarded(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalDiscarded();
	  return( 0 );
	}

	public long getHashFails(){
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getNbHashFails();
	  return( 0 );
	}

	public int getTotalAverage() {
		PEPeerManager	pm = download_manager.getPeerManager();
	  if (pm != null)
		return pm.getStats().getTotalAverage();
	  return( 0 );
	}
      
	public void setDownloadedUploaded(long downloaded,long uploaded) {
	  this.downloaded = downloaded;
	  this.uploaded = uploaded;
	}
  
	public void setCompleted(int completed) {
	  this.completed = completed;
	}
  
	public int getShareRatio() {
	  long downloaded,uploaded;
	  PEPeerManager	pm = download_manager.getPeerManager();
	  if(pm != null) {
		downloaded = pm.getStats().getTotalReceived();
		uploaded = pm.getStats().getTotalSent();
	  } else {
		downloaded = this.downloaded;
		uploaded = this.uploaded;
	  }
        
	  if(downloaded == 0) {
		return -1;
	  }
	  else {
		return (int) ((1000 * uploaded) / downloaded);
	  }
	}
	
	protected long
	getDownloadedInternal()
	{
		return( downloaded );
	}
	
	protected void
	setDownloadedInternal(
		long	l )
	{
		downloaded = l;
	}
	
	protected long
	getUploadedInternal()
	{
		return( uploaded );
	}
	
	protected void
	setUploadedInternal(
		long	l )
	{
		uploaded = l;
	}
}
