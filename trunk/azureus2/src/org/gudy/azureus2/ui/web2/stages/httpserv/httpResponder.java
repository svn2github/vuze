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

import seda.sandStorm.api.QueueElementIF;

/**
 * This class is used to wrap an HTTP response along with the 
 * connection which it is destined for. 
 *
 * @author Matt Welsh
 * @see httpResponse
 * @see httpConnection
 */
public class httpResponder implements httpConst, QueueElementIF {

  private httpResponse resp;
  private httpConnection conn;
  private boolean closeConnection;
  private boolean sendHeader;

  /**
   * Create an httpResponder with the given response and connection.
   * @param closeConnection Indicate that the connection should be
   *   closed after sending this response.
   * @param sendHeader Indicate that the header of the response should
   *   be sent along with the payload.
   */
  public httpResponder(httpResponse resp, httpConnection conn,
      boolean closeConnection, boolean sendHeader) {
    this.resp = resp;
    this.conn = conn;
    this.closeConnection = closeConnection;
    this.sendHeader = sendHeader;
  }

  /**
   * Create an httpResponder with the given response and connection.
   * @param closeConnection Indicate that the connection should be
   *   closed after sending this response.
   */
  public httpResponder(httpResponse resp, httpConnection conn,
      boolean closeConnection) {
    this.resp = resp;
    this.conn = conn;
    this.closeConnection = closeConnection;
    this.sendHeader = true;
  }

  /**
   * Create an httpResponder with the given response and connection.
   */
  public httpResponder(httpResponse resp, httpConnection conn) {
    this(resp, conn, false, true);
  }

  /**
   * Create an httpResponder with the given response, with the
   * connection being derived from the given request.
   * @param closeConnection Indicate that the connection should be
   *   closed after sending this response.
   * @param sendHeader Indicate that the header of the response should
   *   be sent along with the payload.
   */
  public httpResponder(httpResponse resp, httpRequest req,
      boolean closeConnection, boolean sendHeader) {
    this(resp, req.getConnection(), closeConnection, sendHeader);
  }

  /**
   * Create an httpResponder with the given response, with the
   * connection being derived from the given request.
   * @param closeConnection Indicate that the connection should be
   *   closed after sending this response.
   */
  public httpResponder(httpResponse resp, httpRequest req,
      boolean closeConnection) {
    this(resp, req.getConnection(), closeConnection);
  }

  /**
   * Create an httpResponder with the given response, with the
   * connection being derived from the given request.
   */
  public httpResponder(httpResponse resp, httpRequest req) {
    this(resp, req.getConnection(), 
	((req.getHttpVer() < httpRequest.HTTPVER_11)?(true):(false)));
  }

  /**
   * Return the connection for this responder. 
   */
  public httpConnection getConnection() {
    return conn;
  }

  /**
   * Return the response for this responder. 
   */
  public httpResponse getResponse() {
    return resp;
  }

  /**
   * Returns whether the connection should be closed after sending this 
   * response. 
   */
  public boolean shouldClose() {
    return closeConnection;
  }

  /**
   * Returns whether the response header should be sent.
   */
  public boolean sendHeader() {
    return sendHeader;
  }

}
