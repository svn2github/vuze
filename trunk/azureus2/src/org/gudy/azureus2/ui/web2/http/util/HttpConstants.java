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


public interface HttpConstants {

  public static final int MINUS = 0xff & (int) '-';
  public static final int ZERO = 0xff & (int) '0';
  public static final int NINE = 0xff & (int) '9';
  public static final int LOWER_A = 0xff & (int) 'a';
  public static final int LOWER_F = 0xff & (int) 'f';
  public static final int LOWER_H = 0xff & (int) 'h';
  public static final int LOWER_P = 0xff & (int) 'p';
  public static final int LOWER_T = 0xff & (int) 't';
  public static final int LOWER_Z = 0xff & (int) 'z';
  public static final int CAP_A = 0xff & (int) 'A';
  public static final int CAP_F = 0xff & (int) 'F';
  public static final int CAP_H = 0xff & (int) 'H';
  public static final int CAP_P = 0xff & (int) 'P';
  public static final int CAP_T = 0xff & (int) 'T';
  public static final int CAP_Z = 0xff & (int) 'Z';
  public static final int COLON = 0xff & (int) ':';
  public static final int SLASH = 0xff & (int) '/';

  public static final int CR = 0x0D;
  public static final int LF = 0x0A;
  public static final int SPACE = 0x20;
  public static final int COMMA = 0x2C;
  public static final int TAB = 0x09;

  public static final HttpString STRING_SPACE = new HttpString(" ");
  public static final HttpString STRING_ZERO = new HttpString("0");
  public static final HttpString CRLF = new HttpString("\r\n");
  public static final HttpString HTTP11 = new HttpString("HTTP/1.1");
  public static final HttpString HTTP10 = new HttpString("HTTP/1.0");

  public static final HttpString GZIP = new HttpString("gzip");
  public static final HttpString CONTENT_ENCODING = new HttpString("Content-Encoding");
  public static final HttpString ACCEPT_ENCODING = new HttpString("Accept-Encoding");
  public static final HttpString CONTENT_TYPE = new HttpString("Content-Type");
  public static final HttpString HEAD_METHOD = new HttpString("HEAD");
  public static final HttpString POST = new HttpString("POST");
  public static final HttpString GET = new HttpString("GET");
  public static final HttpString IDENTITY = new HttpString("identity");
  public static final HttpString HOST = new HttpString("Host");
  public static final HttpString CHUNKED = new HttpString("chunked");
  public static final HttpString CLOSE = new HttpString("close");
  public static final HttpString KEEP_ALIVE = new HttpString("keep-alive");
  public static final HttpString CONNECTION = new HttpString("Connection");
  public static final HttpString PROXY_CONNECTION = new HttpString("Proxy-Connection");
  public static final HttpString TRANSFER_ENCODING = new HttpString("Transfer-Encoding");
  public static final HttpString CONTENT_LENGTH = new HttpString("Content-Length");
  public final static HttpString HTTP_VERSION = new HttpString("HTTP/1.1");

}
