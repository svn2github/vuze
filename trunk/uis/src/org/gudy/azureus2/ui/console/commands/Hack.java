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
import java.util.Iterator;
import java.util.List;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.ui.console.ConsoleInput;
/**
 * @author tobi
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
