/* 
 * Copyright (c) 2001 by Matt Welsh and The Regents of the University of 
 * California. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Author: Matt Welsh <mdw@cs.berkeley.edu>
 * 
 */

package org.gudy.azureus2.ui.web2.stages.http;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.common.util.SLevel;
import org.gudy.azureus2.ui.web2.stages.hdapi.WildcardDynamicHttp;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpConnection;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpConst;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpOKResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpRequest;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpResponder;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpResponse;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpServer;
import org.gudy.azureus2.ui.web2.stages.httpserv.httpServiceUnavailableResponse;
import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.WebConst;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.ManagerIF;
import seda.sandStorm.api.NoSuchStageException;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkClosedEvent;
import seda.sandStorm.api.SinkClosedException;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.core.ssTimer;
import seda.util.MDWUtil;

/**
 * This stage is responsible for accepting new HTTP requests and forwarding
 * them to the page cache (or else, responding with a dynamically-generated
 * page for statistics gathering).
 *
 */
public class HttpRecv implements EventHandlerIF, WebConst {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.http.HttpRecv");

  // If true, enable ATLS support
  private boolean USE_ATLS = false;

  private static final long TIMER_DELAY = 2000;
  private int HTTP_PORT, HTTP_SECURE_PORT;
  private httpServer server, secureServer;
  private ManagerIF mgr;
  private SinkIF mysink, cacheSink, bottleneckSink, sendSink, dynSink, commandSink;
  private int maxConns, maxSimReqs, numConns = 0, numSimReqs = 0;
  private int lastNumConns = 0;
  private String SPECIAL_URL;
  private String BOTTLENECK_URL;
  private ssTimer timer;

  // Empty class representing timer event
  class timerEvent implements QueueElementIF {}

  public HttpRecv() {
    if (UI.httpRecv != null) {
      throw new Error("HttpRecv: More than one HttpRecv running?");
    }
    UI.httpRecv = this;
  }

  public void init(ConfigDataIF config) throws Exception {
    mysink = config.getStage().getSink();
    this.mgr = config.getManager();
    cacheSink = mgr.getStage(CACHE_STAGE).getSink();
    sendSink = mgr.getStage(HTTP_SEND_STAGE).getSink();

    // These are optional
    try {
      dynSink = mgr.getStage(DYNAMIC_HTTP_STAGE).getSink();
    } catch (NoSuchStageException nsse) {
      dynSink = null;
    }

    try {
      bottleneckSink = mgr.getStage(BOTTLENECK_STAGE).getSink();
    } catch (NoSuchStageException nsse) {
      bottleneckSink = null;
    }

    try {
      commandSink = mgr.getStage(COMMAND_STAGE).getSink();
    } catch (NoSuchStageException nsse) {
      commandSink = null;
      logger.error("Warning: Command stage not available.");
    }

    timer = new ssTimer();
    timer.registerEvent(TIMER_DELAY, new timerEvent(), mysink);

    SPECIAL_URL = config.getString("specialURL");
    if (SPECIAL_URL == null)
      throw new IllegalArgumentException("Must specify specialURL");
    BOTTLENECK_URL = config.getString("bottleneckURL");

    String serverName = config.getString("serverName");
    if (serverName != null)
      httpResponse.setDefaultHeader("Server: " + serverName + httpConst.CRLF);
    HTTP_PORT = config.getInt("httpPort");

    HTTP_SECURE_PORT = config.getInt("httpSecurePort");
    USE_ATLS = (HTTP_SECURE_PORT != -1);

    if (USE_ATLS == false) {
      if (HTTP_PORT == -1) {
        throw new IllegalArgumentException("Must specify httpPort");
      }
    }
    if (USE_ATLS == true) {
      if ((HTTP_PORT == -1) && (HTTP_SECURE_PORT == -1)) {
        throw new IllegalArgumentException("Must specify either httpPort or httpSecurePort");
      }
    }

    maxConns = config.getInt("maxConnections");
    maxSimReqs = config.getInt("maxSimultaneousRequests");
    logger.info("HttpRecv: Starting, maxConns=" + maxConns + ", maxSimReqs=" + maxSimReqs);

    if (HTTP_PORT != -1) {
      server = new httpServer(mgr, mysink, HTTP_PORT);
    }

    /* Uncomment the following lines if you want to enable SSL/TLS support */
    /* if (USE_ATLS && (HTTP_SECURE_PORT != -1)) {
     *   secureServer = new seda.sandStorm.lib.aTLS.http.httpSecureServer(mgr, mysink, HTTP_SECURE_PORT);
     *
     * }
     */

  }

  public void destroy() {}

  public void handleEvent(QueueElementIF item) {
    if (logger.isDebugEnabled())
      logger.debug("HttpRecv: GOT QEL: " + item);

    if (item instanceof httpConnection) {
      UI.numConnectionsEstablished++;
      numConns++;
      if (logger.isEnabledFor(SLevel.HTTP))
        logger.log(SLevel.HTTP, "HttpRecv: Got connection " + (UI.numConnectionsEstablished - UI.numConnectionsClosed));

      if ((maxConns != -1) && (numConns == maxConns)) {
        logger.info("Suspending accept() after " + numConns + " connections");
        server.suspendAccept();
      }

    } else if (item instanceof httpRequest) {
      if (logger.isDebugEnabled())
        logger.debug("HttpRecv: Got request " + item);

      httpRequest req = (httpRequest) item;

      // Record time for controller
      req.timestamp = System.currentTimeMillis();

      // Check for special URL
      if (logger.isDebugEnabled())
        logger.debug("HttpRecv: URL is [" + req.getURL() + "]");
        
      if (req.getQuery()!=null) {
        if (!commandSink.enqueue_lossy(req))
          logger.info("Warning: Could not enqueue_lossy " + item + " to Command stage");
      }
        
      if (req.getURL().startsWith(SPECIAL_URL)) {
        if (logger.isDebugEnabled())
          logger.debug("HttpRecv: Doing special");
        doSpecial(req);
        return;
      }

      // Check for bottleneck URL
      if ((bottleneckSink != null) && (BOTTLENECK_URL != null) && (req.getURL().startsWith(BOTTLENECK_URL))) {
        if (!bottleneckSink.enqueue_lossy(req)) {
          //System.err.println("HttpRecv: Warning: Could not enqueue_lossy to bottleneck stage: "+item);
          // Send not available response
          HttpSend.sendResponse(new httpResponder(new httpServiceUnavailableResponse(req, "Bottleneck stage is busy!"), req, true));
        }
        return;
      }

      // Check for dynamic URLs
      if (dynSink != null) {
        try {
          if (WildcardDynamicHttp.handleRequest(req))
            return;
        } catch (Exception e) {
          // Send not available response
          //System.err.println("*************** Haboob: Could not enqueue request to HDAPI: "+e);
          HttpSend.sendResponse(new httpResponder(new httpServiceUnavailableResponse(req, "Could not enqueue request to HDAPI [" + req.getURL() + "]: " + e), req, true));
          return;
        }
      }

      // Threshold maximum number of in-flight requests
      if (maxSimReqs != -1) {
        synchronized (this) {
          numSimReqs++;
          while (numSimReqs >= maxSimReqs) {
            try {
              this.wait();
            } catch (InterruptedException ie) {
              // Ignore
            }
          }
        }
      }

      if (logger.isDebugEnabled())
        logger.debug("HttpRecv: Sending to cacheSink");
      if (!cacheSink.enqueue_lossy(item)) {
        logger.info("HttpRecv: Warning: Could not enqueue_lossy " + item);
      }

    } else if (item instanceof SinkClosedEvent) {
      // Connection closed by remote peer
      if (logger.isDebugEnabled())
        logger.debug("HttpRecv: Closed connection " + item);
      UI.numConnectionsClosed++;

      numConns--;
      if ((maxConns != -1) && (numConns == maxConns - 1)) {
        logger.info("Resuming accept() for " + numConns + " connections");
        server.resumeAccept();
      }
      cacheSink.enqueue_lossy(item);

      if (logger.isEnabledFor(SLevel.HTTP))
        logger.log(SLevel.HTTP, "HttpRecv: Closed connection " + (UI.numConnectionsEstablished - UI.numConnectionsClosed));

    } else if (item instanceof timerEvent) {

      int nc = (UI.numConnectionsEstablished - UI.numConnectionsClosed);
      if (nc != lastNumConns) {
        logger.info((UI.numConnectionsEstablished - UI.numConnectionsClosed) + " active connections");
      }
      lastNumConns = nc;
      timer.registerEvent(TIMER_DELAY, item, mysink);

    } else {
      if (logger.isDebugEnabled())
        logger.debug("HttpRecv: Got unknown event type: " + item);
    }

  }

  public void handleEvents(QueueElementIF items[]) {
    for (int i = 0; i < items.length; i++) {
      handleEvent(items[i]);
    }
  }

  // Indicate that we are done with a request; used by HttpSend
  void doneWithReq() {
    if (maxSimReqs == -1)
      return;

    synchronized (this) {
      numSimReqs--;
      if (numSimReqs < maxSimReqs)
        this.notify();
    }
  }

  // Close the given connection; used by HttpSend
  void closeConnection(httpConnection conn) {
    UI.numConnectionsClosed++;
    numConns--;
    try {
      conn.close(cacheSink);
    } catch (SinkClosedException sce) {
      if (logger.isDebugEnabled())
        logger.debug("Warning: Tried to close connection " + conn + " multiple times");
    }
    if ((maxConns != -1) && (numConns == maxConns - 1)) {
      logger.info("Resuming accept() for " + numConns + " connections");
      server.resumeAccept();
    }
  }

  private void doSpecial(httpRequest req) {
    double pct;

    if (req.getURL().endsWith("?graph")) {
      mgr.getProfiler().getGraphProfiler().dumpGraph();
    }

    String repl = "<html><head><title>Haboob Web Server Admin Page</title></head><body bgcolor=white><font face=helvetica><h2>Haboob Admin Page</h2>\n";

    if (req.getURL().endsWith("?graph")) {
      mgr.getProfiler().getGraphProfiler().dumpGraph();
      repl += "<p><b><font color=red>Graph dumped.</font></b>";
    }

    repl += "<p><b>Server Statistics</b>\n";
    Runtime r = Runtime.getRuntime();
    double totalmemkb = r.totalMemory() / 1024.0;
    double freememkb = r.freeMemory() / 1024.0;
    repl += "<br>Total memory in use: " + MDWUtil.format(totalmemkb) + " KBytes\n";
    repl += "<br>Free memory: " + MDWUtil.format(freememkb) + " KBytes\n";

    repl += "<p><b>HTTP Request Statistics</b>\n";
    repl += "<br>Total requests: " + UI.numRequests + "\n";
    pct = (UI.numErrors * 100.0 / UI.numRequests);
    repl += "<br>Errors: " + UI.numErrors + " (" + MDWUtil.format(pct) + "%)\n";

    repl += "\n<p><b>Cache Statistics</b>\n";
    double cacheSizeKb = UI.cacheSizeBytes / 1024.0;
    repl += "<br>Current size of page cache: " + UI.cacheSizeEntries + " files, " + MDWUtil.format(cacheSizeKb) + " KBytes\n";
    pct = (UI.numCacheHits * 100.0 / UI.numRequests);
    repl += "<br>Cache hits: " + UI.numCacheHits + " (" + MDWUtil.format(pct) + "%)\n";
    pct = (UI.numCacheMisses * 100.0 / UI.numRequests);
    repl += "<br>Cache misses: " + UI.numCacheMisses + " (" + MDWUtil.format(pct) + "%)\n";

    repl += "\n<p><b>Connection Statistics</b>\n";
    int numconns = UI.numConnectionsEstablished - UI.numConnectionsClosed;
    repl += "<br>Number of connections: " + numconns + "\n";
    repl += "<br>Total connections: " + UI.numConnectionsEstablished + "\n";

    repl += "\n<p><b>Profiling Information</b>\n";
    double cacheLookupTime = 0, cacheAllocateTime = 0, cacheRejectTime = 0, fileReadTime = 0;
    if (UI.numCacheLookup != 0)
      cacheLookupTime = (UI.timeCacheLookup * 1.0) / UI.numCacheLookup;
    if (UI.numCacheAllocate != 0)
      cacheAllocateTime = (UI.timeCacheAllocate * 1.0) / UI.numCacheAllocate;
    if (UI.numCacheReject != 0)
      cacheRejectTime = (UI.timeCacheReject * 1.0) / UI.numCacheReject;
    if (UI.numFileRead != 0)
      fileReadTime = (UI.timeFileRead * 1.0) / UI.numFileRead;
    repl += "<br>Cache lookup time: " + MDWUtil.format(cacheLookupTime) + " ms avg (" + UI.timeCacheLookup + " total, " + UI.numCacheLookup + " times)\n";
    repl += "<br>Cache allocate time: " + MDWUtil.format(cacheAllocateTime) + " ms avg (" + UI.timeCacheAllocate + " total, " + UI.numCacheAllocate + " times)\n";
    repl += "<br>Cache reject time: " + MDWUtil.format(cacheRejectTime) + " ms avg (" + UI.timeCacheReject + " total, " + UI.numCacheReject + " times)\n";
    repl += "<br>File read time: " + MDWUtil.format(fileReadTime) + " ms avg (" + UI.timeFileRead + " total, " + UI.numFileRead + " times)\n";

    repl += "<p></font></body></html>" + httpConst.CRLF;

    httpOKResponse resp = new httpOKResponse("text/html", new BufferElement(repl.getBytes()));
    HttpSend.sendResponse(new httpResponder(resp, req, true));

  }

}
