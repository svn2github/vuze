/*
 * UserInterfaceMain.java
 *
 * Created on 9. Oktober 2003, 19:50
 */

package org.gudy.azureus2.ui.common;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.*;

/**
 *
 * @author  Tobias Minich
 */
public class Main {
  
  public static ArrayList UIS = null;
  public static String DEFAULT_UI = "swt";
  
  public static GlobalManager GM = null;
  public static StartServer start = null;
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    if (start==null) {
      System.setProperty( "sun.net.client.defaultConnectTimeout", "120000");
      System.setProperty( "sun.net.client.defaultReadTimeout", "60000" );
      COConfigurationManager.checkConfiguration();
      start = new StartServer();
      if ((start == null) || (start.getState()==StartServer.STATE_FAULTY))
        System.err.println("Warning: StartServer startup failed. Unable to add torrents via command line");
      else
        start.start();

      CommandLine commands = null;
      CommandLineParser parser = new PosixParser();
      Options options = new Options();
      options.addOption("h", "help", false, "Show this help.");
      options.addOption(OptionBuilder.withLongOpt("ui")
                                     .withDescription("Run <uis>. ',' separated list of user interfaces to run. The first one given will respond to requests without determinable source UI (e.g. further torrents added via command line).\r\nAvailable: swt (default), web, console")
                                     .withValueSeparator()
                                     .withArgName("uis")
                                     .hasArg()
                                     .create('u'));
      try {
        commands = parser.parse(options, args, true);
      } catch( ParseException exp ) {
        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        System.exit(2);
      }
      
      if (commands.hasOption('h')) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java org.gudy.azureus2.ui.common.Main", options, true);
        System.exit(0);
      }

      UIS = new ArrayList();
      if (commands.hasOption('u')) {
        String uinames = commands.getOptionValue('u');
        if (uinames.indexOf(',')==-1) {
          UIS.add(UserInterfaceFactory.getUI(uinames));
        } else {
          StringTokenizer stok = new StringTokenizer(uinames, ",");
          while (stok.hasMoreTokens()) {
            String uin = stok.nextToken();
            UIS.add(UserInterfaceFactory.getUI(uin));
          }
        }
      } else {
        UIS.add(UserInterfaceFactory.getUI(DEFAULT_UI));
      }

      Iterator uis = UIS.iterator();
      boolean isFirst = true;
      String [] theRest = commands.getArgs();
      while (uis.hasNext()) {
        IUserInterface ui = (IUserInterface) uis.next();
        ui.init(isFirst, (UIS.size()>1));
        theRest = ui.processArgs(theRest);
        isFirst = false;
      }

      GM = GlobalManagerFactory.create();

      uis = UIS.iterator();
      while (uis.hasNext())
        ((IUserInterface) uis.next()).startUI();
      
      openTorrents(theRest);
      
    } else new StartSocket(args);
  }
  
  public static void shutdown() {
    if (start!=null)
      start.stopIt();
    if (GM!=null)
      GM.stopAll();
    System.exit(0);
  }
  
  public static void openTorrents(String[] torrents) {
    if ((Main.UIS!=null) && (!Main.UIS.isEmpty()) && (torrents.length>0)) {
      for(int l=0; l<torrents.length; l++) {
        ((IUserInterface) Main.UIS.get(0)).openTorrent(torrents[l]);
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
