/*
 * UserInterfaceMain.java
 *
 * Created on 9. Oktober 2003, 19:50
 */

package org.gudy.azureus2.ui.common;

import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

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

import org.gudy.azureus2.ui.console.ConsoleInput;

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
    options.addOption(OptionBuilder.withLongOpt("exec")
                                   .hasArg()
                                   .withArgName("file")
                                   .withDescription("Execute script file. The file should end with 'logout', otherwise the parser thread doesn't stop.")
                                   .create('e'));
    options.addOption(OptionBuilder.withLongOpt("command")
                                   .hasArg()
                                   .withArgName("command")
                                   .withDescription("Execute single script command. Try '-c help' for help on commands.")
                                   .create('c'));
    options.addOption(OptionBuilder.withLongOpt("ui")
                                   .withDescription("Run <uis>. ',' separated list of user interfaces to run. The first one given will respond to requests without determinable source UI (e.g. further torrents added via command line).")
                                   .withArgName("uis")
                                   .hasArg()
                                   .create('u'));
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
  
  protected static void shutdown() {
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
      }

      uis = UIConst.UIS.values().iterator();
      while (uis.hasNext())
        ((IUserInterface) uis.next()).startUI();
      
      if (commands.hasOption('e')) {
        try {
          new ConsoleInput(commands.getOptionValue('e'), UIConst.GM, new FileReader(commands.getOptionValue('e')), System.out, false);
        } catch (java.io.FileNotFoundException e) {
          Logger.getLogger("azureus2").error("Script file not found: "+e.toString());
        }
      }
      
      if (commands.hasOption('c')) {
        String comm = commands.getOptionValue('c');
        comm+="\nlogout\n";
        new ConsoleInput(commands.getOptionValue('c'), UIConst.GM, new StringReader(comm), System.out, false);
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
        sck = new Socket("localhost",6880);
        pw = new PrintWriter(new OutputStreamWriter(sck.getOutputStream()));
        StringBuffer buffer = new StringBuffer("args;");
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
