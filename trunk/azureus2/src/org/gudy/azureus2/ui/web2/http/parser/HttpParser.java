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

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.http.util.HttpConstants;
import org.gudy.azureus2.ui.web2.http.util.HttpString;
import org.gudy.azureus2.ui.web2.util.AssertionViolatedException;

import seda.sandStorm.api.SinkIF;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.lib.aSocket.ATcpConnection;
import seda.sandStorm.lib.aSocket.ATcpInPacket;

public class HttpParser implements HttpConstants {

  public static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.HttpParser");
  static {
    //logger.setLevel(org.apache.log4j.Level.DEBUG);
  }

  protected static final int STATE_INIT = 0;
  protected static final int STATE_SECOND_WORD = 1;
  protected static final int STATE_THIRD_WORD = 2;
  protected static final int STATE_FIRST_LINE_CR = 3;
  protected static final int STATE_HEADER_FIELD = 4;
  protected static final int STATE_HEADER_FIELD_CR = 5;
  protected static final int STATE_HEADER_FIELD_SPACE = 6;
  protected static final int STATE_HEADER_VALUE = 7;
  protected static final int STATE_HEADER_VALUE_SPACE = 8;
  protected static final int STATE_HEADER_VALUE_CR = 9;
  protected static final int STATE_BODY_LENGTH = 10;
  protected static final int STATE_BODY_CHUNKED = 12;
  protected static final int STATE_BODY_CHUNKED_CR = 13;
  protected static final int STATE_BODY_CHUNK_SIZE = 14;
  protected static final int STATE_BODY_CHUNKED_TAIL = 15;
  protected static final int STATE_BODY_CHUNKED_TAIL_CR = 16;
  protected static final int STATE_BODY_CHUNKED_TRAILING_WS = 17;

  protected static final String[] state_to_string = { "STATE_INIT", "STATE_SECOND_WORD", "STATE_THIRD_WORD", "STATE_FIRST_LINE_CR", "STATE_HEADER_FIELD", "STATE_HEADER_FIELD_CR", "STATE_HEADER_FIELD_SPACE", "STATE_HEADER_VALUE", "STATE_HEADER_VALUE_SPACE", "STATE_HEADER_VALUE_CR", "STATE_BODY_LENGTH", "STATE_BODY_CHUNKED", "STATE_BODY_CHUNKED_CR", "STATE_BODY_CHUNK_SIZE", "STATE_BODY_CHUNKED_TAIL", "STATE_BODY_CHUNKED_TAIL_CR", "STATE_BODY_CHUNKED_TRAILING_WS" };

  /**
   * I manage the linked lists by hand to avoid the overhead of iterators.
   */
  protected static class InOrderPacket {
    BufferElement buf;
    InOrderPacket next;
    InOrderPacket(BufferElement b) {
      buf = b;
    }
  }

  public HttpParser(String tag, ATcpConnection conn, SinkIF compq) {
    connection = conn;
    this.compq = compq;
  }

  public static String print_packet(ATcpInPacket packet) {
    String result = "";
    BufferElement b = packet.getBufferElement();
    for (int i = b.offset; i < b.offset + b.size; ++i) {
      char c = (char) b.data[i];
      if (Character.isISOControl(c))
        result += "<" + Integer.toHexString(b.data[i]) + ">";
      else
        result += (char) b.data[i];
    }
    return result;
  }

  public void add_packet(ATcpInPacket packet) {
    if (logger.isDebugEnabled()) {
      String data = "";
      BufferElement b = packet.getBufferElement();
      for (int i = b.offset; i < b.offset + b.size; ++i)
        data += (char) b.data[i];
      logger.debug("got packet: \"" + data + "\"");
    }

    long seq = packet.getSequenceNumber();
    if (seq == next_seq_num) {
      BufferElement buf = packet.getBufferElement();
      InOrderPacket pkt = new InOrderPacket(buf);
      if (first_pkt == null) {
        first_pkt = last_packet = pkt;
        first_pkt_offset = buf.offset;
        if (end != null)
          end = end.next = new HttpStreamElement(first_pkt.buf);
        else
          reset_start();
      } else
        last_packet = last_packet.next = pkt;

      ++next_seq_num;
    } else {
      out_of_order_packets.put(new Long(seq), packet);
    }

    while (!out_of_order_packets.isEmpty()) {
      Long smallest_key = (Long) out_of_order_packets.firstKey();
      if ((smallest_key).longValue() != next_seq_num)
        break;
      packet = (ATcpInPacket) out_of_order_packets.remove(smallest_key);
      BufferElement buf = packet.getBufferElement();
      last_packet = last_packet.next = new InOrderPacket(buf);
      ++next_seq_num;
    }

    advance_state();
  }

  protected final int next_byte() {
    if (first_pkt == null)
      return -1;
    int result = 0xff & (int) (first_pkt.buf.data[first_pkt_offset++]);
    length += 1;
    if (first_pkt_offset == first_pkt.buf.offset + first_pkt.buf.size) {
      first_pkt = first_pkt.next;
      if (first_pkt != null) {
        first_pkt_offset = first_pkt.buf.offset;
        end = end.next = new HttpStreamElement(first_pkt.buf);
      }
    }
    return result;
  }

  public final void connection_closed() {
    if ((state == STATE_INIT) || (state == STATE_BODY_CHUNK_SIZE) || (state == STATE_BODY_LENGTH)) {
      if (logger.isDebugEnabled())
        logger.debug("got connection closed.");
      chunk_size = 0;
      advance_state();
    } else {
      if (logger.isDebugEnabled())
        logger.debug("got unexpected connection closed (state=" + state + "); sitting quietly.");
      dont_advance = true;
    }
  }

  protected void reset_start() {
    if (first_pkt != null) {
      start = end = new HttpStreamElement(first_pkt.buf);
      start_offset = first_pkt_offset;
    } else {
      start = end = null;
      start_offset = 0;
    }

    length = 0;
  }

  protected HttpString finish_string() {
    HttpString result = new HttpString(start, start_offset, length);
    reset_start();
    return result;
  }

  public boolean error() {
    return dont_advance;
  }

  protected boolean dont_advance = false;
  protected void advance_state() {

    if (dont_advance) {
      throw new AssertionViolatedException("in a bad state (" + state + "), shouldn't be advancing");
    }

    while (true) {

      //if (DEBUG) Debug.printtagln (TAG, "state = " + state);

      if ((state == STATE_BODY_CHUNK_SIZE) || (state == STATE_BODY_LENGTH)) {

        boolean sent_done = false;

        // Read all of the packets we have and send them to the
        // completion queue, up to the size of the chunk.

        while ((chunk_size > 0) && (first_pkt != null)) {
          int first_pkt_bytes_remaining = first_pkt.buf.size - first_pkt_offset + first_pkt.buf.offset;

          if (logger.isDebugEnabled())
            logger.debug("chunk_size=" + chunk_size + ", first_pkt_bytes_remaining=" + first_pkt_bytes_remaining);

          HttpBodyFragment frag = null;
          if (first_pkt_bytes_remaining <= chunk_size) {
            if (first_pkt_offset == first_pkt.buf.offset) {
              if (logger.isDebugEnabled())
                logger.debug("whole pkt");
              // Use existing buffer element
              frag = new HttpBodyFragment(connection, first_pkt.buf);
            } else {
              if (logger.isDebugEnabled())
                logger.debug("tail pkt");
              // Create a new buffer element
              frag = new HttpBodyFragment(connection, first_pkt.buf.data, first_pkt_offset, first_pkt_bytes_remaining);
            }

            first_pkt = first_pkt.next;
            if (first_pkt != null)
              first_pkt_offset = first_pkt.buf.offset;

            chunk_size -= first_pkt_bytes_remaining;
          } else {
            if (logger.isDebugEnabled())
              logger.debug("head pkt");
            frag = new HttpBodyFragment(connection, first_pkt.buf.data, first_pkt_offset, chunk_size);
            first_pkt_offset += chunk_size;
            chunk_size = 0;
          }

          if (logger.isDebugEnabled())
            logger.debug("new body fragment of " + frag.buf.size + " bytes");

          if ((chunk_size == 0) && (state != STATE_BODY_CHUNK_SIZE)) {

            // For non-chunked bodies, set the done flag of the
            // last fragment before sending it instead of
            // sending a HttpBodyDone event.

            frag.done = true;
            sent_done = true;
          }

          if (!compq.enqueue_lossy(frag)) {
            if (logger.isDebugEnabled())
              logger.debug("couldn't enqueue header to compq.");
          }
        }

        if (chunk_size == 0) {
          if (state == STATE_BODY_CHUNK_SIZE) {
            state = STATE_BODY_CHUNKED_TAIL;
          } else {
            if (!sent_done) {
              if (logger.isDebugEnabled())
                logger.debug("done reading body");
              if (!compq.enqueue_lossy(new HttpBodyDone(connection))) {
                if (logger.isDebugEnabled())
                  logger.debug("couldn't enqueue body done to compq.");
              }
            }
            state = STATE_INIT;
          }
          reset_start();
        }
      }

      int b = next_byte();
      if (b == -1)
        break;

      switch (state) {

        case STATE_INIT :
          if ((length == 1) && ((b == SPACE) || (b == CR) || (b == LF))) {
            reset_start();
            break;
          }

          if (b == SPACE) {
            // We've read the first word of the first line.
            state = STATE_SECOND_WORD;
            fw = finish_string();
            fw.truncate(fw.length - 1); // strip SPACE
            if (logger.isDebugEnabled())
              logger.debug("fw=\"" + fw + "\"");
          }
          break;

        case STATE_SECOND_WORD :
          if (b == SPACE) {
            // We've read the second word of the first line.
            state = STATE_THIRD_WORD;
            sw = finish_string();
            sw.truncate(sw.length - 1); // strip SPACE
            if (logger.isDebugEnabled())
              logger.debug("sw=\"" + sw + "\"");
          }
          break;

        case STATE_THIRD_WORD :
          if (b == CR)
            state = STATE_FIRST_LINE_CR;
          break;

        case STATE_FIRST_LINE_CR :
          if (b == LF) {
            // We've read the whole first line.
            state = STATE_HEADER_FIELD;
            HttpString tw = finish_string();
            tw.truncate(tw.length - 2); // strip CRLF
            if (logger.isDebugEnabled())
              logger.debug("tw=\"" + tw + "\"");
            if ((fw.equals(GET)) || (fw.equals(POST))) {
              if (logger.isDebugEnabled())
                logger.debug(fw.toString());
              header = new HttpRequestHeader(connection, fw, sw, tw);
            } else {
              if (logger.isDebugEnabled())
                logger.debug("Response");
              header = new HttpResponseHeader(connection, fw, sw, tw);
            }
          } else {
            // False alarm--only the CR of the CRLF pair.
            state = STATE_INIT;
          }
          break;

        case STATE_HEADER_FIELD :
          if (b == COLON) {
            // We've read the whole field name.
            state = STATE_HEADER_FIELD_SPACE;
            HttpString fn = finish_string();
            fn.truncate(fn.length - 1); // strip COLON
            if (logger.isDebugEnabled())
              logger.debug("fn=\"" + fn + "\"");
            field = header.append_field(fn);
          }
          if (b == CR)
            state = STATE_HEADER_FIELD_CR;
          break;

        case STATE_HEADER_FIELD_CR :
          if (b == LF) {
            // We've read a blank line. The header is done.
            if (logger.isDebugEnabled())
              logger.debug("header finished");

            if (!compq.enqueue_lossy(header)) {
              if (logger.isDebugEnabled())
                logger.debug("couldn't enqueue header to compq.");
            }

            // Decide about the body.
            reset_start();

            // Look for Connection:close first.
            HttpHeaderField query = header.get_field(CONNECTION);
            if ((query != null) && (query.num_values() == 1) && (query.get_first_value().equals(CLOSE))) {
              if (logger.isDebugEnabled())
                logger.debug("done on close");
              header.close = true;
            }

            if (header instanceof HttpResponseHeader) {
              HttpResponseHeader resp_hdr = (HttpResponseHeader) header;
              int code = resp_hdr.response_code();
              if (code == 100) {
                if (logger.isDebugEnabled())
                  logger.debug("code 100");
                // No body, but don't send event
                state = STATE_INIT;
                reset_start();
                break;
              } else if ((code / 100 == 1) || (code == 204) || (code == 304)) {
                // No body
                state = STATE_INIT;
                reset_start();
                if (logger.isDebugEnabled())
                  logger.debug("no body");
                if (!compq.enqueue_lossy(new HttpNoBody(connection))) {
                  if (logger.isDebugEnabled())
                    logger.debug("couldn't enqueue HttpNoBody " + "to compq.");
                }
                break;
              }
            }

            if (header instanceof HttpRequestHeader) {
              HttpRequestHeader req_hdr = (HttpRequestHeader) header;
              HttpString method = req_hdr.method();
              if (method.equals(GET)) {
                // No body
                state = STATE_INIT;
                reset_start();
                if (logger.isDebugEnabled())
                  logger.debug("no body");
                if (!compq.enqueue_lossy(new HttpNoBody(connection))) {
                  if (logger.isDebugEnabled())
                    logger.debug("couldn't enqueue HttpNoBody " + "to compq.");
                }
                break;
              } else if (method.equals(POST)) {
                // Calculate the body size like a response.
                // (Do nothing here.)
              } else {
                throw new AssertionViolatedException("don't understand " + method + " method yet");
              }
            }

            // From now on, we know there should be a response
            // body, we just need to know how big it is.

            query = header.get_field(CONTENT_LENGTH);
            if (query != null) {

              // If there's a Content-Length, that tells us
              // the size.

              if (logger.isDebugEnabled())
                logger.debug("content length");
              if (query.num_values() != 1)
                die();
              try {
                // We should handle this with differnent
                // parser states, but I'm lazy.
                HttpString l = query.get_first_value();
                l.chomp_tail();
                chunk_size = l.parse_int();
              } catch (NumberFormatException e) {
                System.err.println("header=" + header);
                throw e;
              }
            } else {
              // Otherwise, we may have to wait for the
              // connection to be closed to know the size.

              chunk_size = Integer.MAX_VALUE;
            }
            state = STATE_BODY_LENGTH;

            // Unless there's a Transfer-Encoding:chunked.

            query = header.get_field(TRANSFER_ENCODING);
            if (query != null) {
              if ((query.num_values() == 1) && (query.get_first_value().equals(IDENTITY))) {
                // Ignore transfer encoding
              } else {
                // Chunked

                if (logger.isDebugEnabled())
                  logger.debug("chunked");
                state = STATE_BODY_CHUNKED;
                header.chunked = true;
              }
            }

            if ((state != STATE_BODY_CHUNKED) && (chunk_size == Integer.MAX_VALUE)) {
              if (logger.isDebugEnabled())
                logger.debug("done on close");
              header.close = true;
            }

            if (state != STATE_HEADER_FIELD_CR)
              break;

            throw new AssertionViolatedException("unreachable");

            // No body
            /*
			 * if (DEBUG) Debug.printtagln (TAG, "no body"); state =
			 * STATE_INIT; reset_start (); if (! compq.enqueue_lossy ( new
			 * HttpNoBody (connection))) { if (DEBUG) Debug.printtagln (TAG,
			 * "couldn't enqueue HttpNoBody " + "to compq."); }
			 */
          } else {
            // False alarm--only the CR of the CRLF pair.
            state = STATE_HEADER_FIELD;
          }
          break;

        case STATE_HEADER_FIELD_SPACE :
          if (b == SPACE) {
            // Okay, move on to the values.
            state = STATE_HEADER_VALUE;
            reset_start();
          } else {
            // do nothing
          }
          break;

        case STATE_HEADER_VALUE :
          if (b == COMMA) {
            // We've read the whole value, but there are more to
            // come.
            state = STATE_HEADER_VALUE_SPACE;
            HttpString v = finish_string();
            v.truncate(v.length - 1); // strip COMMA
            if (logger.isDebugEnabled())
              logger.debug("v=\"" + v + "\"");
            field.append_value(v);
          } else if (b == CR) {
            // may be done with values
            state = STATE_HEADER_VALUE_CR;
          }
          break;

        case STATE_HEADER_VALUE_SPACE :
          // Okay, move on to the next value.
          state = STATE_HEADER_VALUE;
          if (b == SPACE) {
            // If there was a space, don't include it in the
            // next value
            reset_start();
          } else {
            // Assuming all values are at least one character
            // in length.
            state = STATE_HEADER_VALUE;
          }
          break;

        case STATE_HEADER_VALUE_CR :
          if (b == LF) {
            // This is the last value for this field.
            state = STATE_HEADER_FIELD;
            HttpString v = finish_string();
            v.truncate(v.length - 2); // strip CRLF
            if (logger.isDebugEnabled())
              logger.debug("v=\"" + v + "\"");
            field.append_value(v);
          } else {
            // False alarm--only the CR of the CRLF pair.
            state = STATE_HEADER_VALUE;
          }
          break;

        case STATE_BODY_CHUNKED :
          // Read in a hexidecimal number representing the size
          // of the following chunk.

          // Is is a hex digit?
          if ((b >= ZERO) && (b <= NINE)) {
            // good, do nothing
          } else if ((b >= LOWER_A) && (b <= LOWER_F)) {
            // good, do nothing
          } else if ((b >= CAP_A) && (b <= CAP_F)) {
            // good, do nothing
          } else if (b == CR) {
            state = STATE_BODY_CHUNKED_CR;
            ws_cnt = 0;
          } else {
            // Not a hex digit
            if (length == 1)
              reset_start(); // just skip leading whitespace
            else if (b == SPACE) {
              state = STATE_BODY_CHUNKED_TRAILING_WS;
              ws_cnt = 1;
            } else
              throw new AssertionViolatedException("TODO");
          }

          break;

        case STATE_BODY_CHUNKED_TRAILING_WS :
          if (b == SPACE)
            ws_cnt += 1;
          else if (b == CR)
            state = STATE_BODY_CHUNKED_CR;
          else
            throw new AssertionViolatedException("TODO");
          break;

        case STATE_BODY_CHUNKED_CR :
          if (b == LF) {
            HttpString v = finish_string();
            v.truncate(v.length - 2 - ws_cnt); // strip CRLF
            chunk_size = v.parse_hex();
            if (chunk_size == 0) {
              // Done reading body.
              if (logger.isDebugEnabled())
                logger.debug("no more chunks");
              if (!compq.enqueue_lossy(new HttpBodyDone(connection))) {
                if (logger.isDebugEnabled())
                  logger.debug("couldn't enqueue body done to compq.");
              }
              state = STATE_INIT;
            } else {
              if (logger.isDebugEnabled())
                logger.debug("next chunk size = " + chunk_size);
              state = STATE_BODY_CHUNK_SIZE;
            }
          } else {
            // Only expecting hexidecimal digits.
            die();
          }
          break;

        case STATE_BODY_CHUNKED_TAIL :
          if (b != CR)
            throw new AssertionViolatedException("HttpParser: expecting CR tailing chunk, got " + Integer.toHexString(b));
          state = STATE_BODY_CHUNKED_TAIL_CR;
          break;

        case STATE_BODY_CHUNKED_TAIL_CR :
          if (b != LF)
            die();
          state = STATE_BODY_CHUNKED;
          reset_start();
          break;

        default :
          die();
      }
    }
  }

  public String toString() {
    return "(HttpParser state=" + state_to_string[state] + ")";
  }

  protected void die() {
    if (logger.isDebugEnabled())
      logger.debug("error");
    throw new AssertionViolatedException("error");
  }

  protected ATcpConnection connection;
  protected int state;
  protected int ws_cnt;
  protected SortedMap out_of_order_packets = new TreeMap();
  protected long next_seq_num = 1;
  protected InOrderPacket first_pkt, last_packet;
  protected int first_pkt_offset;
  protected HttpStreamElement start, end;
  protected int start_offset, length;

  protected HttpString fw, sw, tw;
  protected HttpHeader header;
  protected HttpHeaderField field;
  protected int body_length, chunk_size;

  protected SinkIF compq;
}
