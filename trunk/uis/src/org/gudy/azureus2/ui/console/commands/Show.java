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

import java.text.DecimalFormat;
import java.util.ArrayList;
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
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Show implements IConsoleCommand {
	
	public static void command(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())){
			String[] sSubcommands = new String[args.size()];
			args.toArray(sSubcommands);
			for (int i = 0; i < sSubcommands.length; i++)
				sSubcommands[i] = sSubcommands[i].trim();
			if (sSubcommands[0].equalsIgnoreCase("options") || sSubcommands[0].equalsIgnoreCase("o")) {
				ci.invokeCommand("set", null);
			} else if (sSubcommands[0].equalsIgnoreCase("torrents") || sSubcommands[0].equalsIgnoreCase("t")) {
				ci.out.println("> -----");
				ci.torrents = (ArrayList) ((ArrayList) ci.gm.getDownloadManagers()).clone();
				Collections.sort(ci.torrents, new Comparator() {
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
				});

				DownloadManager dm;
				int dmstate;
				if (!ci.torrents.isEmpty()) {
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
					for (int i = 1; i < sSubcommands.length; i++) {
						if (sSubcommands[i].equalsIgnoreCase("active") || sSubcommands[i].equalsIgnoreCase("a")) {
							bShowOnlyActive = true;
						} else if (sSubcommands[i].equalsIgnoreCase("complete") || sSubcommands[i].equalsIgnoreCase("c")) {
							bShowOnlyComplete = true;
						} else if (sSubcommands[i].equalsIgnoreCase("incomplete") || sSubcommands[i].equalsIgnoreCase("i")) {
							bShowOnlyIncomplete = true;
						}
					}

					while (torrent.hasNext()) {
						nrTorrent += 1;
						dm = (DownloadManager) torrent.next();
						DownloadManagerStats stats = dm.getStats();
						dmstate = dm.getState();

						boolean bDownloadCompleted = stats.getDownloadCompleted(false) == 1000;
						boolean bCanShow = ((bShowOnlyComplete == bShowOnlyIncomplete) || (bDownloadCompleted && bShowOnlyComplete) || (!bDownloadCompleted && bShowOnlyIncomplete));

						if (bCanShow && bShowOnlyActive) {
							bCanShow = (dmstate == DownloadManager.STATE_SEEDING) || (dmstate == DownloadManager.STATE_DOWNLOADING) || (dmstate == DownloadManager.STATE_CHECKING) || (dmstate == DownloadManager.STATE_INITIALIZING) || (dmstate == DownloadManager.STATE_ALLOCATING);
						}

						if (bCanShow) {
							TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
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
							String tstate = ((nrTorrent < 10) ? " " : "") + Integer.toString(nrTorrent) + " [";
							if (dmstate == DownloadManager.STATE_INITIALIZING)
								tstate += "I";
							else if (dmstate == DownloadManager.STATE_ALLOCATING)
								tstate += "A";
							else if (dmstate == DownloadManager.STATE_CHECKING)
								tstate += "C";
							else if (dmstate == DownloadManager.STATE_DOWNLOADING)
								tstate += ">";
							else if (dmstate == DownloadManager.STATE_ERROR)
								tstate += "E";
							else if (dmstate == DownloadManager.STATE_SEEDING)
								tstate += "*";
							else if (dmstate == DownloadManager.STATE_STOPPED)
								tstate += "!";
							else if (dmstate == DownloadManager.STATE_WAITING)
								tstate += ".";
							else if (dmstate == DownloadManager.STATE_READY)
								tstate += ":";
							else if (dmstate == DownloadManager.STATE_QUEUED)
								tstate += "-";
							else
								tstate += "?";
							tstate += "] ";
							DecimalFormat df = new DecimalFormat("000.0%");

							tstate += df.format(stats.getCompleted() / 1000.0);
							tstate += "\t";
							if (dmstate == DownloadManager.STATE_ERROR)
								tstate += dm.getErrorDetails();
							else {
								if (dm.getName() == null)
									tstate += "?";
								else
									tstate += dm.getName();
							}
							tstate += " (" + DisplayFormatters.formatByteCountToKiBEtc(dm.getSize()) + ") ETA:" + DisplayFormatters.formatETA(stats.getETA()) + "\r\n\t\tSpeed: ";
							tstate += DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDownloadAverage()) + " / ";
							tstate += DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getUploadAverage()) + "\tAmount: ";
							tstate += DisplayFormatters.formatDownloaded(stats) + " / ";
							tstate += DisplayFormatters.formatByteCountToKiBEtc(stats.getUploaded()) + "\tConnections: ";
							if (hd == null || !hd.isValid()) {
								tstate += Integer.toString(dm.getNbSeeds()) + "(?) / ";
								tstate += Integer.toString(dm.getNbPeers()) + "(?)";
							} else {
								tstate += Integer.toString(dm.getNbSeeds()) + "(" + Integer.toString(hd.getSeeds()) + ") / ";
								tstate += Integer.toString(dm.getNbPeers()) + "(" + Integer.toString(hd.getPeers()) + ")";
							}
							ci.out.println(tstate);
							//out.println(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(),
							// true));
							ci.out.println();
						}
					}
					ci.out.println("Total Speed (down/up): " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getDownloadAverage()) + " / " + DisplayFormatters.formatByteCountToKiBEtcPerSec(ci.gm.getStats().getUploadAverage()));

					ci.out.println("Transferred Volume (down/up/discarded): " + DisplayFormatters.formatByteCountToKiBEtc(totalReceived) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalSent) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalDiscarded));
					ci.out.println("Total Connected Peers (seeds/peers): " + Integer.toString(connectedSeeds) + " / " + Integer.toString(connectedPeers));
				} else
					ci.out.println("No Torrents");
				ci.out.println("> -----");
			} else {
				try {
					int number = Integer.parseInt(sSubcommands[0]);
					if ((ci.torrents == null) || (ci.torrents != null) && ci.torrents.isEmpty()) {
						ci.out.println("> Command 'show': No torrents in list (try 'show torrents' first).");
					} else {
						String name;
						DownloadManager dm;
						if ((number > 0) && (number <= ci.torrents.size())) {
							dm = (DownloadManager) ci.torrents.get(number - 1);
							if (dm.getName() == null)
								name = "?";
							else
								name = dm.getName();
							TRTrackerClient trackerclient = dm.getTrackerClient();
							ci.out.println("> -----");
							ci.out.println("Info on Torrent #" + sSubcommands[0] + " (" + name + ")");
							ci.out.println("- General Info -");
							String[] health = { "- no info -", "stopped", "no remote connections", "no tracker", "OK", "ko" };
							try {
								ci.out.println("Health: " + health[dm.getHealthStatus()]);
							} catch (Exception e) {
								ci.out.println("Health: " + health[0]);
							}
							ci.out.println("State: " + Integer.toString(dm.getState()));
							if (dm.getState() == DownloadManager.STATE_ERROR)
								ci.out.println("Error: " + dm.getErrorDetails());
							ci.out.println("Hash: " + ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true));
							ci.out.println("- Torrent file -");
							ci.out.println("Filename: " + dm.getTorrentFileName());
							ci.out.println("Created By: " + dm.getTorrentCreatedBy());
							ci.out.println("Comment: " + dm.getTorrentComment());
							ci.out.println("- Tracker Info -");
							if (trackerclient != null) {
								ci.out.println("URL: " + trackerclient.getTrackerUrl());
								int time = trackerclient.getTimeUntilNextUpdate();
								String timestr;
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
								ci.out.println("Time till next Update: " + timestr);
								ci.out.println("Status: " + trackerclient.getStatusString());
							} else
								ci.out.println("  Not available");
							ci.out.println("- Files Info -");
							DiskManager dim = dm.getDiskManager();
							if (dim != null) {
								DiskManagerFileInfo files[] = dim.getFiles();
								if (files != null) {
									for (int i = 0; i < files.length; i++) {
										ci.out.print(((i < 10) ? "   " : "  ") + Integer.toString(i+1) + " (");
										String tmp = ">";
										if (files[i].isPriority())
											tmp = "+";
										if (files[i].isSkipped())
											tmp = "!";
										ci.out.print(tmp + ") ");
										if (files[i] != null) {
											long fLen = files[i].getLength();
											if (fLen > 0) {
												DecimalFormat df = new DecimalFormat("000.0%");
												ci.out.print(df.format(files[i].getDownloaded() * 1.0 / fLen));
												ci.out.println("\t" + files[i].getName());
											} else
												ci.out.println("Info not available.");
										} else
											ci.out.println("Info not available.");
									}
								} else
									ci.out.println("  Info not available.");
							} else
								ci.out.println("  Info not available.");
							ci.out.println("> -----");
						} else
							ci.out.println("> Command 'show': Torrent #" + sSubcommands[0] + " unknown.");

					}
				} catch (Exception e) {
					ci.out.println("> Command 'show': Subcommand '" + sSubcommands[0] + "' unknown.");
				}
			}
		} else {
			ci.out.println("> -----");
			ci.out.println("'show' options: ");
			ci.out.println("<#>\t\t\t\tFurther info on a single torrent. Run 'show torrents' first for the number.");
			ci.out.println("options\t\t\to\tShow list of options for 'set' (also available by 'set' without parameters).");
			ci.out.println("torrents [options]\tt\tShow list of torrents. torrent options mayb be any (or none) of:");
			ci.out.println("\t\tactive\t\ta\tShow only active torrents.");
			ci.out.println("\t\tcomplete\tc\tShow only complete torrents.");
			ci.out.println("\t\tincomplete\ti\tShow only incomplete torrents.");
			ci.out.println("> -----");
		}
	}
	
	public static void RegisterCommands() {
		try {
			ConsoleInput.RegisterCommand("show", Show.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("sh", Show.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("show [<various options>]\tsh\tShow info. Use without parameter to get a list of available options.");
		} catch (Exception e) {e.printStackTrace();}
	}
}
