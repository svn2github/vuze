/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Show.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 */
public class Show extends IConsoleCommand {
	
	private static final class TorrentComparator implements Comparator {
		public final int compare(Object a, Object b) {
			DownloadManager aDL = (DownloadManager) a;
			DownloadManager bDL = (DownloadManager) b;
			boolean aIsComplete = aDL.getStats().getDownloadCompleted(false) == 1000;
			boolean bIsComplete = bDL.getStats().getDownloadCompleted(false) == 1000;
			if (aIsComplete && !bIsComplete)
				return 1;
			if (!aIsComplete && bIsComplete)
				return -1;
			return aDL.getPosition() - bDL.getPosition();
		}
	}

	public Show()
	{
		super( new String[] { "show", "sh" });
	}

	public String getCommandDescriptions() {
		return("show [<various options>]\tsh\tShow info. Use without parameter to get a list of available options.");
	}

	public void printHelp(PrintStream out, List args) {
		out.println("> -----");
		out.println("'show' options: ");
		out.println("<#>\t\t\t\tFurther info on a single torrent. Run 'show torrents' first for the number.");
		out.println("options\t\t\to\tShow list of options for 'set' (also available by 'set' without parameters).");
		out.println("files\t\t\tf\tShow list of files found from the 'add -f' command (also available by 'add -l')");
		out.println("torrents [options]\tt\tShow list of torrents. torrent options mayb be any (or none) of:");
		out.println("\t\tactive\t\ta\tShow only active torrents.");
		out.println("\t\tcomplete\tc\tShow only complete torrents.");
		out.println("\t\tincomplete\ti\tShow only incomplete torrents.");
		out.println("> -----");
	}

	public void execute(String commandName, ConsoleInput ci, List args) {
		if( args.isEmpty() )
		{
			printHelp(ci.out, args);
			return;
		}
		String subCommand = (String) args.remove(0);
		if (subCommand.equalsIgnoreCase("options") || subCommand.equalsIgnoreCase("o")) {
			ci.invokeCommand("set", null);
		} else if(subCommand.equalsIgnoreCase("files") || subCommand.equalsIgnoreCase("f")) {
			ci.invokeCommand("add", Arrays.asList( new String[] { "--list"} ));
		} else if (subCommand.equalsIgnoreCase("torrents") || subCommand.equalsIgnoreCase("t")) {
			ci.out.println("> -----");
			ci.torrents.clear();
			ci.torrents.addAll(ci.gm.getDownloadManagers());
			Collections.sort(ci.torrents, new TorrentComparator());

			if (ci.torrents.isEmpty()) {
				ci.out.println("No Torrents");
				ci.out.println("> -----");
				return;
			}
			
			DownloadManager dm;
			Iterator torrent = ci.torrents.iterator();
			long totalReceived = 0;
			long totalSent = 0;
			long totalDiscarded = 0;
			int connectedSeeds = 0;
			int connectedPeers = 0;
			PEPeerManagerStats ps;
			int nrTorrent = 0;
			boolean bShowOnlyActive = false;
			boolean bShowOnlyComplete = false;
			boolean bShowOnlyIncomplete = false;
			for (Iterator iter = args.iterator(); iter.hasNext();) {
				String arg = (String) iter.next();
				if ("active".equalsIgnoreCase(arg) || "a".equalsIgnoreCase(arg)) {
					bShowOnlyActive = true;
				} else if ("complete".equalsIgnoreCase(arg) || "c".equalsIgnoreCase(arg)) {
					bShowOnlyComplete = true;
				} else if ("incomplete".equalsIgnoreCase(arg) || "i".equalsIgnoreCase(arg)) {
					bShowOnlyIncomplete = true;
				}
			}

			while (torrent.hasNext()) {
				nrTorrent++;
				dm = (DownloadManager) torrent.next();
				DownloadManagerStats stats = dm.getStats();

				boolean bDownloadCompleted = stats.getDownloadCompleted(false) == 1000;
				boolean bCanShow = ((bShowOnlyComplete == bShowOnlyIncomplete) || (bDownloadCompleted && bShowOnlyComplete) || (!bDownloadCompleted && bShowOnlyIncomplete));

				if (bCanShow && bShowOnlyActive) {
					int dmstate = dm.getState();
					bCanShow = (dmstate == DownloadManager.STATE_SEEDING) || (dmstate == DownloadManager.STATE_DOWNLOADING) || (dmstate == DownloadManager.STATE_CHECKING) || (dmstate == DownloadManager.STATE_INITIALIZING) || (dmstate == DownloadManager.STATE_ALLOCATING);
				}

				if (bCanShow) {
					try {
						ps = dm.getPeerManager().getStats();
					} catch (Exception e) {
						ps = null;
					}
					if (ps != null) {
						totalReceived += dm.getStats().getDownloaded();
						totalSent += dm.getStats().getUploaded();
						totalDiscarded += ps.getTotalDiscarded();
						connectedSeeds += dm.getNbSeeds();
						connectedPeers += dm.getNbPeers();
					}				
					ci.out.print(((nrTorrent < 10) ? " " : "") + nrTorrent + " ");
					ci.out.println(getTorrentSummary(dm));
					ci.out.println();
				}
			}
			ci.out.println("Total Speed (down/up): " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getDownloadAverage()) + " / " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getUploadAverage()));

			ci.out.println("Transferred Volume (down/up/discarded): " + DisplayFormatters.formatByteCountToKiBEtc(totalReceived) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalSent) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalDiscarded));
			ci.out.println("Total Connected Peers (seeds/peers): " + Integer.toString(connectedSeeds) + " / " + Integer.toString(connectedPeers));
			ci.out.println("> -----");
		} else {
			if ((ci.torrents == null) || (ci.torrents != null) && ci.torrents.isEmpty()) {
				ci.out.println("> Command 'show': No torrents in list (try 'show torrents' first).");
				return;
			}
			try {
				int number = Integer.parseInt(subCommand);
				if ((number == 0) || (number > ci.torrents.size())) {
					ci.out.println("> Command 'show': Torrent #" + number + " unknown.");
					return;
				}
				DownloadManager dm = (DownloadManager) ci.torrents.get(number - 1);
				printTorrentDetails(ci.out, dm, number);
			}
			catch (Exception e) {
				ci.out.println("> Command 'show': Subcommand '" + subCommand + "' unknown.");
				return;
			}				
		} 
	}

	/**
	 * returns the summary details for the specified torrent
	 * @return
	 */
	private static String getTorrentSummary(DownloadManager dm) {
		StringBuffer tstate = new StringBuffer();
		int dmstate = dm.getState();
		tstate.append("[");
		tstate.append(getShortStateString(dmstate));
		tstate.append("] ");

		DecimalFormat df = new DecimalFormat("000.0%");
		DownloadManagerStats stats = dm.getStats();
		tstate.append(df.format(stats.getCompleted() / 1000.0));
		tstate.append("\t");
		if (dmstate == DownloadManager.STATE_ERROR)
			tstate.append(dm.getErrorDetails());
		else {
			if (dm.getDisplayName() == null)
				tstate.append("?");
			else
				tstate.append(dm.getDisplayName());
		}
		tstate.append(" (" + DisplayFormatters.formatByteCountToKiBEtc(dm.getSize()) + ") ETA:" + DisplayFormatters.formatETA(stats.getETA()));
		tstate.append("\r\n");
		DiskManager dim = dm.getDiskManager();
		long to = 0;
		long tot = 0;
		if (dim != null) {
			DiskManagerFileInfo files[] = dim.getFiles();
			if (files != null) {
				if (files.length>1) { 
					int c=0;
					for (int i = 0; i < files.length; i++) {
						if (files[i] != null) {
							if (!files[i].isSkipped()) {
								c += 1;
								tot += files[i].getLength();
								to += files[i].getDownloaded();
							}
						}
					}
					if (c == files.length)
						tot = 0;
				}
			}
		}
		if (tot > 0) {
			tstate.append("      ("+df.format(to * 1.0 / tot)+")");
		} else
			tstate.append("\t");
		tstate.append("\tSpeed: ");
		tstate.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDownloadAverage()) + " / ");
		tstate.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getUploadAverage()) + "\tAmount: ");
		tstate.append(DisplayFormatters.formatDownloaded(stats) + " / ");
		tstate.append(DisplayFormatters.formatByteCountToKiBEtc(stats.getUploaded()) + "\tConnections: ");
		TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
		if (hd == null || !hd.isValid()) {
			tstate.append(Integer.toString(dm.getNbSeeds()) + "(?) / ");
			tstate.append(Integer.toString(dm.getNbPeers()) + "(?)");
		} else {
			tstate.append(Integer.toString(dm.getNbSeeds()) + "(" + Integer.toString(hd.getSeeds()) + ") / ");
			tstate.append(Integer.toString(dm.getNbPeers()) + "(" + Integer.toString(hd.getPeers()) + ")");
		}
		return tstate.toString();
	}

	/**
	 * returns a string representation of the specified state number
	 * suitable for inclusion in a torrent summary
	 * @param dmstate
	 * @return
	 */
	private static String getShortStateString(int dmstate) {
		switch( dmstate )
		{
		case DownloadManager.STATE_INITIALIZING:
			return("I");
		case DownloadManager.STATE_ALLOCATING:
			return("A");
		case DownloadManager.STATE_CHECKING:
			return("C");
		case DownloadManager.STATE_DOWNLOADING:
			return(">");
		case DownloadManager.STATE_ERROR:
			return("E");
		case DownloadManager.STATE_SEEDING:
			return("*");
		case DownloadManager.STATE_STOPPED:
			return("!");
		case DownloadManager.STATE_WAITING:
			return(".");
		case DownloadManager.STATE_READY:
			return(":");
		case DownloadManager.STATE_QUEUED:
			return("-");
		default:
			return("?");
		}
	}

	/**
	 * prints out the full details of a particular torrent
	 * @param out
	 * @param dm
	 * @param torrentNum
	 */
	private static void printTorrentDetails( PrintStream out, DownloadManager dm, int torrentNum)
	{
		String name = dm.getDisplayName();
		if (name == null)
			name = "?";
		out.println("> -----");
		out.println("Info on Torrent #" + torrentNum + " (" + name + ")");
		out.println("- General Info -");
		String[] health = { "- no info -", "stopped", "no remote connections", "no tracker", "OK", "ko" };
		try {
			out.println("Health: " + health[dm.getHealthStatus()]);
		} catch (Exception e) {
			out.println("Health: " + health[0]);
		}
		out.println("State: " + Integer.toString(dm.getState()));
		if (dm.getState() == DownloadManager.STATE_ERROR)
			out.println("Error: " + dm.getErrorDetails());
		out.println("Hash: " + ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true));
		out.println("- Torrent file -");
		out.println("Filename: " + dm.getTorrentFileName());
		out.println("Created By: " + dm.getTorrentCreatedBy());
		out.println("Comment: " + dm.getTorrentComment());
		out.println("- Tracker Info -");
		TRTrackerClient trackerclient = dm.getTrackerClient();
		if (trackerclient != null) {
			out.println("URL: " + trackerclient.getTrackerUrl());
			String timestr;
			try {
				int time = trackerclient.getTimeUntilNextUpdate();
				if (time < 0) {
					timestr = MessageText.getString("GeneralView.label.updatein.querying");
				} else {
					int minutes = time / 60;
					int seconds = time % 60;
					String strSeconds = "" + seconds;
					if (seconds < 10) {
						strSeconds = "0" + seconds; //$NON-NLS-1$
					}
					timestr = minutes + ":" + strSeconds;
				}
			} catch (Exception e) {
				timestr = "unknown";
			}
			out.println("Time till next Update: " + timestr);
			out.println("Status: " + trackerclient.getStatusString());
		} else
			out.println("  Not available");
		
		out.println("- Files Info -");
		DiskManager dim = dm.getDiskManager();
		if (dim != null) {
			DiskManagerFileInfo files[] = dim.getFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					out.print(((i < 9) ? "   " : "  ") + Integer.toString(i+1) + " (");
					String tmp = ">";
					if (files[i].isPriority())
						tmp = "+";
					if (files[i].isSkipped())
						tmp = "!";
					out.print(tmp + ") ");
					if (files[i] != null) {
						long fLen = files[i].getLength();
						if (fLen > 0) {
							DecimalFormat df = new DecimalFormat("000.0%");
							out.print(df.format(files[i].getDownloaded() * 1.0 / fLen));
							out.println("\t" + files[i].getName());
						} else
							out.println("Info not available.");
					} else
						out.println("Info not available.");
				}
			} else
				out.println("  Info not available.");
		} else
			out.println("  Info not available.");
		out.println("> -----");
	}
}
