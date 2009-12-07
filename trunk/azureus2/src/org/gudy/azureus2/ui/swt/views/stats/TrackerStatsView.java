package org.gudy.azureus2.ui.swt.views.stats;

import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerFactoryListener;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;

public class TrackerStatsView
	extends AbstractIView
{
	private AzureusCore core;

	public TrackerStatsView() {
  	AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				init(core);
			}
		});
	}

	protected void init(AzureusCore core) {
		this.core = core;
		
		TRTrackerAnnouncerFactory.addListener(new TRTrackerAnnouncerFactoryListener() {
			public void clientDestroyed(TRTrackerAnnouncer client) {
				System.out.println("ADD " + client.getTrackerURL().toString());
			}
			
			public void clientCreated(TRTrackerAnnouncer client) {
			}
		});
		List dms = core.getGlobalManager().getDownloadManagers();
		for (Object oDM : dms) {
			DownloadManager dm = (DownloadManager) oDM;
		}
	}
}
