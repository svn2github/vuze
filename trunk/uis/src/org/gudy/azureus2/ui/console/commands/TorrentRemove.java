package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentRemove extends TorrentCommand {

	public TorrentRemove()
	{
		super(new String[] {"remove", "r"}, "Removing");
	}

	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		try {
			dm.stopIt( DownloadManager.STATE_STOPPED, false, false );
			ci.gm.removeDownloadManager(dm);
		} catch (GlobalManagerDownloadRemovalVetoException e) {
			ci.out.println("> Veto when removing torrent (" + e.getMessage() + ")");
			return false;
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			return false;
		}
		return true;
	}

	public String getCommandDescriptions() {
		return("remove (<torrentoptions>)\tr\tRemove torrent(s).");
	}

}
