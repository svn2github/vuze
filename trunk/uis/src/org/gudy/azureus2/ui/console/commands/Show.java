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
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
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
						totalReceived += dm.getStats().getTotalDataBytesReceived();
						totalSent += dm.getStats().getTotalDataBytesSent();
						totalDiscarded += ps.getTotalDiscarded();
						connectedSeeds += dm.getNbSeeds();
						connectedPeers += dm.getNbPeers();
					}				
					ci.out.print(((nrTorrent < 10) ? " " : "") + nrTorrent + " ");
					ci.out.println(getTorrentSummary(dm));
					ci.out.println();
				}
			}
			ci.out.println("Total Speed (down/up): " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getDataReceiveRate() + ci.gm.getStats().getProtocolReceiveRate() ) + " / " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getDataSendRate() + ci.gm.getStats().getProtocolSendRate() ));

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
	 * returns the summary details for the specified torrent. - we do this by obtaining
	 * the summary format and then performing variable substitution 
	 * NOTE: we currently reprocess
	 * the summary format string each time however we could pre-parse this once.. its 
	 * probably not that important though.
	 * @return
	 */
	protected String getTorrentSummary(DownloadManager dm) {
		StringBuffer tstate = new StringBuffer();
		String summaryFormat = getDefaultSummaryFormat();
		char lastch = '0';
		char []summaryChars = summaryFormat.toCharArray();
		for (int i = 0; i < summaryChars.length; i++) {
			char ch = summaryChars[i];
			if( ch == '%' && lastch != '\\' )
			{
				i++;
				if( i >= summaryChars.length )
					tstate.append('%');
				else
					tstate.append(expandVariable(summaryChars[i], dm));
			}
			else
				tstate.append(ch);
			
			lastch = ch;			
		}
		return tstate.toString();
	}

	/**
	 * expands the specified variable character into a string. <br>currently available
	 * variables that can be expanded are:<br>
	 * <hr>
	 * %a for state<br>
	 * %c percentage complete<br>
	 * %t torrent details - error message if error, otherwise torrent name<br>
	 * %z size<br>
	 * %e ETA<br>
	 * %r progress, if we have disabled some files<br>
	 * %d download speed<br>
	 * %u upload speed<br>
	 * %D amount downloaded<br>
	 * %U amount uploaded<br>
	 * %s connected seeds<br>
	 * %p connected peers<br>
	 * %S tracker seeds<br>
	 * %P tracker peers<br>
	 * @param variable variable character, eg: 'e' for ETA
	 * @param dm download manager object
	 * @return string expansion of the variable
	 */
	protected String expandVariable( char variable, DownloadManager dm )
	{
		switch( variable )
		{
			case 'a':
				return getShortStateString(dm.getState());
			case 'c':
				DecimalFormat df = new DecimalFormat("000.0%");
				return df.format(dm.getStats().getCompleted() / 1000.0);
			case 't':
				if (dm.getState() == DownloadManager.STATE_ERROR)
					return dm.getErrorDetails();
				else {
					if (dm.getDisplayName() == null)
						return "?";
					else
						return dm.getDisplayName();
				}
			case 'z':
				return DisplayFormatters.formatByteCountToKiBEtc(dm.getSize());
			case 'e':
				return DisplayFormatters.formatETA(dm.getStats().getETA());
			case 'r':
				long to = 0;
				long tot = 0;
				if (dm.getDiskManager() != null) {
					DiskManagerFileInfo files[] = dm.getDiskManager().getFiles();
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
				DecimalFormat df1 = new DecimalFormat("000.0%");
				if (tot > 0) {
					return "      ("+df1.format(to * 1.0 / tot)+")";
				} else
					return "\t";
			case 'd':
				return DisplayFormatters.formatByteCountToKiBEtcPerSec(dm.getStats().getDataReceiveRate());
			case 'u':
				return DisplayFormatters.formatByteCountToKiBEtcPerSec(dm.getStats().getDataSendRate());
			case 'D':
				return DisplayFormatters.formatDownloaded(dm.getStats());
			case 'U':
				return DisplayFormatters.formatByteCountToKiBEtc(dm.getStats().getTotalDataBytesSent());
			case 's':
				return Integer.toString(dm.getNbSeeds());
			case 'p':
				return Integer.toString(dm.getNbPeers());	
			case 'I':
				int downloadSpeed = dm.getStats().getDownloadRateLimitBytesPerSecond();
				if( downloadSpeed <= 0 )
					return "";
				return "(max " + DisplayFormatters.formatByteCountToKiBEtcPerSec(downloadSpeed) + ")";
			case 'O':
				int uploadSpeed = dm.getStats().getUploadRateLimitBytesPerSecond();
				if( uploadSpeed <= 0 )
					return "";
				return "(max " + DisplayFormatters.formatByteCountToKiBEtcPerSec(uploadSpeed) + ")";
				
			case 'S':
			case 'P':
				TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
				if (hd == null || !hd.isValid())
					return "?";
				else
				{
					if( variable == 'S' )
						return Integer.toString(hd.getSeeds());
					else
						return Integer.toString(hd.getPeers());
				}
			default: 
				return "??" + variable + "??";
		}
	}
	
	/**
	 * returns the format string (in printf style format) to use for displaying the torrent summary
	 * @return
	 */
	protected String getDefaultSummaryFormat()
	{
		return "[%a] %c\t%t (%z) ETA: %e\r\n%r\tSpeed: %d%I / %u%O\tAmount: %D / %U\tConnections: %s(%S) / %p(%P)";
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
		TRTrackerAnnouncer trackerclient = dm.getTrackerClient();
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
