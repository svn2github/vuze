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

import java.util.List;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Hack implements IConsoleCommand {

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.console.commands.IConsoleCommand#RegisterCommands(org.gudy.azureus2.ui.console.ConsoleInput)
	 */
	public static void commandHack(ConsoleInput ci, List args) {
		ci.out.println("Hack!");
	}
	
	public static void RegisterCommands() {
		try {
			//System.out.println(">>> Hack init");
			ConsoleInput.RegisterCommand("hack", Hack.class.getMethod("commandHack", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("#", Hack.class.getMethod("commandHack", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("hack\t\t\t\t#\tCurrently only for testing purposes =)");
		} catch (Exception e) {}
	}

}
