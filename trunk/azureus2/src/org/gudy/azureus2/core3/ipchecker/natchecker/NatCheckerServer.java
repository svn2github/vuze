/*
 * File    : NatCheckerServer.java
 * Created : 12 oct. 2003 19:05:09
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.ipchecker.natchecker;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Olivier
 * 
 */
public class NatCheckerServer extends Thread{
    
    int port;
    String check;
    ServerSocket server;
    boolean valid;
    
    boolean bContinue;
    
    public NatCheckerServer(int port,String check) {     
      super("Nat Checker Server");
      try {
        this.port = port;
        this.check = check;
        server = new ServerSocket(port);
        bContinue = true;
        valid = true;
      } catch(Exception e) {
        //Do nothing;
      }      
    }
    
    public void run() {
      while(bContinue) {
        try {
          Socket sck = server.accept();
          sck.getOutputStream().write(check.getBytes());
        } catch(Exception e) {
          bContinue = false;
        }
      }
    }
    
    public boolean isValid() {
      return this.valid;
    }
    
    public void stopIt() {
      try {
      bContinue = false;
      if(server != null && server.isBound())
        server.close();
      } catch(Exception e) {
      }
    }
  }
