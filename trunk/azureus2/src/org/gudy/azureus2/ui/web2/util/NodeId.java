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

package org.gudy.azureus2.ui.web2.util;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

/**
 * Abstract node identifier. Currently we use 32-bit IPv4 addressed.
 * 
 * @author Sean C. Rhea
 * @version $Id: NodeId.java,v 1.1 2003-12-01 00:58:01 belgabor Exp $
 */
public class NodeId implements Comparable, Cloneable {

  public static final Logger logger = Logger.getLogger("azureus2.ui.web.util.NodeId");

  public class BadFormat extends Exception {
    public BadFormat(String msg) {
      super(msg);
    }

    public String toString() {
      return "BadFormat: " + getMessage();
    }
  }

  /**
   * Construct a new NodeId.
   * 
   * @param port
   *                  The network port; must be between 0 and 65535 inclusive.
   * @param addr
   *                  The IP address
   */
  public NodeId(int port, InetAddress addr) {
    _addr = addr;
    _port = port;

  }

  /**
   * Read this NodeId in from a string of the same format as produced by
   * toString (), below.
   */
  public NodeId(String peer) throws BadFormat, UnknownHostException {
    int colon = peer.indexOf((int) ':');
    if (colon < 0)
      throw new BadFormat(peer);
    String ip_str = peer.substring(0, colon);
    String port_str = peer.substring(colon + 1, peer.length());
    _port = Integer.parseInt(port_str);
    // try to parse out the ip bytes first, then go with a resolve
    // operation if that doesn't work

    _ip_bytes = new byte[4];
    String munch = ip_str;
    int b = 0;
    for (; b < 4; ++b) {
      int dot = munch.indexOf((int) '.');
      if (dot == -1)
        break;
      String this_byte = munch.substring(0, dot);
      try {
        _ip_bytes[b] = Byte.parseByte(this_byte);
      } catch (NumberFormatException e) {
        break;
      }
      munch = munch.substring(dot + 1, munch.length());
    }

    if (b != 4) {
      _ip_bytes = null;
      _addr = InetAddress.getByName(ip_str);
    }

    if (logger.isDebugEnabled()) {
      if (_port > 10000) {
        logger.debug("NodeId.init: "+ _addr + " has a big port: " + _port);
      }
    }
  }

  public NodeId() {
  }

  public void serialize(OutputBuffer buffer) {
    if (_ip_bytes == null)
      _ip_bytes = _addr.getAddress();
    buffer.add((short) _port);
    buffer.add((byte) _ip_bytes.length);
    buffer.add(_ip_bytes);
  }

  public NodeId(InputBuffer buffer) throws QSException {
    _port = 0xFFFF & (int) buffer.nextShort();
    int length = (int) buffer.nextByte();
    if (length > 16) // big enough for IPv4 and IPv6
      throw new QSException("IP addr len = " + length);
    _ip_bytes = new byte[length];
    buffer.nextBytes(_ip_bytes, 0, length);
    _addr = null;
  }

  /**
   * Return the InetAddress associated with this NodeId; does not work under
   * the simulator.
   * 
   * <p>
   * <b>JUST TO MAKE THAT CLEAR, THIS FUNCTION WILL NOT WORK IN THE OCEANSTORE
   * SIMULATOR. YOU HAVE BEEN WARNED.</b>
   */
  public InetAddress address() {
    if (_addr == null) {
      String addr = "";
      for (int i = 0; i < _ip_bytes.length; ++i) {
        long value = ByteUtils.byteToUnsignedInt(_ip_bytes[i]);
        addr += value;
        if ((i + 1) != _ip_bytes.length)
          addr += ".";
      }
      try {
        _addr = InetAddress.getByName(addr);
      } catch (UnknownHostException e) {
        if (logger.isDebugEnabled())
          logger.debug("NodeId.address: Could not construct an InetAddress from " + addr + ":  Unknown host exception.");
        return null;
      }
    }
    return _addr;
  }

  public int port() {
    return _port;
  }

  public String toString() {
    // Do not change this format. It was chosen specifically to be
    // compatible with Steve Gribble's networking code.
    return address().getHostAddress() + ":" + _port;
  }

  /**
   * Specified by the <code>Comparable</code> interface.
   */
  public int compareTo(Object other) {

    if (equals(other)) {
      return 0;
    } else if (less_than((NodeId) other)) {
      return -1;
    } else {
      return 1;
    }

  }

  public boolean equals(Object other) {
    if (other == null)
      return false;
    NodeId rhs = (NodeId) other;
    if (_port != rhs._port)
      return false;
    return address().equals(rhs.address());
  }

  /**
   * <code>a.less_than (b)</code> returns true iff <code>a</code> is less
   * than <code>b</code>. Used for sorting <code>NodeId</code>s.
   */
  public boolean less_than(NodeId other) {
    if (_ip_bytes == null) {
      _ip_bytes = _addr.getAddress();
      if (_ip_bytes == null) {
        logger.fatal("_ip_bytes == null, _addr = " + _addr + ".  This usually happens because you're running " + "with Java 1.4 under SandStorm (NBIO, to be exact)." + "  Make sure you're running with Java 1.3, and it " + "should go away.");
      }
    }
    if (other._ip_bytes == null)
      other._ip_bytes = other._addr.getAddress();
    for (int i = 0; i < _ip_bytes.length; ++i) {
      if (_ip_bytes[i] < other._ip_bytes[i])
        return true;
      else if (_ip_bytes[i] > other._ip_bytes[i])
        return false;
    }
    // addresses are equal, compare ports
    return _port < other._port;
  }

  public int hashCode() {
    if (_ip_bytes == null) {
      _ip_bytes = _addr.getAddress();
      if (_ip_bytes == null) {
        logger.fatal("_ip_bytes == null, _addr = " + _addr + ".  This usually happens because you're running " + "with Java 1.4 under SandStorm (NBIO, to be exact)." + "  Make sure you're running with Java 1.3, and it " + "should go away.");
      }
    }
    int result = 0;
    for (int i = 0; i < _ip_bytes.length; ++i) {
      result <<= 8;
      result |= (_ip_bytes[i] >= 0) ? _ip_bytes[i] : (_ip_bytes[i] * -1);
    }
    result ^= _port;
    return result;
  }

  /**
   * Create an exact copy of this <CODE>NodeId</CODE>
   * 
   * @return new <CODE>NodeId</CODE> identical in value to this one
   * @exception CloneNotSupportedException
   *                       if clone() is not supported
   */
  public Object clone() throws CloneNotSupportedException {
    return new NodeId(port(), address());
  }

  private byte _ip_bytes[];
  private InetAddress _addr;
  private int _port;
}
