/*  TimedInput.java -- Timer function that allows other classes to 'time out'
 
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

import java.util.*;

public class TimeOutHandler extends TimerTask {
  private Thread threadID;
  private MainServer server;
  
  TimeOutHandler(Thread t_threadID, MainServer _server) {
    this.threadID = t_threadID;
    this.server = _server;
  }
  
  public void run() {
    this.server.putSysMessage(SLevel.THREAD, "TIMER MESSAGE --> Sending Interrupt() to " + this.threadID.getName() + "...");
    this.threadID.interrupt();
  }
}