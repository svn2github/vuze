/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * ConsoleInput.java
 *
 * Created on 6. Oktober 2003, 23:26
 */

package org.gudy.azureus2.ui.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import java.text.DecimalFormat;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.DenyAllFilter;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.HTTPDownloader;

import org.pf.file.FileFinder;

/**
 *
 * @author  Tobias Minich
 */
public class ConsoleInput extends Thread {
  
  GlobalManager gm;
  CommandReader br;
  PrintStream out;
  ArrayList torrents = null;
  File[] adds = null;
  boolean controlling;
  
  /** Creates a new instance of ConsoleInput */
  public ConsoleInput(String con, GlobalManager _gm, Reader _in, PrintStream _out, boolean _controlling) {
    super("Console Input: "+con);
    gm = _gm;
    out = _out;
    controlling = _controlling;
    br = new CommandReader(_in, new OutputStreamWriter(_out));
    start();
  }
  
  public ConsoleInput(String con, GlobalManager _gm, InputStream _in, PrintStream _out, boolean _controlling) {
    this(con, _gm, new InputStreamReader(_in), _out, _controlling);
  }
  
  public static void printconsolehelp(PrintStream os) {
    os.println("Available console commands:");
    os.println("Command\t\t\tShort\tDescription");
    os.println(".\t\t\t\tRepeats last command (Initially 'show torrents').");
    os.println("help [torrents]\t\t?\tShow this help. 'torrents' shows info about the show torrents display.");
    os.println("log (on|off)\t\tl\tTurn on/off console logging");
    os.println("remove (#|all|hash #)\tr\tRemove torrent(s).");
    os.println("show torrents\t\tsh t\tShow running torrents.");
    os.println("start (#|all|hash #)\ts\tStart torrent(s).");
    os.println("stop (#|all|hash #)\th\tStop torrent(s).");
    os.println("quit\t\t\tq\tShutdown Azureus");
  }
  
  private void quit(boolean finish) {
    if (finish)
      org.gudy.azureus2.ui.common.Main.shutdown();
  }
  
  public void run() {
    String s = null;
    String oldcommand = "sh t";
    String command;
    String subcommand = "";
    boolean running = true;
    while (running) {
      try {
        s = br.readLine();
      } catch (Exception e) {running = false;}
      if (s!=null) {
        if (oldcommand != null) {
          if (s.equals("."))
            s = oldcommand;
        } else {
          if (s.equals("."))
            out.println("No old command. Remove commands are not repeated to prevent errors");
        }
        oldcommand = s;
        if (s.indexOf(" ")==-1) {
          command = s;
          subcommand = null;
        } else {
          command = s.substring(0, s.indexOf(" "));
          subcommand = s.substring(s.indexOf(" ")+1);
        }
        if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("?")) {
          out.println("> -----");
          if (subcommand==null) {
            printconsolehelp(out);
          } else {
            if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
              out.println("# [state] PercentDone Name (Filesize) ETA\r\n\tDownSpeed / UpSpeed\tDownloaded/Uploaded\tConnectedSeeds(total) / ConnectedPeers(total)");
              out.println();
              out.println("States:");
              out.println(" > Downloading");
              out.println(" * Seeding");
              out.println(" ! Stopped");
              out.println(" . Waiting (for allocation/checking)");
              out.println(" : Ready (waiting for a free download slot)");
              out.println(" A Allocating");
              out.println(" C Checking");
              out.println(" E Error");
              out.println(" I Initializing");
              out.println(" ? Unknown");
            } else
              printconsolehelp(out);
          }
          out.println("> -----");
        } else if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("q"))  {
          if (controlling) {
            running = false;
            quit(controlling);
          } else {
            if ((subcommand == null) || (!subcommand.equalsIgnoreCase("IAMSURE")))
              out.println("> The 'quit' command exits azureus. Since this is a non-controlling shell thats probably not what you wanted. Use 'logout' to quit it or 'quit iamsure' to really exit azureus.");
            else
              quit(true);
          }
        } else if (command.equalsIgnoreCase("logout"))  {
          running = false;
        } else if (command.equalsIgnoreCase("show") || command.equalsIgnoreCase("sh")) {
          if (subcommand != null) {
            if (subcommand.equalsIgnoreCase("torrents") || subcommand.equalsIgnoreCase("t")) {
              out.println("> -----");
              torrents = (ArrayList) ((ArrayList)gm.getDownloadManagers()).clone();
              DownloadManager dm;
              int dmstate;
              if (!torrents.isEmpty()) {
                Iterator torrent = torrents.iterator();
                long totalReceived = 0;
                long totalSent = 0;
                long totalDiscarded = 0;
                int connectedSeeds = 0;
                int connectedPeers = 0;
                PEPeerStats ps;
                int nrTorrent = 0;
                while (torrent.hasNext()) {
                  dm = (DownloadManager) torrent.next();
                  TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
                  dmstate = dm.getState();
                  try {
                    ps = dm.getPeerManager().getStats();
                  } catch (Exception e) {ps = null;}
                  if (ps != null) {
                    totalReceived += ps.getTotalReceived();
                    totalSent += ps.getTotalSent();
                    totalDiscarded += ps.getTotalDiscarded();
                    connectedSeeds += dm.getNbSeeds();
                    connectedPeers += dm.getNbPeers();
                  }
                  nrTorrent+=1;
                  String tstate=((nrTorrent<10)?" ":"")+Integer.toString(nrTorrent)+" [";
                  if (dmstate == DownloadManager.STATE_INITIALIZING)
                    tstate+="I";
                  else if (dmstate == DownloadManager.STATE_ALLOCATING)
                    tstate+="A";
                  else if (dmstate == DownloadManager.STATE_CHECKING)
                    tstate+="C";
                  else if (dmstate == DownloadManager.STATE_DOWNLOADING)
                    tstate+=">";
                  else if (dmstate == DownloadManager.STATE_ERROR)
                    tstate+="E";
                  else if (dmstate == DownloadManager.STATE_SEEDING)
                    tstate+="*";
                  else if (dmstate == DownloadManager.STATE_STOPPED)
                    tstate+="!";
                  else if (dmstate == DownloadManager.STATE_WAITING)
                    tstate+=".";
                  else if (dmstate == DownloadManager.STATE_READY)
                    tstate+=":";
                  else
                    tstate+="?";
                  tstate+="] ";
                  DecimalFormat df = new DecimalFormat("000.0%");
                  
                  DownloadManagerStats stats = dm.getStats();
                  
                  tstate+=df.format(stats.getCompleted()/1000.0);
                  tstate+="\t";
                  if (dmstate == DownloadManager.STATE_ERROR)
                    tstate+=dm.getErrorDetails();
                  else {
                    if (dm.getName()==null)
                      tstate+="?";
                    else
                      tstate+=dm.getName();
                  }
                  tstate+=" ("+DisplayFormatters.formatByteCountToKBEtc(dm.getSize())+") ETA:"+stats.getETA()+"\r\n\t\tSpeed: ";
                  tstate+=DisplayFormatters.formatByteCountToKBEtcPerSec(stats.getDownloadAverage())+" / ";
                  tstate+=DisplayFormatters.formatByteCountToKBEtcPerSec(stats.getUploadAverage())+"\tAmount: ";
                  tstate+=DisplayFormatters.formatDownloaded(stats)+" / ";
                  tstate+=DisplayFormatters.formatByteCountToKBEtc(stats.getUploaded())+"\tConnections: ";
                  if (hd == null || ! hd.isValid()) {
                    tstate+=Integer.toString(dm.getNbSeeds())+"(?) / ";
                    tstate+=Integer.toString(dm.getNbPeers())+"(?)";
                  } else {
                    tstate+=Integer.toString(dm.getNbSeeds())+"("+Integer.toString(hd.getSeeds())+") / ";
                    tstate+=Integer.toString(dm.getNbPeers())+"("+Integer.toString(hd.getPeers())+")";
                  }
                  out.println(tstate);
                  //out.println(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true));
                  out.println();
                }
                out.println(	"Total Speed (down/up): "+
								DisplayFormatters.formatByteCountToKBEtcPerSec(gm.getStats().getDownloadAverage())+" / "+
								DisplayFormatters.formatByteCountToKBEtcPerSec(gm.getStats().getUploadAverage()));
								
                out.println("Transferred Volume (down/up/discarded): "+DisplayFormatters.formatByteCountToKBEtc(totalReceived)+" / "+DisplayFormatters.formatByteCountToKBEtc(totalSent)+" / "+DisplayFormatters.formatByteCountToKBEtc(totalDiscarded));
                out.println("Total Connected Peers (seeds/peers): "+Integer.toString(connectedSeeds)+" / "+Integer.toString(connectedPeers));
              } else
                out.println("No Torrents");
              out.println("> -----");
            } else {
              out.println("> Command 'show': Subcommand '"+subcommand+"' unknown.");
            }
          } else {
            out.println("> Missing subcommand for 'show'\r\n> show syntax: show torrents");
          }
        } else if (command.equalsIgnoreCase("add") || command.equalsIgnoreCase("a")) {
          if (subcommand != null) {
            boolean scansubdir = false;
            CommandLine commands = null;
            CommandLineParser parser = new PosixParser();
            Options options = new Options();
            OptionGroup addy = new OptionGroup();
            options.addOption("o", "output", true, "Output Directory.");
            addy.addOption(new Option("r", "recurse", false, "Recurse Subdirs."));
            addy.addOption(OptionBuilder.hasArgs()
                                        .withDescription("Add found file nr x.")
                                        .create('n'));
            options.addOption(new Option("f", "find", false, "Only find files, don't add."));
            options.addOptionGroup(addy);
            try {
              commands = parser.parse(options, subcommand.split(" "), true);
            } catch( ParseException exp ) {
              out.println( "> Parsing add commandline failed. Reason: " + exp.getMessage() );
            }
            if (commands.hasOption('r'))
              scansubdir = true;
            String outputDir = "";
            if (commands.hasOption('o'))
              outputDir = commands.getOptionValue('o');
            else
              try {
                outputDir = COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory");
              } catch(Exception e) {e.printStackTrace();}
            
            boolean Finding = false;
            
            if (commands.hasOption('n')) {
              String[] numbers = commands.getOptionValues('n');
              out.println("> Numbers:");
              for(int i=0; i<numbers.length; i++)
                out.println(numbers[i]);
              String[] whatelse = commands.getArgs();
              out.println("> Else:");
              for(int i=0; i<whatelse.length; i++)
                out.println(whatelse[i]);
            } else {
              String[] whatelse = commands.getArgs();
              for(int j=0; j<whatelse.length; j++) {
                if (whatelse[j].toUpperCase().startsWith("HTTP://")){
                  try {
                    out.println("> Starting Download of "+whatelse[j]+" ...");
                    HTTPDownloader dl = new HTTPDownloader(whatelse[j], COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
                    String file = dl.download();
                    gm.addDownloadManager(file, outputDir);
                    out.println("> Download of "+whatelse[j]+" succeeded");
                  } catch (Exception e) {
                    out.println("> Download of "+whatelse[j]+" failed");
                  }
                } else {
                  File test = new File(whatelse[j]);
                  if (test.exists()) {
                    if (test.isDirectory()) {
                      File[] toadd = FileFinder.findFiles(whatelse[j], "*.torrent", false);
                      if ((toadd != null) && (toadd.length>0)) {
                        for(int i=0;i<toadd.length;i++) {
                          gm.addDownloadManager(toadd[i].getAbsolutePath(), outputDir);
                          out.println("> '"+toadd[i].getAbsolutePath()+"' added.");
                        }
                      } else {
                        out.println("> Directory '"+whatelse[j]+"' seems to contain no torrent files.");
                      }
                    } else {
                      gm.addDownloadManager(whatelse[j], outputDir);
                      out.println("> '"+whatelse[j]+"' added.");
                    }
                  } else {
                    adds = FileFinder.findFiles(whatelse[j].substring(0, whatelse[j].lastIndexOf(System.getProperty("file.separator"))), whatelse[j].substring(whatelse[j].lastIndexOf(System.getProperty("file.separator"))+1), false);
                    if ((adds != null) && (adds.length>0)) {
                      out.println("> Found "+Integer.toString(adds.length)+" files:");
                      for(int i=0;i<adds.length;i++) {
                        try {
                          out.println(adds[i].getCanonicalPath());
                        } catch (Exception e) {
                          out.println(adds[i].getAbsolutePath());
                        }
                      }
                    } else {
                      out.println("> No files found. Searched for '"+subcommand.substring(subcommand.lastIndexOf(System.getProperty("file.separator"))+1)+"' in '"+subcommand.substring(0, subcommand.lastIndexOf(System.getProperty("file.separator"))));
                    }
                  }
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'add'\r\n> add syntax: see 'help add'");
          }
        } else if (command.equalsIgnoreCase("start") || command.equalsIgnoreCase("s")) {
          if (subcommand != null) {
            if ((torrents!=null) && torrents.isEmpty()) {
              out.println("> Command 'start': No torrents in list.");
            } else {
              String name;
              DownloadManager dm;
              try {
                int number = Integer.parseInt(subcommand);
                if ((number>0) && (number<=torrents.size())) {
                  dm = (DownloadManager) this.torrents.get(number-1);
                  if (dm.getName()==null)
                    name="?";
                  else
                    name=dm.getName();
                  dm.startDownloadInitialized(true);
                  out.println("> Torrent #"+subcommand+" ("+name+") started.");
                } else
                  out.println("> Command 'start': Torrent #"+subcommand+" unknown.");
              } catch (NumberFormatException e) {
                if (subcommand.equalsIgnoreCase("all")) {
                  Iterator torrent = torrents.iterator();
                  int nr = 0;
                  while (torrent.hasNext()) {
                    dm = (DownloadManager) torrent.next();
                    if (dm.getName()==null)
                      name="?";
                    else
                      name=dm.getName();
                    dm.startDownloadInitialized(true);
                    out.println("> Torrent #"+Integer.toString(++nr)+" ("+name+") started.");
                  }
                } else if (subcommand.toUpperCase().startsWith("HASH")) {
                  String hash = subcommand.substring(subcommand.indexOf(" ")+1);
                  List torrents = gm.getDownloadManagers();
                  boolean foundit=false;
                  if (!torrents.isEmpty()) {
                    Iterator torrent = torrents.iterator();
                    while (torrent.hasNext()) {
                      dm = (DownloadManager) torrent.next();
                      if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
                        if (dm.getName()==null)
                          name="?";
                        else
                          name=dm.getName();
                        dm.startDownloadInitialized(true);
                        out.println("> Torrent "+hash+" ("+name+") started.");
                        foundit = true;
                        break;
                      }
                    }
                    if (!foundit)
                      out.println("> Command 'start': Hash '"+hash+"' unknown.");
                  }
                } else {
                  out.println("> Command 'start': Subcommand '"+subcommand+"' unknown.");
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'start'\r\n> start syntax: start (#|all)");
          }
        } else if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("h")) {
          if (subcommand != null) {
            if ((torrents!=null) && torrents.isEmpty()) {
              out.println("> Command 'stop': No torrents in list.");
            } else {
              String name;
              DownloadManager dm;
              try {
                int number = Integer.parseInt(subcommand);
                if ((number>0) && (number<=torrents.size())) {
                  dm = (DownloadManager) this.torrents.get(number-1);
                  if (dm.getName()==null)
                    name="?";
                  else
                    name=dm.getName();
                  dm.stopIt();
                  out.println("> Torrent #"+subcommand+" ("+name+") stopped.");
                } else
                  out.println("> Command 'stop': Torrent #"+subcommand+" unknown.");
              } catch (NumberFormatException e) {
                if (subcommand.equalsIgnoreCase("all")) {
                  Iterator torrent = torrents.iterator();
                  int nr = 0;
                  while (torrent.hasNext()) {
                    dm = (DownloadManager) torrent.next();
                    if (dm.getName()==null)
                      name="?";
                    else
                      name=dm.getName();
                    dm.stopIt();
                    out.println("> Torrent #"+Integer.toString(++nr)+" ("+name+") stopped.");
                  }
                } else if (subcommand.toUpperCase().startsWith("HASH")) {
                  String hash = subcommand.substring(subcommand.indexOf(" ")+1);
                  List torrents = gm.getDownloadManagers();
                  boolean foundit=false;
                  if (!torrents.isEmpty()) {
                    Iterator torrent = torrents.iterator();
                    while (torrent.hasNext()) {
                      dm = (DownloadManager) torrent.next();
                      if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
                        if (dm.getName()==null)
                          name="?";
                        else
                          name=dm.getName();
                        dm.stopIt();
                        out.println("> Torrent "+hash+" ("+name+") stopped.");
                        foundit = true;
                        break;
                      }
                    }
                    if (!foundit)
                      out.println("> Command 'stop': Hash '"+hash+"' unknown.");
                  }
                } else {
                  out.println("> Command 'stop': Subcommand '"+subcommand+"' unknown.");
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'stop'\r\n> stop syntax: stop (#|all)");
          }
        } else if (command.equalsIgnoreCase("remove") || command.equalsIgnoreCase("r")) {
          if (subcommand != null) {
            if ((torrents!=null) && torrents.isEmpty()) {
              out.println("> Command 'remove': No torrents in list.");
            } else {
              String name;
              DownloadManager dm;
              try {
                int number = Integer.parseInt(subcommand);
                if ((number>0) && (number<=torrents.size())) {
                  dm = (DownloadManager) this.torrents.get(number-1);
                  if (dm.getName()==null)
                    name="?";
                  else
                    name=dm.getName();
                  dm.stopIt();
                  gm.removeDownloadManager(dm);
                  out.println("> Torrent #"+subcommand+" ("+name+") removed.");
                  oldcommand = null;
                } else
                  out.println("> Command 'remove': Torrent #"+subcommand+" unknown.");
              } catch (NumberFormatException e) {
                if (subcommand.equalsIgnoreCase("all")) {
                  Iterator torrent = torrents.iterator();
                  int nr = 0;
                  while (torrent.hasNext()) {
                    dm = (DownloadManager) torrent.next();
                    if (dm.getName()==null)
                      name="?";
                    else
                      name=dm.getName();
                    dm.stopIt();
                    gm.removeDownloadManager(dm);
                    out.println("> Torrent #"+Integer.toString(++nr)+" ("+name+") removed.");
                  }
                } else if (subcommand.toUpperCase().startsWith("HASH")) {
                  String hash = subcommand.substring(subcommand.indexOf(" ")+1);
                  List torrents = gm.getDownloadManagers();
                  boolean foundit=false;
                  if (!torrents.isEmpty()) {
                    Iterator torrent = torrents.iterator();
                    while (torrent.hasNext()) {
                      dm = (DownloadManager) torrent.next();
                      if (hash.equals(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true))) {
                        if (dm.getName()==null)
                          name="?";
                        else
                          name=dm.getName();
                        dm.stopIt();
                        gm.removeDownloadManager(dm);
                        out.println("> Torrent "+hash+" ("+name+") removed.");
                        foundit = true;
                        break;
                      }
                    }
                    if (!foundit)
                      out.println("> Command 'remove': Hash '"+hash+"' unknown.");
                  }
                } else {
                  out.println("> Command 'remove': Subcommand '"+subcommand+"' unknown.");
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'stop'\r\n> stop syntax: stop (#|all)");
          }
        } else if (command.equalsIgnoreCase("log") || command.equalsIgnoreCase("l")) {
          Appender con = Logger.getRootLogger().getAppender("ConsoleAppender");
          if ((con != null) && (subcommand!=null)) {
            if (subcommand.equalsIgnoreCase("off")) {
              con.addFilter(new DenyAllFilter());
              out.println("> Console logging off");
            } else if (subcommand.equalsIgnoreCase("on")) {
              con.clearFilters();
              out.println("> Console logging on");
            } else {
              out.println("> Command 'log': Subcommand '"+subcommand+"' unknown.");
            }
          } else {
            out.println("> Console logger not found or missing subcommand for 'log'\r\n> log syntax: log (on|off)");
          }
        } else if (command.equalsIgnoreCase("logtest")) {
          Logger.getLogger("azureus2").fatal("Logging test"+((subcommand==null)?"":": "+subcommand));
        } else {
          out.println("> Command '"+command+"' unknown (or . used without prior command)");
        }
      }
    }
  }
}
