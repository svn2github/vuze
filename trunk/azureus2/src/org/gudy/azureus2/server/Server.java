/*  Server.java - thread class that is started when a new connection is initiated from a client
 
    Copyright (C) 2000 - 2001 Jan De Luyck & Kris Van Hulle
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.server;

import java.io.*;
import java.net.*;

import org.gudy.azureus2.core.GlobalManager;

public class Server extends Thread {
  private Socket sock;
  private InetAddress remoteIP;
  
  private InputHandler input;
  private OutputHandler output;
  
  private MainServer server;
  private GlobalManager gm;
  
  Server(Socket socketname, InetAddress socket_remoteIP, GlobalManager _gm, MainServer _server) {
    this.sock = socketname;
    this.remoteIP = socket_remoteIP;
    this.server = _server;
    this.gm = _gm;
    
    this.input = new InputHandler(this.sock, _server);
    this.output = new OutputHandler(this.sock, _gm, _server);
  }
  
  public void run() {
    this.server.putSysMessage(SLevel.THREAD, "New Thread Of WebServer Started, " + Thread.currentThread().getName());
    
    this.server.setAmountOfThreads(this.server.amountOfThreads() + 1);
    
    Connection conn = null;
    
    try {
      conn = new Connection(input, output, remoteIP, this.gm, this.server);
      do {
        this.server.putSysMessage(SLevel.INFO,"Processing a request...");
        conn.processRequest();
        conn.doMagicStuff();
      }
      while (false /*conn.connectionType().startsWith("keep-alive")*/);
      
    }
    catch (UnknownProtocolException e) {
      this.server.putSysMessage(SLevel.WARNING, "Started Connection: UnknownProtocol Error: " + e);
    }
    catch (NoDataReceivedException e) {
      this.server.putSysMessage(SLevel.WARNING, "Started Connection: NoDataReceived: " + e);
    }
    catch (ObsoleteHTTPBrowserException e) {
      this.server.putSysMessage(SLevel.WARNING, "Obsolete Browser Detected! Ignoring Totally: " + e);
    }
    catch (RFCViolationException e) {
      this.server.putSysMessage(SLevel.WARNING, "RFC Violation: " + e);
    }
    finally {
      if (conn != null) {
        try {
          input.close();
          output.close();
          sock.close();
        }
        catch (IOException e) {
          this.server.putSysMessage(SLevel.WARNING, "Closing Connection: IOException: " + e);
        }
      }
    }
    this.server.setAmountOfThreads(this.server.amountOfThreads() - 1);
    this.server.putSysMessage(SLevel.THREAD,"End of thread " + Thread.currentThread().getName());
  }
}