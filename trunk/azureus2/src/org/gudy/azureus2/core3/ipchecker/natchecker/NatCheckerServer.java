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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class NatCheckerServer extends AEThread{
    
    private String check;
    private ServerSocket server;
    private boolean valid = false;
    private boolean isAlreadyListening = false;
    
    private boolean bContinue = true;
    
    public NatCheckerServer(int _port, String _check) {     
      super("Nat Checker Server");
      try {
        check = _check;
        
        String bind_ip 	= COConfigurationManager.getStringParameter("Bind IP", "");

        if ( bind_ip.length() < 7 ){
        	
        	server = new ServerSocket(_port);
        	
        }else{
        	
        	server = new ServerSocket( _port, 8, InetAddress.getByName(bind_ip));
        }
        
        valid = true;
      
      } catch (IOException ioe) {
      	int confPort = COConfigurationManager.getIntParameter("TCP.Listen.Port", 6881);
      	if (_port == confPort) {
            //test port and currently-configured listening port
            //are the same, so testing not possible
            valid = false;
            isAlreadyListening = true;
      	}
        
      } catch(Exception e) {
        //Do nothing;
      }      
    }
    
    public void runSupport() {
      while(bContinue) {
        try {
          if (isAlreadyListening) {
            //already open, just NOOP loop sleep
            Thread.sleep(20);
          }
          else {
            //listen for accept
          	Socket sck = server.accept();
          	sck.getOutputStream().write(check.getBytes());
          }
        } catch(Exception e) {
          bContinue = false;
        }
      }
    }
    
    public boolean isValid() {
      return this.valid;
    }
    
    public boolean isAlreadyListening() { return this.isAlreadyListening; }
    
    public void stopIt() {
      try {
      bContinue = false;
      if(server != null && server.isBound())
        server.close();
      } catch(Exception e) {
      }
    }
  }
