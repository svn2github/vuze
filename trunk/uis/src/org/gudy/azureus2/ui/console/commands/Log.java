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

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.varia.DenyAllFilter;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author Tobias Minich
 */
public class Log extends OptionsConsoleCommand {

	public Log()
	{
		super( new String[] { "log", "l" });
	}
	
	protected Options getOptions()
	{
		Options options = new Options();
		options.addOption( new Option("f", "filename", true, "filename to write log to"));
		return options;
	}
	
	public void execute(String commandName, ConsoleInput ci, CommandLine commandLine)
	{
		Appender con = Logger.getRootLogger().getAppender("ConsoleAppender");
		List args = commandLine.getArgList();
		if ((con != null) && (!args.isEmpty())) {
			String subcommand = (String) args.get(0);
			if ("off".equalsIgnoreCase(subcommand) ) {
				con.addFilter(new DenyAllFilter());
				ci.out.println("> Console logging off");
			} else if ("on".equalsIgnoreCase(subcommand) ) {
				if( commandLine.hasOption('f') )
				{
					// send log output to a file
					String filename = commandLine.getOptionValue('f');
					
					try
					{
						Appender newAppender = new FileAppender(new PatternLayout("%d{ISO8601} %c{1}-%p: %m%n"), filename, true);
						newAppender.setName("ConsoleAppender");
						Logger.getRootLogger().removeAppender(con);
						Logger.getRootLogger().addAppender(newAppender);
						ci.out.println("> Logging to filename: " + filename);
					} catch (IOException e)
					{
						ci.out.println("> Unable to log to file: " + filename + ": " + e);
					}					
				}
				else
				{
					if( ! (con instanceof ConsoleAppender) )
					{
						Logger.getRootLogger().removeAppender(con);
						con = new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN));
						con.setName("ConsoleAppender");
					    Logger.getRootLogger().addAppender(con);
					}
					// switch back to console appender
					ci.out.println("> Console logging on");
				}
				con.clearFilters();
				
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
		return("log [-f filename] (on|off)\t\t\tl\tTurn on/off console logging");
	}
}
