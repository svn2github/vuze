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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.console.commands.IConsoleCommand;

/**
 * @author Tobias Minich
 */
public class ConsoleInput extends Thread implements IConsoleCommand {

	private static final String commandclasses[] = {"XML", "Hack", "Set", "Show", "AddFind", "Torrent", "Log", "Move"};
	
	private static HashMap commands = new HashMap();
	private static TreeSet helplines = new TreeSet();
	private static TreeSet helpextra = new TreeSet();

	public GlobalManager gm;
	CommandReader br;
	public PrintStream out;
	public ArrayList torrents = null;
	public File[] adds = null;
	boolean controlling;
	boolean running;
	String oldcommand = "sh t";
	
	static {
		try {
			ConsoleInput.RegisterCommand("help", ConsoleInput.class.getMethod("commandHelp", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("?", ConsoleInput.class.getMethod("commandHelp", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("quit", ConsoleInput.class.getMethod("commandQuit", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("ui", ConsoleInput.class.getMethod("commandUI", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("u", ConsoleInput.class.getMethod("commandUI", ConsoleCommandParameters));
			ConsoleInput.RegisterCommand("logout", ConsoleInput.class.getMethod("commandLogout", ConsoleCommandParameters));
			ConsoleInput.RegisterHelp("help [torrents]\t\t\t?\tShow this help. 'torrents' shows info about the show torrents display.");
			ConsoleInput.RegisterHelp("quit\t\t\t\t\tShutdown Azureus");
			ConsoleInput.RegisterHelp("ui <interface>\t\t\tu\tStart additional user interface.");
		} catch (Exception e) {e.printStackTrace();}

		/*
        byte[] buf = new byte[1024];
        InputStream res = ClassLoader.getSystemResourceAsStream("org/gudy/azureus2/ui/console/commands/");
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        while (true) {
          try {
            int i = res.read(buf);
            if (i > 0)
              bao.write(buf, 0, i);
            else
              break;
          } catch (IOException e) {
            break;
          }
        }
		String cls[] = bao.toString().split("\n");*/
		String cls[] = commandclasses;
		for (int i=0; i<cls.length;i++) {
			//if (cls[i].indexOf(".class") != -1) {
				String cl = cls[i]/*.substring(0, cls[i].indexOf(".class"))*/;
				try {
					Class regme = Class.forName("org.gudy.azureus2.ui.console.commands."+cl);
					/*Class intf[] = regme.getInterfaces();
					if (intf == null) continue;
					boolean implemented = false;
					for (int j=0; j<intf.length;j++)
						if (intf[j]==IConsoleCommand.class) implemented=true;
					if (!implemented) continue;*/
					Method regit = regme.getMethod("RegisterCommands", null);
					regit.invoke(null, null);
				} catch (Exception e) {
					
				}
			//}
		}
	}
	
	public static void RegisterCommand(String command, Method m) {
		commands.put(command, m);
	}
	
	public static void RegisterHelp(String helpstr) {
		helplines.add(helpstr);
	}

	public static void RegisterHelpExtra(String helpstr) {
		helpextra.add(helpstr);
	}

	/** Creates a new instance of ConsoleInput */
	public ConsoleInput(String con, GlobalManager _gm, Reader _in, PrintStream _out, Boolean _controlling) {
		super("Console Input: " + con);
		gm = _gm;
		out = _out;
		controlling = _controlling.booleanValue();
		br = new CommandReader(_in, new OutputStreamWriter(_out));
		start();
	}

	public ConsoleInput(String con, GlobalManager _gm, InputStream _in, PrintStream _out, Boolean _controlling) {
		this(con, _gm, new InputStreamReader(_in), _out, _controlling);
	}

	public static void printconsolehelp(PrintStream os) {
		os.println("> -----");
		os.println("Available console commands:");
		os.println("Command\t\t\t\tShort\tDescription");
		os.println(".\t\t\t\t\tRepeats last command (Initially 'show torrents').");
		Iterator helps = helplines.iterator();
		while (helps.hasNext())
			os.println((String) helps.next());
		helps = helpextra.iterator();
		while (helps.hasNext()) {
			os.println();
			os.println((String) helps.next());
		}
		os.println("> -----");
	}

	private static void quit(boolean finish) {
		if (finish)
			UIConst.shutdown();
	}

	public static void commandHelp(ConsoleInput ui, List args) {
		ui.out.println("> -----");
		if ((args == null) || (args.isEmpty())){
			printconsolehelp(ui.out);
		} else {
			String subcommand = (String) args.get(0);
			if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
				ui.out.println("# [state] PercentDone Name (Filesize) ETA\r\n\tDownSpeed / UpSpeed\tDownloaded/Uploaded\tConnectedSeeds(total) / ConnectedPeers(total)");
				ui.out.println();
				ui.out.println("States:");
				ui.out.println(" > Downloading");
				ui.out.println(" * Seeding");
				ui.out.println(" ! Stopped");
				ui.out.println(" . Waiting (for allocation/checking)");
				ui.out.println(" : Ready");
				ui.out.println(" - Queued");
				ui.out.println(" A Allocating");
				ui.out.println(" C Checking");
				ui.out.println(" E Error");
				ui.out.println(" I Initializing");
				ui.out.println(" ? Unknown");
			} else
				printconsolehelp(ui.out);
		}
		ui.out.println("> -----");
	}

	public static void commandQuit(ConsoleInput ci, List args) {
		if (ci.controlling) {
			ci.running = false;
			quit(ci.controlling);
		} else {
			if ((args == null) || (args.isEmpty()) || (!args.get(0).toString().equalsIgnoreCase("IAMSURE")))
				ci.out.println("> The 'quit' command exits azureus. Since this is a non-controlling shell thats probably not what you wanted. Use 'logout' to quit it or 'quit iamsure' to really exit azureus.");
			else
				quit(true);
		}
	}

	public static void commandLogout(ConsoleInput ci, List args) {
		ci.running = false;
	}

	public static void commandUI(ConsoleInput ci, List args) {
		if ((args != null) && (!args.isEmpty())){
			UIConst.startUI(args.get(0).toString(), null);
		} else {
			ci.out.println("> Missing subcommand for 'ui'\r\n> ui syntax: ui <interface>");
		}
	}

	public boolean invokeCommand(String command, Vector cargs) {
		if (commands.containsKey(command)) {
			Method inv = (Method) commands.get(command);
			Object args[] = {this, cargs};
			try {
				inv.invoke(null, args);
				return true;
			} catch(Exception e) {
				out.println("> Invoking Command '"+command+"' failed. Exception: "+e.getMessage());
				return false;
			}
		} else
			return false;
	}

	public void run() {
		String s = null;
		String command;
		String subcommand = "";
		Vector comargs = new Vector();
		running = true;
		while (running) {
			try {
				s = br.readLine();
				comargs = (Vector) br.commandargs.clone();
			} catch (Exception e) {
				running = false;
			}
			if (s != null) {
				/*if (br.commandargs != null) {
					for (int i=0; i<br.commandargs.size(); i++) {
						out.println("0> +"+((String) br.commandargs.get(i))+"+");
					}
				}*/
				if (oldcommand != null) {
					if (s.equals("."))
						s = oldcommand;
				} else {
					if (s.equals("."))
						out.println("No old command. Remove commands are not repeated to prevent errors");
				}
				oldcommand = s;
				if (s.indexOf(" ") == -1) {
					command = s;
					subcommand = null;
				} else {
					command = s.substring(0, s.indexOf(" "));
					subcommand = s.substring(s.indexOf(" ") + 1).trim();
				}
				command = command.toLowerCase();
				comargs.removeElementAt(0);
				if (!invokeCommand(command, comargs)) {
					out.println("> Command '" + command + "' unknown (or . used without prior command)");
				}
			}
		}
	}
}
