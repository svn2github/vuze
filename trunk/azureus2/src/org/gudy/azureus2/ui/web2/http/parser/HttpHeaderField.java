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
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.http.util.*;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.api.SinkException;

public class HttpHeaderField implements Cloneable {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpHeaderField");

  protected static class Value {
    public Value(HttpString v) {
      value = v;
    }
    public HttpString value;
    public Value next;
  }

  public HttpHeaderField(HttpString n) {
    name = n;
  }

  public Object clone() throws CloneNotSupportedException {
    HttpHeaderField result = (HttpHeaderField) super.clone();
    result.name = name;
    result.values_start = values_start;
    result.values_end = values_end;
    result.next = next;
    return result;
  }

  /**
   * Copy everything but the string values.
   */
  public HttpHeaderField deep_copy() {
    // Use clone to get the right result class
    HttpHeaderField result = null;
    try {
      result = (HttpHeaderField) clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionViolatedException("should be cloneable");
    }
    result.values_start = result.values_end = null;
    Value w = values_start;
    while (w != null) {
      result.append_value(w.value);
      w = w.next;
    }
    return result;
  }

  public void append_value(HttpString v) {
    if (values_start == null)
      values_start = values_end = new Value(v);
    else
      values_end = values_end.next = new Value(v);
  }

  public void replace_values(HttpString v) {
    values_start = values_end = new Value(v);
  }

  protected static final HttpString COMMASPACE = new HttpString(", ");
  protected static final HttpString COLONSPACE = new HttpString(": ");

  public void enqueue(HttpOutputBuffer ob) throws SinkException {
    name.enqueue(ob);
    COLONSPACE.enqueue(ob);
    Value walker = values_start;
    while (true) {
      walker.value.enqueue(ob);
      if (walker.next == null)
        break;
      COMMASPACE.enqueue(ob);
      walker = walker.next;
    }
  }

  protected static class FieldIterator implements Iterator {
    protected Value walker;
    protected FieldIterator(Value walker) {
      this.walker = walker;
    }

    public boolean hasNext() {
      return walker != null;
    }

    public Object next() {
      Value result = walker;
      walker = walker.next;
      return result.value;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public Iterator value_iterator() {
    return new FieldIterator(values_start);
  }

  /**
   * Such a common request, we have a special function for it.
   */
  public HttpString get_first_value() {
    if (values_start == null)
      return HttpString.EMPTY_STRING;
    return values_start.value;
  }

  public HttpString remove_first_value() {
    HttpString result = values_start.value;
    if (values_start != null)
      values_start = values_start.next;
    return result;
  }

  public int num_values() {
    int result = 0;
    Value walker = values_start;
    while (walker != null) {
      ++result;
      walker = walker.next;
    }
    return result;
  }

  /**
   * For debugging.
   */
  public String toString() {
    String result = name.toString() + ": ";
    if (values_start != null) {
      result += values_start.value;
      Value walker = values_start.next;
      while (walker != null) {
        result += ", " + walker.value;
        walker = walker.next;
      }
    }
    return result;
  }

  // TODO: add values iterator...

  public HttpString name;
  protected Value values_start, values_end;
  HttpHeaderField next;
}
