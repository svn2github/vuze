package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentStop extends TorrentCommand {

	/**
	 * @param args
	 * @param action
	 */
	public TorrentStop() {
		super(new String[] {"stop", "h"}, "Stopping");
	}

	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		try {
			dm.stopIt( DownloadManager.STATE_STOPPED, false, false );
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			return false;
		}
		return true;
	}

	public String getCommandDescriptions() {
		return("stop (<torrentoptions>)\t\th\tStop torrent(s).");
	}

}
