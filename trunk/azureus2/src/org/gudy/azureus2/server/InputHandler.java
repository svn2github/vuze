/*  InputHandler.java -- all functions related to input
 
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

import java.net.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core.ConfigurationManager;

public class InputHandler {
  private Socket sock;
  private BufferedReader in;
  private MainServer server;
  
  public InputHandler(Socket i_sock, MainServer _server) {
    try {
      this.sock = i_sock;
      this.server = _server;
      this.in = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING, "IO Exception caught: " + e);
    }
  }
  
  public String charReadLine() {
    /* reads 1 line */
    
        /* we need to put a timeout on input from the users
           because otherwise we could be waiting here a VERY long time :-( */
    Timer timeOutTimer = new Timer(true);    /* true to make the timer daemonstyle */
    TimeOutHandler timeOut = new TimeOutHandler(Thread.currentThread(), this.server);
    timeOutTimer.schedule(timeOut, ConfigurationManager.getInstance().getIntParameter("Server_iTimeout") * 1000);
    
    this.server.putSysMessage(SLevel.INFO, "Starting Timer... on " + Thread.currentThread().getName() + "...");
    
    String temp = null;
    int character = 0;
    try {
      /* read until we get a '\n' */
      do {
        if (this.in.ready()) {
          character = this.in.read();
          temp += (char) character;
        }
      }
      while ((character != -1) && (character != '\n') && (character != '\n') && (Thread.currentThread().isInterrupted() == false));
      
      if (Thread.currentThread().isInterrupted() == true) {
            /* check if this thread has been interrupted, if so we go back with a
            ThreadInterruptedException() */
        this.server.putSysMessage(SLevel.WARNING,"Thread interrupted...");
        temp = "";
      }
      
      /* manipulate the string a bit */
      if (temp.equals("") == false) {
        temp = temp.substring(0, temp.length() - 1); /* remove the final '\n' or '-1' */
        if (temp.charAt(temp.length() - 1) == '\r') temp = temp.substring(0, temp.length() - 1);
        temp = temp.trim();
      }
      
      if (temp.equals("")) temp = null;
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING,"InputHandler: " + e);
    }
    finally {
      /* stop the timer, we got our stuff */
      this.server.putSysMessage(SLevel.INFO, "Stopping Timer... on " + Thread.currentThread().getName() + "...");
      timeOutTimer.cancel();
    }
    
    
    this.server.putSysMessage(SLevel.INFO, "Temp String: " + temp);
    return temp;
  }
  
  public String readLine() {
    String temp;
    
    try {
      temp = this.in.readLine();
      if (temp != null) temp = temp.trim();
    }
    catch (IOException e) {
      //this.server.putSysMessage(1, "InputHandler: " + e);
      temp = null;
    }
    
    //this.server.putSysMessage(0, "Temp String: " + temp);
    return temp;
    
  }
  
  public int read(char[] cbuf, int off, int len) {
    int temp;
    
    try {
      temp = this.in.read(cbuf, off, len);
    }
    catch (IOException e) {
      //this.server.putSysMessage(1, "InputHandler: " + e);
      temp = 0;
    }
    
    //this.server.putSysMessage(0, "Temp String: " + temp);
    return temp;
    
  }
  
  public void close() {
    /* closes the input stream */
    try {
      this.in.close();
    }
    catch (IOException e) {
      this.server.putSysMessage(SLevel.WARNING,"InputHandler: " + e);
    }
  }
}