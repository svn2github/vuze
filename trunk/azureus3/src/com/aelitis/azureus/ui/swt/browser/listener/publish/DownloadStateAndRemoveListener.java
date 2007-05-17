package com.aelitis.azureus.ui.swt.browser.listener.publish;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Semaphore;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;


public class DownloadStateAndRemoveListener implements DownloadManagerListener, DownloadListener, DownloadWillBeRemovedListener {
	
	public static final String REMOVAL_ATTRIBUTE_KEY = "REMOVAL ALLOWED";
	
	
	private PluginInterface pluginInterface;
	private UISWTInstance swtInstance;
	private Display display;
	
	private HashMap downloadSemaphores;
	
	public void downloadAdded(Download download) {
		if(!isRemovalAllowed(download)) {
			downloadSemaphores.put(download, pluginInterface.getUtilities().getSemaphore());
			download.addListener(this);
			download.addDownloadWillBeRemovedListener(this);
		}
	}
	
	public void downloadRemoved(Download download) {
		downloadSemaphores.remove(download);
	}
	
	
	public DownloadStateAndRemoveListener(PluginInterface pi,Display display,UISWTInstance swt) {
		this.pluginInterface = pi;
		this.display = display;		
		this.swtInstance = swt;
		
		this.downloadSemaphores = new HashMap();		
	}
	
	public void downloadWillBeRemoved(Download download) throws DownloadRemovalVetoException {
		if(isPublished(download)) {
			
			if(isRemovalAllowed(download)) {
				return;
			}
			
			Semaphore sem = (Semaphore) downloadSemaphores.get(download);
			if(sem != null) {
				sem.reserve();
			}
			
			if(! isRemovalAllowed(download)) {
				throw new DownloadRemovalVetoException("Director Plugin Veto",true);
			}
		}
	}
	
	public void positionChanged(Download download, int oldPosition, int newPosition) {
		//Do nothing
	}
	
	public void stateChanged(Download download, int old_state, int new_state) {
		if(new_state == Download.ST_STOPPED && isPublished(download) && ! isRemovalAllowed(download)) {
			
			final boolean[] stop = new boolean[1];
			
			display.syncExec(new Runnable() {
				public void run() {
						LocaleUtilities msgs = pluginInterface.getUtilities().getLocaleUtilities();
						String title = msgs.getLocalisedMessageText("v3.mb.stopSeeding.title");
						String text = msgs.getLocalisedMessageText("v3.mb.stopSeeding.text");
						int result = swtInstance.promptUser(title, text, new String[] {
								msgs.getLocalisedMessageText("v3.mb.stopSeeding.v3.mb.stopSeeding"),
								msgs.getLocalisedMessageText("v3.mb.stopSeeding.cancel") }, 1);
						stop[0] = (result == 0);
					}
			});
			
			if(!stop[0]) {
				download.setForceStart(true);
			} else {
				setRemovalAllowed(download);
			}
						
			Semaphore sem = (Semaphore) downloadSemaphores.get(download);
			if(sem != null) {
				sem.releaseAllWaiters();
			}
		}
	}
	
	private boolean isRemovalAllowed( Download d ) {
	  	//get the "content" attribute for the download
	  	final TorrentAttribute attrib = pluginInterface.getTorrentManager().getPluginAttribute( TorrentAttribute.TA_CONTENT_MAP );
	  	
	  	Map content_map = d.getMapAttribute( attrib );
			
	    //this one we're uploading as published
			if( content_map != null && content_map.containsKey( REMOVAL_ATTRIBUTE_KEY ) ) {   			
				return true;			
			}
			
			// Somehow the torrent is in stoppped state and the removal attribute wasn't set
			// Allow removal
			if (d.getState() == Download.ST_STOPPED) {
				return true;
			}
	  	return false;
	}
	
	private void setRemovalAllowed( Download d ) {
	  	//	  get the "content" attribute for the download
        TorrentAttribute attrib = pluginInterface.getTorrentManager().getPluginAttribute( TorrentAttribute.TA_CONTENT_MAP );
        Map content_map = d.getMapAttribute( attrib );
        
        if( content_map == null ) {
        	//System.out.print( "torrentIsReady:: content_map == null" );
        	content_map = new HashMap();
        }
        
        content_map.put( REMOVAL_ATTRIBUTE_KEY, new Long(1) );   //mark this download as "Removable" so we can pick it up later
        d.setMapAttribute( attrib, content_map );
	}

	private boolean isPublished( Download d ) {
	  	//get the "content" attribute for the download
	  	final TorrentAttribute attrib = pluginInterface.getTorrentManager().getPluginAttribute( TorrentAttribute.TA_CONTENT_MAP );
	  	
	  	Map content_map = d.getMapAttribute( attrib );
			
	    //this one we're uploading as published
			if( content_map != null && content_map.containsKey( PublishTransaction.PUBLISH_ATTRIBUTE_KEY ) ) {   			
				return true;			
			}
	  	
	  	return false;
	}

}
