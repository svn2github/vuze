/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Move.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Move implements IConsoleCommand {

	public static void command(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())){
			String[] sSubcommands = new String[args.size()];
			args.toArray(sSubcommands);
			//String subcommand = (String) args.get(0);
			if ((ci.torrents != null) && ci.torrents.isEmpty()) {
				ci.out.println("> Command 'move': No torrents in list.");
			} else {
				String name;
				DownloadManager dm;
				try {
					int ncommand;
					int nmoveto = -1;
					boolean moveto = false;
					if (sSubcommands.length > 1) {
						ncommand = Integer.parseInt(sSubcommands[0]);
						nmoveto = Integer.parseInt(sSubcommands[1]);
						moveto = true;
					} else
						ncommand = Integer.parseInt(sSubcommands[0]);
					int number = Math.abs(ncommand);
					if ((number > 0) && (number <= ci.torrents.size())) {
						dm = (DownloadManager) ci.torrents.get(number - 1);
						if (dm.getDisplayName() == null)
							name = "?";
						else
							name = dm.getDisplayName();
						if (moveto) {
							ci.gm.moveTo(dm, nmoveto - 1);
							ci.gm.fixUpDownloadManagerPositions();
							ci.out.println("> Torrent #" + Integer.toString(number) + " (" + name + ") moved to #" + Integer.toString(nmoveto) + ".");
						} else if (ncommand > 0) {
							if (dm.isMoveableUp()) {
								while (dm.isMoveableUp())
									dm.moveUp();
								ci.gm.fixUpDownloadManagerPositions();
								ci.out.println("> Torrent #" + Integer.toString(number) + " (" + name + ") moved to top.");
							} else {
								ci.out.println("> Torrent #" + Integer.toString(number) + " (" + name + ") already at top.");
							}
						} else {
							if (dm.isMoveableDown()) {
								while (dm.isMoveableDown())
									dm.moveDown();
								ci.gm.fixUpDownloadManagerPositions();
								ci.out.println("> Torrent #" + Integer.toString(number) + " (" + name + ") moved to bottom.");
							} else {
								ci.out.println("> Torrent #" + Integer.toString(number) + " (" + name + ") already at bottom.");
							}
						}
					} else
						ci.out.println("> Command 'move': Torrent #" + Integer.toString(number) + " unknown.");
				} catch (NumberFormatException e) {
					ci.out.println("> Command 'move': Subcommand '" + sSubcommands[0] + "' unknown.");
				}
			}
		} else {
			ci.out.println("> Missing subcommand for 'move'\r\n> move syntax: move <#from> [<#to>]");
		}
	}

	public static void RegisterCommands() {
		try {
			ConsoleInput.RegisterCommand("move", Move.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("m", Move.class.getMethod("command", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("move <from #> [<to #>]\t\tm\tMove torrent from to to. If to is omitted, the torrent is moved to top or to the bottom if given negative.");
		} catch (Exception e) {e.printStackTrace();}
	}

}
