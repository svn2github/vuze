/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * 
 * Main.java
 * 
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.web2;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import seda.sandStorm.main.Sandstorm;
import seda.sandStorm.main.SandstormConfig;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.common.util.LGLogger2Log4j;
import org.gudy.azureus2.ui.common.util.SLevel;
import org.gudy.azureus2.ui.web2.util.LegacyHashtable;
import org.gudy.azureus2.ui.web2.util.WebLogAppender;

/**
 * @author Tobias Minich
 */
public class UI extends org.gudy.azureus2.ui.common.UITemplateHeadless implements ILocaleUtilChooser, IUserInterface, WebConst {

  public static int numRequests;
  public static int numErrors;

  // Cache statistics
  public static int numStaticRequests;
  public static int numCacheHits;
  public static int numCacheMisses;
  public static int cacheSizeBytes;
  public static int cacheSizeEntries;

  // Profiling
  public static long timeCacheLookup;
  public static int numCacheLookup;
  public static long timeCacheAllocate;
  public static int numCacheAllocate;
  public static long timeCacheReject;
  public static int numCacheReject;
  public static long timeFileRead;
  public static int numFileRead;

  public static int numConnectionsEstablished;
  public static int numConnectionsClosed;

  public static List logList = new LinkedList();
  public static final Logger logger = Logger.getLogger("azureus2.ui.web");

  SandstormConfig cfg = null;
  Sandstorm storm = null;
  public static Hashtable messagetextmap = null;
  public static Hashtable parameterlegacy = null;
  public static Hashtable status = null;

  static {
    parameterlegacy = new LegacyHashtable();
    parameterlegacy.put("Core_sOverrideIP", "Override Ip");
    parameterlegacy.put("Core_bAllocateNew", "Zero New");
    parameterlegacy.put("Core_iLowPort", "Low Port");
    parameterlegacy.put("Core_iHighPort", "High Port");
    parameterlegacy.put("Core_iMaxActiveTorrents", "max active torrents");
    parameterlegacy.put("Core_iMaxDownloads", "max downloads");
    parameterlegacy.put("Core_iMaxClients", "Max Clients");
    parameterlegacy.put("Core_iMaxUploads", "Max Uploads");
    parameterlegacy.put("Core_iMaxUploadSpeed", "Max Upload Speed");
    parameterlegacy.put("Core_bUseResume", "Use Resume");
    parameterlegacy.put("Core_iSaveResumeInterval", "Save Resume Interval");
    parameterlegacy.put("Core_bIncrementalAllocate", "Enable incremental file creation");
    parameterlegacy.put("Core_bCheckPiecesOnCompletion", "Check Pieces on Completion");
    parameterlegacy.put("Core_iSeedingShareStop", "Stop Ratio");
    parameterlegacy.put("Core_iSeedingRatioStop", "Stop Peers Ratio");
    parameterlegacy.put("Core_iSeedingRatioStart", "Start Peers Ratio");
    //parameterlegacy.put("Core_bDisconnectSeed", "Disconnect Seed");
    parameterlegacy.put("Core_bSwitchPriority", "Switch Priority");
    parameterlegacy.put("Core_sPriorityExtensions", "priorityExtensions");
    messagetextmap = new LegacyHashtable();
    messagetextmap.put("allocatenew", "zeronewfiles");
    messagetextmap.put("lowport", "serverportlow");
    messagetextmap.put("highport", "serverporthigh");
    messagetextmap.put("useresume", "usefastresume");
    messagetextmap.put("enableincrementalfilecreation", "incrementalfile");
    messagetextmap.put("checkpiecesoncompletion", "checkOncompletion");
    messagetextmap.put("stopratio", "stopRatio");
    messagetextmap.put("stoppeersratio", "stopRatioPeers");
    messagetextmap.put("startpeersratio", "startRatioPeers");
    status = new Hashtable();
    status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
    status.put(new Integer(DownloadManager.STATE_INITIALIZING), MessageText.getString("Main.download.state.waiting"));
    status.put(new Integer(DownloadManager.STATE_INITIALIZED), MessageText.getString("Main.download.state.waiting"));
    status.put(new Integer(DownloadManager.STATE_ALLOCATING), MessageText.getString("Main.download.state.allocating"));
    status.put(new Integer(DownloadManager.STATE_CHECKING), MessageText.getString("Main.download.state.checking"));
    status.put(new Integer(DownloadManager.STATE_READY), MessageText.getString("Main.download.state.ready"));
    status.put(new Integer(DownloadManager.STATE_DOWNLOADING), MessageText.getString("Main.download.state.downloading"));
    status.put(new Integer(DownloadManager.STATE_WAITING), MessageText.getString("Main.download.state.waiting"));
    status.put(new Integer(DownloadManager.STATE_SEEDING), MessageText.getString("Main.download.state.seeding"));
    status.put(new Integer(DownloadManager.STATE_STOPPING), MessageText.getString("Main.download.state.stopped"));
    status.put(new Integer(DownloadManager.STATE_STOPPED), MessageText.getString("Main.download.state.stopped"));
    status.put(new Integer(DownloadManager.STATE_ERROR), MessageText.getString("Main.download.state.error"));
  }

  /** Creates a new instance of Main */
  public UI() {
  }

  public void init(boolean first, boolean others) {
    super.init(first, others);
    System.setProperty("java.awt.headless", "true");
    String defaultargs[] = {
      // Default URL
      "defaultURL=index.html",
      // Http Port
      "listen_port=" + Integer.toString(COConfigurationManager.getIntParameter("Server_iPort")),
      // Max http requests. -1 = unlimited
      "maxRequests=-1",
      // Max Connections. -1 = unlimited
      "maxConnections=-1",
      //"+Integer.toString(COConfigurationManager.getIntParameter("Server_iMaxHTTPConnections")),
      "maxSimultaneousRequests=-1",
      // Max chache size
      "maxCacheSize=204800",
      // Number of buffers
      "numBuffers=1024",
      // Server name
      "serverName=" + COConfigurationManager.getStringParameter("Server_sName") /* + " v1.0\r\n" + "Cache-Control: no-cache, must-revalidate\r\nConnection: close"*/
      ,
      // Stats URL
      "specialURL=/stats",
      // Fake pass through URL
      "fake_local_server=" + COConfigurationManager.getStringParameter("Server_sAccessHost"),
      // Html root dir
      "rootDir=" + FileUtil.getApplicationPath() + "template" };
    this.cfg = new SandstormConfig();
    cfg.putInt("global.threadPool.initialThreads", 1);
    cfg.putInt("global.threadPool.minThreads", 1);
    cfg.putInt("global.threadPool.maxThreads", 20);
    cfg.putInt("global.threadPool.blockTime", 1000);
    cfg.putBoolean("global.threadPool.sizeController.autoMaxDetect", false);
    cfg.putBoolean("global.threadPool.sizeController.enable", true);
    cfg.putInt("global.threadPool.sizeController.delay", 2000);
    cfg.putInt("global.threadPool.sizeController.threshold", 10);
    cfg.putInt("global.threadPool.sizeController.idleTimeThreshold", 1000);
    cfg.putBoolean("global.batchController.enable", false);
    cfg.putBoolean("global.profile.enable", false);
    cfg.putBoolean("global.aSocket.enable", true);
    /*    try {
          System.loadLibrary("NBIO");
          cfg.putString("global.aSocket.provider", "NBIO");
        } catch (UnsatisfiedLinkError e) {
    */
    cfg.putString("global.aSocket.provider", "NIO");
    //    }
    cfg.putBoolean("global.aDisk.enable", true);
    cfg.putInt("global.aDisk.threadPool.initialThreads", 1);
    cfg.putBoolean("global.aDisk.threadPool.sizeController.enable", true);
    cfg.putInt("global.aDisk.threadPool.sizeController.delay", 1000);
    cfg.putInt("global.aDisk.threadPool.sizeController.threshold", 20);
    try {
      /*
      cfg.addStage("HttpRecv", STAGES + "http.HttpRecv", defaultargs);
      cfg.addStage("HttpSend", STAGES + "http.HttpSend", defaultargs);
      cfg.addStage("HttpCommand", STAGES + "http.HttpCommand", defaultargs);
      cfg.addStage("CacheStage", STAGES + "cache.PageCacheSized", defaultargs);
      cfg.addStage("ResouceReader", STAGES + "cache.ResourceReader", defaultargs);
      cfg.addStage("DynamicHttp", STAGES + "hdapi.WildcardDynamicHttp", defaultargs);
      cfg.addStage("HttpProxy", STAGES + "proxy.HttpProxy", defaultargs);
      */
      cfg.addStage(COMMAND_STAGE, STAGES + "http.httpCommandHandler", defaultargs);
      cfg.addStage(CACHE_STAGE, STAGES + "cache.PageCacheSized", defaultargs);
      cfg.addStage(RESOURCE_STAGE, STAGES + "cache.ResourceReader", defaultargs);
      cfg.addStage(HTTP_HANDLER_STAGE, STAGES + "http.httpRequestHandler", defaultargs);
      cfg.addStage(DYNAMIC_HTTP_STAGE, STAGES + "hdapi.WildcardDynamicHttp", defaultargs);
      cfg.addStage(HTTP_SERVER_STAGE, STAGES + "http.httpProxyServer", new String[] { //
        "listen_port=" + Integer.toString(COConfigurationManager.getIntParameter("Server_iPort")), //
        "fake_local_server=" + COConfigurationManager.getStringParameter("Server_sAccessHost"), //
        "rtController.enable=false" //
      });

    } catch (Exception e) {
      logger.fatal("Webinterface configuration failed: " + e.getMessage(), e);
    }

  }

  public String[] processArgs(String[] args) {
    return args;
  }

  public void startUI() {
    TorrentDownloaderFactory.initManager(UIConst.GM, true, true);
    if ((!isStarted()) || (storm == null)) {
      try {
        this.storm = new Sandstorm(this.cfg);
        super.startUI();
        initLoggers();
        System.out.println("Running on port " + COConfigurationManager.getIntParameter("Server_iPort"));
      } catch (Exception e) {
        logger.fatal("Startup of webinterface failed: " + e.getMessage(), e);
      }
    }
  }

  public void openTorrent(String fileName) {
    try {
      if (!FileUtil.isTorrentFile(fileName)) { //$NON-NLS-1$
        logger.error(fileName + " doesn't seem to be a torrent file. Not added.");
        return;
      }
    } catch (Exception e) {
      logger.error("Something is wrong with " + fileName + ". Not added. (Reason: " + e.getMessage() + ")");
      return;
    }
    if (UIConst.GM != null) {
      try {
        UIConst.GM.addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
      } catch (Exception e) {
        logger.error("The torrent " + fileName + " could not be added.", e);
      }
    }
  }

  public static void initLoggers() {
    //Logger.getRootLogger().removeAllAppenders();
    Logger.getRootLogger().removeAppender("WebLogAppender");
    Logger.getRootLogger().removeAppender("LogFileAppender");
    //BasicConfigurator.configure();
    Appender app;
    app = new WebLogAppender(logList);
    app.setName("WebLogAppender");
    Logger.getRootLogger().addAppender(app);
    if (COConfigurationManager.getBooleanParameter("Server_bLogFile")) {
      try {
        app = new FileAppender(new PatternLayout(), COConfigurationManager.getStringParameter("Server_sLogFile"), true);
        app.setName("LogFileAppender");
        Logger.getRootLogger().addAppender(app);
      } catch (Exception e) {
      }
    }
    LGLogger2Log4j.core.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelCore")));
    logger.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelWebinterface")));
  }

}
