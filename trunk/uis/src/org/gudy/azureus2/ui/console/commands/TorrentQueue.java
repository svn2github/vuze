package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentQueue extends TorrentCommand {

	public TorrentQueue()
	{
		super( new String[] { "queue", "q" }, "Queueing");
	}
	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		try {
			if (dm.getState() == DownloadManager.STATE_STOPPED)
				dm.setState(DownloadManager.STATE_QUEUED);
			else if (dm.getState() == DownloadManager.STATE_DOWNLOADING || dm.getState() == DownloadManager.STATE_SEEDING)
				dm.stopIt( DownloadManager.STATE_QUEUED, false, false );
			else
				return false;
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			return false;
		}
		return true;
	}

	public String getCommandDescriptions() {
		return("queue (<torrentoptions>)\tq\tQueue torrent(s).");
	}

}
