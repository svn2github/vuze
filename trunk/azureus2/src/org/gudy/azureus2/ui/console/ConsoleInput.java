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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.text.DecimalFormat;

import java.util.Iterator;
import java.util.ArrayList;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.DenyAllFilter;

import org.gudy.azureus2.core.DownloadManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.PeerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;

/**
 *
 * @author  Tobias Minich
 */
public class ConsoleInput extends Thread {
  
  GlobalManager gm;
  BufferedReader br;
  PrintStream out;
  ArrayList torrents = null;
  
  /** Creates a new instance of ConsoleInput */
  public ConsoleInput(GlobalManager _gm, InputStream _in, PrintStream _out) {
    super("Console Input");
    gm = _gm;
    out = _out;
    br = new BufferedReader(new InputStreamReader(_in));
    start();
  }
  
  public static void printconsolehelp(PrintStream os) {
    os.println("Available console commands:");
    os.println(".\t\tRepeats last command.");
    os.println("help [torrents]\tShow this help. 'torrents' shows info about the show torrents display.");
    os.println("log (on|off)\tTurn on/off console logging");
    os.println("show torrents\tShow running torrents.");
    os.println("start (#|all)\tStart torrent(s).");
    os.println("stop (#|all)\tStop torrent(s).");
    os.println("quit\t\tShutdown Azureus");
  }
  
  public void run() {
    String s = null;
    String oldcommand = null;
    String command;
    String subcommand = "";
    while (true) {
      try {
        s = br.readLine();
      } catch (Exception e) {}
      if (s!=null) {
        if (oldcommand != null) {
          if (s.equals("."))
            s = oldcommand;
        }
        oldcommand = s;
        if (s.indexOf(" ")==-1) {
          command = s;
          subcommand = null;
        } else {
          command = s.substring(0, s.indexOf(" "));
          subcommand = s.substring(s.indexOf(" ")+1);
        }
        if (command.equalsIgnoreCase("help")) {
          out.println("> -----");
          if (subcommand==null) {
            printconsolehelp(out);
          } else {
            if (subcommand.equalsIgnoreCase("torrents")) {
              out.println("# [state] PercentDone Name (Filesize) ETA\r\n\tDownSpeed / UpSpeed\tDownloaded/Uploaded\tConnectedSeeds(total) / ConnectedPeers(total)");
              out.println();
              out.println("States:");
              out.println(" > Downloading");
              out.println(" * Seeding");
              out.println(" ! Stopped");
              out.println(" A Allocating");
              out.println(" C Checking");
              out.println(" E Error");
              out.println(" I Initializing");
              out.println(" ? Unknown");
            } else
              printconsolehelp(out);
          }
          out.println("> -----");
        } else if (command.equalsIgnoreCase("quit"))  {
          org.gudy.azureus2.ui.common.Main.shutdown();
        } else if (command.equalsIgnoreCase("show")) {
          if (subcommand != null) {
            if (subcommand.equalsIgnoreCase("torrents")) {
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
                PeerStats ps;
                int nrTorrent = 0;
                while (torrent.hasNext()) {
                  dm = (DownloadManager) torrent.next();
                  TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
                  dmstate = dm.getState();
                  try {
                    ps = dm.peerManager.getStats();
                  } catch (Exception e) {ps = null;}
                  if (ps != null) {
                    totalReceived += ps.getTotalReceivedRaw();
                    totalSent += ps.getTotalSentRaw();
                    totalDiscarded += ps.getTotalDiscardedRaw();
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
                  else
                    tstate+="?";
                  tstate+="] ";
                  DecimalFormat df = new DecimalFormat("000.0%");
                  tstate+=df.format(dm.getCompleted()/1000.0);
                  tstate+=" ";
                  if (dmstate == DownloadManager.STATE_ERROR)
                    tstate+=dm.getErrorDetails();
                  else {
                    if (dm.getName()==null)
                      tstate+="?";
                    else
                      tstate+=dm.getName();
                  }
                  tstate+=" ("+DisplayFormatters.formatByteCountToKBEtc(dm.getSize())+") "+dm.getETA()+"\r\n\t";
                  tstate+=dm.getDownloadSpeed()+" / ";
                  tstate+=dm.getUploadSpeed()+"\t";
                  tstate+=dm.getDownloaded()+" / ";
                  tstate+=dm.getUploaded()+"\t";
                  if (hd == null) {
                    tstate+=Integer.toString(dm.getNbSeeds())+"(?) / ";
                    tstate+=Integer.toString(dm.getNbPeers())+"(?)";
                  } else {
                    tstate+=Integer.toString(dm.getNbSeeds())+"("+Integer.toString(hd.getSeeds())+") / ";
                    tstate+=Integer.toString(dm.getNbPeers())+"("+Integer.toString(hd.getPeers())+")";
                  }
                  out.println(tstate);
                  out.println();
                }
                out.println("Total Speed (down/up): "+gm.getDownloadSpeed()+" / "+gm.getUploadSpeed());
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
        } else if (command.equalsIgnoreCase("start")) {
          if (subcommand != null) {
            if (torrents.isEmpty()) {
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
                } else {
                  out.println("> Command 'start': Subcommand '"+subcommand+"' unknown.");
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'start'\r\n> start syntax: start (#|all)");
          }
        } else if (command.equalsIgnoreCase("stop")) {
          if (subcommand != null) {
            if (torrents.isEmpty()) {
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
                } else {
                  out.println("> Command 'stop': Subcommand '"+subcommand+"' unknown.");
                }
              }
            }
          } else {
            out.println("> Missing subcommand for 'stop'\r\n> stop syntax: stop (#|all)");
          }
        } else if (command.equalsIgnoreCase("log")) {
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
          Logger.getLogger("azureus2.webinterface").fatal("Logging test"+((subcommand==null)?"":": "+subcommand));
        } else {
          out.println("> Command '"+command+"' unknown (or . used without prior command)");
        }
      }
    }
    
  }
  
}
