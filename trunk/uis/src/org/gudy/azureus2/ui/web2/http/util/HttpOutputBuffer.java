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

package org.gudy.azureus2.ui.web2.http.util;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.api.SinkException;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.lib.aSocket.ATcpConnection;

public class HttpOutputBuffer  {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpOutputBuffer");

  protected static final boolean DEBUG = false;
  protected static final int MIN_SIZE = 1448;

  public HttpOutputBuffer(ATcpConnection conn) {
    this.conn = conn;
  }

  public void enqueue(byte[] data, int offset, int length) throws SinkException {
    if (offset + length > data.length) {
      throw new AssertionViolatedException("data.length=" + data.length + ", offset=" + offset + ", length=" + length);
    }

    while (length > 0) {
      if (current == null)
        current = new BufferElement(new byte[MIN_SIZE], 0, 0);

      int todo = Math.min(current.data.length - current.size, length);
      System.arraycopy(data, offset, current.data, current.size, todo);
      current.size += todo;
      offset += todo;
      length -= todo;

      if (current.size == current.data.length) {
        enqueue(current);
        current = null;
      }
    }
  }

  public void enqueue(BufferElement buf, int offset, int length) throws SinkException {
    if (offset + length > buf.data.length) {
      throw new AssertionViolatedException("buf.data.length=" + buf.data.length + ", offset=" + offset + ", length=" + length);
    }

    while (length > 0) {
      if (current == null)
        current = new BufferElement(new byte[MIN_SIZE], 0, 0);

      int todo = Math.min(current.data.length - current.size, length);
      System.arraycopy(buf.data, offset, current.data, current.size, todo);
      current.size += todo;
      offset += todo;
      length -= todo;

      if (current.size == current.data.length) {
        enqueue(current);
        current = null;
      }
    }
  }

  public void flush() throws SinkException {
    if (current != null) {
      enqueue(current);
      current = null;
    }
  }

  protected final void enqueue(BufferElement b) throws SinkException {
    if (DEBUG) {
      System.out.print("Enqueuing \"");
      for (int i = b.offset; i < b.offset + b.size; ++i) {
        char c = (char) b.data[i];
        if (((b.data[i] >= 0x20) && (b.data[i] < 0x7f)) || (b.data[i] == 0xd) || (b.data[i] == 0xa) || (b.data[i] == 0x9))
          System.out.print((char) b.data[i]);
        else {
          String s = Integer.toHexString(0xff & ((int) b.data[i]));
          if (s.length() == 1)
            System.out.print("[0" + s + "]");
          else
            System.out.print("[" + s + "]");
        }
      }
      System.out.println("\"");
    }
    conn.enqueue(b);
  }

  protected ATcpConnection conn;
  protected BufferElement current;
}
