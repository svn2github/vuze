/*
 * Created on 16-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.extseed.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;

public class 
ExternalSeedHTTPDownloader 
{
	private URL			url;
	
	public
	ExternalSeedHTTPDownloader(
		URL		_url )
	{
		url		= _url;
	}
	
	public byte[]
	download(
		long		offset,
		int			length )
	
		throws ExternalSeedException
	{
		try{
			HttpURLConnection	connection = (HttpURLConnection)url.openConnection();
			
			connection.setRequestProperty( "Connection", "Keep-Alive" );
			connection.setRequestProperty( "User-Agent", "Fred" );
			connection.setRequestProperty( "Range", "bytes=" + offset + "-" + (offset+length-1) );
			
			int	response = connection.getResponseCode();
			
			if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
					response == HttpURLConnection.HTTP_OK ||
					response == HttpURLConnection.HTTP_PARTIAL ){
				
				byte[]	data = new byte[length];
				
				InputStream	is = connection.getInputStream();
				
				try{
					int	pos = 0;
					
					while( pos < length ){
						
						int	len = is.read( data, pos, length-pos );
						
						if ( len < 0 ){
							
							break;
						}
						
						pos	+= len;
					}
					
					if ( pos != length ){
						
						throw( new ExternalSeedException("Connection failed: data too short" ));
					}
				}finally{
					
					is.close();
				}
				
				return( data );
			}else{
				
				ExternalSeedException	error = new ExternalSeedException("Connection failed: " + connection.getResponseMessage());
				
				error.setPermanentFailure( true );
				
				throw( error );
			}
		}catch( Throwable e ){
			
			throw( new ExternalSeedException("Connection failed", e ));
		}
	}
}
