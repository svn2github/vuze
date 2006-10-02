/*
 * Created on 2 Oct 2006
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
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;

import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;

public class 
HTTPMessageDecoder 
	implements MessageStreamDecoder
{
	private static final int 	MAX_HEADER	= 1024;
	private static final String	NL			= "\r\n";

	private TOTorrent				torrent;
	
	private volatile boolean		paused;
	private volatile boolean		destroyed;
	
	private StringBuffer	header = new StringBuffer();
	
	private List			messages = new ArrayList();
	
	private int				protocol_bytes_read;
	
	public void
	setTorrent(
		TOTorrent		_torrent )
	{
		torrent	= _torrent;
	}
	
	public int
	performStreamDecode( 
		Transport 	transport, 
		int 		max_bytes ) 
	
		throws IOException
	{
		protocol_bytes_read	= 0;
		
		int	rem = max_bytes;
		
		byte[]	bytes = new byte[1];
		
		ByteBuffer		bb	= ByteBuffer.wrap( bytes );
		
		ByteBuffer[]	bbs = { bb };
		
		while( rem > 0 && !paused ){
			
			if ( transport.read( bbs,0, 1 ) == 0 ){
				
				break;
			}
			
			rem--;
			
			protocol_bytes_read++;
			
			bb.flip();
			
			char	c = (char)(bytes[0]&0xff);
			
			header.append( c );
			
			if ( header.length() > MAX_HEADER ){
				
				throw( new IOException( "HTTP header exceeded maximum of " + MAX_HEADER ));
			}
			
			if ( c == '\n' ){
				
				String	header_str = header.toString();
				
				if ( header_str.endsWith( NL + NL )){
					
					receiveHeader( header_str );
				
					header.setLength(0);
				}
			}
		}
		
		return( max_bytes - rem );
	}
	  
	protected void
	receiveHeader(
		String		header )
	
		throws IOException
	{
		System.out.println( "got header: " + header );
		
		int	pos = header.indexOf( NL );
		
		String	line = header.substring(4,pos);
		
		pos = line.lastIndexOf( ' ' );
		
		String	url = line.substring( 0, pos ).trim();
		
		StringTokenizer	tok = new StringTokenizer( url, "&" );
		
		int	piece = -1;
		
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
				}
			}
		}
		
		if ( piece == -1 ){
			
			throw( new IOException( "Piece number not specified" ));
		}
		
		long	total_size	= torrent.getSize();
		long	piece_size 	= torrent.getPieceLength();
		int		pieces 		= torrent.getNumberOfPieces();
		
		int	this_piece_size;
		
		if ( piece == pieces - 1 ){
			
			this_piece_size = (int)(total_size - ((long)(pieces - 1) * (long)piece_size));
			
		}else{
			
			this_piece_size = (int)piece_size;
		}
		
		messages.add( new BTRequest( piece, 0, this_piece_size ));
	}
	
	public Message[] 
	removeDecodedMessages()
	{
		if ( messages.isEmpty()){
			
			return null;
		}
		    
		Message[] msgs = (Message[])messages.toArray( new Message[messages.size()] );
		
		messages.clear();
		    
		return( msgs );
	}
	  
	public int 
	getProtocolBytesDecoded()
	{
		return( protocol_bytes_read );
	}
	  
	public int 
	getDataBytesDecoded()
	{
		return( 0 );
	}
	  
	public int 
	getPercentDoneOfCurrentMessage()
	{
		return( 0 );
	}
	  
	public void 
	pauseDecoding()
	{
		paused	= true;
	}
	
	public void 
	resumeDecoding()
	{
		paused	= false;
	}
	  
	public ByteBuffer 
	destroy()
	{
		paused		= true;
		destroyed	= true;
		
	    try{

	    	for( int i=0; i<messages.size(); i++ ){
	    		
	    		Message msg = (Message)messages.get( i );
	    		
			    msg.destroy();
			}
		}catch( ArrayIndexOutOfBoundsException e ){
		    	// as access to messages_last_read isn't synchronized we can get this error if we destroy the
		    	// decoder in parallel with messages being removed. We don't really want to synchornized access
		    	// to this so we'll take the hit here
		}
		    
		messages.clear();
		  
		return( null );
	}
}
