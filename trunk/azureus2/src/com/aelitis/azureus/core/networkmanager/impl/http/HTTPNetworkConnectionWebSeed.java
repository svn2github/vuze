/*
 * Created on 4 Oct 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

public class 
HTTPNetworkConnectionWebSeed
	extends HTTPNetworkConnection
{
	protected
	HTTPNetworkConnectionWebSeed(
		HTTPNetworkManager		_manager,
		NetworkConnection		_connection,
		PEPeerTransport			_peer,
		String					_url )
	{
		super( _manager, _connection, _peer, _url );
	}
	
	protected void
	decodeHeader(
		String		header )
	
		throws IOException
	{
		if ( !isSeed()){
			
			return;
		}
		
		int	pos = header.indexOf( NL );
		
		String	line = header.substring(4,pos);
		
		pos = line.lastIndexOf( ' ' );
		
		String	url = line.substring( 0, pos ).trim();
		
		StringTokenizer	tok = new StringTokenizer( url, "&" );
		
		int		piece 	= -1;
		List	ranges 	= new ArrayList();
		
		while( tok.hasMoreElements()){
			
			String	token = tok.nextToken();
			
			pos = token.indexOf('=');
			
			if ( pos != -1 ){
				
				String	lhs = token.substring(0,pos).toLowerCase();
				String	rhs = token.substring(pos+1);
				
				if ( lhs.equals( "piece" )){
					
					try{
						piece = Integer.parseInt( rhs );
						
					}catch( Throwable e ){
						
						throw( new IOException( "Invalid piece number '" + rhs +"'" ));
					}
				}else if ( lhs.equals( "ranges" )){
					
					StringTokenizer	range_tok = new StringTokenizer( rhs, "," );
					
					while( range_tok.hasMoreTokens()){
						
						String	range = range_tok.nextToken();
						
						int	sep = range.indexOf( '-' );
						
						if ( sep == -1 ){
							
							throw( new IOException( "Invalid range specification '" + rhs + "'" ));
						}
						
						try{
							ranges.add( 
									new int[]{ 
										Integer.parseInt( range.substring(0,sep)), 
										Integer.parseInt( range.substring( sep+1 ))});
							
						}catch( Throwable e ){
							
							throw( new IOException( "Invalid range specification '" + rhs + "'" ));
						}
					}
				}
			}
		}
		
		if ( piece == -1 ){
			
			throw( new IOException( "Piece number not specified" ));
		}
		
		boolean	keep_alive = header.toLowerCase().indexOf( "keep-alive" ) != -1;
		
		PEPeerControl	control = getPeerControl();
		
		int	this_piece_size = control.getPieceLength( piece );
		
		if ( ranges.size() == 0 ){
			
			ranges.add( new int[]{ 0, this_piece_size-1});
		}
				
		long[]	offsets	= new long[ranges.size()];
		long[]	lengths	= new long[ranges.size()];
		
		long	piece_offset = piece * control.getPieceLength(0);
		
		for (int i=0;i<ranges.size();i++){
			
			int[]	range = (int[])ranges.get(i);
			
			int	start 	= range[0];
			int end		= range[1];
			
			if ( 	start < 0 || start >= this_piece_size ||
					end < 0 || end >= this_piece_size ||
					start > end ){
				
				throw( new IOException( "Invalid range specification '" + start + "-" + end + "'" ));
			}
			
			offsets[i] 	= piece_offset + start;
			lengths[i]	= ( end - start ) + 1; 
		}
		
		addRequest( new httpRequest( offsets, lengths, false, keep_alive ));
	}
}
