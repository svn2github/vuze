package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentForceStart extends TorrentCommand {
	public TorrentForceStart()
	{
		super(new String[] {"forcestart"}, "Force Starting");
	}
	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		try {
	          dm.setForceStart(true);
        } catch (Exception e) {
          e.printStackTrace(ci.out);
          return false;
        }
        return true;
	}

	public String getCommandDescriptions() {
		return("forcestart (<torrentoptions>)\tr\tStart torrent ignoring other limits/rules.");
	}

}
