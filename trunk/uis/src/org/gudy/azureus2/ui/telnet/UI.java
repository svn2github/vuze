/*
 * Created on 23-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * this is a telnet UI that starts up a server socket that listens for new connections 
 * on a (configurable) port. when an incoming connection is recieved, we check the host 
 * against our list of allowed hosts and if this host is permitted, we start a new 
 * command line interface for that connection.
 * @author fatal
 */
public class UI extends org.gudy.azureus2.ui.common.UITemplateHeadless implements IUserInterface 
{
	private ServerSocket serverSocket;

	public String[] processArgs(String[] args) {
		return args;
	}
	/**
	 * start up a server socket thread on an appropriate port as obtained from the configuration manager.
	 */
	public void startUI() {
		super.startUI();
		if( ! isStarted() || serverSocket == null || serverSocket.isClosed() )
		{
			try {
				int telnetPort = COConfigurationManager.getIntParameter("Telnet_iPort", 57006);
				String allowedHostsStr = COConfigurationManager.getStringParameter("Telnet_allowedHosts", "127.0.0.1,titan");
				StringTokenizer st = new StringTokenizer(allowedHostsStr, ",");
				Set allowedHosts = new HashSet();
				while( st.hasMoreTokens() )
					allowedHosts.add(st.nextToken().toLowerCase());
				
				Thread thread = new Thread(new SocketServer(telnetPort, allowedHosts), "Telnet Socket Server Thread");
				thread.setDaemon(true);
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * shamelessly copied from the console ui. could this be extracted into a static utility method?
	 */
	public void openTorrent(String fileName) {
	   if( fileName.toUpperCase().startsWith( "HTTP://" ) ) {
	      System.out.println( "Downloading torrent from url: " + fileName );
	      TorrentDownloaderFactory.downloadManaged( fileName );
	      return;
	    }
	    
	    try {
	      if (!FileUtil.isTorrentFile(fileName)) {//$NON-NLS-1$
	        Logger.getLogger("azureus2.ui.telnet").error(fileName+" doesn't seem to be a torrent file. Not added.");
	        return;
	      }
	    } catch (Exception e) {
	      Logger.getLogger("azureus2.ui.telnet").error("Something is wrong with "+fileName+". Not added. (Reason: "+e.getMessage()+")");
	      return;
	    }
	    if (UIConst.getGlobalManager()!=null) {
	      try {
	        UIConst.getGlobalManager().addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("Default save path"));
	      } catch (Exception e) {
	        Logger.getLogger("azureus2.ui.telnet").error("The torrent "+fileName+" could not be added.", e);
	      }
	    }
	}
	/**
	 * creates a new console input using the specified input/output streams.
	 * we create the new input in non-controlling mode because we don't want the 'quit'
	 * command to shut down the whole interface - simply this clients connection.
	 * @param consoleName
	 * @param inputStream
	 * @param outputStream
	 */
	public static void createNewConsoleInput(String consoleName, InputStream inputStream, PrintStream outputStream)
	{
	     ConsoleInput console = new ConsoleInput(consoleName, UIConst.getAzureusCore(), inputStream, outputStream, Boolean.FALSE);
	     console.printconsolehelp();
	}
	
	private static final class SocketServer implements Runnable
	{
		private final ServerSocket serverSocket;
		private final Set allowedHosts;
		public SocketServer(int port, Set allowedHosts) throws IOException
		{
			this.allowedHosts = allowedHosts;
			serverSocket = new ServerSocket(port);
		}
		/**
		 * start up the server socket and when a new connection is received, check that
		 * the source address is in our permitted list and if so, start a new console input
		 * on that socket.
		 */
		public void run()
		{
			int threadNum = 1;
			System.out.println("Telnet server started. Listening on port: " + serverSocket.getLocalPort());
			while(true)
			{
				try {
					Socket socket = serverSocket.accept();
					InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
					if( addr.isUnresolved() || ! isAllowed(addr) )
					{
						System.out.println("rejecting connection: " + addr);
						socket.close();
					}
					else
					{
						System.out.println("accepting connection: " + addr);
						createNewConsoleInput("Telnet Console " + threadNum++, socket.getInputStream(), new PrintStream(socket.getOutputStream()));
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}
		/**
		 * check that the specified host/ip is allowed
		 * @param addr
		 * @return
		 */
		private boolean isAllowed(InetSocketAddress addr) {
			InetAddress address = addr.getAddress();
			if( checkHost(address.getHostAddress()) )
				return true;
			else if( checkHost(address.getHostName()))
				return true;
			else
				return false;
		}
		/**
		 * compare the specified host (might be a hostname or an IP - dont really care) 
		 * and see if it is a match against one of the allowed hosts
		 * @param hostName
		 * @return true if this hostname matches one in our allowed hosts
		 */
		private boolean checkHost(String hostName) {
			if( hostName == null )
				return false;
			hostName = hostName.toLowerCase();
//			System.out.println("checking host: " + hostName);
			for (Iterator iter = allowedHosts.iterator(); iter.hasNext();) {
				String allowedHost = (String) iter.next();
				if( hostName.equals(allowedHost) )
					return true;
			}
			return false;
		}
	}
}