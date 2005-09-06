/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 */

package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * base interface for all console commands
 * @author Tobias Minich
 */
public abstract class IConsoleCommand {
	private final Set commandNames = new HashSet();
	
	public IConsoleCommand(String []_commandNames)
	{
		for (int i=0;i<_commandNames.length;i++){
			
			commandNames.add( _commandNames[i] );
		}
	}
	
	/**
	 * execute the command with the specified name using the specified arguments 
	 * @param commandName
	 * @param console
	 * @param arguments
	 */
	public abstract void execute(String commandName, ConsoleInput console, List arguments);
	
	/**
	 * return high-level help about the commands supported by this object.
	 * @return
	 */
	public abstract String getCommandDescriptions();
	
	/**
	 * do nothing by default
	 * @param out
	 * @param args
	 */
	public void printHelp(PrintStream out, List args)
	{
		out.println("No further help for this command");
	}
	
	/**
	 * helper method if subclasses want to print out help for a particular subcommand
	 * @param out
	 * @param arg
	 */
	protected final void printHelp(PrintStream out, String arg)
	{
		List args;
		if( arg != null )
		{
			args = new ArrayList();
			args.add(arg);
		}
		else
			args = Collections.EMPTY_LIST;
		
		printHelp(out, args);
	}
	
	/**
	 * returns the set of command names that this command understands.
	 * eg: the 'quit' command might understand 'quit', 'q', 'bye'
	 * other commands might actually have several command names and
	 * execute different code depending upon the command name
	 * @return
	 */
	public Set getCommandNames()
	{
		return Collections.unmodifiableSet(commandNames);
	}
	
	/**
	 * print some 'extra' help that is displayed after all of the help commands.
	 * eg: explain some options that are common to a group of commands
	 * @return
	 */
	public String getHelpExtra()
	{
		return null;
	}
}
