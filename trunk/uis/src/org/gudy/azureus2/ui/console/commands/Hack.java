/*
 * Created on 22.03.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
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
			ConsoleInput.commands.put("hack", Hack.class.getMethod("commandHack", ConsoleCommandParameters));
			ConsoleInput.commands.put("#", Hack.class.getMethod("commandHack", ConsoleCommandParameters));
			ConsoleInput.helplines.add("hack\t\t\t\t#\tCurrently only for testing purposes =)");
		} catch (Exception e) {}
	}

}
