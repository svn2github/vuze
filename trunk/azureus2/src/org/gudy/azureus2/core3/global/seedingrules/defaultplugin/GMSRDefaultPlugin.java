/*
 * File    : GMSRDefaultPlugin.java
 * Created : 12-Jan-2004
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

package org.gudy.azureus2.core3.global.seedingrules.defaultplugin;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.config.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;

public class 
GMSRDefaultPlugin 
	implements Plugin
{	
	protected PluginInterface		plugin_interface;
	protected DownloadManager		download_manager;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{
		
		download_manager = plugin_interface.getDownloadManager();
		
			// initial implementation loops - change to event driven
		
		Thread	t = new Thread()
			{
				public void
				run()
				{
					while(true){
						try{
							
							process();
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					
						try{
							Thread.sleep(5000);
							
						}catch( InterruptedException e ){
							
							e.printStackTrace();
						}
					}
				}
			};
		
		t.setDaemon(true);
		
		t.start();
	}
	
	protected synchronized void
	process()
	{
		Download[]	downloads = download_manager.getDownloads();
		
		int	started		= 0;
		int	downloading	= 0;
		
		for (int i=0;i<downloads.length;i++){
			
			Download	download = downloads[i];
			
			int	state = download.getState();
			
			if ( state == Download.ST_DOWNLOADING){
				
				started++;
				
				downloading++;
				
			}else if ( state == Download.ST_SEEDING){
				
				started++;

				//First condition to be met to be able to stop a torrent is that the number of seeds
				//Is greater than the minimal set, if any.
				
			
				int nbMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", 0);
				
				
				DownloadScrapeResult sr = download.getLastScrapeResult();

				boolean mayStop = false;
				
				if ( sr.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
					
					if (sr.getSeedCount() > nbMinSeeds) {
						
						mayStop = true;
					}
				}else{
					
					mayStop = true;
				}
	
					//Checks if any condition to stop seeding is met
				
				int minShareRatio = 1000 * COConfigurationManager.getIntParameter("Stop Ratio", 0);
				
				int shareRatio = download.getStats().getShareRatio();
				
					//0 means unlimited
				
				if (minShareRatio != 0 && shareRatio > minShareRatio && mayStop && ! download.isStartStopLocked()){
					
					try{
						download.stop();
						
					}catch( DownloadException e ){
						
						e.printStackTrace();
					}
				}

				int minSeedsPerPeersRatio = COConfigurationManager.getIntParameter("Stop Peers Ratio", 0);
				
					//0 means never stop
				
				if (mayStop && minSeedsPerPeersRatio != 0) {
					
					if ( sr.getResponseType() == DownloadScrapeResult.RT_SUCCESS) {
						
						int nbPeers = sr.getNonSeedCount();
						
						int nbSeeds = sr.getSeedCount();
						
						//If there are no seeds, avoid / by 0
						
						if (nbSeeds != 0) {
							
							int ratio = nbPeers / nbSeeds;
							
								//Added a test over the shareRatio greater than 500
								//Avoids disconnecting too early, even with many peers
							
							if (ratio < minSeedsPerPeersRatio && (shareRatio > 500 || shareRatio == -1) && ! download.isStartStopLocked()){
								
								try{
									download.stop();
									
								}catch( DownloadException e ){
									
									e.printStackTrace();
								}
							}
						}
					}
				}
				
			}else if ( state == Download.ST_STOPPED && download.getStats().getCompleted() == 1000){
								
				//Checks if any condition to start seeding is met
				
				int nbMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", 0);
				
				int minSeedsPerPeersRatio = COConfigurationManager.getIntParameter("Start Peers Ratio", 0);
				
				//0 means never start
				
				if ( minSeedsPerPeersRatio != 0 && ! download.isStartStopLocked()){
					
					DownloadScrapeResult sr = download.getLastScrapeResult();
					
					if ( sr.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
						
						int nbPeers = sr.getNonSeedCount();
						
						int nbSeeds = sr.getSeedCount();
						
							//If there are no seeds, avoid / by 0
						
						if (nbPeers != 0){
							
							try{							
						
								if (nbSeeds != 0){
									
									int ratio = nbPeers / nbSeeds;
									
									if (ratio >= minSeedsPerPeersRatio){
									
										download.start();
									}
								}else{
										//No seeds, at least 1 peer, let's start download.
									
									download.start();
								}
							}catch( DownloadException e ){
								
								e.printStackTrace();
							}
						}
					}
				}
				
				if (nbMinSeeds > 0 && ! download.isStartStopLocked()) {
					
					DownloadScrapeResult sr = download.getLastScrapeResult();
					
					if ( sr.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
						
						int nbSeeds = sr.getSeedCount();
						
						if (nbSeeds < nbMinSeeds){
							
							try{
								
								download.start();
								
							}catch( DownloadException e ){
								
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	/*
	boolean alreadyOneAllocatingOrChecking = false;
	for (int i = 0; i < managers.size(); i++) {
		DownloadManager manager = (DownloadManager) managers.get(i);
		if (((manager.getState() == DownloadManager.STATE_ALLOCATING)
				|| (manager.getState() == DownloadManager.STATE_CHECKING)
				|| (manager.getState() == DownloadManager.STATE_INITIALIZED))) {
			alreadyOneAllocatingOrChecking = true;
		}
	}

	for (int i = 0; i < managers.size(); i++) {
		DownloadManager manager = (DownloadManager) managers.get(i);
		if ((manager.getState() == DownloadManager.STATE_WAITING) && !alreadyOneAllocatingOrChecking) {
			manager.initialize();
			alreadyOneAllocatingOrChecking = true;
		}
		int nbMax = COConfigurationManager.getIntParameter("max active torrents", 4);
		int nbMaxDownloads = COConfigurationManager.getIntParameter("max downloads", 4);
		if (manager.getState() == DownloadManager.STATE_READY
				&& ((nbMax == 0) || (nbStarted < nbMax))
				&& (manager.getStats().getCompleted() == 1000 || ((nbMaxDownloads == 0) || (nbDownloading < nbMaxDownloads)))) {
			manager.startDownload();
			
			//set previous hash fails and discarded values
			manager.getStats().setSavedDiscarded();
			manager.getStats().setSavedHashFails();
			
			nbStarted++;
			if (manager.getStats().getCompleted() != 1000)
				nbDownloading++;
		}

		if (manager.getState() == DownloadManager.STATE_ERROR) {
			DiskManager dm = manager.getDiskManager();
			if (dm != null && dm.getState() == DiskManager.FAULTY)
				manager.setErrorDetail(dm.getErrorMessage());
		}

		if ((manager.getState() == DownloadManager.STATE_SEEDING)
				&& (! manager.isPriorityLocked())
				&& (manager.getPriority() == DownloadManager.HIGH_PRIORITY)
				&& COConfigurationManager.getBooleanParameter("Switch Priority", false)) {
			manager.setPriority(DownloadManager.LOW_PRIORITY);
		}

		if ((manager.getState() == DownloadManager.STATE_ERROR)
				&& (manager.getErrorDetails() != null && manager.getErrorDetails().equals("File Not Found"))) {
			
			try{
				removeDownloadManager(manager);
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				e.printStackTrace();
			}
		}
		*/
	}
}
