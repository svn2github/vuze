/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Log.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.DenyAllFilter;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 */
public class Log extends IConsoleCommand {

	public Log()
	{
		super( new String[] { "log", "l" });
	}
	public void execute(String commandName, ConsoleInput ci, List args) {
		Appender con = Logger.getRootLogger().getAppender("ConsoleAppender");
		if ((con != null) && (args != null) && (!args.isEmpty())) {
			String subcommand = (String) args.get(0);
			if (subcommand.equalsIgnoreCase("off")) {
				con.addFilter(new DenyAllFilter());
				ci.out.println("> Console logging off");
			} else if (subcommand.equalsIgnoreCase("on")) {
				con.clearFilters();
				ci.out.println("> Console logging on");
			} else {
				ci.out.println("> Command 'log': Subcommand '" + subcommand + "' unknown.");
			}
		} else {
			ci.out.println("> Console logger not found or missing subcommand for 'log'\r\n> log syntax: log (on|off)");
		}
	}

	public static void commandLogtest(ConsoleInput ci, List args) {
		Logger.getLogger("azureus2").fatal("Logging test" + (((args == null) || (args.isEmpty())) ? "" : ": " + args.get(0).toString()));
	}

	public String getCommandDescriptions()
	{
		return("log (on|off)\t\t\tl\tTurn on/off console logging");
	}
}
