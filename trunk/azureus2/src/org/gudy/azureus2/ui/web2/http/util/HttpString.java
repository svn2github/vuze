/*
 * Copyright (c) 2001 Regents of the University of California. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 * 	1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 
 * 	2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 
 * 	3. Neither the name of the University nor the names of its contributors may be used 
 * to endorse or promote products derived from this software without specific prior written
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

package org.gudy.azureus2.ui.web2.http.util;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.http.parser.HttpStreamElement;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.api.SinkException;
import seda.sandStorm.core.BufferElement;

/**
 * Our own string class, the mark of any true Java program.
 */
public class HttpString implements HttpConstants {

  public static final Logger logger = Logger.getLogger("azureus2.ui.web.satges.HttpString");

  static {
  }

  public static final HttpString EMPTY_STRING = new HttpString(null, 0, 0);

  public HttpString(HttpStreamElement s, int o, int l) {
    sel = s;
    offset = o;
    length = l;
  }

  public HttpString(String s) {
    BufferElement buf = new BufferElement(s.getBytes());
    sel = new HttpStreamElement(buf);
    offset = 0;
    length = buf.size;
  }

  public HttpStreamElement sel;
  public int offset;
  public int length;

  public void truncate(int new_length) {
    HttpStreamElement walker = sel;
    int togo = new_length + offset - walker.buf.size;
    while (togo > 0) {
      walker = walker.next;
      togo -= walker.buf.size;
    }
    // We're on the last buffer element contained in the first
    // new_length bytes. Free any remaining buffer elements.
    if (walker.next != null)
      walker.next = null;
    length = new_length;
  }

  public void enqueue(HttpOutputBuffer ob) throws SinkException {
    HttpStreamElement walker = sel;
    int walker_offset = offset;
    int togo = length;
    while (togo > 0) {
      int size = Math.min(walker.buf.size, togo);
      try {
        ob.enqueue(walker.buf, walker_offset, size);
      } catch (AssertionViolatedException e) {
        System.err.println("caught assertion violation on enqueue.");
        HttpStreamElement walker2 = sel;
        int walker2_offset = offset;
        int togo2 = length;
        while (togo2 > 0) {
          int size2 = Math.min(walker2.buf.size, togo2);
          System.err.print("len=" + walker2.buf.size + ", off=" + walker2_offset + ", cnt=" + size2);
          int i = 0;
          System.err.print("data=\"");
          while ((i + walker2_offset + size2 < walker2.buf.size) && (i < size2)) {
            System.err.print((char) walker2.buf.data[walker2_offset + i]);
          }
          System.err.println("\"");
          togo2 -= size2;
          walker2 = walker2.next;
          if (walker2 != null)
            walker2_offset = walker2.buf.offset;
        }
        throw e;
      }
      togo -= size;
      walker = walker.next;
      if (walker != null)
        walker_offset = walker.buf.offset;
    }
  }

  public Object[] parse_server() {
    HttpStreamElement server = null, port = null, file = null;
    int server_offset = 0, port_offset = 0, file_offset = 0;
    int port_pos = -1;
    int file_length = -1, port_length = -1, server_length = -1;

    int pos = 0;
    HttpStreamElement walker = sel;
    String backe = new String(sel.buf.getBytes());
    int walker_offset = offset;
    int togo = length;
    while (togo-- > 0) {
      if (walker_offset == walker.buf.offset + walker.buf.size) {
        walker = walker.next;
        walker_offset = walker.buf.offset;
      }
      int value = walker.buf.data[walker_offset];

      if (pos == 0) {
        if (value == SLASH) { // handle local request
          server = null;
          file = walker;
          file_offset = walker_offset;
          file_length = length - pos;
          break;
        } else if ((value != LOWER_H) && (value != CAP_H))
          return null;
      } else if (pos == 1) {
        if ((value != LOWER_T) && (value != CAP_T))
          return null;
      } else if (pos == 2) {
        if ((value != LOWER_T) && (value != CAP_T))
          return null;
      } else if (pos == 3) {
        if ((value != LOWER_P) && (value != CAP_P))
          return null;
      } else if (pos == 4) {
        if ((value != COLON) && (value != COLON))
          return null;
      } else if (pos == 5) {
        if ((value != SLASH) && (value != SLASH))
          return null;
      } else if (pos == 6) {
        if ((value != SLASH) && (value != SLASH))
          return null;
      } else if (pos == 7) {
        server = walker;
        server_offset = walker_offset;
      } else if (pos == port_pos) {
        port = walker;
        port_offset = walker_offset;
      } else if (value == COLON) {
        server_length = pos - 7;
        port_pos = pos + 1;
      } else if (value == SLASH) {
        if ((server_length != -1) && (port_length == -1))
          port_length = pos - server_length - 8;
        file = walker;
        file_offset = walker_offset;
        file_length = length - pos;
        break;
      }

      ++pos;
      ++walker_offset;
    }

    //if (pos < 8)
    //  return null;
    //else {
      int p = 0;
      if (server != null) {
        p = 80;
        if (port_pos == -1)
          server_length = pos - 7;
        else
          p = (new HttpString(port, port_offset, port_length)).parse_int();
      }
      Object[] result = { (server==null) ? null : new HttpString(server, server_offset, server_length), new Integer(p), (file == null) ? null : (new HttpString(file, file_offset, file_length))};
      return result;
    //}
  }

  public void chomp_tail() {
    if (length < 1)
      return;

    HttpStreamElement walker = sel;
    int walker_offset = offset;
    int togo = length;
    int last_nonspace_index = 0;
    int index = 0;

    while (togo-- > 0) {
      if (walker_offset == walker.buf.offset + walker.buf.size) {
        walker = walker.next;
        walker_offset = walker.buf.offset;
      }
      int value = walker.buf.data[walker_offset++];

      if ((value != SPACE) && (value != TAB) && (value != CR) && (value != LF)) {

        last_nonspace_index = index;
      }
      ++index;
    }

    truncate(last_nonspace_index + 1);
  }

  public int parse_int() {
    int result = 0;
    int digit = 0;
    boolean negate = false;

    HttpStreamElement walker = sel;
    int walker_offset = offset;
    int togo = length;

    if (length < 1)
      throw new NumberFormatException();

    while (togo-- > 0) {
      if (walker_offset == walker.buf.offset + walker.buf.size) {
        walker = walker.next;
        walker_offset = walker.buf.offset;
      }
      int value = walker.buf.data[walker_offset++];

      if ((digit == 0) && (value == MINUS)) {
        negate = true;
      }
      if ((value >= ZERO) && (value <= NINE)) {
        result = result * 10 + value - ZERO;
      } else {
        throw new NumberFormatException("not an int: " + this +"didn't like value 0x" + Integer.toHexString(value));
      }

      ++digit;
    }

    return (negate ? (-1 * result) : result);
  }

  public int parse_hex() {
    int result = 0;
    int digit = 0;
    boolean negate = false;

    HttpStreamElement walker = sel;
    int walker_offset = offset;
    int togo = length;

    if (length < 1)
      throw new NumberFormatException();

    while (togo-- > 0) {
      if (walker_offset == walker.buf.offset + walker.buf.size) {
        walker = walker.next;
        walker_offset = walker.buf.offset;
      }
      int value = walker.buf.data[walker_offset++];

      if ((digit == 0) && (value == MINUS)) {
        negate = true;
      }
      if ((value >= ZERO) && (value <= NINE)) {
        result = result * 16 + value - ZERO;
      } else if ((value >= LOWER_A) && (value <= LOWER_F)) {
        result = result * 16 + value - LOWER_A + 10;
      } else if ((value >= CAP_A) && (value <= CAP_F)) {
        result = result * 16 + value - CAP_A + 10;
      } else {
        throw new NumberFormatException("value='" + (char) value + "', ascii 0x" + Integer.toHexString(value));
      }

      ++digit;
    }

    return (negate ? (-1 * result) : result);
  }

  public String toString() {
    StringBuffer result = new StringBuffer(length);
    HttpStreamElement walker = sel;
    int walker_offset = offset;
    int togo = length;
    while (togo-- > 0) {
      if (walker_offset == walker.buf.offset + walker.buf.size) {
        walker = walker.next;
        walker_offset = walker.buf.offset;
      }
      result.append((char) walker.buf.data[walker_offset++]);
    }
    return result.toString();
  }

  /**
   * Case insensitive equals.
   */
  public boolean equals(Object rhs) {
    if (logger.isDebugEnabled())
      logger.debug("equals: starting");

    HttpString other = (HttpString) rhs;
    if (length != other.length) {
      if (logger.isDebugEnabled())
        logger.debug("equals: false 1");
      return false;
    }

    HttpStreamElement one = sel, two = other.sel;
    int one_offset = offset, two_offset = other.offset;

    int togo = length;
    if (logger.isDebugEnabled())
      logger.debug("equals: togo=" + togo);
    while (togo > 0) {

      if (logger.isDebugEnabled())
        logger.debug("equals: togo=" + togo);
      if (one_offset == one.buf.offset + one.buf.size) {
        one = one.next;
        one_offset = one.buf.offset;
      }
      if (two_offset == two.buf.offset + two.buf.size) {
        two_offset = 0;
        two_offset = two.buf.offset;
      }

      if (logger.isDebugEnabled())
        logger.debug("equals: one_offset=" + one_offset + ", one.buf.size=" + one.buf.size);
      if (logger.isDebugEnabled())
        logger.debug("equals: two_offset=" + two_offset + ", two.buf.size=" + two.buf.size);

      int cnt = min(one.buf.size - one_offset, two.buf.size - two_offset, togo);

      if (logger.isDebugEnabled())
        logger.debug("equals: cnt=" + cnt);
      if (cnt == 0)
        throw new AssertionViolatedException("cnt == 0");

      togo -= cnt;
      while (cnt-- > 0) {
        if (logger.isDebugEnabled())
          logger.debug("equals: " + (char) one.buf.data[one_offset] + " " + (char) two.buf.data[two_offset]);

        int o = 0xff & (int) one.buf.data[one_offset];
        int t = 0xff & (int) two.buf.data[two_offset];

        if (o == t) {
          // do nothing
        } else {
          // switch both to upper case
          if ((o >= LOWER_A) && (o <= LOWER_Z))
            o = o - LOWER_A + CAP_A;
          if ((t >= LOWER_A) && (t <= LOWER_Z))
            t = t - LOWER_A + CAP_A;

          if (o != t) {
            if (logger.isDebugEnabled())
              logger.debug("equals: false 2");
            return false;
          }
        }

        ++one_offset;
        ++two_offset;
      }
    }

    if (logger.isDebugEnabled())
      logger.debug("equals: true");
    return true;
    /*
    * }
    * 
    * public boolean equals (Object rhs) {
    * 
    * 
    * if (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: starting");
    * 
    * HttpString other = (HttpString) rhs; if (length != other.length) { if
    * (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: false 1");
    * return false; }
    * 
    * HttpStreamElement one = sel, two = other.sel; int one_offset = offset,
    * two_offset = other.offset;
    * 
    * int togo = length; if (logger.isDebugEnabled()) Debug.printtagln (TAG,
    * "equals: togo=" + togo); while (togo > 0) {
    * 
    * if (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: togo=" +
    * togo); if (one_offset == one.buf.offset + one.buf.size) { one =
    * one.next; one_offset = one.buf.offset; } if (two_offset ==
    * two.buf.offset + two.buf.size) { two_offset = 0; two_offset =
    * two.buf.offset; }
    * 
    * if (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals:
    * one_offset=" + one_offset + ", one.buf.size=" + one.buf.size); if
    * (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: two_offset=" +
    * two_offset + ", two.buf.size=" + two.buf.size);
    * 
    * int cnt = min (one.buf.size - one_offset, two.buf.size - two_offset,
    * togo);
    * 
    * if (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: cnt=" +
    * cnt); if (cnt == 0) throw new AssertionViolatedException ("cnt == 0");
    * 
    * togo -= cnt; while (cnt-- > 0) { if (logger.isDebugEnabled())
    * Debug.printtagln (TAG, "equals: " + (char) one.buf.data [one_offset] + " " +
    * (char) two.buf.data [two_offset]); if (one.buf.data [one_offset++] !=
    * two.buf.data [two_offset++]) { if (logger.isDebugEnabled())
    * Debug.printtagln (TAG, "equals: false 2"); return false; } } }
    * 
    * if (logger.isDebugEnabled()) Debug.printtagln (TAG, "equals: true");
    * return true;
    */
  }

  public int hashCode() {
    throw new NoSuchMethodError(); // TODO
  }

  protected static final int min(int a, int b, int c) {
    return Math.min(a, Math.min(b, c));
  }
}
