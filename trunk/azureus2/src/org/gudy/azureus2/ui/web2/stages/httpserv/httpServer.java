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

package org.gudy.azureus2.ui.web2.stages.httpserv;

import seda.sandStorm.api.*;
import seda.sandStorm.lib.aSocket.*;

import java.io.IOException;
import java.util.Hashtable;

/**
 * An httpServer is a SandStorm stage which accepts incoming HTTP 
 * connections. The server has a client sink associated with it, onto 
 * which httpConnection and httpRequest events are pushed. When a 
 * connection is closed, a SinkClosedEvent is pushed, with the 
 * sink pointer set to the httpConnection that closed. 
 *
 * @author Matt Welsh (mdw@cs.berkeley.edu)
 * @see httpConnection
 * @see httpRequest
 */
public class httpServer implements EventHandlerIF, httpConst {

  private static final boolean DEBUG = false;

  // These are protected to allow subclasses to use them
  protected int listenPort;
  protected ATcpServerSocket servsock;
  protected ManagerIF mgr;
  protected SinkIF mySink, clientSink;

  // ATcpConnection -> httpConnection
  private Hashtable connTable; 

  private static int num_svrs = 0;

  /**
   * Create an HTTP server listening for incoming connections on 
   * the default port of 8080.
   */
  public httpServer(ManagerIF mgr, SinkIF clientSink) throws Exception {
    this(mgr, clientSink, DEFAULT_HTTP_PORT);
  }

  /** 
   * Create an HTTP server listening for incoming connections on
   * the given listenPort. 
   */
  public httpServer(ManagerIF mgr, SinkIF clientSink, int listenPort) throws Exception {
    this.mgr = mgr;
    this.clientSink = clientSink;
    this.listenPort = listenPort;

    this.connTable = new Hashtable();

    // Create the stage and register it
    String sname = "httpServer "+num_svrs+" <port "+listenPort+">";
    // Disable the RT controller for this stage
    //mgr.getConfig().putBoolean("stages."+sname+".rtController.enable", false);
    //mgr.createStage(sname, this, null);
    mgr.createStage(sname, this, new String[] {
    		"rtController.enable=false"
    } );
    num_svrs++;
  }

  /** 
   * The Sandstorm stage initialization method.
   */
  public void init(ConfigDataIF config) throws Exception {
    mySink = config.getStage().getSink();

    servsock = new ATcpServerSocket(listenPort, mySink, WRITE_CLOG_THRESHOLD);
  }

  /** 
   * The Sandstorm stage destroy method.
   */
  public void destroy() {
  }

  /**
   * The main event handler.
   */
  public void handleEvent(QueueElementIF qel) {
    if (DEBUG) System.err.println("httpServer got qel: "+qel);

    if (qel instanceof ATcpInPacket) {
      ATcpInPacket pkt = (ATcpInPacket)qel;

      if (DEBUG) {
	System.err.println("httpServer got packet: -----------------------");
	String s = new String(pkt.getBytes());
	System.err.println(s+"\n----------------------------------");
      }

      httpConnection hc = (httpConnection)connTable.get(pkt.getConnection());
      if (hc == null) return; // Connection may have been closed

      try {
	hc.parsePacket(pkt);
      } catch (IOException ioe) {
	System.err.println("httpServer: Got IOException during packet processing for connection "+hc+": "+ioe);
	ioe.printStackTrace();
	// XXX Should close connection
      }

    } else if (qel instanceof ATcpConnection) {
      ATcpConnection conn = (ATcpConnection)qel;
      httpConnection hc = new httpConnection(conn, this, clientSink);
      connTable.put(conn, hc);

      // Profile the connection if profiling enabled
      ProfilerIF profiler = mgr.getProfiler();
      SandstormConfigIF cfg = mgr.getConfig();
      if ((profiler != null) && cfg.getBoolean("global.profile.sockets")) profiler.add(conn.toString(), conn);
      conn.startReader(mySink);

    } else if (qel instanceof aSocketErrorEvent) {
      System.err.println("httpServer got error: "+qel.toString());

    } else if (qel instanceof SinkDrainedEvent) {
      // Ignore

    } else if (qel instanceof SinkCloggedEvent) {
      // Some connection is clogged; tell the user 
      SinkCloggedEvent sce = (SinkCloggedEvent)qel;
      httpConnection hc = (httpConnection)connTable.get(sce.sink);
      if (hc != null) clientSink.enqueue_lossy(new SinkCloggedEvent(hc, null));

    } else if (qel instanceof SinkClosedEvent) {
      // Some connection closed; tell the user 
      SinkClosedEvent sce = (SinkClosedEvent)qel;
      httpConnection hc = (httpConnection)connTable.get(sce.sink);
      if (hc != null) {
	clientSink.enqueue_lossy(new SinkClosedEvent(hc));
        cleanupConnection(hc);
      }

    } else if (qel instanceof ATcpListenSuccessEvent) {
      clientSink.enqueue_lossy(qel);
    }
  }

  public void handleEvents(QueueElementIF[] qelarr) {
    for (int i = 0; i < qelarr.length; i++) {
      handleEvent(qelarr[i]);
    }
  }

  void cleanupConnection(httpConnection hc) {
    connTable.remove(hc.getConnection());
  }

  public String toString() {
    return "httpServer [listen="+listenPort+"]";
  }

  /** 
   * Register a sink to receive incoming packets on this
   * connection.
   */
  public void registerSink(SinkIF sink) {
    this.clientSink = sink;
  }

  /** 
   * Suspend acceptance of new connections on this server.
   * This request will not be effective immediately.
   */
  public void suspendAccept() {
    servsock.suspendAccept();
  }

  /** 
   * Resume acceptance of new connections on this server.
   * This request will not be effective immediately.
   */
  public void resumeAccept() {
    servsock.resumeAccept();
  }

  // Return my sink so that httpConnection can redirect
  // packet completions to it
  SinkIF getSink() {
    return mySink;
  }

  /**
   * Return the server socket being used by this httpServer.
   */
  public ATcpServerSocket getServerSocket() {
    return servsock;
  }

}
