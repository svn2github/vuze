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

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.http.util.HttpConstants;
import org.gudy.azureus2.ui.web2.http.util.HttpOutputBuffer;

import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.core.BufferElement;

/**
 * This is an abstract class corresponding to an HTTP response.
 * Use one of the subclasses (such as httpOKResponse or httpNotFoundResponse)
 * to push responses back to the client.
 * 
 * @author Matt Welsh
 * @see httpOKResponse
 * @see httpNotFoundResponse
 */
public abstract class httpResponse implements HttpConstants, QueueElementIF {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.http.httpResponse");

  static {
    //logger.setLevel(org.apache.log4j.Level.DEBUG);
  }

  /** Code corresponding to '200 OK'. */
  public static final int RESPONSE_OK = 200;
  /** Code corresponding to '301 Moved Permanently'. */
  public static final int RESPONSE_REDIRECT = 301;
  /** Code corresponding to '400 Bad Request'. */
  public static final int RESPONSE_BAD_REQUEST = 400;
  /** Code corresponding to '404 Not Found'. */
  public static final int RESPONSE_NOT_FOUND = 404;
  /** Code corresponding to '500 Internal Server Error'. */
  public static final int RESPONSE_INTERNAL_SERVER_ERROR = 500;
  /** Code corresponding to '503 Service Unavailable'. */
  public static final int RESPONSE_SERVICE_UNAVAILABLE = 503;

  /** The default MIME type for responses, which is "text/html". */
  public static final String DEFAULT_MIME_TYPE = "text/html";

  /** The code corresponding to the response. */
  protected int code;
  /** The default response header. */
  protected static String defaultHeader = "Server: Sandstorm (unknown version)" + CRLF;

  /** The actual data of the response. */
  protected BufferElement combinedData;
  /** The header for the response. */
  protected BufferElement header;
  /** The payload for the response. */
  protected BufferElement payload;
  /** The MIME type of the response. */
  protected String contentType;
  /** The content-length header. */
  protected int contentLength;

  /**
   * Create an httpResponse with the given response code with the given
   * payload. 
   *
   * @param code The response code; should be one of the constants
   *  from httpResponse.RESPONSE_*.
   * @param contentType The MIME type of the response content. Should
   *  not be CRLF-terminated.
   * @param payload The payload of the response.
   */
  protected httpResponse(int code, String contentType, BufferElement payload) {
    this.code = code;
    this.contentType = contentType;
    this.contentLength = payload.size;

    this.combinedData = null;
    String hdrString = genHeader();
    byte hdr[] = hdrString.getBytes();
    this.header = new BufferElement(hdr);
    this.payload = payload;
  }

  /**
   * Create an httpResponse with the given response code with the given
   * payload. 
   *
   * @param code The response code; should be one of the constants
   *  from httpResponse.RESPONSE_*.
   * @param contentType The MIME type of the response content. Should
   *  not be CRLF-terminated.
   * @param payload The payload of the response.
   * @param contentLength The contentLength to place in the header.
   */
  protected httpResponse(int code, String contentType, BufferElement payload, int contentLength) {
    this.code = code;
    this.contentType = contentType;
    this.contentLength = contentLength;

    this.combinedData = null;
    String hdrString = genHeader();
    byte hdr[] = hdrString.getBytes();
    this.header = new BufferElement(hdr);
    this.payload = payload;
  }

  /**
   * Create an httpResponse with the given response code with no payload.
   * A payload can be assigned later using setPayload().
   *
   * @param code The response code; should be one of the constants
   *  from httpResponse.RESPONSE_*.
   * @param contentType The MIME type of the response content. Should
   *  not be CRLF-terminated.
   * @param payload The payload of the response.
   */
  protected httpResponse(int code, String contentType) {
    this.code = code;
    this.contentType = contentType;
    this.contentLength = 0; // Don't know it yet

    this.combinedData = null;
    this.header = null;
    this.payload = null;
  }

  /**
   * Create an httpResponse with the the given response code, with an
   * empty payload of the given size. This can be more efficient than
   * providing a payload separately, as the entire contents of the 
   * httpResponse can be sent as a single TCP packet. The payload can
   * be filled in using the getPayload() method.
   *
   * @param code The response code; should be one of the constants
   *  from httpResponse.RESPONSE_*.
   * @param contentType The MIME type of the response content. Should
   *  not be CRLF-terminated.
   * @param payloadSize The size of the payload to allocate.
   * @param compQ The completion queue for the payload.
   */
  protected httpResponse(int code, String contentType, int payloadSize, SinkIF compQ) {
    this.code = code;
    this.contentType = contentType;
    this.contentLength = payloadSize;

    String hdrString = genHeader();
    byte hdr[] = hdrString.getBytes();
    this.combinedData = new BufferElement(hdr.length + payloadSize);
    combinedData.compQ = compQ;
    this.header = new BufferElement(combinedData.data, 0, hdr.length);
    System.arraycopy(hdr, 0, header.data, 0, hdr.length);
    this.payload = new BufferElement(combinedData.data, hdr.length, payloadSize);
  }

  /**
   * Create an httpResponse with the the given response code, with an
   * empty payload of the given size. This can be more efficient than
   * providing a payload separately, as the entire contents of the 
   * httpResponse can be sent as a single TCP packet. The payload can
   * be filled in using the getPayload() method.
   *
   * @param code The response code; should be one of the constants
   *  from httpResponse.RESPONSE_*.
   * @param contentType The MIME type of the response content. Should
   *  not be CRLF-terminated.
   * @param payloadSize The size of the payload to allocate.
   */
  protected httpResponse(int code, String contentType, int payloadSize) {
    this(code, contentType, payloadSize, null);
  }

  /** 
   * Return the entity header as a String. Must be implemented by 
   * subclasses of httpResponse.
   */
  protected abstract String getEntityHeader();

  /**
   * Used to set the payload after creating the response with an 
   * empty payload. XXX Should not be used if the payload was allocated 
   * by this response (that is, if the payloadSize was specified in the 
   * constructor). 
   */
  public void setPayload(BufferElement payload) {
    this.payload = payload;
    this.contentLength = payload.size;
  }

  /**
   * Returns the header for this response.
   */
  public BufferElement getHeader() {
    if (this.header == null) {
      String hdrString = genHeader();
      byte hdr[] = hdrString.getBytes();
      this.header = new BufferElement(hdr);
    }
    return this.header;
  }

  /**
   * Returns the payload for this response.
   */
  public BufferElement getPayload() {
    return payload;
  }

  /**
   * Set the default header string sent in all responses.
   */
  public static void setDefaultHeader(String defhdr) {
    defaultHeader = defhdr;
  }

  /**
   * Return the default header string sent in all responses.
   */
  public static String getDefaultHeader() {
    return defaultHeader;
  }

  /**
   * Generate the header.
   */
  private String genHeader() {
    String hdrString;
    switch (code) {
      case RESPONSE_OK :
        hdrString = HTTP_VERSION + " 200 OK\n";
        break;
      case RESPONSE_REDIRECT :
        hdrString = HTTP_VERSION + " 301 MOVED PERMANENTLY\n";
        break;
      case RESPONSE_BAD_REQUEST :
        hdrString = HTTP_VERSION + " 400 BAD REQUEST\n";
        break;
      case RESPONSE_NOT_FOUND :
        hdrString = HTTP_VERSION + " 404 NOT FOUND\n";
        break;
      case RESPONSE_INTERNAL_SERVER_ERROR :
        hdrString = HTTP_VERSION + " 500 INTERNAL SERVER ERROR\n";
        break;
      case RESPONSE_SERVICE_UNAVAILABLE :
        hdrString = HTTP_VERSION + " 503 SERVICE UNAVAILABLE\n";
        break;
      default :
        throw new Error("Bad code in httpResponse: " + code);
    }
    if (defaultHeader != null)
      hdrString += defaultHeader;
    if (contentType != null) {
      hdrString += "Content-Type: " + contentType + CRLF;
    }
    if (contentLength != 0) {
      hdrString += "Content-Length: " + contentLength + CRLF;
    }
    String ehdr = getEntityHeader();
    if (ehdr != null) {
      hdrString += ehdr;
    }
    hdrString += CRLF;
    return hdrString;
  }

  /**
   * Get an array of BufferElements corresponding to this response.
   * Used internally when sending the response to a client.
   *
   * @param sendHeader Indicate whether the header should be included.
   */
  public BufferElement[] getBuffers(boolean sendHeader) {
    if (logger.isDebugEnabled())
      logger.debug("httpResponse: getBuffers() called");

    BufferElement bufarr[] = null;
    if (combinedData != null) {
      if (sendHeader) {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Returning combinedData (len=" + combinedData.size + ")");
        bufarr = new BufferElement[1];
        bufarr[0] = combinedData;
      } else {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Returning combinedData payload only (len=" + payload.size + ")");
        bufarr = new BufferElement[1];
        bufarr[0] = payload;
      }
    } else if (sendHeader) {
      if (payload != null) {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Returning header and payload (paylen=" + payload.size + ")");
        bufarr = new BufferElement[2];
        bufarr[0] = getHeader();
        bufarr[1] = getPayload();
      } else {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Returning header only (len=" + header.size + ")");
        bufarr = new BufferElement[1];
        bufarr[0] = getHeader();
      }
    } else {
      // Don't send header
      if (payload != null) {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Returning payload only (paylen=" + payload.size + ")");
        bufarr = new BufferElement[1];
        bufarr[0] = payload;
      } else {
        if (logger.isDebugEnabled())
          logger.debug("httpResponse: Nothing to return!");
        bufarr = null;
      }
    }

    return bufarr;
  }

  /**
   * @param buffer
   */
  public void enqueue(HttpOutputBuffer buffer) {
    try {
      if (logger.isDebugEnabled())
        logger.debug("Enqueueing Header");
      buffer.enqueue(getHeader(), 0, getHeader().size);
    } catch (SinkException e) {
      if (logger.isDebugEnabled())
        logger.debug("Enqueueing Header failed!");
    }
    try {
      if (logger.isDebugEnabled())
        logger.debug("Enqueueing Body");
      buffer.enqueue(getPayload(), 0, getPayload().size);
    } catch (SinkException e) {
      if (logger.isDebugEnabled())
        logger.debug("Enqueueing Body failed!");
    }
    try {
      if (logger.isDebugEnabled())
        logger.debug("Flushing enqueue buffer");
      buffer.flush();
    } catch (SinkException e) {
      if (logger.isDebugEnabled())
        logger.debug("Flushing enqueue buffer failed!");
    }
  }

}
