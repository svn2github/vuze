package org.gudy.azureus2.ui.console.commands;

import java.util.List;
import java.util.Vector;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author pauld
 */
public class TorrentStart extends TorrentCommand {

	// we use this flag to effectively pass data between the
	// execute() and the performCommand() methods, since execute
	// will call performCommand
	private boolean startNow;
	
	public TorrentStart()
	{
		super( new String[] { "start" , "s" }, "Starting" );
	}
	public void execute(String commandName, ConsoleInput console, List args) {
		startNow = false;
		Vector newargs = new Vector(args);
		if (!newargs.isEmpty() && newargs.contains("now") ) {
			newargs.removeElement("now");
			startNow = true;
		}
		super.execute(commandName, console, args);
	}
	protected boolean performCommand(ConsoleInput ci, DownloadManager dm)
	{
		try {
			dm.setState(DownloadManager.STATE_WAITING);
			if( startNow )
			{
				dm.startDownloadInitialized(true);
			}
		} catch (Exception e) {
			e.printStackTrace(ci.out);
			return false;
		}
		return true;
	}

	public String getCommandDescriptions() {
		return "start (<torrentoptions>) [now]\ts\tStart torrent(s).";
	}
	
	public String getHelpExtra()
	{
		return ("<torrentoptions> can be one of:\n"+
		"<#>\t\tNumber of a torrent. You have to use 'show torrents' first. as the number is taken from there.\n"+
		"all\t\tCommand is applied to all torrents\n"+
		"hash <hash>\tApplied to torrent with the hash <hash> as given in the xml output or extended torrent info ('show <#>').");
	}
}
