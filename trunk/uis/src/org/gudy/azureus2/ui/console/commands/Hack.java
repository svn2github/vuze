/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Hack.java
 * 
 * Created on 22.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.ui.console.ConsoleInput;
/**
 * @author Tobias Minich
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Hack implements IConsoleCommand {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.ui.console.commands.IConsoleCommand#RegisterCommands(org.gudy.azureus2.ui.console.ConsoleInput)
	 */
	private static void commandHelp(ConsoleInput ci, String command) {
		ci.out.println("> -----");
		ci.out.println("'hack' syntax:");
		if (command != null) {
			if (command.equalsIgnoreCase("file") || command.equalsIgnoreCase("f")) {
				ci.out.println("hack <torrent id> file <#> <priority>");
				ci.out.println();
				ci.out.println("<#> Number of the file.");
				ci.out.println();
				ci.out.println("<priority> can be one of the following:");
				ci.out.println("normal\t\tn\tNormal Priority");
				ci.out.println("high\t\th|+\tHigh Priority");
				ci.out.println("nodownload\t!|-\tDon't download this file.");
				ci.out.println("> -----");
				return;
			}
			if (command.equalsIgnoreCase("tracker") || command.equalsIgnoreCase("t")) {
				ci.out.println("hack <torrent id> tracker [command] <new value>");
				ci.out.println();
				ci.out.println("[command] can be one of the following:");
				ci.out.println("url\t\tu\tChange the full URL (Note: you have to include the '/announce' part).");
				ci.out.println("host\t\th\tChange the host.");
				ci.out.println("port\t\tp\tChange the port.");
				ci.out.println();
				ci.out.println("You can also omit [command] and only give a new full URL (just like the [command] 'url').");
				ci.out.println("> -----");
				return;
			}
		}
		ci.out.println("hack <torrent id> <command> <command options>");
		ci.out.println();
		ci.out.println("<torrent id> can be one of the following:");
		ci.out.println("<#>\t\tNumber of a torrent. You have to use 'show torrents' first as the number is taken from there.");
		ci.out.println("hash <hash>\tApplied to torrent with the hash <hash> as given in the xml output or extended torrent info ('show <#>').");
		ci.out.println("help\t\tDetailed help for <command>");
		ci.out.println();
		ci.out.println("Available <command>s:");
		ci.out.println("file\t\tf\tModify priority of a single file of a batch torrent.");
		ci.out.println("tracker\t\tt\tModify Tracker URL of a torrent.");
		ci.out.println("> -----");
	}
	
	public static void command(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())) {
			String[] sSubcommands = new String[args.size()];
			args.toArray(sSubcommands);
			DownloadManager dm = null;
			int commandoffset = 0;
			if (sSubcommands[0].equalsIgnoreCase("hash")) {
				String hash = sSubcommands[1];
				List torrents = ci.gm.getDownloadManagers();
				boolean foundit = false;
				if (!torrents.isEmpty()) {
					Iterator torrent = torrents.iterator();
					while (torrent.hasNext()) {
						dm = (DownloadManager) torrent.next();
						if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
							foundit = true;
							break;
						}
					}
				}
				if (!foundit) {
					ci.out.println("> Command 'hack': Hash '" + hash + "' unknown.");
					return;
				}
				commandoffset = 2;
			} else if (sSubcommands[0].equalsIgnoreCase("help") || sSubcommands[0].equalsIgnoreCase("?")) {
				try {
					commandHelp(ci, sSubcommands[1]);
				} catch (Exception e) {
					commandHelp(ci, null);
				}
				return;
			} else {
				try {
					int number = Integer.parseInt(sSubcommands[0]);
					if ((ci.torrents != null) && ci.torrents.isEmpty()) {
						ci.out.println("> Command 'hack': No torrents in list.");
						return;
					} else {
						if ((number > 0) && (number <= ci.torrents.size())) {
							dm = (DownloadManager) ci.torrents.get(number - 1);
						} else {
							ci.out.println("> Command 'hack': Torrent #" + sSubcommands[0] + " unknown.");
							return;
						}
					}
					commandoffset = 1;
				} catch (NumberFormatException e) {
					ci.out.println("> Command 'hack': First parameter '" + sSubcommands[0] + "' unknown.");
					return;
				}
			}
			if (dm == null) {
				ci.out.println("> Command 'hack': Couldn't determine Torrent.");
				return;
			}
			if (sSubcommands[commandoffset].equalsIgnoreCase("file") || sSubcommands[commandoffset].equalsIgnoreCase("f")) {
				if (sSubcommands.length < (commandoffset + 3)) {
					ci.out.println("> Command 'hack': Not enough parameters for command parameter '" + sSubcommands[commandoffset] + "'.");
					return;
				}
				try {
					DiskManager disk = dm.getDiskManager();
					DiskManagerFileInfo files[] = disk.getFiles();
					int file = Integer.parseInt(sSubcommands[commandoffset + 1]);
					String c = sSubcommands[commandoffset + 2];
					if (c.equalsIgnoreCase("normal") || c.equalsIgnoreCase("n")) {
						files[file - 1].setSkipped(false);
						files[file - 1].setPriority(false);
						ci.out.println("> Set file '"+files[file - 1].getName()+"' to normal priority.");
					} else if (c.equalsIgnoreCase("high") || c.equalsIgnoreCase("h") || c.equalsIgnoreCase("+")) {
						files[file - 1].setSkipped(false);
						files[file - 1].setPriority(true);
						ci.out.println("> Set file '"+files[file - 1].getName()+"' to high priority.");
					} else if (c.equalsIgnoreCase("nodownload") || c.equalsIgnoreCase("!") || c.equalsIgnoreCase("-")) {
						files[file - 1].setSkipped(true);
						files[file - 1].setPriority(false);
						ci.out.println("> Stopped to download file '"+files[file - 1].getName()+"'.");
					} else {
						ci.out.println("> Command 'hack': Unknown priority '" + c + "' for command parameter '" + sSubcommands[commandoffset] + "'.");
						return;
					}
				} catch (Exception e) {
					ci.out.println("> Command 'hack': Exception while parsing command parameter '" + sSubcommands[commandoffset] + "': " + e.getMessage());
					return;
				}
			} else if (sSubcommands[commandoffset].equalsIgnoreCase("tracker") || sSubcommands[commandoffset].equalsIgnoreCase("t")) {
				if (sSubcommands.length < (commandoffset + 1)) {
					ci.out.println("> Command 'hack': Not enough parameters for command parameter '" + sSubcommands[commandoffset] + "'.");
					return;
				}
				try {
					String trackercommand = sSubcommands[commandoffset+1];
					TRTrackerClient client = dm.getTrackerClient();
					//ci.out.println("> Command 'hack': Debug: '"+trackercommand+"'");
					if (client == null) {
						ci.out.println("> Command 'hack': Tracker interface not available.");
						return;
					}
					if (trackercommand.equalsIgnoreCase("url") || trackercommand.equalsIgnoreCase("u")) {
						ci.out.println("> Command 'hack': Debug: url");
						if (sSubcommands.length < (commandoffset + 2)) {
							ci.out.println("> Command 'hack': Not enough parameters for command parameter '" + sSubcommands[commandoffset] + "'.");
							return;
						}
						try {
							URI test = new URI(sSubcommands[commandoffset+2]);
						} catch (Exception e) {
							ci.out.println("> Command 'hack': Parsing tracker url failed: "+e.getMessage());
							return;
						}
						client.setTrackerUrl(new URL(sSubcommands[commandoffset+2]));
						ci.out.println("> Set Tracker URL for '"+dm.getTorrentSaveDirAndFile()+"' to '"+sSubcommands[commandoffset+2]+"'");
					} else if (trackercommand.equalsIgnoreCase("host") || trackercommand.equalsIgnoreCase("h")) {
						//ci.out.println("> Command 'hack': Debug: host");
						if (sSubcommands.length < (commandoffset + 2)) {
							ci.out.println("> Command 'hack': Not enough parameters for command parameter '" + sSubcommands[commandoffset] + "'.");
							return;
						}
						URI uold = new URI(client.getTrackerUrl().toString());
						try {
							URI unew = new URI(uold.getScheme(), uold.getUserInfo(), sSubcommands[commandoffset+2], uold.getPort(), uold.getPath(), uold.getQuery(), uold.getFragment());
							client.setTrackerUrl(new URL(unew.toString()));
							ci.out.println("> Set Tracker URL for '"+dm.getTorrentSaveDirAndFile()+"' to '"+unew.toString()+"'");
						} catch (Exception e) {
							ci.out.println("> Command 'hack': Assembling new tracker url failed: "+e.getMessage());
							return;
						}
					} else if (trackercommand.equalsIgnoreCase("port") || trackercommand.equalsIgnoreCase("p")) {
						//ci.out.println("> Command 'hack': Debug: port");
						if (sSubcommands.length < (commandoffset + 2)) {
							ci.out.println("> Command 'hack': Not enough parameters for command parameter '" + sSubcommands[commandoffset] + "'.");
							return;
						}
						URI uold = new URI(client.getTrackerUrl().toString());
						try {
							URI unew = new URI(uold.getScheme(), uold.getUserInfo(), uold.getHost(), Integer.parseInt(sSubcommands[commandoffset+2]), uold.getPath(), uold.getQuery(), uold.getFragment());
							client.setTrackerUrl(new URL(unew.toString()));
							ci.out.println("> Set Tracker URL for '"+dm.getTorrentSaveDirAndFile()+"' to '"+unew.toString()+"'");
						} catch (Exception e) {
							ci.out.println("> Command 'hack': Assembling new tracker url failed: "+e.getMessage());
							return;
						}
					} else {
						try {
							URI test = new URI(sSubcommands[commandoffset+1]);
						} catch (Exception e) {
							ci.out.println("> Command 'hack': Parsing tracker url failed: "+e.getMessage());
							return;
						}
						client.setTrackerUrl(new URL(sSubcommands[commandoffset+1]));
						ci.out.println("> Set Tracker URL for '"+dm.getTorrentSaveDirAndFile()+"' to '"+sSubcommands[commandoffset+1]+"'");
					}
				} catch (Exception e) {
					ci.out.println("> Command 'hack': Exception while parsing command parameter '" + sSubcommands[commandoffset] + "': " + e.getMessage());
					return;
				}
			} else
				ci.out.println("> Command 'hack': Command parameter '" + sSubcommands[commandoffset] + "' unknown.");
		} else
			commandHelp(ci, null);
	}
	public static void RegisterCommands() {
		try {
			//System.out.println(">>> Hack init");
			ConsoleInput.RegisterCommand("hack", Hack.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("#", Hack.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("hack [<various options>]\t#\tModify torrent settings. Use without parameters for further help.");
		} catch (Exception e) {
		}
	}
}
