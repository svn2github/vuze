/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * 
 * ConsoleInput.java
 * 
 * Created on 6. Oktober 2003, 23:26
 */

package org.gudy.azureus2.ui.console;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import java.text.DecimalFormat;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.DenyAllFilter;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.stats.StatsWriterFactory;
import org.gudy.azureus2.core3.stats.StatsWriterStreamer;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.UIConst;

import org.pf.file.FileFinder;

/**
 * @author Tobias Minich
 */
public class ConsoleInput extends Thread {
	
	private static final int TORRENTCOMMAND_START = 0;
	private static final int TORRENTCOMMAND_STOP = 1;
	private static final int TORRENTCOMMAND_REMOVE = 2;
	private static final int TORRENTCOMMAND_QUEUE = 3;
	private static final int TORRENTCOMMAND_STARTNOW = 4;
	
  GlobalManager gm;
  CommandReader br;
  PrintStream out;
  ArrayList torrents = null;
  File[] adds = null;
  boolean controlling;
  boolean running;
  String oldcommand = "sh t";
  
  /** Creates a new instance of ConsoleInput */
  public ConsoleInput(String con, GlobalManager _gm, Reader _in, PrintStream _out, boolean _controlling) {
    super("Console Input: " + con);
    gm = _gm;
    out = _out;
    controlling = _controlling;
    br = new CommandReader(_in, new OutputStreamWriter(_out));
    start();
  }

  public ConsoleInput(String con, GlobalManager _gm, InputStream _in, PrintStream _out, boolean _controlling) {
    this(con, _gm, new InputStreamReader(_in), _out, _controlling);
  }

  public static void printconsolehelp(PrintStream os) {
    os.println("Available console commands:");
    os.println("Command\t\t\t\tShort\tDescription");
    os.println(".\t\t\t\t\tRepeats last command (Initially 'show torrents').");
    os.println("help [torrents]\t\t\t?\tShow this help. 'torrents' shows info about the show torrents display.");
    os.println("log (on|off)\t\t\tl\tTurn on/off console logging");
    os.println("queue (#|all|hash <hash>)\t-\tQueue torrent(s).");
    os.println("remove (#|all|hash <hash>)\tr\tRemove torrent(s).");
    os.println("show (#|torrents)\t\tsh t\tShow info (Running torrents or further info on a certain torrent).");
    os.println("start (#|all|hash <hash>) [now]\ts\tStart torrent(s).");
    os.println("stop (#|all|hash <hash>)\th\tStop torrent(s).");
    os.println("ui <interface>\t\tu\tStart additional user interface.");
    os.println("xml [<file>]\t\t\t\tOutput stats in xml format (to <file> if given)");
    os.println("quit\t\t\t\tq\tShutdown Azureus");
  }

  private void quit(boolean finish) {
    if (finish)
      UIConst.shutdown();
  }
  
  private void commandHelp(String subcommand) {
  	out.println("> -----");
  	if (subcommand == null) {
  		printconsolehelp(out);
  	} else {
  		if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
  			out.println("# [state] PercentDone Name (Filesize) ETA\r\n\tDownSpeed / UpSpeed\tDownloaded/Uploaded\tConnectedSeeds(total) / ConnectedPeers(total)");
  			out.println();
  			out.println("States:");
  			out.println(" > Downloading");
  			out.println(" * Seeding");
  			out.println(" ! Stopped");
  			out.println(" . Waiting (for allocation/checking)");
  			out.println(" : Ready");
  			out.println(" - Queued");
  			out.println(" A Allocating");
  			out.println(" C Checking");
  			out.println(" E Error");
  			out.println(" I Initializing");
  			out.println(" ? Unknown");
  		} else
  			printconsolehelp(out);
  	}
  	out.println("> -----");
  }
  
  private void commandQuit(String subcommand) {
  	if (controlling) {
  		running = false;
  		quit(controlling);
  	} else {
  		if ((subcommand == null) || (!subcommand.equalsIgnoreCase("IAMSURE")))
  			out.println("> The 'quit' command exits azureus. Since this is a non-controlling shell thats probably not what you wanted. Use 'logout' to quit it or 'quit iamsure' to really exit azureus.");
  		else
  			quit(true);
  	}
  }
  
  private void commandXML(String subcommand) {
  	StatsWriterStreamer sws = StatsWriterFactory.createStreamer(gm);
  	if (subcommand == null) {
  		try {
  			out.println("> -----");
  			sws.write(out);
  			out.println("> -----");
  		} catch (Exception e) {
  			out.println("> Exception while trying to output xml stats:" + e.getMessage());
  		}
  	} else {
  		try {
  			FileOutputStream os = new FileOutputStream(subcommand);

  			try {

  				sws.write(os);

  			} finally {

  				os.close();
  			}
  			out.println("> XML stats successfully written to " + subcommand);
  		} catch (Exception e) {
  			out.println("> Exception while trying to write xml stats:" + e.getMessage());
  		}
  	}
  }
  
  private void commandUI(String subcommand) {
  	if (subcommand != null) {
  		UIConst.startUI(subcommand, null);
  	} else {
  		out.println("> Missing subcommand for 'ui'\r\n> ui syntax: ui <interface>");
  	}
  }
  
  private void commandShow(String subcommand) {
  	if (subcommand != null) {
  		if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
  			out.println("> -----");
  			torrents = (ArrayList) ((ArrayList) gm.getDownloadManagers()).clone();
  			DownloadManager dm;
  			int dmstate;
  			if (!torrents.isEmpty()) {
  				Iterator torrent = torrents.iterator();
  				long totalReceived = 0;
  				long totalSent = 0;
  				long totalDiscarded = 0;
  				int connectedSeeds = 0;
  				int connectedPeers = 0;
  				PEPeerManagerStats ps;
  				int nrTorrent = 0;
  				while (torrent.hasNext()) {
  					dm = (DownloadManager) torrent.next();
  					TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
  					dmstate = dm.getState();
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
  					nrTorrent += 1;
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

  					DownloadManagerStats stats = dm.getStats();

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
  					out.println(tstate);
  					//out.println(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(),
  					// true));
  					out.println();
  				}
  				out.println("Total Speed (down/up): " + DisplayFormatters.formatByteCountToKiBEtcPerSec(gm.getStats().getDownloadAverage()) + " / " + DisplayFormatters.formatByteCountToKiBEtcPerSec(gm.getStats().getUploadAverage()));

  				out.println("Transferred Volume (down/up/discarded): " + DisplayFormatters.formatByteCountToKiBEtc(totalReceived) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalSent) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalDiscarded));
  				out.println("Total Connected Peers (seeds/peers): " + Integer.toString(connectedSeeds) + " / " + Integer.toString(connectedPeers));
  			} else
  				out.println("No Torrents");
  			out.println("> -----");
  		} else {
  			try {
  				int number = Integer.parseInt(subcommand);
  				if ((torrents != null) && torrents.isEmpty()) {
  					out.println("> Command 'show': No torrents in list (try 'show torrents' first).");
  				} else {
  					String name;
  					DownloadManager dm;
  					if ((number > 0) && (number <= torrents.size())) {
  						dm = (DownloadManager) this.torrents.get(number - 1);
  						if (dm.getName() == null)
  							name = "?";
  						else
  							name = dm.getName();
  						TRTrackerClient trackerclient = dm.getTrackerClient();
  						out.println("> -----");
  						out.println("Info on Torrent #" + subcommand + " (" + name + ")");
  						out.println("- General Info -");
  						out.println("State: "+Integer.toString(dm.getState()));
  						out.println("- Tracker Info -");
  						if (trackerclient != null) {
  							out.println("URL: " + trackerclient.getTrackerUrl());
  							int time =trackerclient.getTimeUntilNextUpdate();
  							String timestr;
  							if ( time < 0 ){
  								timestr =  MessageText.getString("GeneralView.label.updatein.querying");
  							}else{
  								int minutes = time / 60;
  								int seconds = time % 60;
  								String strSeconds = "" + seconds;
  								if (seconds < 10){
  									strSeconds = "0" + seconds; //$NON-NLS-1$
  								}
  								timestr =  minutes + ":" + strSeconds; 
  							}
  							out.println("Time till next Update: " + timestr);
  							out.println("Status: " + trackerclient.getStatusString());
  						} else
  							out.println("  Not available");
  						out.println("> -----");
  					} else
  						out.println("> Command 'show': Torrent #" + subcommand + " unknown.");
  					
  				}	
  			} catch (Exception e) {
  				out.println("> Command 'show': Subcommand '" + subcommand + "' unknown.");
  			}
  		}
  	} else {
  		out.println("> Missing subcommand for 'show'\r\n> show syntax: show torrents");
  	}
  }
  
  private void commandAdd(String subcommand) {
  	if (subcommand != null) {
  		boolean scansubdir = false;
  		CommandLine commands = null;
  		CommandLineParser parser = new PosixParser();
  		Options options = new Options();
  		OptionGroup addy = new OptionGroup();
  		options.addOption("o", "output", true, "Output Directory.");
  		addy.addOption(new Option("r", "recurse", false, "Recurse Subdirs."));
  		addy.addOption(OptionBuilder.hasArgs().withDescription("Add found file nr x.").create('n'));
  		options.addOption(new Option("f", "find", false, "Only find files, don't add."));
  		options.addOptionGroup(addy);
  		try {
  			commands = parser.parse(options, subcommand.split(" "), true);
  		} catch (ParseException exp) {
  			out.println("> Parsing add commandline failed. Reason: " + exp.getMessage());
  		}
  		if (commands.hasOption('r'))
  			scansubdir = true;
  		String outputDir = "";
  		if (commands.hasOption('o'))
  			outputDir = commands.getOptionValue('o');
  		else
  			try {
  				outputDir = COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory");
  			} catch (Exception e) {
  				e.printStackTrace();
  			}

  			boolean Finding = false;

  			if (commands.hasOption('n')) {
  				String[] numbers = commands.getOptionValues('n');
  				out.println("> Numbers:");
  				for (int i = 0; i < numbers.length; i++)
  					out.println(numbers[i]);
  				String[] whatelse = commands.getArgs();
  				out.println("> Else:");
  				for (int i = 0; i < whatelse.length; i++)
  					out.println(whatelse[i]);
  			} else {
  				String[] whatelse = commands.getArgs();
  				for (int j = 0; j < whatelse.length; j++) {
  					if (whatelse[j].toUpperCase().startsWith("HTTP://")) {
  						/*
  						 * try { out.println("> Starting Download of "+whatelse[j]+"
  						 * ..."); HTTPDownloader dl = new HTTPDownloader(whatelse[j],
  						 * COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
  						 * String file = dl.download(); gm.addDownloadManager(file,
  						 * outputDir); out.println("> Download of "+whatelse[j]+"
  						 * succeeded"); } catch (Exception e) { out.println(">
  						 * Download of "+whatelse[j]+" failed");
  						 */
  						out.println("> Starting Download of " + whatelse[j] + " ...");
  						TorrentDownloaderFactory.downloadManaged(whatelse[j]);
  					} else {
  						File test = new File(whatelse[j]);
  						if (test.exists()) {
  							if (test.isDirectory()) {
  								File[] toadd = FileFinder.findFiles(whatelse[j], "*.torrent;*.tor", false);
  								if ((toadd != null) && (toadd.length > 0)) {
  									for (int i = 0; i < toadd.length; i++) {
  										gm.addDownloadManager(toadd[i].getAbsolutePath(), outputDir);
  										out.println("> '" + toadd[i].getAbsolutePath() + "' added.");
  									}
  								} else {
  									out.println("> Directory '" + whatelse[j] + "' seems to contain no torrent files.");
  								}
  							} else {
  								gm.addDownloadManager(whatelse[j], outputDir);
  								out.println("> '" + whatelse[j] + "' added.");
  							}
  						} else {
  							adds = FileFinder.findFiles(whatelse[j].substring(0, whatelse[j].lastIndexOf(System.getProperty("file.separator"))), whatelse[j].substring(whatelse[j].lastIndexOf(System.getProperty("file.separator")) + 1), false);
  							if ((adds != null) && (adds.length > 0)) {
  								out.println("> Found " + Integer.toString(adds.length) + " files:");
  								for (int i = 0; i < adds.length; i++) {
  									try {
  										out.println(adds[i].getCanonicalPath());
  									} catch (Exception e) {
  										out.println(adds[i].getAbsolutePath());
  									}
  								}
  							} else {
  								out.println("> No files found. Searched for '" + subcommand.substring(subcommand.lastIndexOf(System.getProperty("file.separator")) + 1) + "' in '" + subcommand.substring(0, subcommand.lastIndexOf(System.getProperty("file.separator"))));
  							}
  						}
  					}
  				}
  			}
  	} else {
  		out.println("> Missing subcommand for 'add'\r\n> add syntax: see 'help add'");
  	}
  }
  
  private boolean performTorrentCommand(int command, DownloadManager dm) {
  	switch (command) {
  		case TORRENTCOMMAND_START: {
  			//dm.startDownloadInitialized(true);
  			try {
  				dm.setState(DownloadManager.STATE_WAITING);
  			} catch (Exception e) {
  				e.printStackTrace(out);
  				return false;
  			}
  			return true;
  		}
  		case TORRENTCOMMAND_STOP: {
  			try {
  				dm.stopIt();
  			} catch (Exception e) {
  				e.printStackTrace(out);
  				return false;
  			}
  			return true;
  		}
  		case TORRENTCOMMAND_REMOVE: {
  			try {
  				dm.stopIt();
  				gm.removeDownloadManager(dm);
  			} catch(GlobalManagerDownloadRemovalVetoException e ){
  				out.println( "> Veto when removing torrent (" + e.getMessage() + ")");
  				return false;
			} catch (Exception e) {
  				e.printStackTrace(out);
  				return false;
  			}
  			return true;
  		}
  		case TORRENTCOMMAND_QUEUE: {
  			try {
		    	if (dm.getState() == DownloadManager.STATE_STOPPED)
      				dm.setState(DownloadManager.STATE_QUEUED);
      			else if (dm.getState() == DownloadManager.STATE_DOWNLOADING || dm.getState() == DownloadManager.STATE_SEEDING) 
      				dm.stopIt(DownloadManager.STATE_QUEUED);
				else
					return false;
  			} catch (Exception e) {
  				e.printStackTrace(out);
  				return false;
  			}
  			return true;
  		}
  		case TORRENTCOMMAND_STARTNOW: {
  			//dm.startDownloadInitialized(true);
  			try {
  				dm.setState(DownloadManager.STATE_WAITING);
				dm.startDownloadInitialized(true);
  			} catch (Exception e) {
  				e.printStackTrace(out);
  				return false;
  			}
  			return true;
  		}
  	}
  	return false;
  }
  
  private void commandTorrentCommand(int command, String subcommand) {
	String[] commands = {"start", "stop", "remove", "queue", "start"};
	String[] actions = {"Starting", "Stopping", "Removing", "Queueing", "Starting"};
  	if (subcommand != null) {
  		if ((torrents != null) && torrents.isEmpty()) {
  			out.println("> Command '"+commands[command]+"': No torrents in list (Maybe you forgot to 'show torrents' first).");
  		} else {
  			String name;
  			DownloadManager dm;
  			try {
  				int number = Integer.parseInt(subcommand);
  				if ((number > 0) && (number <= torrents.size())) {
  					dm = (DownloadManager) this.torrents.get(number - 1);
  					if (dm.getName() == null)
  						name = "?";
  					else
  						name = dm.getName();
					if (performTorrentCommand(command, dm))
	  					out.println("> "+actions[command]+" Torrent #" + subcommand + " (" + name + ") succeeded.");
					else
	  					out.println("> "+actions[command]+" Torrent #" + subcommand + " (" + name + ") failed.");
  				} else
  					out.println("> Command '"+commands[command]+"': Torrent #" + subcommand + " unknown.");
  			} catch (NumberFormatException e) {
  				if (subcommand.equalsIgnoreCase("all")) {
  					Iterator torrent = torrents.iterator();
  					int nr = 0;
  					while (torrent.hasNext()) {
  						dm = (DownloadManager) torrent.next();
  						if (dm.getName() == null)
  							name = "?";
  						else
  							name = dm.getName();
						if (performTorrentCommand(command, dm))
		  					out.println("> "+actions[command]+" Torrent #" + subcommand + " (" + name + ") succeeded.");
						else
		  					out.println("> "+actions[command]+" Torrent #" + subcommand + " (" + name + ") failed.");
  					}
  				} else if (subcommand.toUpperCase().startsWith("HASH")) {
  					String hash = subcommand.substring(subcommand.indexOf(" ") + 1);
  					List torrents = gm.getDownloadManagers();
  					boolean foundit = false;
  					if (!torrents.isEmpty()) {
  						Iterator torrent = torrents.iterator();
  						while (torrent.hasNext()) {
  							dm = (DownloadManager) torrent.next();
  							if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
  								if (dm.getName() == null)
  									name = "?";
  								else
  									name = dm.getName();
								if (performTorrentCommand(command, dm))
				  					out.println("> "+actions[command]+" Torrent " + hash + " (" + name + ") succeeded.");
								else
				  					out.println("> "+actions[command]+" Torrent " + hash + " (" + name + ") failed.");
  								foundit = true;
  								break;
  							}
  						}
  						if (!foundit)
  							out.println("> Command '"+commands[command]+"': Hash '" + hash + "' unknown.");
  					}
  				} else {
  					out.println("> Command '"+commands[command]+"': Subcommand '" + subcommand + "' unknown.");
  				}
  			}
  		}
  	} else {
  		out.println("> Missing subcommand for '"+commands[command]+"'\r\n> "+commands[command]+" syntax: "+commands[command]+" (#|all|hash <hash>)");
  	}
  }
  
  private void commandStart(String subcommand) {
	if (subcommand != null) {
		if (subcommand.toLowerCase().indexOf("now")!=-1) {
			String su = subcommand.replaceAll("now", "").trim();
			commandTorrentCommand(TORRENTCOMMAND_STARTNOW, su);
			return;
		}
	}
		
	commandTorrentCommand(TORRENTCOMMAND_START, subcommand);
  	/*if (subcommand != null) {
  		if ((torrents != null) && torrents.isEmpty()) {
  			out.println("> Command 'start': No torrents in list.");
  		} else {
  			String name;
  			DownloadManager dm;
  			try {
  				int number = Integer.parseInt(subcommand);
  				if ((number > 0) && (number <= torrents.size())) {
  					dm = (DownloadManager) this.torrents.get(number - 1);
  					if (dm.getName() == null)
  						name = "?";
  					else
  						name = dm.getName();
  					//dm.startDownloadInitialized(true);
  					dm.setState(DownloadManager.STATE_WAITING);
  					out.println("> Torrent #" + subcommand + " (" + name + ") started.");
  				} else
  					out.println("> Command 'start': Torrent #" + subcommand + " unknown.");
  			} catch (NumberFormatException e) {
  				if (subcommand.equalsIgnoreCase("all")) {
  					Iterator torrent = torrents.iterator();
  					int nr = 0;
  					while (torrent.hasNext()) {
  						dm = (DownloadManager) torrent.next();
  						if (dm.getName() == null)
  							name = "?";
  						else
  							name = dm.getName();
  						//dm.startDownloadInitialized(true);
  						dm.setState(DownloadManager.STATE_WAITING);
  						out.println("> Torrent #" + Integer.toString(++nr) + " (" + name + ") started.");
  					}
  				} else if (subcommand.toUpperCase().startsWith("HASH")) {
  					String hash = subcommand.substring(subcommand.indexOf(" ") + 1);
  					List torrents = gm.getDownloadManagers();
  					boolean foundit = false;
  					if (!torrents.isEmpty()) {
  						Iterator torrent = torrents.iterator();
  						while (torrent.hasNext()) {
  							dm = (DownloadManager) torrent.next();
  							if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
  								if (dm.getName() == null)
  									name = "?";
  								else
  									name = dm.getName();
  								dm.setState(DownloadManager.STATE_WAITING);
  								//dm.startDownloadInitialized(true);
  								out.println("> Torrent " + hash + " (" + name + ") started.");
  								foundit = true;
  								break;
  							}
  						}
  						if (!foundit)
  							out.println("> Command 'start': Hash '" + hash + "' unknown.");
  					}
  				} else {
  					out.println("> Command 'start': Subcommand '" + subcommand + "' unknown.");
  				}
  			}
  		}
  	} else {
  		out.println("> Missing subcommand for 'start'\r\n> start syntax: start (#|all)");
  	}*/
  }
  
  private void commandStop(String subcommand) {
	commandTorrentCommand(TORRENTCOMMAND_STOP, subcommand);
  	/*if (subcommand != null) {
  		if ((torrents != null) && torrents.isEmpty()) {
  			out.println("> Command 'stop': No torrents in list.");
  		} else {
  			String name;
  			DownloadManager dm;
  			try {
  				int number = Integer.parseInt(subcommand);
  				if ((number > 0) && (number <= torrents.size())) {
  					dm = (DownloadManager) this.torrents.get(number - 1);
  					if (dm.getName() == null)
  						name = "?";
  					else
  						name = dm.getName();
  					dm.stopIt();
  					out.println("> Torrent #" + subcommand + " (" + name + ") stopped.");
  				} else
  					out.println("> Command 'stop': Torrent #" + subcommand + " unknown.");
  			} catch (NumberFormatException e) {
  				if (subcommand.equalsIgnoreCase("all")) {
  					Iterator torrent = torrents.iterator();
  					int nr = 0;
  					while (torrent.hasNext()) {
  						dm = (DownloadManager) torrent.next();
  						if (dm.getName() == null)
  							name = "?";
  						else
  							name = dm.getName();
  						dm.stopIt();
  						out.println("> Torrent #" + Integer.toString(++nr) + " (" + name + ") stopped.");
  					}
  				} else if (subcommand.toUpperCase().startsWith("HASH")) {
  					String hash = subcommand.substring(subcommand.indexOf(" ") + 1);
  					List torrents = gm.getDownloadManagers();
  					boolean foundit = false;
  					if (!torrents.isEmpty()) {
  						Iterator torrent = torrents.iterator();
  						while (torrent.hasNext()) {
  							dm = (DownloadManager) torrent.next();
  							if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
  								if (dm.getName() == null)
  									name = "?";
  								else
  									name = dm.getName();
  								dm.stopIt();
  								out.println("> Torrent " + hash + " (" + name + ") stopped.");
  								foundit = true;
  								break;
  							}
  						}
  						if (!foundit)
  							out.println("> Command 'stop': Hash '" + hash + "' unknown.");
  					}
  				} else {
  					out.println("> Command 'stop': Subcommand '" + subcommand + "' unknown.");
  				}
  			}
  		}
  	} else {
  		out.println("> Missing subcommand for 'stop'\r\n> stop syntax: stop (#|all)");
  	}*/
  }
  
  private void commandRemove(String subcommand) {
	commandTorrentCommand(TORRENTCOMMAND_REMOVE, subcommand);
  	/*if (subcommand != null) {
  		if ((torrents != null) && torrents.isEmpty()) {
  			out.println("> Command 'remove': No torrents in list.");
  		} else {
  			String name;
  			DownloadManager dm;
  			try {
  				int number = Integer.parseInt(subcommand);
  				if ((number > 0) && (number <= torrents.size())) {
  					dm = (DownloadManager) this.torrents.get(number - 1);
  					if (dm.getName() == null)
  						name = "?";
  					else
  						name = dm.getName();
  					dm.stopIt();
  					try{
  						gm.removeDownloadManager(dm);
  						out.println("> Torrent #" + subcommand + " (" + name + ") removed.");
  					}catch(GlobalManagerDownloadRemovalVetoException e ){
  						out.println( "> Exception when removing torrent (" + e.getMessage() + ")");
  					}
  					oldcommand = null;
  				} else
  					out.println("> Command 'remove': Torrent #" + subcommand + " unknown.");
  			} catch (NumberFormatException e) {
  				if (subcommand.equalsIgnoreCase("all")) {
  					Iterator torrent = torrents.iterator();
  					int nr = 0;
  					while (torrent.hasNext()) {
  						dm = (DownloadManager) torrent.next();
  						if (dm.getName() == null)
  							name = "?";
  						else
  							name = dm.getName();
  						dm.stopIt();
  						try{
  							gm.removeDownloadManager(dm);
  							out.println("> Torrent #" + Integer.toString(++nr) + " (" + name + ") removed.");
  						}catch(GlobalManagerDownloadRemovalVetoException f ){
  							out.println( "> Exception when removing torrent (" + f.getMessage() + ")");
  						}
  					}
  				} else if (subcommand.toUpperCase().startsWith("HASH")) {
  					String hash = subcommand.substring(subcommand.indexOf(" ") + 1);
  					List torrents = gm.getDownloadManagers();
  					boolean foundit = false;
  					if (!torrents.isEmpty()) {
  						Iterator torrent = torrents.iterator();
  						while (torrent.hasNext()) {
  							dm = (DownloadManager) torrent.next();
  							if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
  								if (dm.getName() == null)
  									name = "?";
  								else
  									name = dm.getName();
  								dm.stopIt();
  								try{
  									gm.removeDownloadManager(dm);
  									out.println("> Torrent " + hash + " (" + name + ") removed.");
  								}catch(GlobalManagerDownloadRemovalVetoException f ){
  									out.println( "> Exception when removing torrent (" + f.getMessage() + ")");
  								}
  								
  								foundit = true;
  								break;
  							}
  						}
  						if (!foundit)
  							out.println("> Command 'remove': Hash '" + hash + "' unknown.");
  					}
  				} else {
  					out.println("> Command 'remove': Subcommand '" + subcommand + "' unknown.");
  				}
  			}
  		}
  	} else {
  		out.println("> Missing subcommand for 'remove'\r\n> remove syntax: remove (#|all)");
  	}*/
  }
  
  private void commandLog(String subcommand) {
  	Appender con = Logger.getRootLogger().getAppender("ConsoleAppender");
  	if ((con != null) && (subcommand != null)) {
  		if (subcommand.equalsIgnoreCase("off")) {
  			con.addFilter(new DenyAllFilter());
  			out.println("> Console logging off");
  		} else if (subcommand.equalsIgnoreCase("on")) {
  			con.clearFilters();
  			out.println("> Console logging on");
  		} else {
  			out.println("> Command 'log': Subcommand '" + subcommand + "' unknown.");
  		}
  	} else {
  		out.println("> Console logger not found or missing subcommand for 'log'\r\n> log syntax: log (on|off)");
  	}
  }
  	
  public void run() {
    String s = null;
    String command;
    String subcommand = "";
    running = true;
    while (running) {
      try {
        s = br.readLine();
      } catch (Exception e) {
        running = false;
      }
      if (s != null) {
        if (oldcommand != null) {
          if (s.equals("."))
            s = oldcommand;
        } else {
          if (s.equals("."))
            out.println("No old command. Remove commands are not repeated to prevent errors");
        }
        oldcommand = s;
        if (s.indexOf(" ") == -1) {
          command = s;
          subcommand = null;
        } else {
          command = s.substring(0, s.indexOf(" "));
          subcommand = s.substring(s.indexOf(" ") + 1);
        }
        if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("?")) {
        	commandHelp(subcommand);
        } else if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("q")) {
        	commandQuit(subcommand);
        } else if (command.equalsIgnoreCase("logout")) {
          running = false;
        } else if (command.equalsIgnoreCase("set") || command.equalsIgnoreCase("+")) {
          // Nothing for the moment
        } else if (command.equalsIgnoreCase("xml")) {
        	commandXML(subcommand);
        } else if (command.equalsIgnoreCase("ui") || command.equalsIgnoreCase("u")) {
        	commandUI(subcommand);
        } else if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("sh")) {
        	commandShow(subcommand);
        } else if (command.equalsIgnoreCase("add") || command.equalsIgnoreCase("a")) {
        	commandAdd(subcommand);
        } else if (command.equalsIgnoreCase("start") || command.equalsIgnoreCase("s")) {
        	commandStart(subcommand);
        } else if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("h")) {
        	commandStop(subcommand);
        } else if (command.equalsIgnoreCase("remove") || command.equalsIgnoreCase("r")) {
        	commandRemove(subcommand);
        } else if (command.equalsIgnoreCase("queue") || command.equalsIgnoreCase("-")) {
        	commandTorrentCommand(TORRENTCOMMAND_QUEUE, subcommand);
        } else if (command.equalsIgnoreCase("log") || command.equalsIgnoreCase("l")) {
        	commandLog(subcommand);
        } else if (command.equalsIgnoreCase("logtest")) {
          Logger.getLogger("azureus2").fatal("Logging test" + ((subcommand == null) ? "" : ": " + subcommand));
        } else {
          out.println("> Command '" + command + "' unknown (or . used without prior command)");
        }
      }
    }
  }
}
