/*
 * UserInterfaceMain.java
 *
 * Created on 9. Oktober 2003, 19:50
 */

package org.gudy.azureus2.ui.common;

import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import java.lang.reflect.Constructor;
import java.net.Socket;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.plugins.PluginManager;
/**
 *
 * @author  Tobias Minich
 */
public class Main {
  
  public static String DEFAULT_UI = "swt";
  
  public static StartServer start = null;
  
  private static CommandLine parseCommands(String[] args, boolean constart) {
    
    if (args==null)
      return null;
    
    CommandLineParser parser = new PosixParser();
    Options options = new Options();
    options.addOption("h", "help", false, "Show this help.");
 
    OptionBuilder.withLongOpt("exec");
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("file");
    OptionBuilder.withDescription("Execute script file. The file should end with 'logout', otherwise the parser thread doesn't stop.");
    options.addOption( OptionBuilder.create('e'));
    
    OptionBuilder.withLongOpt("command");
    OptionBuilder.hasArg();
    OptionBuilder.withArgName("command");
    OptionBuilder.withDescription("Execute single script command. Try '-c help' for help on commands.");
    options.addOption(OptionBuilder.create('c'));
    
    OptionBuilder.withLongOpt("ui");
    OptionBuilder.withDescription("Run <uis>. ',' separated list of user interfaces to run. The first one given will respond to requests without determinable source UI (e.g. further torrents added via command line).");
    OptionBuilder.withArgName("uis");
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create('u'));
    
    CommandLine commands = null;
    try {
      commands = parser.parse(options, args, true);
    } catch( ParseException exp ) {
      Logger.getLogger("azureus2").error("Parsing failed.  Reason: " + exp.getMessage(), exp);
      if (constart)
        System.exit(2);
    }
    if (commands.hasOption('h')) {
      if (constart) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java org.gudy.azureus2.ui.common.Main", "Optionally you can put torrent files to add to the end of the command line.\r\n", options, "Available User Interfaces: swt (default), web, console\r\nThe default interface is not started if you give either the '-e' or '-c' option (But you can start it by hand with '-u').", true);
        System.exit(0);
      }
    }
    return commands;
  }
  
  public static void initRootLogger() {
    if (Logger.getRootLogger().getAppender("ConsoleAppender")==null) {
      Appender app;
      app = new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN));
      app.setName("ConsoleAppender");
      Logger.getRootLogger().addAppender(app);
    }
  }
  
  public static void main(String[] args) {
  	COConfigurationManager.setSystemProperties();
  	
    initRootLogger();
    CommandLine commands = parseCommands(args, true);

    start = new StartServer();
      
    if ((start == null) || (start.getState()==StartServer.STATE_FAULTY)) {
       new StartSocket(args);
    } else {
      COConfigurationManager.checkConfiguration();
      start.start();
      
      processArgs(args, true, commands);
    }
  }
  
  public static void shutdown() {
    if (start!=null)
      start.stopIt();
    if (UIConst.GM!=null)
    UIConst.GM.stopAll();
    SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    Logger.getLogger("azureus2").fatal("Azureus stopped at "+temp.format(new Date()));
    System.exit(0);
  }
  
  public static void processArgs(String[] args, boolean creategm, CommandLine commands) {
    if (commands==null)
      commands = parseCommands(args, false);
    if (((commands!=null) && (args.length>0)) || creategm) {
      if (UIConst.UIS == null)
      UIConst.UIS = new HashMap();
      if (commands.hasOption('u')) {
        String uinames = commands.getOptionValue('u');
        if (uinames.indexOf(',')==-1) {
          if (!UIConst.UIS.containsKey(uinames))
          UIConst.UIS.put(uinames,UserInterfaceFactory.getUI(uinames));
        } else {
          StringTokenizer stok = new StringTokenizer(uinames, ",");
          while (stok.hasMoreTokens()) {
            String uin = stok.nextToken();
            if (!UIConst.UIS.containsKey(uin))
              UIConst.UIS.put(uin,UserInterfaceFactory.getUI(uin));
          }
        }
      } else {
        if (UIConst.UIS.isEmpty() && !commands.hasOption('c') && !commands.hasOption('e'))
          UIConst.UIS.put(DEFAULT_UI, UserInterfaceFactory.getUI(DEFAULT_UI));
      }

      Iterator uis = UIConst.UIS.values().iterator();
      boolean isFirst = true;
      String [] theRest = commands.getArgs();
      while (uis.hasNext()) {
        IUserInterface ui = (IUserInterface) uis.next();
        ui.init(isFirst, (UIConst.UIS.size()>1));
        theRest = ui.processArgs(theRest);
        isFirst = false;
      }

      if (creategm) {
        SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        UIConst.startTime = new Date();
        Logger.getLogger("azureus2").fatal("Azureus started at "+temp.format(UIConst.startTime));
        UIConst.GM = GlobalManagerFactory.create();
        
        PluginInitializer.getSingleton(UIConst.GM,null).initializePlugins( PluginManager.UI_NONE );
        
        new Thread("Plugin Init Complete")
        {
        	public void
        	run()
        	{
        		PluginInitializer.initialisationComplete();
        	}
        }.start();      
      }

      uis = UIConst.UIS.values().iterator();
      while (uis.hasNext())
        ((IUserInterface) uis.next()).startUI();
      
      if (UIConst.GM != null)
      	UIConst.GM.startChecker();
      
      Class clConsoleInput;
      Constructor conConsoleInput =null;
      try {
      	clConsoleInput = Class.forName("org.gudy.azureus2.ui.console.ConsoleInput");
      	Class params[] = {String.class, GlobalManager.class, Reader.class, PrintStream.class, Boolean.class};
      	conConsoleInput=clConsoleInput.getConstructor(params);
      } catch (Exception e) {
      	e.printStackTrace();
      }
      if (commands.hasOption('e')) {
      	if (conConsoleInput != null) {
	        try {
	        	Object params[] = {commands.getOptionValue('e'), UIConst.GM, new FileReader(commands.getOptionValue('e')), System.out, Boolean.FALSE};
	        	conConsoleInput.newInstance(params);
	        } catch (java.io.FileNotFoundException e) {
	          Logger.getLogger("azureus2").error("Script file not found: "+e.toString());
	        } catch (Exception e) {
	        	Logger.getLogger("azureus2").error("Error invocating the script processor: "+e.toString());
	        }
      	} else
      		Logger.getLogger("azureus2").error("ConsoleInput class not found. You need the console ui package to use '-e'");
      }
      
      if (commands.hasOption('c')) {
      	if (conConsoleInput != null) {
	        String comm = commands.getOptionValue('c');
	        comm+="\nlogout\n";
	        Object params[] = {commands.getOptionValue('c'), UIConst.GM, new StringReader(comm), System.out, Boolean.FALSE};
	        try {
	        	conConsoleInput.newInstance(params);
	        } catch (Exception e) {
	        	Logger.getLogger("azureus2").error("Error invocating the script processor: "+e.toString());
	        }
      	} else
      		Logger.getLogger("azureus2").error("ConsoleInput class not found. You need the console ui package to use '-e'");
      }
      
      openTorrents(theRest);
    } else {
      Logger.getLogger("azureus2").error("No commands to process");
    }
  }
  
  public static void openTorrents(String[] torrents) {
    if ((UIConst.UIS!=null) && (!UIConst.UIS.isEmpty()) && (torrents.length>0)) {
      for(int l=0; l<torrents.length; l++) {
        ((IUserInterface) UIConst.UIS.values().toArray()[0]).openTorrent(torrents[l]);
      }
    }
  }
  
  public static class StartSocket {
    public StartSocket(String args[]) {
      Socket sck = null;
      PrintWriter pw = null;
      try {
        System.out.println("StartSocket: passing startup args to already-running process.");
        
        sck = new Socket("127.0.0.1",6880);
        pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream()));
        StringBuffer buffer = new StringBuffer(StartServer.ACCESS_STRING+";args;");
        for(int i = 0 ; i < args.length ; i++) {
          String arg = args[i].replaceAll("&","&&").replaceAll(";","&;");
          buffer.append(arg);
          buffer.append(';');
        }
        pw.println(buffer.toString());
        pw.flush();
      } catch(Exception e) {
        e.printStackTrace();
      } finally {
        try {
          if (pw != null)
            pw.close();
        } catch (Exception e) {
        }
        try {
          if (sck != null)
            sck.close();
        } catch (Exception e) {
        }
      }
    }
  }
}
