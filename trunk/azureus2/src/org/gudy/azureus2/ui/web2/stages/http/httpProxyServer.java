/*
 * Copyright (c) 2001 Regents of the University of California. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of the
 * University nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.gudy.azureus2.ui.web2.stages.http;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.WebConst;
import org.gudy.azureus2.ui.web2.http.parser.*;
import org.gudy.azureus2.ui.web2.http.request.httpRequestFakeTcpConnection;
import org.gudy.azureus2.ui.web2.http.response.httpResponder;
import org.gudy.azureus2.ui.web2.http.util.*;
import org.gudy.azureus2.ui.web2.stages.net.ADns;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;
import org.gudy.azureus2.ui.web2.util.NodeId;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.ManagerIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SingleThreadedEventHandlerIF;
import seda.sandStorm.api.SinkClosedEvent;
import seda.sandStorm.api.SinkClosedException;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkFlushedEvent;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.core.ssTimer;
import seda.sandStorm.lib.aSocket.ATcpClientSocket;
import seda.sandStorm.lib.aSocket.ATcpConnectFailedEvent;
import seda.sandStorm.lib.aSocket.ATcpConnection;
import seda.sandStorm.lib.aSocket.ATcpInPacket;
import seda.sandStorm.lib.aSocket.ATcpListenSuccessEvent;
import seda.sandStorm.lib.aSocket.ATcpServerSocket;

public class httpProxyServer implements HttpConstants, EventHandlerIF, SingleThreadedEventHandlerIF, WebConst {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpProxy");

  static {
    //logger.setLevel(org.apache.log4j.Level.DEBUG);
  }

  protected int parser_num = 0;
  protected class ConnState {
    public ConnState() {
    }

    public ConnState(ATcpConnection connection) {
      if (connection == null)
        conn = new httpRequestFakeTcpConnection(handler_sink, sink);
      else
        conn = connection;
      pnum = parser_num++;
      parser = new HttpParser("Parser-" + (pnum), conn, sink);
      outb = new HttpOutputBuffer(conn);
      conn.startReader(sink);
    }

    public String toString() {
      return parser.toString();
    }

    ATcpConnection conn;
    HttpParser parser;
    HttpOutputBuffer outb;
    boolean closed;
    int pnum;
  }

  protected int _next_xact_id = 0;
  static final String[] state_to_string = { "INIT", "HAVE_REQ", "NEED_PARENT_CONN", "HAVE_PARENT_CONN", "SENT_REQUEST", "SENT_RESP_HDR", "SENT_RESP_BODY", "STATE_DONE" };

  protected class XactState {

    //private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpProxy.XactStage");

    protected XactState() {
      id = _next_xact_id++;
      start_time = System.currentTimeMillis();
      if (logger.isDebugEnabled()) {
        XTAG = "HttpProxy." + id;
        logger.debug("XactState " + id + " created");
      }
    }

    public String toString() {
      String result = "(XactState state=" + state_to_string[state];
      if (upstream != null)
        result += ", upstream=" + upstream;
      if (downstream != null)
        result += ", downstream=" + downstream;
      return result + ")";
    }

    protected void set_downstream(ConnState connstate) {
      if (connstate == null)
        throw new AssertionViolatedException("null state");
      downstream = connstate;
      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": downstream connection is " + downstream.conn);
      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": downstream parser is " + downstream.pnum);
      xacts.put(connstate.conn, this);
    }

    protected void set_upstream(ConnState connstate) {
      upstream = connstate;
      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": upstream connection is " + upstream.conn);
      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": upstream parser is " + upstream.pnum);
      xacts.put(connstate.conn, this);
    }

    public int id;
    public String XTAG;

    static final public int STATE_INIT = 0;
    static final public int STATE_HAVE_REQ = 1;
    static final public int STATE_WAITING_ON_DNS = 2;
    static final public int STATE_NEED_PARENT_CONN = 3;
    static final public int STATE_WAITING_FOR_PARENT_CONN = 4;
    static final public int STATE_HAVE_PARENT_CONN = 5;
    static final public int STATE_SENT_REQUEST = 6;
    static final public int STATE_SENT_RESP_HDR = 7;
    static final public int STATE_SENT_RESP_BODY = 8;
    static final public int STATE_DONE = 9;

    protected int state;
    protected ConnState downstream, upstream;
    protected HttpRequestHeader client_req, server_req;
    protected HttpResponseHeader client_resp, server_resp;
    protected LinkedList upstream_pipe = new LinkedList();
    protected LinkedList downstream_pipe = new LinkedList();
    protected String hostname;
    protected InetAddress host;
    protected int port;
    protected HttpString file;
    protected NodeId hostport;
    protected int parent_conn_retries;
    protected long start_time;
    protected boolean dns_request_done;
    protected boolean local_request;

    protected void upstream_closed() {
      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": upstream connection closed");
      upstream.parser.connection_closed();

      // TODO: I'm not sure this check is quite complete enough
      if (upstream.parser.error()) {
        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": opening new one");
        // Resend it.
        xacts.remove(upstream.conn);
        upstream = null;

        // Open up a new connection...
        ATcpClientSocket client_socket = new ATcpClientSocket(host, port, sink);
        pending_connections.put(client_socket, this);

        state = XactState.STATE_WAITING_FOR_PARENT_CONN;
      } else {
        upstream.closed = true;
        advance();
      }
    }

    public boolean intercept_request() {
      return false;
    }

    public void advance() {

      //if (logger.isDebugEnabled()) Debug.printtagln (XTAG, "state=" + state);

      if ((state == STATE_INIT) && (client_req != null)) {
        state = STATE_HAVE_REQ;
      }

      if (state == STATE_HAVE_REQ) {

        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": client req hdr = \"" + client_req + "\"");

        // Resolve the host name.
        // TODO: How we can we do this asynchronously?

        if (!intercept_request()) {
          Object[] urlparts = client_req.url().parse_server();
          // save this for later
          file = (urlparts[2] == null) ? new HttpString("/") : (HttpString) urlparts[2];
          hostname = (urlparts[0] == null) ? null : urlparts[0].toString();
          if ((hostname == null) || ((fake_local != null) && (fake_local.equalsIgnoreCase(hostname)))) {
            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": Local request for: " + file);
            local_request = true;
            state = STATE_HAVE_PARENT_CONN;
          } else if (proxy_addr == null) {
            port = ((Integer) urlparts[1]).intValue();
            local_request = false;

            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": Looking up host " + hostname + "...");

            state = STATE_WAITING_ON_DNS;
            dns_request_done = false;

            try {
              dns_sink.enqueue(new ADns.LookupReq(hostname, this, sink));
            } catch (SinkException e) {
              throw new AssertionViolatedException("can't handle sink exception");
            }
          } else {
            host = proxy_addr;
            port = proxy_port;
            hostport = new NodeId(port, host);

            state = STATE_NEED_PARENT_CONN;
          }
        }
      }

      if ((state == STATE_WAITING_ON_DNS) && (dns_request_done)) {
        if (host != null) {
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": Dns Succeeded.");
          hostport = new NodeId(port, host);
          state = STATE_NEED_PARENT_CONN;
        } else {
          logger.info("DNS request failed: " + hostname);
          state = STATE_SENT_RESP_BODY;
        }
      }

      if (state == STATE_NEED_PARENT_CONN) {

        ConnState us = remove_free_parent_connection(hostport, null);

        if (us == null) {

          // Open up a new connection...
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": openning new connection");

          ATcpClientSocket client_socket = new ATcpClientSocket(host, port, sink);
          pending_connections.put(client_socket, this);

          state = STATE_WAITING_FOR_PARENT_CONN;
        } else {
          set_upstream(us);
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": moving " + upstream.conn + " from free_parent_connections into xacts");
          state = STATE_HAVE_PARENT_CONN;
        }
      }

      if (state == STATE_WAITING_FOR_PARENT_CONN) {
        if (upstream != null)
          state = STATE_HAVE_PARENT_CONN;
      }

      if (state == STATE_HAVE_PARENT_CONN) {
        if (local_request) {
          set_upstream(new ConnState(null));
          upstream.conn.userTag = this;
        }
        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": have parent");

        if (client_req.method().equals(HEAD_METHOD)) {
          throw new AssertionViolatedException("can't handle HEAD requests yet");
        }

        server_req = (HttpRequestHeader) client_req.deep_copy();

        if (!local_request)
          modify_server_req_hdr(server_req);

        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": server req hdr = \"" + server_req + "\"");

        try {
          server_req.enqueue(upstream.outb);
          upstream.outb.flush();
          upstream.conn.flush(sink);
        } catch (SinkException e) {
          throw new AssertionViolatedException("can't handle sink exception");
        }

        state = STATE_SENT_REQUEST;
      }

      if (state < STATE_SENT_REQUEST)
        return;

      // Send bits of the request body as they come in.

      if (upstream != null) {
        if (flush_pipe(upstream_pipe, upstream.outb, server_req.chunked, false)) {
          if (upstream.conn instanceof httpRequestFakeTcpConnection)
             ((httpRequestFakeTcpConnection) upstream.conn).send();
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": request done");
        }
      }

      // If we have an unsent response header, send it.

      if ((state < STATE_SENT_RESP_HDR) && (server_resp != null)) {

        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": server resp hdr = \"" + server_resp + "\"");

        create_client_resp();
        if (client_resp == null) {
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": not sending response to client");
          state = STATE_SENT_RESP_HDR;
        } else {

          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": client resp hdr = \"" + client_resp + "\"");
          try {
            client_resp.enqueue(downstream.outb);
            downstream.outb.flush();
          } catch (SinkException e) {
            throw new AssertionViolatedException("can't handle sink exception: " + e.getMessage());
          }

          if (client_resp.response_code() == 100) {
            // If we get a continue, there is another response to
            // come, so keep waiting.

            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": setting continue_resp to true");

            state = STATE_SENT_REQUEST;
            //continue_resp = true;
            server_resp = client_resp = null;
          } else
            state = STATE_SENT_RESP_HDR;
        }
      }

      if (state < STATE_SENT_RESP_HDR)
        return;

      // Once we've sent the response header, send bits of the
      // response body as they come in. If the client has close its
      // end of the downstream connection, finish the transaction.

      if (downstream == null) {
        state = STATE_SENT_RESP_BODY;
      } else {
        if (flush_pipe(downstream_pipe, downstream.outb, (client_resp == null ? false : client_resp.chunked), false)) {

          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": response done");
          state = STATE_SENT_RESP_BODY;
        }
      }

      if (state < STATE_SENT_RESP_BODY)
        return;

      // Reliquish our connections.
      if (upstream != null) {
        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": taking " + upstream.conn + " out of xacts");
        xacts.remove(upstream.conn);

        if (!upstream.closed) {
          if (server_resp.close) {
            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": closing " + upstream.conn);
          } else {
            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": putting " + upstream.conn + " into free_parent_connections " + "with key " + hostport);
            LinkedList ll = (LinkedList) free_parent_connections.get(hostport);
            if (ll == null) {
              ll = new LinkedList();
              free_parent_connections.put(hostport, ll);
            }
            ll.addLast(upstream);
          }
        }
      }

      if (downstream != null) {
        // For the downstream connection, the current recipient of
        // new incoming events may no longer be us, in which case
        // we can't take it out of xacts.

        XactState owner = (XactState) xacts.get(downstream.conn);
        if (this == owner) {
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": taking " + downstream.conn + " out of xacts");
          xacts.remove(downstream.conn);

          if (!downstream.closed) {

            if (client_resp == null || client_resp.close) {
              if (logger.isDebugEnabled())
                logger.debug(XTAG + ": closing child");
              try {
                downstream.conn.close(sink);
              } catch (SinkException e) {
                // ignore it, we were getting rid of it
                // anyway
              }
            } else {
              if (logger.isDebugEnabled())
                logger.debug(XTAG + ": putting " + downstream.conn + " into free_child_connections");
              free_child_connections.put(downstream.conn, downstream);
            }
          }
        } else if (owner != null) {
          if (client_resp.close)
            throw new AssertionViolatedException("connection should have been closed.");

          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": current " + "owner of our downstream connection is " + "xact " + owner.id);
        }
      }

      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": XactState " + id + " done");

      state = STATE_DONE;
    }

    protected void modify_server_req_hdr(HttpRequestHeader server_req) {
      server_req.set_version(1, 1);

      // Remove all connection:close-like header fields.
      server_req.remove_field(CONNECTION);
      server_req.remove_field(PROXY_CONNECTION);

      if (proxy_addr == null) {
        // If we're not sending this to a proxy, take the
        // server name out of the URL and put it in a host
        // header field.

        if (logger.isDebugEnabled())
          logger.debug(XTAG + ": Adding host field; new url=" + file);

        HttpHeaderField hostfield = server_req.get_field(HOST);
        if (hostfield == null)
          hostfield = server_req.prepend_field(HOST);
        HttpString new_value = new HttpString(hostname + ((port == 80) ? "" : (":" + port)));
        hostfield.replace_values(new_value);
        server_req.set_url(file);
      }
    }

    protected void create_client_resp() {
      client_resp = (HttpResponseHeader) server_resp.deep_copy();

      if (logger.isDebugEnabled())
        logger.debug(XTAG + ": create_client_resp");

      HttpHeaderField req_close = client_req.get_field(PROXY_CONNECTION);
      if (req_close != null) {
        if (req_close.num_values() != 1)
          throw new AssertionViolatedException("multivalued close");
      }

      if (((client_req.major_version() == 1) && (client_req.minor_version() == 0)) || ((req_close != null) && (req_close.get_first_value().equals(CLOSE)))) {

        // If this is a 1.0 client or there is a
        // connection:close field in the request, we just close
        // the connection on the response.

        client_resp.close = true;

        req_close = client_resp.get_field(PROXY_CONNECTION);
        if (req_close == null)
          req_close = client_resp.append_field(PROXY_CONNECTION);
        req_close.replace_values(CLOSE);
      } else {

        // Otherwise, we have to be a little smarter. The
        // server may close the connection in order to indicate
        // its size. If so, and we want to keep the child
        // connection open, we have to make sure the size is
        // somehow specified.

        HttpHeaderField sc = client_resp.get_field(CONNECTION);

        if (sc != null) {
          if (sc.num_values() != 1) {
            throw new AssertionViolatedException("TODO");
          }
          if (sc.get_first_value().equals(CLOSE)) {

            int code = client_resp.response_code();
            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": code=" + code);
            if ((code / 100 != 1) && (code != 204) && (code != 304)) {
              HttpHeaderField cl = client_resp.get_field(CONTENT_LENGTH);
              HttpHeaderField te = client_resp.get_field(TRANSFER_ENCODING);

              if (cl != null) {
                // The content length is already specified. We
                // don't need to do anything.
              } else if (te == null) {
                // There is no transfer encoding. Add one for
                // chunked.
                HttpHeaderField ckd = client_resp.append_field(TRANSFER_ENCODING);
                ckd.append_value(CHUNKED);
                client_resp.chunked = true;
              } else {
                // There is already a transfer encoding. Make
                // sure it's chunked.

                if ((te.num_values() != 1) || (!te.get_first_value().equals(CHUNKED))) {
                  throw new AssertionViolatedException("TODO");
                }
              }
            }
          } else if (sc.get_first_value().equals(KEEP_ALIVE)) {
            // do nothing
          } else {
            throw new AssertionViolatedException("TODO");
          }

          client_resp.remove_field(CONNECTION);
        } else {
          // There is no Connection field in the header, and this
          // is a 1.1 protocol response. If there is a
          // Content-Length or a Transfer-Encoding:Chunked in the
          // header, then we're okay. Also, if this is a 1xx,
          // 204, or 304, we're okay. Otherwise, we need to add
          // the latter so we can keep the child connection open.

          int code = client_resp.response_code();
          if (logger.isDebugEnabled())
            logger.debug(XTAG + ": code=" + code);
          if ((code / 100 != 1) && (code != 204) && (code != 304)) {

            HttpHeaderField f = client_resp.get_field(CONTENT_LENGTH);
            if (f == null) {
              if (logger.isDebugEnabled())
                logger.debug(XTAG + ": code=" + code);
              f = client_resp.get_field(TRANSFER_ENCODING);
              if (f == null)
                f = client_resp.append_field(TRANSFER_ENCODING);
              f.replace_values(CHUNKED);
              client_resp.chunked = true;
            }
          }
        }

        client_resp.remove_field(PROXY_CONNECTION);
        client_resp.close = false;
      }
    }

    public void enqueue_fragment(BufferElement chunk, HttpOutputBuffer buf, boolean chunked) {

      try {
        if (chunked) {
          String a = Integer.toHexString(chunk.size);
          HttpString b = new HttpString(a);
          b.enqueue(buf);
          CRLF.enqueue(buf);
        }
        //if (logger.isDebugEnabled()) Debug.printtagln (XTAG, "sending " +
        //	chunk.size + " bytes");
        buf.enqueue(chunk, chunk.offset, chunk.size);
        if (chunked) {
          CRLF.enqueue(buf);
        }
        //buf.flush (); // probably hurts over modem
      } catch (SinkException e) {
        throw new AssertionViolatedException("can't handle sink exception");
      }
    }

    public boolean flush_pipe(LinkedList pipe, HttpOutputBuffer buf, boolean chunked, boolean done) {

      while (!pipe.isEmpty()) {
        Object obj = pipe.removeFirst();
        if (obj instanceof HttpBodyFragment) {
          HttpBodyFragment frag = (HttpBodyFragment) obj;
          enqueue_fragment(frag.buf, buf, chunked);
          if (frag.done)
            done = true;
        } else {
          if ((obj instanceof HttpBodyDone) && (chunked)) {
            if (logger.isDebugEnabled())
              logger.debug(XTAG + ": sending 0 bytes");
            try {
              HttpString b = new HttpString("0");
              b.enqueue(buf);
              CRLF.enqueue(buf);
              CRLF.enqueue(buf);
              buf.flush();
            } catch (SinkException e) {
              throw new AssertionViolatedException("can't handle sink exception");
            }
          }
          done = true;
          break;
        }
      }
      if (!pipe.isEmpty())
        throw new AssertionViolatedException("more than one end event?");

      try {
        if (done)
          buf.flush();
      } catch (SinkException e) {
        throw new AssertionViolatedException("can't handle sink exception");
      }

      return done;
    }
  }

  /**
   * ATcpClientSocket to XactState
   */
  protected Map pending_connections = new HashMap();

  /**
   * NodeId to ConnState
   */
  protected Map free_parent_connections = new HashMap();

  /**
   * ATcpConnection to ConnState
   */
  protected Map free_child_connections = new HashMap();

  /**
   * ATcpConnection to XactState
   */
  protected Map xacts = new HashMap();

  protected SinkIF sink;
  protected SinkIF dns_sink;
  protected SinkIF handler_sink;
  protected ManagerIF mgr;
  protected int listen_port;
  protected InetAddress proxy_addr;
  protected int proxy_port;
  protected String fake_local = null;

  public static final int REQUEST_TIMEOUT = 60; // 1
  // minute

  public ConnState remove_free_parent_connection(NodeId hostport, ATcpConnection conn) {
    if (hostport != null) {
      ConnState result = null;
      LinkedList ll = (LinkedList) free_parent_connections.get(hostport);
      if (ll != null) {
        if (conn == null)
          result = (ConnState) ll.removeLast();
        else {
          Iterator i = ll.iterator();
          while (i.hasNext()) {
            result = (ConnState) i.next();
            if (result.conn == conn) {
              i.remove();
              break;
            }
          }
        }
        if (ll.isEmpty())
          free_parent_connections.remove(hostport);
      }
      return result;
    } else
      return null;
  }

  protected void handle_alarm(AlarmEvent alarm_event) {
    long now = System.currentTimeMillis();

    int fpcsz = 0;
    Iterator i = free_parent_connections.keySet().iterator();
    while (i.hasNext()) {
      NodeId n = (NodeId) i.next();
      LinkedList ll = (LinkedList) free_parent_connections.get(n);
      fpcsz += ll.size();
    }

    logger.info(pending_connections.size() + " pc, " + fpcsz + " fpc, " + free_child_connections.size() + " fcc, " + ((xacts.size() + 1) / 2) + " ar");

    i = xacts.keySet().iterator();
    LinkedList restart_them = null;

    while (i.hasNext()) {
      ATcpConnection c = (ATcpConnection) i.next();
      XactState xs = (XactState) xacts.get(c);

      if ((xs.upstream != null) && (c == xs.upstream.conn) && (now - xs.start_time > REQUEST_TIMEOUT * 1000)) {

        if (xs.state != XactState.STATE_SENT_REQUEST) {
          logger.error("HttpProxy." + xs.id + ": not waiting on response");
        } else {
          if (restart_them == null)
            restart_them = new LinkedList();
          restart_them.addLast(c);
        }
      }
    }

    while ((restart_them != null) && (!restart_them.isEmpty())) {

      ATcpConnection c = (ATcpConnection) restart_them.removeFirst();
      XactState xs = (XactState) xacts.get(c);

      System.err.println("Timeout: sending new request to " + xs.hostname);

      // Close the upstream connection and restart.
      xacts.remove(c);
      try {
        xs.upstream.conn.close(sink);
      } catch (SinkClosedException e) {
        // do nothing
      }
      xs.upstream = null;

      xs.state = XactState.STATE_NEED_PARENT_CONN;
      xs.advance();
    }

    timer.registerEvent(30 * 1000, alarm_event, sink);
  }

  protected ssTimer timer;
  public class AlarmEvent implements QueueElementIF {
  }

  public void init(ConfigDataIF config) throws Exception {
    listen_port = config.getInt("listen_port");
    sink = config.getStage().getSink();
    this.mgr = config.getManager();
    handler_sink = mgr.getStage(HTTP_HANDLER_STAGE).getSink();
    System.out.println("my sink=" + sink);
    new ATcpServerSocket(listen_port, sink);
    String proxy_str = config.getString("proxy_addr");
    if (proxy_str != null) {
      proxy_addr = InetAddress.getByName(proxy_str);
      proxy_port = config.getInt("proxy_port");
    }

    fake_local = config.getString("fake_local_server");

    timer = new ssTimer();
    timer.registerEvent(30 * 1000, new AlarmEvent(), sink);

    // Create the ADns Stage.
    dns_sink = config.getManager().createStage("ADns", new ADns(), null).getSink();
  }

  public void handleEvent(QueueElementIF item) throws EventHandlerException {
    //if (logger.isDebugEnabled()) Debug.printtagln (TAG, "event " + item);
    if (item instanceof AlarmEvent) {
      handle_alarm((AlarmEvent) item);
    } else if (item instanceof ADns.LookupResp) {
      ADns.LookupResp resp = (ADns.LookupResp) item;
      XactState xact = (XactState) resp.user_data;
      xact.host = resp.address;
      xact.dns_request_done = true;
      xact.advance();
    } else if (item instanceof ATcpConnection) {
      ATcpConnection connection = (ATcpConnection) item;
      if (logger.isDebugEnabled())
        logger.debug("Got TcpConnection: " + connection);
      ATcpClientSocket client_socket = connection.getClientSocket();
      if (client_socket != null)
        handle_connection_from_upstream(connection, client_socket);
      else
        handle_connection_from_downstream(connection);
    } else if (item instanceof ATcpInPacket) {
      ATcpInPacket packet = (ATcpInPacket) item;
      ATcpConnection connection = packet.getConnection();
      if (logger.isDebugEnabled())
        logger.debug("HttpProxy: Got Tcp Packet: " + packet);
      XactState xact = (XactState) xacts.get(connection);
      if (xact != null) {
        if (logger.isDebugEnabled())
          logger.debug("HttpProxy." + xact.id + ": Got Tcp Packet: " + packet);
        if ((xact.upstream != null) && (connection == xact.upstream.conn)) {
          if (logger.isDebugEnabled())
            logger.debug("HttpProxy." + xact.id + ": upstream packet of " + packet.getBufferElement().size + " bytes");
          xact.upstream.parser.add_packet(packet);
        } else if ((xact.downstream != null) && (connection == xact.downstream.conn)) {
          xact.downstream.parser.add_packet(packet);
        } else {
          throw new AssertionViolatedException("neither");
        }
      } else {
        ConnState cstate = (ConnState) free_child_connections.get(connection);
        if (cstate != null) {
          xact = new_xact();
          if (logger.isDebugEnabled())
            logger.debug("HttpProxy." + xact.id + ": Setting downstream from TcpInPacket");
          xact.set_downstream(cstate);
          cstate.parser.add_packet(packet);
        } else {
          if (logger.isDebugEnabled())
            logger.debug("unknown packet \"" + HttpParser.print_packet(packet) + "\" from " + connection + ".  Dropping it.");
          return;
        }
      }
    } else if (item instanceof httpResponder) {
      httpResponder response = (httpResponder) item;
      XactState xact = (XactState) response.getTag();
      if (xact.downstream != null) {
        response.getResponse().enqueue(xact.downstream.outb);
        xact.state = XactState.STATE_SENT_RESP_BODY;
        xact.upstream_closed();
      }
    } else if (item instanceof SinkClosedEvent) {
      SinkClosedEvent sce = (SinkClosedEvent) item;
      if (sce.sink instanceof ATcpConnection) {
        ATcpConnection connection = (ATcpConnection) sce.sink;

        if (logger.isDebugEnabled())
          logger.debug("got sce for " + connection);

        XactState xact = (XactState) xacts.get(connection);
        if (xact != null) {
          if ((xact.upstream != null) && (connection == xact.upstream.conn)) {
            xact.upstream_closed();
          } else if ((xact.downstream != null) && (connection == xact.downstream.conn)) {
            if (logger.isDebugEnabled())
              logger.debug("HttpProxy." + xact.id + ": downstream connection closed");
            xact.downstream.parser.connection_closed();
            xact.downstream.closed = true;
            xact.advance();
          } else {
            throw new AssertionViolatedException("neither");
          }

          return;
        }

        ConnState cs = (ConnState) free_child_connections.remove(connection);
        if (cs != null) {
          if (logger.isDebugEnabled())
            logger.debug("taking " + cs.conn + " out of free_child_connections");
          return;
        }
        NodeId n = new NodeId(connection.getPort(), connection.getAddress());
        cs = remove_free_parent_connection(n, connection);
        if (cs != null) {
          if (logger.isDebugEnabled())
            logger.debug("taking " + cs.conn + " out of free_parent_connections");
          return;
        } else {
          if (logger.isDebugEnabled())
            logger.debug("couldn't find " + connection + " free_parent_connections with key " + n);
        }
        // Sometimes sandStorm sends us two SinkClosedExceptions
        // throw new AssertionViolatedException ("none");
      } else {
        throw new AssertionViolatedException("unknown sce");
      }
    } else if (item instanceof SinkFlushedEvent) {
      // who cares?
    } else if (item instanceof ATcpListenSuccessEvent) {
      // who cares?
    } else if (item instanceof ATcpConnectFailedEvent) {
      ATcpConnectFailedEvent failed = (ATcpConnectFailedEvent) item;
      ATcpClientSocket client_socket = failed.getSocket();
      XactState xact = (XactState) pending_connections.remove(client_socket);
      xact.parent_conn_retries += 1;

      if (xact.parent_conn_retries > 5) {
        if (logger.isDebugEnabled())
          logger.debug("Failed to connect 5 times.  Giving up.");
        throw new AssertionViolatedException("connection to " + xact.host + ":" + xact.port + " failed");
      } else {
        if (logger.isDebugEnabled())
          logger.debug("Connection to " + xact.host + ":" + xact.port + " failed.  Trying again.");
        client_socket = new ATcpClientSocket(xact.host, xact.port, sink);
        pending_connections.put(client_socket, xact);
      }
    } else if (item instanceof HttpRequestHeader) {
      HttpRequestHeader hdr = (HttpRequestHeader) item;
      ATcpConnection connection = hdr.connection;
      if (connection.getServerSocket() == null)
        throw new AssertionViolatedException("should only get requests from downstream");

      handle_request_header(hdr);
    } else if (item instanceof HttpResponseHeader) {
      HttpResponseHeader hdr = (HttpResponseHeader) item;
      ATcpConnection connection = hdr.connection;
      if (connection.getClientSocket() == null)
        throw new AssertionViolatedException("should only get responses from downstream");

      handle_response_header(hdr);
    } else if (item instanceof HttpBodyFragment) {
      handle_body_fragment((HttpBodyFragment) item);
    } else if (item instanceof HttpBodyDone) {
      handle_body_done((HttpBodyDone) item);
    } else if (item instanceof HttpNoBody) {
      handle_no_body((HttpNoBody) item);
    } else {
      throw new AssertionViolatedException("unknown event: " + item);
    }
  }

  protected void handle_connection_from_upstream(ATcpConnection connection, ATcpClientSocket client_socket) {

    // Look up the transaction.
    XactState xact = (XactState) pending_connections.remove(client_socket);
    if (xact == null) {
      throw new AssertionViolatedException("HttpProxy: got unknown connection " + connection);
    }

    if (logger.isDebugEnabled())
      logger.debug("HttpProxy." + xact.id + ": handling connection from upstream");

    // Create a new connection state.
    xact.set_upstream(new ConnState(connection));

    // Try to advance the transaction.
    xact.advance();
  }

  protected void handle_connection_from_downstream(ATcpConnection connection) {

    XactState xact = new_xact();
    if (logger.isDebugEnabled())
      logger.debug("HttpProxy." + xact.id + ": Setting downstream from TcpConnection");
    xact.set_downstream(new ConnState(connection));
    // wait for a request header event
  }

  protected void handle_request_header(HttpRequestHeader hdr) {

    ATcpConnection connection = hdr.connection;
    XactState xact = (XactState) xacts.get(connection);
    if (xact == null) {
      // There is no transaction currently using this connection.
      xact = new_xact();
      xact.set_downstream((ConnState) free_child_connections.get(connection));
    } else if (xact.client_req != null) {
      // The existing xaction using this child connection is not
      // quite done with it yet. It should be done receiving packets
      // on it though, so we can get away with this.

      // Create a new state.

      XactState old_xact = xact;
      xact = new_xact();

      // Steal the old state's downstream connection.

      xact.set_downstream(old_xact.downstream);
    }
    xact.client_req = hdr;
    xact.advance();
  }

  protected XactState new_xact() {
    return new XactState();
  }

  protected void handle_response_header(HttpResponseHeader hdr) {

    ATcpConnection connection = hdr.connection;
    XactState xact = (XactState) xacts.get(connection);
    if (xact == null)
      throw new AssertionViolatedException("no transaction for " + connection + ", resp was \"" + hdr + "\"");

    xact.server_resp = hdr;
    xact.advance();
  }

  protected void handle_body_fragment(HttpBodyFragment frag) {
    ATcpConnection connection = frag.connection;
    XactState xact = (XactState) xacts.get(connection);
    if (xact == null)
      throw new AssertionViolatedException("no transaction for " + connection + ", frag was \"" + frag + "\"");

    if (frag.connection.getClientSocket() == null) {
      // This fragment is headed to the parent
      xact.upstream_pipe.addLast(frag);
    } else {
      // This fragment is headed to the child
      xact.downstream_pipe.addLast(frag);
    }

    xact.advance();
  }

  protected void handle_body_done(HttpBodyDone done) {
    ATcpConnection connection = done.connection;
    XactState xact = (XactState) xacts.get(connection);
    if (xact == null)
      throw new AssertionViolatedException("no transaction for " + connection + ", done was \"" + done + "\"");

    if (done.connection.getClientSocket() == null) {
      // This event is headed to the parent
      xact.upstream_pipe.addLast(done);
    } else {
      // This event is headed to the child
      xact.downstream_pipe.addLast(done);
    }

    xact.advance();
  }

  protected void handle_no_body(HttpNoBody done) {
    ATcpConnection connection = done.connection;
    XactState xact = (XactState) xacts.get(connection);

    if (done.connection.getClientSocket() == null) {
      // This event is headed to the parent
      if (xact != null)
        xact.upstream_pipe.addLast(done);
    } else {
      if (xact == null)
        throw new AssertionViolatedException("no transaction for " + connection + ", done was \"" + done + "\"");

      // This event is headed to the child
      xact.downstream_pipe.addLast(done);
    }

    if (xact != null)
      xact.advance();
  }

  public void handleEvents(QueueElementIF items[]) throws EventHandlerException {
    for (int i = 0; i < items.length; i++)
      handleEvent(items[i]);
  }

  public void destroy() throws Exception {
    // do nothing
  }

}
