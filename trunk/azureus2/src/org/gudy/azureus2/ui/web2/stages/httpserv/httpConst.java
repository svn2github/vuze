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

/**
 * This interface defines various constants used by the HTTP
 * protocol implementation.
 * 
 * @author Matt Welsh
 */
public interface httpConst {

  /**
   * The default port on which httpServer listens.
   */
  final static int DEFAULT_HTTP_PORT = 80;

  /**
   * The threshold for outgoing TCP connections; currently set to
   * -1, meaning infinite threshold.
   */
  final static int WRITE_CLOG_THRESHOLD = -1;

  /**
   * Standard HTTP end-of-line marker, equal to <tt>"\r\n"</tt>.
   */
  final static String CRLF = "\r\n";

  /**
   * The HTTP version sent in the response header.
   */
  final static String HTTP_VERSION = "HTTP/1.1";
}
