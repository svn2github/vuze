/*
 * Created on 22 Jun 2006
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

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.security.SecureRandom;
import java.util.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.net.udp.uc.PRUDPPacketReply;

public class 
UDPConnectionSet 
{
	private static final byte	PROTOCOL_VERSION	= 1;
	
	private static final byte	COMMAND_DATA		= 1;
	
	private static final byte[]	KEYA_IV	= "UDPDriverKeyA".getBytes();
	private static final byte[]	KEYB_IV	= "UDPDriverKeyB".getBytes();
	private static final byte[]	KEYC_IV	= "UDPDriverKeyC".getBytes();
	private static final byte[]	KEYD_IV	= "UDPDriverKeyD".getBytes();
	
	private static final int MIN_MSS	= 256;
	private static final int MAX_HEADER	= 128;
	public static final int MIN_WRITE_PAYLOAD	= MIN_MSS - MAX_HEADER;
	
	private UDPConnectionManager	manager;
	private int						local_port;
	private InetSocketAddress		remote_address;
	
	private UDPConnection	lead_connection;
	private int				sent_packet_count;
	private int				received_packet_count;
	
	private int				first_unreceived_sequence_number;
	private short			duplicate_packet_count;
	private short			missed_packet_count;
	
	private RC4Engine		header_cipher_out;
	private RC4Engine		header_cipher_in;
	private RC4Engine		seq_cipher_out;
	private RC4Engine		seq_cipher_in;
	
	private Random			sequence_in;
	private Random			sequence_out;
	
	private volatile boolean	crypto_done;
	
	
	private Map	connections = new HashMap();
	
	protected
	UDPConnectionSet(
		UDPConnectionManager	_manager,
		int						_local_port,
		InetSocketAddress		_remote_address )
	{
		manager			= _manager;
		local_port		= _local_port;
		remote_address	= _remote_address;
	}
	
	protected int
	getLocalPort()
	{
		return( local_port );
	}
	
	protected InetSocketAddress
	getRemoteAddress()
	{
		return( remote_address );
	}
	
	protected void
	add(
		UDPConnection	connection )
	{
		UDPConnection	old_connection = null;
		
		synchronized( connections ){
			
			old_connection =  (UDPConnection)connections.put( new Integer( connection.getID()), connection );
			
			if ( connections.size() == 1 && lead_connection == null ){
				
				lead_connection = connection;
			}
		}
		
		if ( old_connection != null ){
			
			Debug.out( "Duplicate connection" );
			
			old_connection.close( "Duplication connection" );
		}
	}
	
	protected boolean
	remove(
		UDPConnection	connection )
	{
		synchronized( connections ){
	
			connections.remove( connection );
			
			return( connections.size() == 0 );
		}
	}
	
	protected void
	poll()
	{
		synchronized( connections ){

			Iterator	it = connections.values().iterator();
			
			while( it.hasNext()){
				
				((UDPConnection)it.next()).poll();
			}
		}
	}
	
	protected void
	setSecret(
		UDPConnection	connection,
		byte[]			session_secret )
	{
		try{
			if ( connection == lead_connection ){
					
				System.out.println( connection.getTransport().getAddress() + ": crypto done" );
				
			    SHA1Hasher	hasher = new SHA1Hasher();
			    
			    hasher.update( KEYA_IV );
			    hasher.update( session_secret );
			    	
			    byte[]	a_key = hasher.getDigest();
			    
			    hasher = new SHA1Hasher();
			    
			    hasher.update( KEYB_IV );
			    hasher.update( session_secret );
			    	
			    byte[]	b_key = hasher.getDigest();
			    
			    hasher = new SHA1Hasher();
			    
			    hasher.update( KEYC_IV );
			    hasher.update( session_secret );
			    	
			    byte[]	c_key = hasher.getDigest();
	
			    hasher = new SHA1Hasher();
			    
			    hasher.update( KEYD_IV );
			    hasher.update( session_secret );
			    	
			    byte[]	d_key = hasher.getDigest();
				    
			    	// for RC4 enc/dec is irrelevant
			    
			    RC4Engine rc4_engine_a	= getCipher( a_key );	
	    		RC4Engine rc4_engine_b	= getCipher( b_key );
	    		RC4Engine rc4_engine_c	= getCipher( c_key );	
	    		RC4Engine rc4_engine_d	= getCipher( d_key );	
	    		
	    		
		    	if ( lead_connection.isIncoming()){
	    			
	    			header_cipher_out	= rc4_engine_a;
	    			header_cipher_in	= rc4_engine_b;
	    			seq_cipher_out		= rc4_engine_c;
	    			seq_cipher_in		= rc4_engine_d;
	    			
	    				// we use deterministic random number sequences as packet numbers to
	    				// help avoid bit twiddling attacks, I wanted to use SHA1PRNG but this isn't
	    				// a standard algorithm it is Sun's own...
	    			
	       			sequence_in 	= new Random( bytesToLong( c_key ));
		 			sequence_out	= new Random( bytesToLong( d_key ));
	
	      			 
	    		}else{
	    			
	       			header_cipher_out	= rc4_engine_b;
	    			header_cipher_in	= rc4_engine_a;
	    			seq_cipher_out		= rc4_engine_d;
	    			seq_cipher_in		= rc4_engine_c;

	       			sequence_in 	= new Random( bytesToLong( d_key ));
		 			sequence_out	= new Random( bytesToLong( c_key ));
		    	}
	    		
	    		crypto_done	= true;
	    		
			}else if ( !crypto_done ){
				
				Debug.out( "Secondary setSecret but crypto not done" );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			connection.close( "Crypto problems: "+ Debug.getNestedExceptionMessage(e));
		}
	}
	
	protected RC4Engine
	getCipher(
		byte[]			key )
	{
	    SecretKeySpec	secret_key_spec = new SecretKeySpec( key, "RC4" );
	    
	    RC4Engine rc4_engine	= new RC4Engine();
		
		CipherParameters	params_a = new KeyParameter( secret_key_spec.getEncoded());
		
			// for RC4 enc/dec is irrelevant
		
		rc4_engine.init( true, params_a ); 
		
			// skip first 1024 bytes of stream to protected against a Fluhrer, Mantin and Shamir attack
    	
    	byte[]	temp = new byte[1024];
	
    	rc4_engine.processBytes( temp, temp );
    	
    	return( rc4_engine );
	}
	
	protected void
	send(
		UDPPacket		packet )
	
		throws IOException
	{
		byte[]	payload = packet.getBuffer();
				
		System.out.println( "Connection::write(" + packet.getConnection().getID() + ") loc="+local_port + " - " + remote_address + ",total=" + payload.length );

			// cache packet for resend
		
		sent_packet_count++;
		
		manager.send( local_port, remote_address, payload );
	}
	
	public void
	receive(
		byte[]			data )
	{
		UDPConnection	connection	= null;
		
		try{				
			boolean	new_connection = false;
	
			ByteBuffer	buffer = ByteBuffer.wrap( data );
			
			if ( !crypto_done ){
				
					// first packet - connection setup and crypto handshake
				
				synchronized( connections ){
	
					if ( connections.size() == 0 ){
						
							// -1 for connection id as we don't know what it is yet
						
						connection	= new UDPConnection( this, -1 );
				
						connections.put( new Integer( connection.getID()), connection );
							
						lead_connection	= connection;
						
						new_connection	= true;
						
					}else{
						
						connection = lead_connection;
					}
				}				
			}else{
				
				buffer.getInt();	// seq1
				int	seq2 = buffer.getInt();
				buffer.getInt();	// seq3
						
				int[]	seq_in = getNextSequenceNumber( sequence_in, seq_cipher_in );
				
					// TODO !
				
				if ( seq2 != seq_in[1] ){
					
					throw( new IOException( "seq mismatch" ));
				}
				
				header_cipher_in.processBytes( data, 12, 2, data, 12 );
	
				int	header_len = buffer.getShort()&0xffff;
				
				if ( header_len > data.length ){
					
					throw( new IOException( "Header length too large" ));
				}
				
				header_cipher_in.processBytes( data, 14, header_len-14, data, 14 );
				
				byte[]	hash = new SHA1Simple().calculateHash( data, 0, header_len - 4 ); 
						
				for (int i=0;i<4;i++){
					
					if ( hash[i] != data[header_len-4+i] ){	
					
						throw( new IOException( "hash incorrect" ));
					}
				}
				
				byte	version = buffer.get();
				
				if ( version != PROTOCOL_VERSION ){
					
					throw( new IOException( "Invalid protocol version '" + version + "'" ));
				}
				
				byte	flags = buffer.get();
				
				short	their_duplicate_packets = buffer.getShort();
				short	their_missed_packets 	= buffer.getShort();
				
				long	their_first_unreceived_sequence_number = buffer.getInt();
				
				byte	command = buffer.get();
				
				if ( command == COMMAND_DATA ){
					
					int	connection_id = buffer.getInt();
								
					synchronized( connections ){
						
						connection = (UDPConnection)connections.get( new Integer( connection_id ));
						
						if ( connection == null ){
											
							connection = (UDPConnection)connections.remove( new Integer( -1 ));

							if ( connection != null ){
									
								connection.setID( connection_id );
									
								connections.put( new Integer( connection_id ), connection );
							}
						}
						
						if ( connection == null ){
							
							if ( connections.size() == 128 ){
								
								throw( new IOException( "Connection limit reached" ));
							}
							
							connection	= new UDPConnection( this, connection_id );
							
							connections.put( new Integer( connection.getID()), connection );
															
							new_connection	= true;
						}
					}
					
					buffer.position( header_len );
					
				}else{
					
					throw( new IOException( "Invalid command" ));
				}
			}
			
			received_packet_count++;
	
			if ( new_connection ){
				
				manager.accept( local_port, remote_address, connection );
			}
				
			System.out.println( "Connection::read(" + connection.getID() + ") loc="+local_port + " - " + remote_address + ",rem="+data.length + "[" + Debug.getCompressedStackTrace());
	
			connection.receive( buffer );
			
		}catch( Throwable e ){
	
			Debug.printStackTrace(e);
			
			if ( connection != null ){
				
				connection.close( Debug.getNestedExceptionMessageAndStack(e));
			}
		}
	}
	
	protected int
	writePacketHeader(
		int[]			sequence_numbers,
		byte[]			bytes,
		UDPConnection	connection )
	
		throws IOException
	{		
		// packet format
	
		// int[3] sequence numbers
		// short header length starting from version
		// byte version
		// byte flags
		// short duplicate packet count
		// short missed packet count
		// int	first unreceived packet number
		// byte command: data, close, re-request
		// 
		// command specific data
		//     	data: connection_id
		//		close: connection_id....
		// header checksum
		// payload
	
		ByteBuffer	buffer = ByteBuffer.wrap( bytes );
		
		buffer.putInt( sequence_numbers[0] );
		buffer.putInt( sequence_numbers[1] );
		buffer.putInt( sequence_numbers[2] );
		
			// insert space for length added later
		
		int	length_pos	= buffer.position();
		
		buffer.putShort((short)0 );
				
		buffer.put((byte)PROTOCOL_VERSION);
		buffer.put((byte)0);
		
		buffer.putShort( duplicate_packet_count );
		buffer.putShort( missed_packet_count );

		buffer.putInt( first_unreceived_sequence_number );
		
		buffer.put((byte)COMMAND_DATA);
		
		buffer.putInt(connection.getID());
		
		short	total_length = (short)buffer.position();
				
		buffer.position( length_pos );
		
		buffer.putShort((short)( total_length + 4 ));
		
			// hash includes everything up to but obviously not including the hash
		
		byte[]	hash = new SHA1Simple().calculateHash( bytes, 0, total_length );

		buffer.position( total_length );
		
		buffer.put( hash, 0, 4 );
		
		total_length += 4;
		
			// don't encrypt the sequence numbers
		
		header_cipher_out.processBytes( bytes, 12, total_length-12, bytes, 12 );
		
		if ( total_length > MAX_HEADER ){
			
			Debug.out( "MAX_HEADER exceeded!!!!" );
			
			throw( new IOException( "MAX_HEADER exceeded" ));
		}
		
		return( total_length );
	}
	
	protected int 
	write( 
		UDPConnection	connection,
		ByteBuffer[]	buffers,
		int				offset,
		int				length )
	
		throws IOException
	{
		if ( !canWrite( connection )){
			
			return( 0 );
		}
		
		int	payload_to_send	= 0;
	

		for (int i=offset;i<offset+length;i++){
			
			payload_to_send += buffers[i].remaining();
		}
		
		UDPPacket	packet;
		
			// regardless of mss we have to get the first phe handshake messages into a single packet 

		if ( sent_packet_count == 0 ){
			
				// first packet, cram it all in
			
			
			byte[]	packet_bytes = new byte[ payload_to_send ];
			
			ByteBuffer packet_buffer = ByteBuffer.wrap( packet_bytes );
			
			for (int i=offset;i<offset+length;i++){
				
				packet_buffer.put( buffers[i] );
			}
				
			packet = new UDPPacket( connection, -1, packet_bytes );
			
		}else{
		
			byte[]	header = new byte[256];
			
			int[]	sequence_numbers = getNextSequenceNumber( sequence_out, seq_cipher_out );
			
			int header_size = writePacketHeader( sequence_numbers, header, connection );
						
			int	mss = connection.getTransport().getMss();

				// just in case we have some crazy limit set
			
			if ( mss < MIN_MSS ){
				
				mss = MIN_MSS;
			}
			
			if ( payload_to_send > mss - header_size ){
				
				payload_to_send	= mss - header_size;
			}
			
			if ( payload_to_send < 0 ){
				
				payload_to_send	= 0;
			}
					
			byte[]	packet_bytes = new byte[ header_size + payload_to_send ];
			
			ByteBuffer packet_buffer = ByteBuffer.wrap( packet_bytes );
			
			packet_buffer.put( header, 0, header_size );
			
			int	rem = payload_to_send;
			
			for (int i=offset;i<offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				int	limit = buffer.limit();
				
				try{
					if ( buffer.remaining() > rem ){
						
						buffer.limit( buffer.position() + rem );
					}
					
					rem -= buffer.remaining();
					
					packet_buffer.put( buffer );
					
				}finally{
					
					buffer.limit( limit );
				}
				
				if ( rem == 0 ){
					
					break;
				}
			}
			
				
			packet = new UDPPacket(connection, sequence_numbers[1], packet_bytes );
		}
			

		System.out.println( "Sent packet: seq=" + packet.getSequence() + ", len=" + payload_to_send );

		send( packet );
		
		return( payload_to_send );
	}
	
	protected boolean
	canWrite(
		UDPConnection	connection )
	{
		if ( !crypto_done ){
			
			if ( connection != lead_connection ){
				
				return( false );
			}
			
			if ( sent_packet_count > 0 ){
				
				return( false );
			}
		}
		
		return( true );	// TODO: write speed limiting
	}
	
	public void
	close(
		UDPConnection	connection,
		String			reason )
	{
		System.out.println( "UDPConnection::close(" + connection.getID() + "): " + reason );
		
		manager.remove( this, connection );
	}
	
	static void
	forDocumentation()
	{
			// this is here to draw attention to the fact that there's a dependency between packets formats...
		
		PRUDPPacketReply.registerDecoders( new HashMap());
	}
	
	protected int[]
	getNextSequenceNumber(
		Random		generator,
		RC4Engine	cipher )
	{
			// damn tracker udp protocol has:
			// request: long (random connection id) int (action)
			// reply: int (action) int (random txn id)
		
			// now action is always < 2048 so all other uses of udp packets will have either
			// 0x00000??? in either bytes 9 or 0 onwards. So we're forced to use 12 byte sequence numbers
		
			// internally we use the middle integer as the packet sequence
		
		final int	mask = 0xfffff800;
		
		while( true ){
			
			int	seq1 = generator.nextInt();
			int	seq2 = generator.nextInt();
			int	seq3 = generator.nextInt();
		
			seq1 = cipherInt( cipher, seq1 );
			seq2 = cipherInt( cipher, seq2 );
			seq3 = cipherInt( cipher, seq3 );
			
			if (( seq1 & mask ) != 0 && seq2 != -1 && ( seq3 & mask ) != 0){
				
				return( new int[]{ seq1, seq2, seq2 });
			}
		}
	}
	
	protected int
	cipherInt(
		RC4Engine	cipher,
		int			i )
	{
		byte[]	bytes = intToBytes( i );
		
		cipher.processBytes( bytes, bytes );
		
		return( bytesToInt( bytes, 0 ));
	}
	
	protected int
	bytesToInt(
		byte[]	bytes,
		int		offset )
	{
		int 	res = 	(bytes[offset++]<<24)&0xff000000 | 
						(bytes[offset++]<<16)&0x00ff0000 | 
						(bytes[offset++]<<8)&0x0000ff00 | 
						bytes[offset++]&0x000000ff;
				
		return( res );
	}
	
	protected byte[]
	intToBytes(
		int		i )
	{
		byte[] res = new byte[]{ (byte)(i>>24), (byte)(i>>16), (byte)(i>>8), (byte)i };
		
		return( res );
	}
	
	protected long
	bytesToLong(
		byte[]	bytes )
	{
		long 	i1 = 	(bytes[0]<<24)&0xff000000L | 
						(bytes[1]<<16)&0x00ff0000L | 
						(bytes[2]<<8)&0x0000ff00L | 
						bytes[3]&0x000000ffL;
						
		long 	i2 = 	(bytes[4]<<24)&0xff000000L | 
						(bytes[5]<<16)&0x00ff0000L | 
						(bytes[6]<<8)&0x0000ff00L | 
						bytes[7]&0x000000ffL;				

		long	res = ( i1 << 32 ) | i2;
				
		return( res );
	}
}
