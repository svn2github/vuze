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

package org.gudy.azureus2.ui.web2.http.response;

import org.gudy.azureus2.ui.web2.http.request.httpRequest;
import org.gudy.azureus2.ui.web2.http.util.HttpConstants;

import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.core.BufferElement;

/**
 * An httpResponse corresponding to a '503 Service Unavailable' error.
 * 
 * @author Matt Welsh
 * @see httpResponse
 */
public class httpServiceUnavailableResponse extends httpResponse implements HttpConstants, QueueElementIF {

  private static final boolean DEBUG = false;

  /**
   * Create an httpServiceUnavailableResponse corresponding to the 
   * given request with the given reason.
   */
  public httpServiceUnavailableResponse(httpRequest request, String reason) {
    super(httpResponse.RESPONSE_SERVICE_UNAVAILABLE, "text/html");

    String str = "<html><head><title>503 Service Unavailable</title></head><body bgcolor=white><font face=\"helvetica\"><big><big><b>503 Service Unavailable</b></big></big><p>The requested service is unavailable. The reason given was:<p><blockquote><tt>"+reason+"</tt></blockquote></body></html>\n";
    BufferElement mypayload = new BufferElement(str.getBytes());
    setPayload(mypayload);
  }

  protected String getEntityHeader() {
    return null;
  }

}
