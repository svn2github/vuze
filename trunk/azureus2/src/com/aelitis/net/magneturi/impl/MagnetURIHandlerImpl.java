/*
 * Created on 03-Mar-2005
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

package com.aelitis.net.magneturi.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.magneturi.MagnetURIHandlerListener;

/**
 * @author parg
 *
 */

public class 
MagnetURIHandlerImpl 
{
		// see http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
	
	private static MagnetURIHandlerImpl		singleton;
	
	public static synchronized void
	addListener(
		MagnetURIHandlerListener	l )
	{
		if ( singleton == null ){
			
			singleton	= new MagnetURIHandlerImpl();
		}
		
		singleton.add( l );
	}
	
	private List	listeners	= new ArrayList();
	
	protected
	MagnetURIHandlerImpl()
	{
		ServerSocket	socket	= null;
		
		for (int i=45100;i<=45199;i++){
			
			try{
				
			   socket = new ServerSocket(i, 50, InetAddress.getByName("127.0.0.1"));

			   break;
			   
			}catch( Throwable e ){
				
			}
		}
		
		if ( socket == null ){
			
			// no free sockets, not much we can do
			
			LGLogger.log( "MagnetURI: no free sockets, giving up");
			
		}else{
			
			LGLogger.log( "MagnetURI: bound on " + socket.getLocalPort());
			
			final ServerSocket	f_socket = socket;
			
			Thread t = 
				new AEThread("MagnetURIHandler")
				{
					public void
					runSupport()
					{
						int	errors 	= 0;
						int	ok		= 0;
						
						while(true){
							
							try{
						
								Socket sck = f_socket.accept();
								
								ok++;
								
								errors	= 0;
								
						        String address = sck.getInetAddress().getHostAddress();
						        
						        if ( address.equals("localhost") || address.equals("127.0.0.1")) {
						        	
						        	BufferedReader br = new BufferedReader(new InputStreamReader(sck.getInputStream(),Constants.DEFAULT_ENCODING));
						        	
						        	String line = br.readLine();
						        	
						        	System.out.println( line );
						        	
						        }else{
						        	
						        	LGLogger.log("MagentURIHandler: connect from invalid address '" + address + "'" );
						        	
						        }
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
								errors++;
								
								if ( errors > 100 ){
									
									LGLogger.log("MagentURIHandler: bailing out, too many socket errors" );
								}
							}
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
		}
	}
	
	
	
	protected void
	add(
		MagnetURIHandlerListener	l )
	{
		listeners.add( l );
	}
}
