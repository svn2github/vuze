/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.web2;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import seda.sandStorm.main.Sandstorm;
import seda.sandStorm.main.SandstormConfig;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.ILocaleUtilChooser;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.common.util.LGLogger2Log4j;
import org.gudy.azureus2.ui.common.util.SLevel;
import org.gudy.azureus2.ui.web2.stages.http.HttpRecv;
import org.gudy.azureus2.ui.web2.stages.http.HttpSend;
import org.gudy.azureus2.ui.web2.util.WebLogAppender;

/**
 *
 * @author  Tobias Minich
 */
public class UI extends org.gudy.azureus2.ui.common.UITemplateHeadless implements ILocaleUtilChooser,IUserInterface {
  
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

  public static HttpRecv httpRecv;
  public static HttpSend httpSend;
  
  public static List logList = new LinkedList();
  public static final Logger logger = Logger.getLogger("azureus2.ui.web");
  
  SandstormConfig cfg = null;
  Sandstorm storm = null;
  
  /** Creates a new instance of Main */
  public UI() {
  }
  
  public void init(boolean first, boolean others) {
    super.init(first,others);
    System.setProperty("java.awt.headless", "true");
    String defaultargs[] = {
      "defaultURL=index.html",
      "httpPort="+Integer.toString(COConfigurationManager.getIntParameter("Server_iPort")),
      "maxRequests=-1",
      "maxConnections=-1",//"+Integer.toString(COConfigurationManager.getIntParameter("Server_iMaxHTTPConnections")),
      "maxSimultaneousRequests=-1",
      "maxCacheSize=204800",
      "numBuffers=1024",
      "serverName="+COConfigurationManager.getStringParameter("Server_sName")+" v1.0\r\n"+
      "Cache-Control: no-cache, must-revalidate\r\nConnection: close",
      "specialURL=/stats",
      "bottleneckURL=/bottleneck",
      "rootDir=/home/tobi/devel/azureus2/org/gudy/azureus2/ui/web/template/"
    };
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
    try {
      System.loadLibrary("NBIO");
      cfg.putString("global.aSocket.provider", "NBIO");
    } catch (Exception e) {
      cfg.putString("global.aSocket.provider", "NIO");
    }
    cfg.putBoolean("global.aDisk.enable", true);
    cfg.putInt("global.aDisk.threadPool.initialThreads", 1);
    cfg.putBoolean("global.aDisk.threadPool.sizeController.enable", true);
    cfg.putInt("global.aDisk.threadPool.sizeController.delay", 1000);
    cfg.putInt("global.aDisk.threadPool.sizeController.threshold", 20);
    try {
      //cfg.addStage("HttpRecv", "seda.apps.Haboob.http.HttpRecv", defaultargs);
      cfg.addStage("HttpRecv", "org.gudy.azureus2.ui.web2.stages.http.HttpRecv", defaultargs);
      //cfg.addStage("HttpSend", "seda.apps.Haboob.http.HttpSend", defaultargs);
      cfg.addStage("HttpSend", "org.gudy.azureus2.ui.web2.stages.http.HttpSend", defaultargs);
      //cfg.addStage("BottleneckStage", "seda.apps.Haboob.bottleneck.Bottleneck", defaultargs);
      //cfg.putInt("BottleneckStage.threadPool.initialThreads", 1);
      //cfg.putInt("BottleneckStage.threadPool.sizeController.threshold", 20);
      //cfg.addStage("CacheStage", "seda.apps.Haboob.cache.PageCacheSized", defaultargs);
      //cfg.addStage("CacheStage", "seda.apps.Haboob.cache.AFileRead", defaultargs);
      //cfg.addStage("CacheStage", "org.gudy.azureus2.ui.web2.stages.cache.AFileRead", defaultargs);
      cfg.addStage("CacheStage", "org.gudy.azureus2.ui.web2.stages.cache.PageCacheSized", defaultargs);
      //seda.sandStorm.lib.http.httpResponse.setDefaultHeader("Cache-Control: no-cache, must-revalidate\r\nConnection: close\r\n");
      cfg.addStage("DynamicHttp", "org.gudy.azureus2.ui.web2.stages.hdapi.WildcardDynamicHttp", defaultargs);
    } catch (Exception e) {
      logger.fatal("Webinterface configuration failed: "+e.getMessage(), e);
    }      
    
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    TorrentDownloaderFactory.initManager(UIConst.GM, true, true);
    if ((!isStarted()) || (storm==null)) {
      try {
        this.storm = new Sandstorm(this.cfg);
        super.startUI();
        initLoggers();
        System.out.println("Running on port " + COConfigurationManager.getIntParameter("Server_iPort"));
      } catch (Exception e) {
        logger.fatal("Startup of webinterface failed: "+e.getMessage(), e);
      }      
    }
  }
  
  public void openTorrent(String fileName) {
    try {
      if (!FileUtil.isTorrentFile(fileName)) {//$NON-NLS-1$
        logger.error(fileName+" doesn't seem to be a torrent file. Not added.");
        return;
      }
    } catch (Exception e) {
      logger.error("Something is wrong with "+fileName+". Not added. (Reason: "+e.getMessage()+")");
      return;
    }
    if (UIConst.GM!=null) {
      try {
        UIConst.GM.addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
      } catch (Exception e) {
        logger.error("The torrent "+fileName+" could not be added.", e);
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
      try{
        app = new FileAppender(new PatternLayout(), COConfigurationManager.getStringParameter("Server_sLogFile"),true);
        app.setName("LogFileAppender");
        Logger.getRootLogger().addAppender(app);
      }catch (Exception e){}
    }
    LGLogger2Log4j.core.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelCore")));
    logger.setLevel(SLevel.toLevel(COConfigurationManager.getIntParameter("Server_iLogLevelWebinterface")));
  }
  
}
