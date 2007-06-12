package com.aelitis.azureus.ui.swt.browser.listener.publish;

import java.util.HashMap;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;
import org.gudy.azureus2.plugins.utils.Semaphore;


public class DownloadStateAndRemoveListener implements DownloadManagerListener, DownloadListener, DownloadWillBeRemovedListener {
	
	
	private PluginInterface pluginInterface;
	private UISWTInstance swtInstance;
	private Display display;
	
	private HashMap downloadSemaphores;
	
	public void downloadAdded(Download download) {
		if(!PublishUtils.isRemovalAllowed(download)) {
			downloadSemaphores.put(download, pluginInterface.getUtilities().getSemaphore());
			download.addListener(this);
			download.addDownloadWillBeRemovedListener(this);
		}
	}
	
	public void downloadRemoved(Download download) {
		downloadSemaphores.remove(download);
	}
	
	
	public DownloadStateAndRemoveListener(PluginInterface pi,Display display) {
		this.pluginInterface = pi;
		this.display = display;		
		swtInstance = UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();
		
		this.downloadSemaphores = new HashMap();		
	}
	
	public void downloadWillBeRemoved(Download download) throws DownloadRemovalVetoException {
		if (PublishUtils.isPublished(download)) {
			
			if(PublishUtils.isRemovalAllowed(download)) {
				return;
			}
			
			Semaphore sem = (Semaphore) downloadSemaphores.get(download);
			if(sem != null) {
				sem.reserve();
			}
			
			if(!PublishUtils.isRemovalAllowed(download)) {
				throw new DownloadRemovalVetoException("Director Plugin Veto",true);
			}
		}
	}
	
	public void positionChanged(Download download, int oldPosition, int newPosition) {
		//Do nothing
	}
	
	public void stateChanged(Download download, int old_state, int new_state) {
		if (new_state == Download.ST_STOPPED && PublishUtils.isPublished(download)
				&& !PublishUtils.isRemovalAllowed(download)) {
			
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
				PublishUtils.setRemovalAllowed(download);
			}
						
			Semaphore sem = (Semaphore) downloadSemaphores.get(download);
			if(sem != null) {
				sem.releaseAllWaiters();
			}
		}
	}
}
