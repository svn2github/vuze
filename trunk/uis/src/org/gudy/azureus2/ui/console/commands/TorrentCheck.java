package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentCheck extends TorrentCommand {
	public TorrentCheck()
	{
		super(new String[] { "check", "c"}, "Initiating recheck of");
	}
	
	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		try {
			if (dm.canForceRecheck()) {
				dm.forceRecheck();
				return true;
			} else
				return false;
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			return false;
		}
	}

	public String getCommandDescriptions() {
		return("check (<torrentoptions>)\tc\tForce recheck on torrent(s).");
	}

}
