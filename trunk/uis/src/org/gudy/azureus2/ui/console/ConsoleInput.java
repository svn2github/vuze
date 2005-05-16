/*
 * Written and copyright 2001-2004 Tobias Minich. Distributed under the GNU
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.LGAlertListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.console.commands.AddFind;
import org.gudy.azureus2.ui.console.commands.Alias;
import org.gudy.azureus2.ui.console.commands.Hack;
import org.gudy.azureus2.ui.console.commands.IConsoleCommand;
import org.gudy.azureus2.ui.console.commands.Log;
import org.gudy.azureus2.ui.console.commands.Move;
import org.gudy.azureus2.ui.console.commands.Set;
import org.gudy.azureus2.ui.console.commands.Share;
import org.gudy.azureus2.ui.console.commands.Show;
import org.gudy.azureus2.ui.console.commands.TorrentCheck;
import org.gudy.azureus2.ui.console.commands.TorrentForceStart;
import org.gudy.azureus2.ui.console.commands.TorrentHost;
import org.gudy.azureus2.ui.console.commands.TorrentQueue;
import org.gudy.azureus2.ui.console.commands.TorrentRemove;
import org.gudy.azureus2.ui.console.commands.TorrentStart;
import org.gudy.azureus2.ui.console.commands.TorrentStop;
import org.gudy.azureus2.ui.console.commands.XML;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author Tobias Minich
 */
public class ConsoleInput extends Thread {

	private static final String ALIASES_CONFIG_FILE = "console.aliases.properties";
	public final AzureusCore azureus_core;
	public final GlobalManager gm;
	public final PrintStream out;
	public final List torrents = new ArrayList();
	public File[] adds = null;
	
	private final CommandReader br;
	private final boolean controlling;
	private boolean running;
	// previous command
	private final Vector oldcommand = new Vector();
	
	private final static List pluginCommands = new ArrayList();
	public final Properties aliases = new Properties();
	private final Map commands = new LinkedHashMap();
	private final List helpItems = new ArrayList();
	/**
	 * can be used by plugins to register console commands since they may not have access o
	 * each ConsoleInput object that is created.
	 */
	public static void registerPluginCommand(Class clazz)
	{
		if( ! clazz.isInstance(IConsoleCommand.class) )
		{
			throw new IllegalArgumentException("Class must be extend IConsoleCommand");
		}
		pluginCommands.add( clazz );
	}
	public static void unregisterPluginCommand(Class clazz)
	{
		pluginCommands.remove(clazz);
	}

	/** Creates a new instance of ConsoleInput */
	public 
	ConsoleInput(
		String 		con, 
		AzureusCore _azureus_core, 
		Reader 		_in, 
		PrintStream _out, 
		Boolean 	_controlling) 
	{
		super("Console Input: " + con);
		this.out = _out;
		this.azureus_core	= _azureus_core;
		this.gm  			= _azureus_core.getGlobalManager();
		this.controlling = _controlling.booleanValue();
		this.br = new CommandReader(_in);
		registerAlertHandler();
		registerCommands();
		registerPluginCommands();
		registerUpdateChecker();
		try {
			loadAliases();
		} catch (IOException e) {
			out.println("Error while loading aliases: " + e.getMessage());
		}
		// populate the old command so that '.' does something sensible first time around
		oldcommand.add("sh");
		oldcommand.add("t");
		start();
	}

	/**
	 * instantiates each of the plugin commands and registers t
	 */
	private void registerPluginCommands() {
		Class clazz;
		for (Iterator iter = pluginCommands.iterator(); iter.hasNext();) {
			clazz = (Class) iter.next();
			try {
				IConsoleCommand command = (IConsoleCommand) clazz.newInstance();
				registerCommand(command);
			} catch (InstantiationException e)
			{
				out.println("Error while registering plugin command: " + clazz.getName() + ":" + e.getMessage());
			} catch (IllegalAccessException e) {
				out.println("Error while registering plugin command: " + clazz.getName() + ":" + e.getMessage());
			}
		}
	}

	protected void
	registerAlertHandler()
	{
		LGLogger.addAlertListener(
			new LGAlertListener()
			{
				private java.util.Set	history = Collections.synchronizedSet( new HashSet());
				
				
				public void
				alertRaised(
					int		type,
					String	message,
					boolean	repeatable )
				{
					if ( !repeatable ){
						
						if ( history.contains( message )){
							
							return;
						}
						
						history.add( message );
					}
					
					out.println( message );
				}
				
				public void
				alertRaised(
					String		message,
					Throwable	exception,
					boolean		repeatable )
				{
					if ( !repeatable ){
						
						if ( history.contains( message )){
							
							return;
						}
						
						history.add( message );
					}
					
					out.println( message );

					exception.printStackTrace( out );
				}
			});
	}
	/**
	 * registers the commands available to be executed from this console 
	 */
	protected void registerCommands() 
	{
		registerCommand(new XML());
		registerCommand(new Hack());
		registerCommand(new AddFind());
		registerCommand(new TorrentCheck());
		registerCommand(new TorrentQueue());
		registerCommand(new TorrentRemove());
		registerCommand(new TorrentStart());
		registerCommand(new TorrentStop());
		registerCommand(new TorrentHost());
		registerCommand(new TorrentForceStart());
		registerCommand(new Log());
		registerCommand(new Move());
		registerCommand(new Share());
		registerCommand(new Set());
		registerCommand(new Show());
		registerCommand(new CommandUI());
		registerCommand(new CommandLogout());
		registerCommand(new CommandQuit());
		registerCommand(new CommandHelp());
		registerCommand(new Alias());
	}

	/**
	 * @param set
	 */
	protected void registerCommand(IConsoleCommand command) 
	{
		for (Iterator iter = command.getCommandNames().iterator(); iter.hasNext();) {
			String cmdName = (String) iter.next();
			commands.put( cmdName, command);
		}
		helpItems.add(command);
	}
	
	protected void unregisterCommand(IConsoleCommand command)
	{
		for (Iterator iter = command.getCommandNames().iterator(); iter.hasNext();) {
			String cmdName = (String) iter.next();
			if( command.equals(commands.get(cmdName)) )
				commands.remove( cmdName );
		}
		helpItems.remove(command);
	}
	protected void unregisterCommand(String commandName)
	{
		IConsoleCommand cmd = (IConsoleCommand)commands.get(commandName);
		if( cmd == null )
			return;
		// check if there are any more commands registered to this command object,
		// otherwise remove it
		int numCommands = 0;
		for (Iterator iter = commands.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			if( cmd.equals(entry.getValue()) )
				numCommands++;
		}
		if( numCommands == 1)
			unregisterCommand(cmd);
		else
			commands.remove(commandName);
	}
	
	public ConsoleInput(String con, AzureusCore _azureus_core, InputStream _in, PrintStream _out, Boolean _controlling) {
		this(con, _azureus_core, new InputStreamReader(_in), _out, _controlling);
	}

	private static void quit(boolean finish) {
		if (finish)
			UIConst.shutdown();
	}

	private class CommandHelp extends IConsoleCommand
	{
		public CommandHelp()
		{
			super(new String[] { "help", "?"});
		}
		public String getCommandDescriptions() {
			return("help [torrents]\t\t\t?\tShow this help. 'torrents' shows info about the show torrents display.");
		}
		public void execute(String commandName, ConsoleInput ci, List args)
		{
			if (args.isEmpty()){
				printconsolehelp(ci.out);
			} else {
				String subcommand = (String) args.get(0);
				IConsoleCommand cmd = (IConsoleCommand) commands.get(subcommand);
				if( cmd != null )
				{
					List newargs = new ArrayList(args);
					newargs.remove(0);
					cmd.printHelp(ci.out, newargs);
				}
				else if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
					ci.out.println("> -----");
					ci.out.println("# [state] PercentDone Name (Filesize) ETA\r\n\tDownSpeed / UpSpeed\tDownloaded/Uploaded\tConnectedSeeds(total) / ConnectedPeers(total)");
					ci.out.println();
					ci.out.println("States:");
					ci.out.println(" > Downloading");
					ci.out.println(" * Seeding");
					ci.out.println(" ! Stopped");
					ci.out.println(" . Waiting (for allocation/checking)");
					ci.out.println(" : Ready");
					ci.out.println(" - Queued");
					ci.out.println(" A Allocating");
					ci.out.println(" C Checking");
					ci.out.println(" E Error");
					ci.out.println(" I Initializing");
					ci.out.println(" ? Unknown");
					ci.out.println("> -----");
				} else
					printconsolehelp(ci.out);
			}
		}
		
	}
	
	public void printconsolehelp()
	{
		printconsolehelp(out);
	}
	private void printconsolehelp(PrintStream os) {
		os.println("> -----");
		os.println("Available console commands:");
		os.println("Command\t\t\t\tShort\tDescription");
		os.println(".\t\t\t\t\tRepeats last command (Initially 'show torrents').");
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		for (Iterator iter = helpItems.iterator(); iter.hasNext();) {
			IConsoleCommand cmd = (IConsoleCommand) iter.next();
			String cmddesc = cmd.getCommandDescriptions();
			if( cmddesc != null )
				os.println(cmddesc);
			String extraHelp = cmd.getHelpExtra();
			if( extraHelp != null )
			{
				pw.println();
				pw.println(extraHelp);
			}
		}
		os.println(sw.toString());
		os.println("> -----");
	}
	
	private static class CommandQuit extends IConsoleCommand
	{
		public CommandQuit()
		{
			super(new String[] {"quit"});
		}
		public String getCommandDescriptions() {
			return("quit\t\t\t\t\tShutdown Azureus");
		}
		public void execute(String commandName, ConsoleInput ci, List args) {
			if (ci.controlling) {
				ci.running = false;
				ci.out.print( "Exiting....." );
				quit( true );
				ci.out.println( "OK" );
			}
			else {
				if (args.isEmpty() || (!args.get(0).toString().equalsIgnoreCase("IAMSURE"))) {
					ci.out.println("> The 'quit' command exits azureus. Since this is a non-controlling shell thats probably not what you wanted. Use 'logout' to quit it or 'quit iamsure' to really exit azureus.");
				}
				else {
					ci.out.print( "Exiting....." );
					quit( true );
					ci.out.println( "OK" );
				}
			}
		}
	}

	private static class CommandLogout extends IConsoleCommand
	{
		public CommandLogout()
		{
			super(new String[] {"logout"});
		}
		public String getCommandDescriptions() {
			return "logout\t\t\t\t\tLog out of the CLI";
		}
		public void execute(String commandName, ConsoleInput ci, List args) {
			try {
				if ( !ci.controlling ){
					
					ci.out.close();
						
					ci.br.close();
				}
				
			}catch (IOException ignored){
					
			}finally{
				
				ci.running = false;
			}
		}
	}

	private static class CommandUI extends IConsoleCommand
	{
		public CommandUI()
		{
			super( new String[] { "ui", "u" });
		}
		public String getCommandDescriptions() {
			return("ui <interface>\t\t\tu\tStart additional user interface.");
		}
		public void execute(String commandName, ConsoleInput ci, List args) {
			if (!args.isEmpty()){
				UIConst.startUI(args.get(0).toString(), null);
			} else {
				ci.out.println("> Missing subcommand for 'ui'\r\n> ui syntax: ui <interface>");
			}
		}
	}
	
	public boolean invokeCommand(String command, List cargs) {
		if( command.startsWith("\\") )
			command = command.substring(1);
		else if( aliases.containsKey(command) )
		{
			List list = br.parseCommandLine(aliases.getProperty(command));
			String newCommand = list.remove(0).toString().toLowerCase();
			list.addAll( cargs );
			return invokeCommand(newCommand, list);
		}
		if (commands.containsKey(command)) {
			IConsoleCommand cmd = (IConsoleCommand) commands.get(command);
			try {
				if( cargs == null )
					cargs = new ArrayList();
				cmd.execute(command, this, cargs);
				return true;
			} catch (Exception e)
			{
				out.println("> Invoking Command '"+command+"' failed. Exception: "+e.getMessage());
				return false;
			}
		} else
			return false;
	}

	public void run() {
		String command;
		List comargs;
		running = true;
		while (running) {
			try {
				String line = br.readLine();
				comargs = br.parseCommandLine(line);
			} catch (Exception e) {
				out.println("Stopping console input reader because of exception: " + e.getMessage());
				running = false;
				break;
			}
			if (!comargs.isEmpty()) {
				command = ((String) comargs.get(0)).toLowerCase();
				if( ".".equals(command) )
				{
					if (oldcommand != null) {
						comargs.clear();
						comargs.addAll(oldcommand);
						command = (String) oldcommand.get(0);
					} else {
						out.println("No old command. Remove commands are not repeated to prevent errors");
					}
				}
				oldcommand.clear();
				oldcommand.addAll(comargs);
				comargs.remove(0);
				if (!invokeCommand(command, comargs)) {
					out.println("> Command '" + command + "' unknown (or . used without prior command)");
				}
			}
		}
	}

	private File getAliasesFile()
	{
		PluginInterface pi = azureus_core.getPluginManager().getDefaultPluginInterface();
		String azureusUserDir = pi.getUtilities().getAzureusUserDir();
		return new File(azureusUserDir, ALIASES_CONFIG_FILE);
	}
	/**
	 * read in the aliases from the alias properties file
	 * @throws IOException
	 */
	private void loadAliases() throws IOException
	{
		File aliasesFile = getAliasesFile();
		out.println("Attempting to load aliases from: " + aliasesFile.getCanonicalPath());
		if ( aliasesFile.exists() )
		{
			FileInputStream fr = new FileInputStream(aliasesFile);
			aliases.clear();
			try {
				aliases.load(fr);
			} finally {
				fr.close();
			}
		}
	}
	
	/**
	 * writes the aliases back out to the alias file 
	 */
	public void saveAliases() {
		File aliasesFile = getAliasesFile();
		try {
			out.println("Saving aliases to: " + aliasesFile.getCanonicalPath());
			FileOutputStream fo = new FileOutputStream(aliasesFile);
			aliases.store(fo, "This aliases file was automatically written by Azureus");
		} catch (IOException e) {
			out.println("> Error saving aliases to " + aliasesFile.getPath() + ":" + e.getMessage());
		}
	}
	
	protected void
	registerUpdateChecker()
	{
		boolean check_at_start	= COConfigurationManager.getBooleanParameter( "update.start", true );

		if ( !check_at_start ){
			
			return;
		}
		
		UpdateManager update_manager = azureus_core.getPluginManager().getDefaultPluginInterface().getUpdateManager();
		
		UpdateCheckInstance	checker = update_manager.createUpdateCheckInstance();
		
		checker.addListener(
			new UpdateCheckInstanceListener()
			{
				public void
				cancelled(
					UpdateCheckInstance		instance )
				{
					
				}
				
				public void
				complete(
					UpdateCheckInstance		instance )
				{
					Update[] 	updates = instance.getUpdates();
					
					for (int i=0;i<updates.length;i++){
						
						Update	update = updates[i];
						
						out.println( "Update available for '" + update.getName() + ", new version = " + update.getNewVersion());
						
						String[]	descs = update.getDescription();
						
						for (int j=0;j<descs.length;j++){
							
							out.println( "\t" + descs[j] );
						}
					}
				}
			});
		
		checker.start();
		
	}
}
