/*
 * Created on 28.11.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.web2.http.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import seda.sandStorm.api.BadQueueElementException;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkClosedException;
import seda.sandStorm.api.SinkException;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.core.BufferElement;
import seda.sandStorm.lib.aSocket.ATcpClientSocket;
import seda.sandStorm.lib.aSocket.ATcpConnection;
import seda.sandStorm.lib.aSocket.ATcpServerSocket;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class httpRequestFakeTcpConnection extends ATcpConnection {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.http.httpRequestFakeTcpConnection");

  private static int Number = 0;
  private int number = 0;

  private SinkIF requestSink;
  private SinkIF completedSink;
  private Vector queue = new Vector();
  private boolean closed = false;

  /**
   * @param arg0
   * @param arg1
   * @param arg2
   */
  public httpRequestFakeTcpConnection(ATcpClientSocket arg0, InetAddress arg1, int arg2) {
    logger.error("Wrong Constructor called");
  }

  /**
   * @param arg0
   * @param arg1
   * @param arg2
   */
  public httpRequestFakeTcpConnection(ATcpServerSocket arg0, InetAddress arg1, int arg2) {
    logger.error("Wrong Constructor called");
  }

  /**
   * 
   */
  public httpRequestFakeTcpConnection(SinkIF requestSink, SinkIF completedSink) {
    Number++;
    this.number = Number;
    this.requestSink = requestSink;
    this.completedSink = completedSink;
    if (logger.isDebugEnabled())
      logger.debug("Constructor called: " + number);
  }

  public void send() {
		if (logger.isDebugEnabled())
			logger.debug("Send called");
    ByteArrayOutputStream acc = new ByteArrayOutputStream();
    Iterator walkit = queue.iterator();
    while (walkit.hasNext())
      try {
      	acc.write(((BufferElement) walkit.next()).getBytes());
      } catch (IOException e) {
        logger.error("IOException while sending", e);
      }
    new httpRequestParser(requestSink, completedSink, userTag, acc.toByteArray()).send();
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#close(seda.sandStorm.api.SinkIF)
   */
  public void close(SinkIF arg0) throws SinkClosedException {
    closed = true;
    if (logger.isDebugEnabled())
      logger.debug("Close called: " + arg0);
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.SinkIF#enqueue_lossy(seda.sandStorm.api.QueueElementIF)
   */
  public boolean enqueue_lossy(QueueElementIF arg0) {
    queue.add(arg0);
    if (logger.isDebugEnabled())
      logger.debug("Enqueue_lossy called: " + arg0);
    return true;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.SinkIF#enqueue_many(seda.sandStorm.api.QueueElementIF[])
   */
  public void enqueue_many(QueueElementIF bufarr[]) throws SinkException {
    if (isClosed())
      throw new SinkClosedException("httpRequestFakeTcpConnection closed");
    for (int i = 0; i < bufarr.length; i++) {
      if (bufarr[i] == null)
        throw new BadQueueElementException("httpRequestFakeTcpConnection.enqueue_many got null element", bufarr[i]);
      queue.add(bufarr[i]);
    }
    if (logger.isDebugEnabled())
      logger.debug("Enqueue_many called: " + bufarr);
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.SinkIF#enqueue(seda.sandStorm.api.QueueElementIF)
   */
  public void enqueue(QueueElementIF arg0) throws SinkException {
    queue.add(arg0);
    if (logger.isDebugEnabled())
      logger.debug("Enqueue called: " + arg0);
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#flush(seda.sandStorm.api.SinkIF)
   */
  public void flush(SinkIF arg0) throws SinkClosedException {
    if (logger.isDebugEnabled())
      logger.debug("Flush called: " + arg0);
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getAddress()
   */
  public InetAddress getAddress() {
    if (logger.isDebugEnabled())
      logger.debug("getAddress called");
    return null;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getClientSocket()
   */
  public ATcpClientSocket getClientSocket() {
    if (logger.isDebugEnabled())
      logger.debug("getAddress called");
    return null;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getPort()
   */
  public int getPort() {
    if (logger.isDebugEnabled())
      logger.debug("getPort called");
    return 0;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getSequenceNumber()
   */
  public long getSequenceNumber() {
    if (logger.isDebugEnabled())
      logger.debug("getSequenceNumber called");
    return 0;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getServerSocket()
   */
  public ATcpServerSocket getServerSocket() {
    if (logger.isDebugEnabled())
      logger.debug("getServerSocket called");
    return null;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#getSocket()
   */
  public Socket getSocket() {
    if (logger.isDebugEnabled())
      logger.debug("getSocket called");
    return null;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#isClosed()
   */
  public boolean isClosed() {
    if (logger.isDebugEnabled())
      logger.debug("isClosed called");
    return closed;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.ProfilableIF#profileSize()
   */
  public int profileSize() {
    if (logger.isDebugEnabled())
      logger.debug("profileSize called");
    return size();
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.api.SinkIF#size()
   */
  public int size() {
    if (logger.isDebugEnabled())
      logger.debug("Size called");
    return 0;
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#startReader(seda.sandStorm.api.SinkIF, int)
   */
  public void startReader(SinkIF arg0, int arg1) {
    if (logger.isDebugEnabled())
      logger.debug("startReader called: " + arg0 + " [" + arg1 + "]");
  }

  /* (non-Javadoc)
   * @see seda.sandStorm.lib.aSocket.ATcpConnection#startReader(seda.sandStorm.api.SinkIF)
   */
  public void startReader(SinkIF arg0) {
    if (logger.isDebugEnabled())
      logger.debug("startReader called: " + arg0);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "httpRequestFakeTcpConnection[" + number + "]";
  }

}
