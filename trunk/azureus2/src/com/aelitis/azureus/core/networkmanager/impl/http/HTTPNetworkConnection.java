/*
 * Created on 3 Oct 2006
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

import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTBitfield;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;

public class 
HTTPNetworkConnection 
{
	private static final String	NL			= "\r\n";

	private NetworkConnection	connection;
	private PEPeerTransport		peer;
	
	private HTTPMessageDecoder	decoder;
	private HTTPMessageEncoder	encoder;
	
	private boolean			sent_handshake	= false;
	
	private byte[]	peer_id	= PeerUtils.createPeerID();

	private boolean	choked	= true;
	
	private List	choked_requests 		= new ArrayList();
	private List	outstanding_requests 	= new ArrayList();
	
	private boolean	destroyed;
	
	protected
	HTTPNetworkConnection(
		NetworkConnection		_connection,
		PEPeerTransport			_peer )
	{
		connection	= _connection;
		peer		= _peer;
		
		decoder	= (HTTPMessageDecoder)connection.getIncomingMessageQueue().getDecoder();
		encoder = (HTTPMessageEncoder)connection.getOutgoingMessageQueue().getEncoder();

		decoder.setConnection( this );
		encoder.setConnection( this );
	}
	
	protected PEPeerTransport
	getPeer()
	{
		return( peer );
	}
	
	protected void
	choke()
	{
		synchronized( outstanding_requests ){
			
			choked	= true;
		}
	}
	
	protected void
	unchoke()
	{
		boolean	wakeup = false;
		
		synchronized( outstanding_requests ){
			
			choked	= false;
			
			for (int i=0;i<choked_requests.size();i++){
				
				wakeup = true;
				
				decoder.addMessage((BTRequest)choked_requests.get(i));
			}
			
			choked_requests.clear();
		}
		
		if ( wakeup ){
			
			connection.getTransport().setReadyForRead();
		}
	}
	
	protected void
	decodeHeader(
		String		header )
	
		throws IOException
	{
		System.out.println( "got header: " + header );
		
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
		
		PEPeerControl	control = peer.getControl();
		
		int	this_piece_size = control.getPieceLength( piece );
		
		if ( ranges.size() == 0 ){
			
			ranges.add( new int[]{ 0, this_piece_size-1});
		}
		
		long	total_length = 0;
		
		for (int i=0;i<ranges.size();i++){
			
			int[]	range = (int[])ranges.get(i);
			
			int	start 	= range[0];
			int end		= range[1];
			
			if ( 	start < 0 || start >= this_piece_size ||
					end < 0 || end >= this_piece_size ||
					start > end ){
				
				throw( new IOException( "Invalid range specification '" + start + "-" + end + "'" ));
			}
			
			total_length += ( end - start ) + 1;
		}
		
		if ( !sent_handshake ){
			
			sent_handshake	= true;
			
			decoder.addMessage( new BTHandshake( control.getHash(), peer_id, false ));
			
			byte[]	bits = new byte[(control.getPieces().length +7) /8];
			
			DirectByteBuffer buffer = new DirectByteBuffer( ByteBuffer.wrap( bits ));
			
			decoder.addMessage( new BTBitfield( buffer ));
		}
		
		for (int i=0;i<ranges.size();i++){
			
			int[]	range = (int[])ranges.get(i);
			
			int	start 	= range[0];
			int end		= range[1];
			
			addRequest( new BTRequest( piece, start, ( end - start ) + 1 ), i==0, total_length );
		}
	}
	
	protected void
	addRequest(
		BTRequest		request,
		boolean			is_first,
		long			total_length )
	
		throws IOException
	{
		synchronized( outstanding_requests ){
				
			if ( destroyed ){
				
				throw( new IOException( "HTTP connection destroyed" ));
			}
			
			outstanding_requests.add( new pendingRequest( request, is_first, total_length ));
			
			if ( choked ){
					
				if ( choked_requests.size() > 1024 ){
					
					Debug.out( "pending request limit exceeded" );
					
				}else{
				
					choked_requests.add( request );
				}
			}else{
				
				decoder.addMessage( request );
			}
		}
	}
	
	protected RawMessage
	encodePiece(
		Message		message )
	{
		BTPiece	piece = (BTPiece)message;
		
		List	ready_requests = new ArrayList();
		
		boolean	found = false;
		
		synchronized( outstanding_requests ){

			if ( destroyed ){
				
				return( getEmptyRawMessage( message ));
			}
		
			for (int i=0;i<outstanding_requests.size();i++){
				
				pendingRequest	req = (pendingRequest)outstanding_requests.get(i);
				
				if ( 	req.getPieceNumber() == piece.getPieceNumber() &&
						req.getStart() == piece.getPieceOffset() &&
						req.getLength() == piece.getPieceData().remaining( DirectByteBuffer.SS_NET )){
		
					if ( req.getBTPiece() == null ){
					
						req.setBTPiece( piece );
						
						found	= true;
						
						if ( i == 0 ){
							
							Iterator	it = outstanding_requests.iterator();
							
							while( it.hasNext()){
								
								pendingRequest r = (pendingRequest)it.next();
								
								BTPiece	btp = r.getBTPiece();
								
								if ( btp == null ){
									
									break;
								}
								
								it.remove();
								
								ready_requests.add( r );
							}
						}
					
						break;
					}
				}
			}
		}
		
		if ( !found ){
			
			Debug.out( "request not matched" );
			
			return( getEmptyRawMessage( message ));
		}
		
		if ( ready_requests.size() == 0 ){
			
			return( getEmptyRawMessage( message ));
		}
		
		pendingRequest req	= (pendingRequest)ready_requests.get(0);
		
		DirectByteBuffer[]	buffers;
		int					buffer_index	= 0;
		
		if ( req.isFirst()){
		
			buffers = new DirectByteBuffer[ ready_requests.size() + 1 ];
			
			byte[]	http_header = (
					"HTTP/1.1 200 OK" + NL + 
					"Content-Type: application/octet-stream" + NL +
					"Server: " + Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + NL +
					"Connection: close" + NL +
					"Content-Length: " + req.getTotalLength() + NL +
					NL ).getBytes();
			
			buffers[buffer_index++] = new DirectByteBuffer( ByteBuffer.wrap( http_header ));
			
		}else{
			
			buffers = new DirectByteBuffer[ ready_requests.size()];
		}
		
		for (int i=0;i<ready_requests.size();i++){
			
			req	= (pendingRequest)ready_requests.get(i);

			buffers[buffer_index++] = req.getBTPiece().getPieceData();
		}
		
		return( 
				new RawMessageImpl( 
						message, 
						buffers,
						RawMessage.PRIORITY_HIGH, 
						true, 
						new Message[0] ));
	}
	
	protected void
	destroy()
	{
		synchronized( outstanding_requests ){

			destroyed	= true;
			
			for (int i=0;i<outstanding_requests.size();i++){
				
				pendingRequest	req = (pendingRequest)outstanding_requests.get(i);
				
				BTPiece	piece = req.getBTPiece();
				
				if ( piece != null ){
					
					piece.destroy();
				}
			}
			
			outstanding_requests.clear();
			
			for (int i=0;i<choked_requests.size();i++){
				
				BTRequest	req = (	BTRequest)choked_requests.get(i);
								
				req.destroy();
			}
			
			choked_requests.clear();
		}
	}
	
	protected RawMessage
	getEmptyRawMessage(
		Message	message )
	{		
		return( 
			new RawMessageImpl( 
					message, 
					new DirectByteBuffer[]{ new DirectByteBuffer( ByteBuffer.allocate(0))},
					RawMessage.PRIORITY_HIGH, 
					true, 
					new Message[0] ));
	}
	
	protected class
	pendingRequest
	{
		private int	piece;
		private int	start;
		private int	length;
		
		private boolean	is_first;
		private long	total_length;
		
		private BTPiece	bt_piece;
		
		protected
		pendingRequest(
			BTRequest		request,
			boolean			_is_first,
			long			_total_length )
		{
			piece	= request.getPieceNumber();
			start	= request.getPieceOffset();
			length	= request.getLength();
			
			is_first		= _is_first;
			total_length	= _total_length;
		}
		
		protected boolean
		isFirst()
		{
			return( is_first );
		}
		
		protected long
		getTotalLength()
		{
			return( total_length );
		}
		
		protected int
		getPieceNumber()
		{
			return( piece );
		}
		
		protected int
		getStart()
		{
			return( start );
		}
		
		protected int
		getLength()
		{
			return( length );
		}
		
		protected BTPiece
		getBTPiece()
		{
			return( bt_piece );
		}
		
		protected void
		setBTPiece(
			BTPiece	_bt_piece )
		{
			bt_piece	= _bt_piece;
		}
	}
}
