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
import seda.sandStorm.core.BufferElement;

/**
 * An httpResponse corresponding to a '301 Moved Permanently' response.
 * 
 * @author Matt Welsh
 */
public class httpRedirectResponse extends httpResponse implements httpConst, QueueElementIF {

  private static final boolean DEBUG = false;
  private String oldURL;
  private String newURL;

  /**
   * Create a redirect response corresponding to the given request with 
   * the given new URL.
   */
  public httpRedirectResponse(httpRequest request, String newURL) {
    super(httpResponse.RESPONSE_REDIRECT, "text/html");
    this.oldURL = request.getURL();
    this.newURL = newURL;

    String str = "<html><head><title>301 Moved Permanently</title></head><body bgcolor=white><font face=\"helvetica\"><big><big><b>301 Moved Permanently</b></big></big><p>The URL you requested:<p><blockquote><tt>"+oldURL+"</tt></blockquote><p>has moved permanently to:<p><blockquote><tt><a href=\""+newURL+"\">"+newURL+"</a></tt></blockquote></body></html>\n";
    BufferElement mypayload = new BufferElement(str.getBytes());
    setPayload(mypayload);
  }


  protected String getEntityHeader() {
    return "Location: "+newURL+CRLF+"Connection: close"+CRLF;
  }

  public String toString() {
    return "httpRedirectResponse [oldURL="+oldURL+", newURL="+newURL+"]";
  }
  
}
