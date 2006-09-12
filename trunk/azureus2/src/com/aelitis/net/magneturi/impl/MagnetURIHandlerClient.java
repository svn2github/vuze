/*
 * Created on 11-Sep-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.net.magneturi.impl;

import java.net.*;
import java.io.*;

public class 
MagnetURIHandlerClient 
{
	protected static final String	NL			= "\015\012";

	public boolean
	load(
		String	magnet_uri,
		int		max_millis_to_wait )
	{
			// limit the subset here as we're looping waiting for something to be alive and we can't afford to take ages getting back to the start
		
		long	start = System.currentTimeMillis();
		
		while( true ){
			
			for (int i=45100;i<=45108;i++){
	
				long	now = System.currentTimeMillis();
				
				if ( now < start ){
					
					start  = now;
				}
				
				if ( now - start > max_millis_to_wait ){
					
					return( false );
				}
				
				Socket	sock = null;
				
				try{
					sock = new Socket();
					
					System.out.println( "trying " + i );
					
					sock.connect( new InetSocketAddress( "127.0.0.1", i ), 500 );
					
					sock.setSoTimeout( 5000 );
					
					PrintWriter	pw = new PrintWriter( sock.getOutputStream());
					
					pw.println( "GET /download/" + magnet_uri + NL + NL );
					
					pw.flush();
					
					InputStream	is = sock.getInputStream();
					
					String	res = "";
					
					byte[]	buffer = new byte[1024];
	
					while( true ){
						
						int	len = is.read( buffer );
						
						if ( len <= 0 ){
							
							break;
						}
						
						res += new String( buffer, 0, len );
						
						if ( res.indexOf( " 200" ) != -1 ){
							
							return( true );
						}
					}
				}catch( Throwable e ){
					
				}finally{
					
					try{
						sock.close();
					}catch( Throwable e ){
					}
				}
			}
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		new MagnetURIHandlerClient().load( "jkjjk", 30000 );
	}
}
