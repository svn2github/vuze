package org.gudy.azureus2.ui.console.commands;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentHost extends TorrentCommand {

	public TorrentHost()
	{
		super(new String[] { "host"}, "Hosting");
	}

	protected boolean performCommand(ConsoleInput ci, DownloadManager dm) {
		TOTorrent torrent = dm.getTorrent();
        if (torrent != null) {
          try {
            ci.azureus_core.getTrackerHost().hostTorrent(torrent);
          } catch (TRHostException e) {
            e.printStackTrace(ci.out);
            return false;
          }
          return true;
        }
        return false;
	}

	public String getCommandDescriptions() {
		return("host (<torrentoptions>)\t\th\tHost torrent(s).");
	}

}
