/* pws.java - Pegasi Web Server main class
 
    Copyright (C) 2000 - 2001 Jan De Luyck & Kris Van Hulle
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import HTML.Template;

import org.gudy.azureus2.core.ConfigurationManager;
import org.gudy.azureus2.core.GlobalManager;
import org.gudy.azureus2.core.Logger;
import org.gudy.azureus2.core.ILoggerListener;

public class MainServer implements ILoggerListener{
  
  /* Constants for the server - DO NOT CHANGE!  (unless needed) */
  private  String crlf = "\r\n";
  private  int defaultReadBufferSize = 1048576;
  
  private  String serverName = "Azureus2 WebInterface";
  private  String serverVersion = "0.1";
  private  String copyRight = "Copyright (C) 2000-2001 Jan De Luyck & Kris Van Hulle";
  
  private  String mainConfigFile = "conf/pws.conf";
  private  String mimeConfigFile = "conf/mimetypes.conf";
  
  public  BufferedWriter errorLog = null;
  public  BufferedWriter accessLog = null;
  
  private  Map serverSettings = new HashMap();  /* general server settings */
  private  Map mimeTypes = new HashMap();       /* mimetype configuration */
  private  Map actionHandlers = new HashMap();  /* action handlers for certain mimetypes/extensions */
  
  /* 0 -> no output, 1 -> error output, 2 -> all output */
  private  int verbosity = 2;
  private  boolean useLogFiles = true;
  
  private  int amountOfThreads = 0;
  
  private  boolean haltOnConfigErrors = false;
  
  public  Vector          allowedURLs = new Vector();
  public  HashMap         htmlTemplates = new HashMap();
  private boolean         bquitServer = false;
  private ServerSocket    theSocketServer = null;
  private GlobalManager   gm;
  private Logger          logger;
  public  Locale          locale = new Locale("", "");
  public  List            logList = new LinkedList();
  public  Date            startTime = new Date();
  
  public  String crlf() {
    return crlf;
  }
  
  public  int verbosity() {
    return verbosity;
  }
  
  public  void setVerbosity(int value) {
    verbosity = value;
  }
  
  public  boolean useLogFiles() {
    return useLogFiles;
  }
  
  public  void setUseLogFiles(boolean value) {
    useLogFiles = value;
  }
  
  public  int amountOfThreads() {
    return amountOfThreads;
  }
  
  public  void setAmountOfThreads(int value) {
    amountOfThreads = value;
  }
  
  public  String getActionHandler(String extension) {
    return (String) actionHandlers.get(extension.toLowerCase());
  }
  
  public void putSysMessage(Level type, String message) {
/*        String typeStr;
        switch (type) {
            case 0: typeStr = "MESG"; break;
            case 1: typeStr = "WARNING"; break;
            case 2: typeStr = "<-->"; break;
            case 3: typeStr = "THREAD"; break;
            case 4: typeStr = "HTTP MESSAGE"; break;
            case 5: typeStr = "FILE"; break;
            case 6: typeStr = "CONF"; break;
            case 7: typeStr = "HELP"; break;
            case 8: typeStr = "ERROR"; break;
            case 9: typeStr = "TORRENT RECEIVED"; break;
            case 10: typeStr = "TORRENT SENT"; break;
            default: typeStr = "-----"; break;
        }
 */
    SimpleDateFormat temp = new SimpleDateFormat("HH:mm:ss");
    String text = "[" + temp.format(new Date()) + "] " + type.getName() + " -- " + message;
    
    if (type.equals(Level.SEVERE)) {
      System.err.println(text);
      logList.add(new LogRecord(type, message));
    } else {
      if (type.equals(Level.WARNING)) {
        if (this.verbosity() > 1) {
          System.err.println(text);
          logList.add(new LogRecord(type, message));
        }
      }
      else if (type.equals(SLevel.TORRENT_RECIEVED) || type.equals(SLevel.TORRENT_SENT)) {
        if (this.verbosity() > 0) {
          System.err.println(text);
          logList.add(new LogRecord(type, message));
        }
      }
      else {
        if (this.verbosity() > 2) {
          System.out.println(text);
          logList.add(new LogRecord(type, message));
        }
      }
    }
    
    if (logList.size() > ConfigurationManager.getInstance().getIntParameter("Server_iLogCount"))
      logList.remove(0);
    
/*        if (this.useLogFiles() == true) {
            try {
                switch (type) {
                    case 1: case 8: {
                        if (this.errorLog != null) {
                            this.errorLog.write(text);
                            this.errorLog.newLine();
                            this.errorLog.flush();
                        }
                        break;
                    }
                    case 5: {
                        if (this.accessLog != null) {
                            this.accessLog.write(text);
                            this.accessLog.newLine();
                            this.accessLog.flush();
                        }
                        break;
                    }
                }
            }
            catch (IOException e)
            {}
        }
 */    }
  
  public void log(int componentId,int event,int color,String text) {
    if (event == Logger.ERROR)
      this.putSysMessage(Level.SEVERE, text);
    else if (event == Logger.RECEIVED)
      this.putSysMessage(SLevel.TORRENT_RECIEVED, text);
    else if (event == Logger.SENT)
      this.putSysMessage(SLevel.TORRENT_SENT, text);
    else
      this.putSysMessage(Level.INFO, text);
  }
  
  public  String getMimeType(String extension) {
    String mimeType = (String) mimeTypes.get(extension.toLowerCase());
    if (mimeType == null)
      return "text/html";
    else
      return mimeType;
  }
  
  public String getContentType(String filename)
  /*	extracts and returns the mimetype of files */
  {
    String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(); /* get position of last part of file (aka extension) */
    String mimeType = getMimeType(extension);
    putSysMessage(Level.INFO, extension + " --> " + mimeType);
    return mimeType;
  }
  
  //    public  String getSetting(String keyWord) {
  //        return (String) serverSettings.get(keyWord.toLowerCase());
  //    }
  
  public  String serverName() {
    return serverName;
  }
  
  public  String serverVersion() {
    return serverVersion;
  }
  
  public  int defaultReadBufferSize() {
    return defaultReadBufferSize;
  }
  
  //    public String getApplicationPath() {
  //        return System.getProperty("user.dir") + System.getProperty("file.separator");
  //    }
  
  private  void loadTemplate(String tmpl) {
    Hashtable args = new Hashtable();
    String[] paths = new String[3];
    
    paths[0]=ConfigurationManager.getInstance().getStringParameter("Server_sTemplate_Directory");
    paths[1]=ConfigurationManager.getApplicationPath()+"org/gudy/azureus2/server/template";
    paths[2]="template";
    //paths[2]="/home/tobi/devel/azureus2/org/gudy/azureus2/server/template";
    
    
    args.put("path", paths);
    args.put("case_sensitive", "true");
    args.put("loop_context_vars", Boolean.TRUE);
    //args.put("debug", Boolean.TRUE);
    
    args.put("filename", tmpl+".tmpl");
    Template t = null;
    try {
      t = new Template(args);
    }
    catch (FileNotFoundException e) {
      putSysMessage(Level.SEVERE, "Loading Template "+tmpl+" failed: "+e);
    }
    catch (IOException e) {
      putSysMessage(Level.SEVERE, "Loading Template "+tmpl+" failed: "+e);
    }
    
    htmlTemplates.put(tmpl, t);
  }
  
  public  void loadTemplates() {
    htmlTemplates.clear();
    loadTemplate("root");
    loadTemplate("menu");
    loadTemplate("torrents");
    loadTemplate("exit");
    loadTemplate("config");
    loadTemplate("dl_fail");
    loadTemplate("log");
  }
  
  public  void quitServer() {
    putSysMessage(Level.INFO, "Quitting... Bye Bye =)");
    bquitServer = true;
    try {
      theSocketServer.close();
    }
    catch (IOException e) {}
  }
  
  private  boolean processCommandLine(String[] commandLine) {
        /* allowed command line options:
            -v -> verbosity increased by 1
                        -n -> no log
            -h -> help
         */
    
    Map validOptions = new HashMap();
    validOptions.put("-v", "0");
    validOptions.put("--verbose", "0");
    validOptions.put("-h", "1");
    validOptions.put("--help", "1");
    validOptions.put("-n", "2");
    validOptions.put("--nolog", "2");
    validOptions.put("-i", "3");
    validOptions.put("--ignore", "3");
    
    boolean noLog = false;
    int verbosityLevel = 0;
    boolean outputHelp = false;
    String illegalOptions = "";
    boolean returnValue = false;
    
    setUseLogFiles(true);
    
    for (int i=0; i < commandLine.length; i++) {
      commandLine[i] = commandLine[i].toLowerCase().trim();
      
      if (validOptions.get(commandLine[i]) != null) {
        switch(Integer.parseInt(validOptions.get(commandLine[i]).toString())) {
          case 0: {
            /* increase verbosity level by 1*/
            verbosityLevel++;
            returnValue = false;
            break;
          }
          case 1: {
            /* output help */
            outputHelp = true;
            returnValue = true;
            break;
          }
          case 2: {
            /* disable loggin tot he log files */
            setUseLogFiles(false);
            break;
          }
          case 3: {
            /* don't halt when a server config error has been detected */
            haltOnConfigErrors = false;
            break;
          }
        }
      }
      else {
        outputHelp = true;
        illegalOptions += commandLine[i] + " ";
        returnValue = true;
      }
    }
    
    if (verbosityLevel > 3) verbosityLevel = 3;
    
    if (illegalOptions.length() > 0)
      System.out.println("Illegal options: " + illegalOptions);
    
    if (outputHelp == true) {
      System.out.println("");
      System.out.println(serverName + " version " + serverVersion + ",\n" + copyRight);
      System.out.println(serverName + " comes with ABSOLUTELY NO WARRANTY. \nFor more details, see the GNU");
      System.out.println("license distributed with this server.");
      System.out.println("This is free software, and you are welcome to redistribute it.");
      System.out.println("");
      System.out.println("usage: pws [-v|--verbose] [-h|--help] [-n|--nolog] [-i|--ignore]");
      System.out.println("");
      System.out.println("-v and --verbose: increase verbosity level by 1;");
      System.out.println("-h and --help   : show this help page;");
      System.out.println("-n and --nolog  : disables the use of the logfiles on harddisk;");
      System.out.println("-i and --ignore : ignores any misconfigurations that might be present.");
      System.out.println("                  (USE AT OWN RISK!)");
      System.out.println("");
    }
    
    if (verbosityLevel > 0)
      verbosity = verbosityLevel;
    else
      verbosity = ConfigurationManager.getInstance().getIntParameter("Server_iVerbosity");
    return returnValue;
  }
  
  
  MainServer(String argv[]) {
    Server aThread;
    
    System.out.println(serverName + " v" + serverVersion + " Starting...\n");
    
    if (processCommandLine(argv) == false) {
      /* parse configfile (main config file) */
      putSysMessage(Level.INFO,"Parsing main configuration file...");
      //    	ConfProcessor.parseConfigurationFile(mainConfigFile,serverSettings, actionHandlers, mimeTypes);
      
      /* check parsed configuration information */
      putSysMessage(Level.INFO,"Checking configuration data...");
      
      gm = new GlobalManager();
      logger = Logger.getLogger();
      logger.setListener(this);
      
      allowedURLs.add("/");
      allowedURLs.add("/exit");
      allowedURLs.add("/menu");
      allowedURLs.add("/torrents");
      allowedURLs.add("/config");
      allowedURLs.add("/log");
      allowedURLs.add("/favicon.ico");
      loadTemplates();
      
      //		int checkValue = ConfProcessor.checkConfigurationData(serverSettings);
      
      if (this.haltOnConfigErrors == false /*|| checkValue == 0*/) {
        
        ConfigurationManager config = ConfigurationManager.getInstance();
        
        /* parse mimetype configuration file */
        putSysMessage(Level.INFO, "Parsing mime-types...");
        //		    ConfProcessor.parseMimeTypes(mimeConfigFile, mimeTypes);
        
        putSysMessage(Level.INFO,"Main Listener Loop Started.");
        putSysMessage(Level.INFO,"Opening port " + String.valueOf(config.getIntParameter("Server_iPort")) + "...");
        
        /* declare socketserver (port listener) and socketreturn */
        Socket socketReturn = null;
        
        /* attempt to open port */
        try {
          if (config.getStringParameter("Server_sBindIP").equals(""))
            theSocketServer= new ServerSocket(config.getIntParameter("Server_iPort"));
          else
            theSocketServer= new ServerSocket(config.getIntParameter("Server_iPort"),0,InetAddress.getByName(config.getStringParameter("Server_sBindIP")));
        }
        catch (IOException e) {
          if (e.toString().substring(e.toString().indexOf(':') + 1).trim().equalsIgnoreCase("Address already in use"))
            putSysMessage(Level.SEVERE, "The port was is already in use. Please use another port.");
          
          putSysMessage(Level.WARNING,"IO Error: " + e.getMessage());
        }
        
                        /* if we get this far, it seems that the port was available, so we start
                        to listen upon it :-)
                        NOTE: this function holds further processing until a connection is established
                        or something else happends (system crash/nerveous breakdown/world blown up/etc...).
                         
                        The server is NOT started (aka no accept is done) if the port could not be opened.
                         */
        
        while (theSocketServer != null && !bquitServer) {
          try {
            socketReturn = theSocketServer.accept();
          }
          catch (IOException e) {
            putSysMessage(Level.WARNING, "IO Error: " + e.getMessage());
          }
          
          /* ok, a client connection was detected. */
          putSysMessage(Level.INFO,"Client connected from " + socketReturn.getInetAddress() + "...");
          
                                /*instantiate a new thread for this client connection...
                                  the new threads are 'daemon' style, so they die with the main thread :-) */
          if (amountOfThreads < config.getIntParameter("Server_iMaxHTTPConnections")) {
            aThread = new Server(socketReturn, socketReturn.getInetAddress(), gm, this);
            aThread.setDaemon(true);
            aThread.start();
          }
          else
            putSysMessage(Level.WARNING,"Maximum client connections reached.");
        }
      }
      gm.stopAll();
    }
  }
  
  public static void main(String argv[]) {
    
        /* if this returns true, stop server, otherwise go on doing whatever it wanted to do in
           this lifetime */
    /* start the server */
    MainServer mainServer = new MainServer(argv);
  }
}