/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Torrent.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Torrent implements IConsoleCommand {
	private static final int TORRENTCOMMAND_START = 0;
	private static final int TORRENTCOMMAND_STOP = 1;
	private static final int TORRENTCOMMAND_REMOVE = 2;
	private static final int TORRENTCOMMAND_QUEUE = 3;
	private static final int TORRENTCOMMAND_STARTNOW = 4;
	private static final int TORRENTCOMMAND_CHECK = 5;
  private static final int TORRENTCOMMAND_HOST = 6;
	
	private static boolean performTorrentCommand(ConsoleInput ci, int command, DownloadManager dm) {
		switch (command) {
			case TORRENTCOMMAND_START :
				{
					//dm.startDownloadInitialized(true);
					try {
						dm.setState(DownloadManager.STATE_WAITING);
					} catch (Exception e) {
						e.printStackTrace(ci.out);
						return false;
					}
					return true;
				}
			case TORRENTCOMMAND_STOP :
				{
					try {
						dm.stopIt();
					} catch (Exception e) {
						e.printStackTrace(ci.out);
						return false;
					}
					return true;
				}
			case TORRENTCOMMAND_REMOVE :
				{
					try {
						dm.stopIt();
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
			case TORRENTCOMMAND_QUEUE :
				{
					try {
						if (dm.getState() == DownloadManager.STATE_STOPPED)
							dm.setState(DownloadManager.STATE_QUEUED);
						else if (dm.getState() == DownloadManager.STATE_DOWNLOADING || dm.getState() == DownloadManager.STATE_SEEDING)
							dm.stopIt(DownloadManager.STATE_QUEUED);
						else
							return false;
					} catch (Exception e) {
						e.printStackTrace(ci.out);
						return false;
					}
					return true;
				}
			case TORRENTCOMMAND_STARTNOW :
				{
					//dm.startDownloadInitialized(true);
					try {
						dm.setState(DownloadManager.STATE_WAITING);
						dm.startDownloadInitialized(true);
					} catch (Exception e) {
						e.printStackTrace(ci.out);
						return false;
					}
					return true;
				}
			case TORRENTCOMMAND_CHECK :
				{
					try {
						if (dm.canForceRecheck()) {
							dm.forceRecheck();
							return true;
						} else
							return false;
					} catch (Exception e) {
						e.printStackTrace(ci.out);
						return false;
					}
				}
      case TORRENTCOMMAND_HOST :
      {       
        TOTorrent torrent = dm.getTorrent();
        if (torrent != null) {
          try {
            TRHostFactory.create().hostTorrent(torrent);
          } catch (TRHostException e) {
            e.printStackTrace(ci.out);
            return false;
          }
          return true;
        }
        return false;
      }
		}
		return false;
	}

	private static void commandTorrentCommand(ConsoleInput ci, int command, List args) {
		String[] commands = { "start", "stop", "remove", "queue", "start", "check" ,"host"};
		String[] actions = { "Starting", "Stopping", "Removing", "Queueing", "Starting", "Initiating recheck of","Hosting" };
		if ((args != null) && (!args.isEmpty())) {
		    String subcommand = (String) args.get(0);
			if ((ci.torrents == null) || (ci.torrents != null) && ci.torrents.isEmpty()) {
				ci.out.println("> Command '" + commands[command] + "': No torrents in list (Maybe you forgot to 'show torrents' first).");
			} else {
				String name;
				DownloadManager dm;
				try {
					int number = Integer.parseInt(subcommand);
					if ((number > 0) && (number <= ci.torrents.size())) {
						dm = (DownloadManager) ci.torrents.get(number - 1);
						if (dm.getName() == null)
							name = "?";
						else
							name = dm.getName();
						if (performTorrentCommand(ci, command, dm))
							ci.out.println("> " + actions[command] + " Torrent #" + subcommand + " (" + name + ") succeeded.");
						else
							ci.out.println("> " + actions[command] + " Torrent #" + subcommand + " (" + name + ") failed.");
					} else
						ci.out.println("> Command '" + commands[command] + "': Torrent #" + subcommand + " unknown.");
				} catch (NumberFormatException e) {
					if (subcommand.equalsIgnoreCase("all")) {
						Iterator torrent = ci.torrents.iterator();
						int nr = 0;
						while (torrent.hasNext()) {
							dm = (DownloadManager) torrent.next();
							if (dm.getName() == null)
								name = "?";
							else
								name = dm.getName();
							if (performTorrentCommand(ci, command, dm))
								ci.out.println("> " + actions[command] + " Torrent #" + subcommand + " (" + name + ") succeeded.");
							else
								ci.out.println("> " + actions[command] + " Torrent #" + subcommand + " (" + name + ") failed.");
						}
					} else if (subcommand.toUpperCase().startsWith("HASH")) {
						String hash = subcommand.substring(subcommand.indexOf(" ") + 1);
						List torrents = ci.gm.getDownloadManagers();
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
									if (performTorrentCommand(ci, command, dm))
										ci.out.println("> " + actions[command] + " Torrent " + hash + " (" + name + ") succeeded.");
									else
										ci.out.println("> " + actions[command] + " Torrent " + hash + " (" + name + ") failed.");
									foundit = true;
									break;
								}
							}
							if (!foundit)
								ci.out.println("> Command '" + commands[command] + "': Hash '" + hash + "' unknown.");
						}
					} else {
						ci.out.println("> Command '" + commands[command] + "': Subcommand '" + subcommand + "' unknown.");
					}
				}
			}
		} else {
			ci.out.println("> Missing subcommand for '" + commands[command] + "'\r\n> " + commands[command] + " syntax: " + commands[command] + " (<#>|all|hash <hash>)");
		}
	}

	public static void commandStart(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())){
			if (args.contains("now")) {
				Vector newargs = new Vector(args);
				newargs.removeElement("now");
				commandTorrentCommand(ci, TORRENTCOMMAND_STARTNOW, newargs);
				return;
			}
		}
		commandTorrentCommand(ci, TORRENTCOMMAND_START, args);
	}
	
	public static void commandStop(ConsoleInput ci, List args) {
		commandTorrentCommand(ci, TORRENTCOMMAND_STOP, args);
	}

	public static void commandCheck(ConsoleInput ci, List args) {
		commandTorrentCommand(ci, TORRENTCOMMAND_CHECK, args);
	}
	
	public static void commandRemove(ConsoleInput ci, List args) {
		commandTorrentCommand(ci, TORRENTCOMMAND_REMOVE, args);
	}
	
	public static void commandQueue(ConsoleInput ci, List args) {
		commandTorrentCommand(ci, TORRENTCOMMAND_QUEUE, args);
	}
  
  public static void commandHost(ConsoleInput ci, List args) {
    commandTorrentCommand(ci, TORRENTCOMMAND_HOST, args);
  }
	
	public static void RegisterCommands() {
		try {
			ConsoleInput.RegisterCommand("check", Torrent.class.getMethod("commandCheck", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("c", Torrent.class.getMethod("commandCheck", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("queue", Torrent.class.getMethod("commandQueue", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("q", Torrent.class.getMethod("commandQueue", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("remove", Torrent.class.getMethod("commandRemove", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("r", Torrent.class.getMethod("commandRemove", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("start", Torrent.class.getMethod("commandStart", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("s", Torrent.class.getMethod("commandStart", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("stop", Torrent.class.getMethod("commandStop", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("h", Torrent.class.getMethod("commandStop", ConsoleCommandParameters));
      ConsoleInput.RegisterCommand("host", Torrent.class.getMethod("commandHost", ConsoleCommandParameters));      
			ConsoleInput.RegisterHelp("check (<torrentoptions>)\tc\tForce recheck on torrent(s).");
			ConsoleInput.RegisterHelp("queue (<torrentoptions>)\tq\tQueue torrent(s).");
			ConsoleInput.RegisterHelp("remove (<torrentoptions>)\tr\tRemove torrent(s).");
			ConsoleInput.RegisterHelp("start (<torrentoptions>) [now]\ts\tStart torrent(s).");
			ConsoleInput.RegisterHelp("stop (<torrentoptions>)\t\th\tStop torrent(s).");
      ConsoleInput.RegisterHelp("host (<torrentoptions>)\t\th\tHost torrent(s).");
			ConsoleInput.RegisterHelpExtra("<torrentoptions> can be one of:\n"+
				"<#>\t\tNumber of a torrent. You have to use 'show torrents' first. as the number is taken from there.\n"+
				"all\t\tCommand is applied to all torrents\n"+
				"hash <hash>\tApplied to torrent with the hash <hash> as given in the xml output or extended torrent info ('show <#>').");
		} catch (Exception e) {e.printStackTrace();}
	}
}
