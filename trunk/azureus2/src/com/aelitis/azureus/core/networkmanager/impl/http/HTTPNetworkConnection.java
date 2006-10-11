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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTBitfield;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHave;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTInterested;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;

public abstract class 
HTTPNetworkConnection 
{
	protected static final LogIDs LOGID = LogIDs.NWMAN;

	private static final int	MAX_OUTSTANDING_BT_REQUESTS	= 16;
	
	protected static final String	NL			= "\r\n";

	private static int        max_read_block_size;

	static{
	
	    ParameterListener param_listener = new ParameterListener() {
	            public void
	            parameterChanged(
	                String  str )
	            {
	                max_read_block_size = COConfigurationManager.getIntParameter( "BT Request Max Block Size" );
	            }
	    };
	
	    COConfigurationManager.addAndFireParameterListener( "BT Request Max Block Size", param_listener);
	}
	
	private HTTPNetworkManager	manager;
	private NetworkConnection	connection;
	private PEPeerTransport		peer;
	private String				url;
	
	private HTTPMessageDecoder	decoder;
	private HTTPMessageEncoder	encoder;
	
	private boolean			sent_handshake	= false;
	
	private byte[]	peer_id	= PeerUtils.createPeerID();

	private boolean	choked	= true;
	
	private List	http_requests			= new ArrayList();
	private List	choked_requests 		= new ArrayList();
	private List	outstanding_requests 	= new ArrayList();
	
	private BitSet	piece_map	= new BitSet();
	
	private boolean	destroyed;
	
	protected
	HTTPNetworkConnection(
		HTTPNetworkManager		_manager,
		NetworkConnection		_connection,
		PEPeerTransport			_peer,
		String					_url )
	{
		manager		= _manager;
		connection	= _connection;
		peer		= _peer;
		url			= _url;
		
		decoder	= (HTTPMessageDecoder)connection.getIncomingMessageQueue().getDecoder();
		encoder = (HTTPMessageEncoder)connection.getOutgoingMessageQueue().getEncoder();

		decoder.setConnection( this );
		encoder.setConnection( this );
	}
	
	protected boolean
	isSeed()
	{
		if ( !peer.getControl().isSeeding()){
			
			if (Logger.isEnabled()){
				Logger.log(new LogEvent(peer,LOGID, "Download is not seeding" ));
			}   	
			
			sendAndClose( manager.getNotFound());
			
			return( false );
		}
		
		return( true );
	}
	
	protected HTTPNetworkManager
	getManager()
	{
		return( manager );
	}
	
	protected NetworkConnection
	getConnection()
	{
		return( connection );
	}
	
	protected PEPeerTransport
	getPeer()
	{
		return( peer );
	}
	protected PEPeerControl
	getPeerControl()
	{
		return( peer.getControl());
	}
	
	protected RawMessage
	encodeChoke()
	{
		synchronized( outstanding_requests ){
			
			choked	= true;
		}
		
		return( null );
	}
	
	protected RawMessage
	encodeUnchoke()
	{		
		synchronized( outstanding_requests ){
			
			choked	= false;
			
			for (int i=0;i<choked_requests.size();i++){
								
				decoder.addMessage((BTRequest)choked_requests.get(i));
			}
			
			choked_requests.clear();
		}
		
		return( null );
	}
	
	protected RawMessage
	encodeBitField()
	{
		decoder.addMessage( new BTInterested());
		
		return( null );
	}
	
	protected void
	readWakeup()
	{
		connection.getTransport().setReadyForRead();
	}

	protected RawMessage
	encodeHandShake(
		Message	message )
	{
		return( null );
	}
	
	protected abstract void
	decodeHeader(
		String		header )
	
		throws IOException;

	protected String
	encodeHeader(
		httpRequest	request )
	{
		return(	"HTTP/1.1 " + (request.isPartialContent()?"206 Partial Content":"200 OK" ) + NL + 
				"Content-Type: application/octet-stream" + NL +
				"Server: " + Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + NL +
				"Connection: " + ( request.keepAlive()?"Keep-Alive":"Close" ) + NL +
				(request.keepAlive()?("Keep-Alive: timeout=30" + NL) :"" ) +
				"Content-Length: " + request.getTotalLength() + NL +
				NL );
	}
	
	protected void
	addRequest(
		httpRequest		request )
	
		throws IOException
	{
		PEPeerControl	control = getPeerControl();
		
		if ( !sent_handshake ){
			
			sent_handshake	= true;
			
			decoder.addMessage( new BTHandshake( control.getHash(), peer_id, false ));
			
			byte[]	bits = new byte[(control.getPieces().length +7) /8];
			
			DirectByteBuffer buffer = new DirectByteBuffer( ByteBuffer.wrap( bits ));
			
			decoder.addMessage( new BTBitfield( buffer ));
		}
		
		synchronized( outstanding_requests ){

			http_requests.add( request );
		}
		
		submitBTRequests();
	}
	
	protected void
	submitBTRequests()
	
		throws IOException
	{
		PEPeerControl	control = getPeerControl();

		long	piece_size = control.getPieceLength(0);
	
		synchronized( outstanding_requests ){

			while( outstanding_requests.size() < MAX_OUTSTANDING_BT_REQUESTS && http_requests.size() > 0 ){
				
				httpRequest	http_request = (httpRequest)http_requests.get(0);
				
				long[]	offsets	= http_request.getOffsets();
				long[]	lengths	= http_request.getLengths();
				
				int	index	= http_request.getIndex();
				
				long	offset 	= offsets[index];
				long	length	= lengths[index];
				
				int		this_piece_number 	= (int)(offset / piece_size);
				int		this_piece_size		= control.getPieceLength( this_piece_number );
				
				int		offset_in_piece 	= (int)( offset - ( this_piece_number * piece_size ));
				
				int		space_this_piece 	= this_piece_size - offset_in_piece;
				
				int		request_size = (int)Math.min( length, space_this_piece );
				
				request_size = Math.min( request_size, max_read_block_size );
				
				addBTRequest( 
					new BTRequest( 
							this_piece_number, 
							offset_in_piece, 
							request_size ),
					http_request );
					
				if ( request_size == length ){
					
					if ( index == offsets.length - 1 ){
						
						http_requests.remove(0);
						
					}else{
						
						http_request.setIndex( index+1 );
					}
				}else{
					offsets[index] += request_size;
					lengths[index] -= request_size;
				}
			}
		}
	}
	
	protected void
	addBTRequest(
		BTRequest		request,
		httpRequest		http_request )
	
		throws IOException
	{
		synchronized( outstanding_requests ){
				
			if ( destroyed ){
				
				throw( new IOException( "HTTP connection destroyed" ));
			}
			
			outstanding_requests.add( new pendingRequest( request, http_request ));
			
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
						req.getStart() 	== piece.getPieceOffset() &&
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
		
		try{
			submitBTRequests();
			
		}catch( IOException e ){
			
		}
		
		pendingRequest req	= (pendingRequest)ready_requests.get(0);
		
		DirectByteBuffer[]	buffers;
		int					buffer_index	= 0;
		
		httpRequest	http_request = req.getHTTPRequest();
		
		buffers = new DirectByteBuffer[ ready_requests.size() + 1 ];

		if ( !http_request.hasSentFirstReply()){
		
			http_request.setSentFirstReply();
						
			String	header = encodeHeader( http_request );
			
			buffers[buffer_index++] = new DirectByteBuffer( ByteBuffer.wrap( header.getBytes()));
			
		}else{
			
				// we have to do this as core code assumes buffer entry 0 is protocol
			
			buffers[buffer_index++] = new DirectByteBuffer( ByteBuffer.allocate(0));
		}
		
		for (int i=0;i<ready_requests.size();i++){
			
			req	= (pendingRequest)ready_requests.get(i);

			BTPiece	this_piece = req.getBTPiece();
			
			int	piece_number = this_piece.getPieceNumber();
			
			if ( !piece_map.get( piece_number )){
				
					// kinda crappy as it triggers on first block of piece, however better
					// than nothing
				
				piece_map.set( piece_number );
				
				decoder.addMessage( new BTHave( piece_number ));
			}
			
			buffers[buffer_index++] = this_piece.getPieceData();
		}
		
		return(	new RawMessageImpl( 
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
	
	protected void
	log(
		String	str )
	{
		if (Logger.isEnabled()){
			Logger.log(new LogEvent( getPeer(),LOGID, str));
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
	
	protected void
	sendAndClose(
		String		data )
	{
		final Message	http_message = new HTTPMessage( data );
		
		getConnection().getOutgoingMessageQueue().registerQueueListener(
			new OutgoingMessageQueue.MessageQueueListener()
			{
				public boolean 
				messageAdded( 
					Message message )
				{	
					return( true );
				}
				   
				public void 
				messageQueued( 
					Message message )
				{			
				}
				    
				public void 
				messageRemoved( 
					Message message )
				{
				}
				    
				public void 
				messageSent( 
					Message message )
				{
					if ( message == http_message ){
						
						getConnection().close();
					}
				}
				    
			    public void 
			    protocolBytesSent( 
			    	int byte_count )
			    {	
			    }
				 
			    public void 
			    dataBytesSent( 
			    	int byte_count )
			    {	
			    }
			});
		
		getConnection().getOutgoingMessageQueue().addMessage( http_message, false );
	}
	
	protected class
	httpRequest
	{
		private long[]	offsets;
		private long[]	lengths;
		private boolean	partial_content;
		
		private int		index;
		private long	total_length;
		private boolean	sent_first_reply;
		
		private boolean	keep_alive;
		
		protected
		httpRequest(
			long[]		_offsets,
			long[]		_lengths,
			boolean		_partial_content,
			boolean		_keep_alive )
		{
			offsets	= _offsets;
			lengths	= _lengths;
			partial_content	= _partial_content;
			keep_alive		= _keep_alive;
			
			for (int i=0;i<lengths.length;i++){
				
				total_length += lengths[i];
			}
		}
		
		protected boolean
		isPartialContent()
		{
			return( partial_content );
		}
		
		protected boolean
		hasSentFirstReply()
		{
			return( sent_first_reply );
		}
		
		protected void
		setSentFirstReply()
		{
			sent_first_reply	= true;
		}
		
		protected long[]
		getOffsets()
		{
			return( offsets );
		}
		
		protected long[]
   		getLengths()
   		{
   			return( lengths );
   		}
		
		protected int
		getIndex()
		{
			return( index );
		}
		
		protected void
		setIndex(
			int	_index )
		{
			index = _index;
		}
		
		protected long
		getTotalLength()
		{
			return( total_length );
		}
		
		protected boolean
		keepAlive()
		{
			return( keep_alive );
		}
	}
	
	protected class
	pendingRequest
	{
		private int	piece;
		private int	start;
		private int	length;
				
		private httpRequest	http_request;
		
		private BTPiece	bt_piece;
		
		protected
		pendingRequest(
			BTRequest		_request,
			httpRequest		_http_request )
		{
			piece	= _request.getPieceNumber();
			start	= _request.getPieceOffset();
			length	= _request.getLength();
			
			http_request	= _http_request;
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
		
		protected httpRequest
		getHTTPRequest()
		{
			return( http_request );
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
