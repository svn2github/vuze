/*
 * Created on 13.11.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.web2.stages.http;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.WebConst;
import org.gudy.azureus2.ui.web2.http.request.httpRequest;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.ManagerIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkIF;

/**
 * @author Tobias Minich
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class httpCommandHandler implements WebConst, EventHandlerIF {

  private ManagerIF mgr;
  private SinkIF mysink;
  private Hashtable dls = new Hashtable();

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.http.HttpCommand");

  static {
    //logger.setLevel(org.apache.log4j.Level.DEBUG);
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.EventHandlerIF#handleEvent(seda.sandStorm.api.QueueElementIF)
   */
  public void handleEvent(QueueElementIF item) throws EventHandlerException {
    if (logger.isDebugEnabled())
      logger.debug("HttpCommand: GOT QEL: " + item);

    if (item instanceof httpRequest) {
      httpRequest req = (httpRequest) item;
      if (req.getRequest() == httpRequest.REQUEST_POST)
        Process(req);
      else {
        if (logger.isDebugEnabled())
          logger.debug("HttpCommand: Got non-POST request: " + item);
      }
    } else {
      if (logger.isDebugEnabled())
        logger.debug("HttpCommand: Got unknown event type: " + item);
    }

  }
  private void ProcessConfigVars(httpRequest req) {
    Enumeration key = req.getQueryKeys();
    File temp;
    while (key.hasMoreElements()) {
      String k = (String) key.nextElement();
      if (k.startsWith("Options_")) {
        String option = k.substring(k.indexOf('_') + 1);
        String value = req.getQuery(k);
        if (option.endsWith("_Directory")) {
          temp = new File(value);
          if (!temp.exists())
            temp.mkdirs();
          else if (!temp.isDirectory()) {
            UI.logger.fatal("Configuration error. This is not a directory: " + value);
            continue;
          }
        }
        if (option.substring(option.indexOf('_') + 1).startsWith("s"))
          COConfigurationManager.setParameter(UIConst.parameterlegacy.get(option).toString(), value);
        else
          COConfigurationManager.setParameter(UIConst.parameterlegacy.get(option).toString(), Integer.parseInt(value));
      }
    }
    COConfigurationManager.save();
    UI.initLoggers();
    //server.initAccess();
  }

  private void ProcessAdd(httpRequest req) {
    if (req.getQuery().containsKey("Add_torrent") && !(req.getQuery("Add_torrent")).equals("")) {
      /*  
      try {
        HTTPDownloader dl = new HTTPDownloader((String) URIvars.get("Add_torrent"), COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory"));
        String file = dl.download();
        server.gm.addDownloadManager(file, COConfigurationManager.getDirectoryParameter("General_sDefaultSave_Directory"));
        server.loggerWeb.info("Download of "+(String)URIvars.get("Add_torrent")+" succeeded");
      } catch (Exception e) {
        server.loggerWeb.error("Download of "+(String)URIvars.get("Add_torrent")+" failed", e);
      }*/
      TorrentDownloaderFactory.downloadManaged(req.getQuery("Add_torrent"));
    }
  }

  private void UpdateDls() {
    dls.clear();
    List torrents = UIConst.GM.getDownloadManagers();
    if (!torrents.isEmpty()) {
      Iterator torrent = torrents.iterator();
      while (torrent.hasNext()) {
        DownloadManager dm = (DownloadManager) torrent.next();
        dls.put(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true), dm);
      }
    }
  }

  private void ProcessTorrent(httpRequest req) {
    if (req.getQuery().containsKey("subcommand")) {
      String subcommand = req.getQuery("subcommand");
      if (logger.isDebugEnabled())
        logger.debug("ProcessTorrent: " + subcommand);
      List torrents = UIConst.GM.getDownloadManagers();
      if (!torrents.isEmpty()) {
        UpdateDls();

        Enumeration ikeys = req.getQueryKeys();
        while (ikeys.hasMoreElements()) {
          String key = (String) ikeys.nextElement();
          String value = req.getQuery(key);
          if (logger.isDebugEnabled())
            logger.debug("ProcessTorrent: (" + key + "/" + value + ")");
          if (value.equals("1") && key.startsWith("Torrent_Hash_")) {
            String hash = key.substring(key.lastIndexOf('_') + 1);
            if (logger.isDebugEnabled())
              logger.debug("ProcessTorrent: \"" + hash + "\"");
            if (dls.containsKey(hash)) {
              if (logger.isDebugEnabled())
                logger.debug("ProcessTorrent: \"" + hash + "\" processed");
              DownloadManager dm = (DownloadManager) dls.get(hash);
              if (subcommand.equals("Pause") && ((dm.getState() != DownloadManager.STATE_STOPPED) || (dm.getState() != DownloadManager.STATE_STOPPING)))
                dm.stopIt();
              else if (subcommand.equals("Start") && ((dm.getState() == DownloadManager.STATE_READY) || (dm.getState() == DownloadManager.STATE_WAITING) || (dm.getState() == DownloadManager.STATE_STOPPED)))
                dm.startDownloadInitialized(true);
              else if (subcommand.equals("Cancel")) {
                dm.stopIt();
                try{
                	UIConst.GM.removeDownloadManager(dm);
                }catch(GlobalManagerDownloadRemovalVetoException e ){
                	e.printStackTrace();
                }
              }
            }
          }
        }
      }
    }
  }

  public void Process(httpRequest req) {
    if (logger.isDebugEnabled())
      logger.debug("Processing " + req);
    if (req.getQuery().containsKey("command")) {
      String command = (String) req.getQuery().get("command");
      if (command.equals("Config"))
        this.ProcessConfigVars(req);
      else if (command.equals("Add"))
        this.ProcessAdd(req);
      else if (command.equals("Exit"))
        UIConst.shutdown();
      else if (command.equals("Torrent"))
        this.ProcessTorrent(req);
    }
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.EventHandlerIF#handleEvents(seda.sandStorm.api.QueueElementIF[])
   */
  public void handleEvents(QueueElementIF[] items) throws EventHandlerException {
    for (int i = 0; i < items.length; i++) {
      handleEvent(items[i]);
    }

  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.EventHandlerIF#init(seda.sandStorm.api.ConfigDataIF)
   */
  public void init(ConfigDataIF config) throws Exception {
    this.mysink = config.getStage().getSink();
    this.mgr = config.getManager();

  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.EventHandlerIF#destroy()
   */
  public void destroy() throws Exception {
  }

}
