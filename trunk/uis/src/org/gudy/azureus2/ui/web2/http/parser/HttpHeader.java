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

package org.gudy.azureus2.ui.web2.http.parser;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.http.util.*;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.api.SinkException;
import seda.sandStorm.lib.aSocket.ATcpConnection;

public abstract class HttpHeader extends HttpParserEvent implements HttpConstants, Cloneable {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpHeader");

  public boolean chunked, close;

  protected HttpString fw, sw, tw;
  protected HttpHeaderField first_field, last_field;

  public abstract int major_version();
  public abstract int minor_version();

  public HttpHeader(ATcpConnection c, HttpString fw, HttpString sw, HttpString tw) {
    super(c);
    this.fw = fw;
    this.sw = sw;
    this.tw = tw;
  }

  public Object clone() throws CloneNotSupportedException {
    HttpHeader result = (HttpHeader) super.clone();
    result.fw = fw;
    result.sw = sw;
    result.tw = tw;
    result.first_field = first_field;
    result.last_field = last_field;
    result.chunked = chunked;
    result.close = close;
    return result;
  }

  /**
   * Copy everything but the string values.
   */
  public HttpHeader deep_copy() {
    // Use clone to get the right result class
    HttpHeader result = null;
    try {
      result = (HttpHeader) clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionViolatedException("should be cloneable");
    }
    result.first_field = result.last_field = null;
    HttpHeaderField w = first_field;
    while (w != null) {
      HttpHeaderField a = w.deep_copy();
      result.append_field(a);
      w = w.next;
    }
    return result;
  }

  public HttpHeaderField prepend_field(HttpString name) {
    HttpHeaderField result = new HttpHeaderField(name);
    if (first_field == null)
      first_field = last_field = result;
    else {
      result.next = first_field;
      first_field = result;
    }
    return result;
  }

  public HttpHeaderField append_field(HttpString name) {
    HttpHeaderField result = new HttpHeaderField(name);
    append_field(result);
    return result;
  }

  protected void append_field(HttpHeaderField field) {
    if (first_field == null)
      first_field = last_field = field;
    else {
      last_field.next = field;
      last_field = last_field.next;
    }
  }

  public HttpHeaderField get_field(HttpString name) {
    HttpHeaderField walker = first_field;
    while (walker != null) {
      if (name.equals(walker.name))
        return walker;
      walker = walker.next;
    }
    return null;
  }

  public HttpHeaderField remove_field(HttpString name) {
    if (first_field == null)
      return null;
    HttpHeaderField result = null;
    if (name.equals(first_field.name)) {
      result = first_field;
      first_field = first_field.next;
      if (first_field == null)
        last_field = null;
    } else {
      HttpHeaderField previous = first_field;
      HttpHeaderField walker = first_field.next;
      while (walker != null) {
        if (name.equals(walker.name)) {
          result = walker;
          previous.next = walker.next;
          if (last_field == walker)
            last_field = previous;
          break;
        }
        previous = walker;
        walker = walker.next;
      }
    }
    return result;
  }

  public void enqueue(HttpOutputBuffer ob) throws SinkException {
    try {
      fw.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on fw, this=" + this);
      throw e;
    }
    try {
      STRING_SPACE.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on spc1, this=" + this);
      throw e;
    }
    try {
      sw.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on sw, this=" + this);
      throw e;
    }
    try {
      STRING_SPACE.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on spc2, this=" + this);
      throw e;
    }
    try {
      tw.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on tw, this=" + this);
      throw e;
    }
    try {
      CRLF.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on crlf1, this=" + this);
      throw e;
    }

    HttpHeaderField walker = first_field;
    while (walker != null) {
      try {
        walker.enqueue(ob);
      } catch (AssertionViolatedException e) {
        System.err.println("exception on field=" + walker);
        System.err.println("this=" + this);
        throw e;
      }
      try {
        CRLF.enqueue(ob);
      } catch (AssertionViolatedException e) {
        System.err.println("exception on crlf2, this=" + this);
        throw e;
      }
      walker = walker.next;
    }

    try {
      CRLF.enqueue(ob);
    } catch (AssertionViolatedException e) {
      System.err.println("exception on crlf3, this=" + this);
      throw e;
    }
  }

  /**
   * For debugging.
   */
  public String toString() {
    String result = "(Header text=\"" + fw + " " + sw + " " + tw + "\n";
    HttpHeaderField walker = first_field;
    while (walker != null) {
      result += walker + "\n";
      walker = walker.next;
    }
    return result + "\" chunked=" + chunked + " close=" + close + ")";
  }
}
