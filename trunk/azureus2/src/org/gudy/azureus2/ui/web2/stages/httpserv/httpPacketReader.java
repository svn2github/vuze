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

import seda.sandStorm.api.SinkIF;
import seda.sandStorm.lib.aSocket.ATcpInPacket;
import seda.sandStorm.lib.aSocket.aSocketInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * This is a package-internal class which reads HTTP request packets.
 * An instance of this class is fed ATcpInPackets (via the 
 * <tt>parsePacket</tt> method). When a complete packet has been
 * read, an httpRequest is pushed to the corresponding SinkIF.
 * This is the bulk of the HTTP protocol implementation.
 * 
 * @author Matt Welsh
 */
class httpPacketReader implements httpConst {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.httpserv.httpPacketReader");

  private static final int STATE_START = 0;
  private static final int STATE_HEADER = 1;
  private static final int STATE_DONE = 2;
  private static final int STATE_CONTENT = 3;

  private int state;
  private aSocketInputStream ais;
  private StreamTokenizer tok;

  private String request;
  private String url;
  private int contentLength;
  private ByteArrayOutputStream content;
  private int httpver;
  private Vector header;
  private httpConnection conn;
  private SinkIF compQ;

  static {
    logger.setLevel(org.apache.log4j.Level.DEBUG);
  }

  /**
   * Create an httpPacketReader with the given httpConnection
   * and completion queue.
   */
  httpPacketReader(httpConnection conn, SinkIF compQ) {
    this.conn = conn;
    this.compQ = compQ;
    this.ais = new aSocketInputStream();
    reset();
  }

  /**
   * Parse the given packet; returns true if a complete HTTP
   * request has been received and parsed.
   */
  boolean parsePacket(ATcpInPacket pkt) throws IOException {
    if (logger.isDebugEnabled())
      logger.debug("GPR: pushPacket called, size " + pkt.getBytes().length);
    ais.addPacket(pkt);

    int origstate;

    do {
      origstate = state;

      switch (state) {
        case STATE_START :
          state = parseURL();
          break;

        case STATE_HEADER :
          state = accumulateHeader();
          break;

        case STATE_CONTENT :
          state = fetchContent();
          break;

        case STATE_DONE :
          processHeader();
          reset();
          return true;

        default :
          throw new Error("Bad state in pushPacket");
      }

    } while (state != origstate);

    return false;
  }

  /**
   * @return
   */
  private int fetchContent() {
    int read = 0;
    try {
      while (content.size() < contentLength) {
        String s = nextWord();
        if (s == null)
          break;
        content.write(s.getBytes());
        read++;
      }
    } catch (IOException e) {}
    if (logger.isDebugEnabled()) {
      logger.debug("Content Read: " + Integer.toString(content.size()) + " of " + Integer.toString(contentLength));
      logger.debug("Content: " + content.toString());
    }
    return STATE_DONE;
  }

  /**
   * Reset the internal state of the packet reader.
   */
  private void reset() {
    state = STATE_START;
    ais.clear();
    tok = new StreamTokenizer(new InputStreamReader(ais));
    tok.resetSyntax();
    tok.wordChars((char) 0, (char) 255);
    tok.whitespaceChars('\u0000', '\u0020');
    tok.eolIsSignificant(true);
    request = null;
    url = null;
    contentLength = 0;
    content = new ByteArrayOutputStream();
    header = null;
    httpver = 0;
  }

  /**
   * Parse the first line of the request header.
   */
  private int parseURL() throws IOException {
    ais.mark(0);
    String req = nextWord();
    url = nextWord();
    String ver = nextWord();
    if ((req == null) || (url == null) || (ver == null)) {
      ais.reset();
      return STATE_START;
    } else {
      request = req;
      if (ver.equals("HTTP/1.0")) {
        httpver = httpRequest.HTTPVER_10;
        String tmp = nextWord(); // Throw away EOL
        return STATE_HEADER;
      } else if (ver.equals("HTTP/1.1")) {
        httpver = httpRequest.HTTPVER_11;
        String tmp = nextWord(); // Throw away EOL
        return STATE_HEADER;
      } else {
        if (!ver.equals(CRLF)) {
          throw new IOException("Unknown HTTP version in request: " + httpver);
        }
        httpver = httpRequest.HTTPVER_09;
        return STATE_DONE;
      }
    }
  }

  /**
   * Accumulate header lines.
   */
  private int accumulateHeader() throws IOException {

    String line;

    do {
      line = nextLine();
      if (logger.isDebugEnabled())
        logger.debug("hpr: accumulateHeader() read line " + line);

      if (line == null) {
        // End of buffer
        return STATE_HEADER;
      } else if (!line.equals("")) {
        if (header == null)
          header = new Vector(1);
        header.addElement(line);
        if (line.toUpperCase().startsWith("CONTENT-LENGTH")) {
          String clen = line.substring(16);
          if (clen.indexOf("\r") != -1)
            clen = clen.substring(0, clen.indexOf("\r"));
          else if (clen.indexOf("\n") != -1)
            clen = clen.substring(0, clen.indexOf("\n"));
          try {
            contentLength = Integer.parseInt(clen);
          } catch (NumberFormatException e) {
            contentLength = 0;
            logger.error("Request " + url + " conteined illegal Content-Lenghth header.");
          }
        }
      }

    } while (!line.equals(""));
    if (contentLength == 0)
      return STATE_DONE;
    else
      return STATE_CONTENT;
  }

  /**
   * Process the header, possibly pushing an httpRequest to the user.
   */
  private void processHeader() throws IOException {
    httpRequest req = (content.size() > 0) ? (new httpRequest(conn, request, url, httpver, header, content.toByteArray())) : (new httpRequest(conn, request, url, httpver, header));
    if (logger.isDebugEnabled())
      logger.debug("httpPacketReader: Pushing req to user");
    if (!compQ.enqueue_lossy(req)) {
      logger.info("httpPacketReader: WARNING: Could not enqueue_lossy to user: " + req);
    }
  }

  /**
   * Read the next whitespace-delimited word from the packet.
   */
  private String nextWord() throws IOException {
    while (true) {
      int type = tok.nextToken();
      switch (type) {

        case StreamTokenizer.TT_EOL :
          return CRLF;

        case StreamTokenizer.TT_EOF :
          return null;

        case StreamTokenizer.TT_WORD :
          if (logger.isDebugEnabled())
            logger.debug("nextWord returning " + tok.sval);
          return tok.sval;

        case StreamTokenizer.TT_NUMBER :
          if (logger.isDebugEnabled())
            logger.debug("nextWord returning number");
          return Double.toString(tok.nval);

        default :
          continue;
      }
    }
  }

  /**
   * Read the next line from the packet.
   */
  private String nextLine() throws IOException {
    String line = new String("");
    boolean first = true;

    while (true) {
      switch (tok.nextToken()) {

        case StreamTokenizer.TT_EOL :
          if (logger.isDebugEnabled())
            logger.debug("nextLine returning " + line);
          return line;

        case StreamTokenizer.TT_EOF :
          return null;

        case StreamTokenizer.TT_WORD :
          if (logger.isDebugEnabled())
            logger.debug("nextLine got word " + tok.sval);
          if (first) {
            line = tok.sval;
            first = false;
          } else {
            line += " " + tok.sval;
          }
          break;

        case StreamTokenizer.TT_NUMBER :
          if (logger.isDebugEnabled())
            logger.debug("nextLine got number " + tok.nval);
          if (first) {
            line = Double.toString(tok.nval);
            first = false;
          } else {
            line += " " + Double.toString(tok.nval);
          }
          break;

        default :
          continue;
      }
    }
  }

}
