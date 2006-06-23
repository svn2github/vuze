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

import java.util.*;
import java.util.zip.CRC32;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.SHA1Hasher;

public class 
UDPConnectionSet 
{
	private static final byte[]	KEYA_IV	= "UDPConnectionSetKeyA".getBytes();
	private static final byte[]	KEYB_IV	= "UDPConnectionSetKeyB".getBytes();

	private UDPConnectionManager	manager;
	private int						local_port;
	private InetSocketAddress		remote_address;
	
	private UDPConnection	lead_connection;
	private int				sent_packet_count;
	private int				received_packet_count;
	
	private RC4Engine		cipher_out;
	private RC4Engine		cipher_in;
	
	private Random			sequence_in;
	private Random			sequence_out;
	
	private volatile boolean	crypto_done;
	
	
	private List	connections = new ArrayList();
	
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
	
	protected void
	add(
		UDPConnection	connection )
	{
		synchronized( connections ){
			
			connections.add( connection );
			
			if ( connections.size() == 1 ){
				
				lead_connection = connection;
			}
		}
	}
	
	protected void
	poll()
	{
		synchronized( connections ){

			for (int i=0;i<connections.size();i++){
				
				((UDPConnection)connections.get(i)).poll();
			}
		}
	}
	
	protected void
	setSecret(
		UDPConnection	connection,
		byte[]			session_secret )
	{
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
		    
		    SecretKeySpec	secret_key_spec_a = new SecretKeySpec( a_key, "RC4" );
		    SecretKeySpec	secret_key_spec_b = new SecretKeySpec( b_key, "RC4" );
		    
		    RC4Engine rc4_engine_a	= new RC4Engine();
    		
    		CipherParameters	params_a = new KeyParameter(secret_key_spec_a.getEncoded());
    		
    		rc4_engine_a.init( true, params_a ); 
    		
    		RC4Engine rc4_engine_b	= new RC4Engine();
    		
    		CipherParameters	params_b = new KeyParameter(secret_key_spec_b.getEncoded());
    		
    		rc4_engine_b.init( false, params_b ); 
    		
    		if ( lead_connection.isIncoming()){
    			
    			cipher_out	= rc4_engine_a;
    			cipher_in	= rc4_engine_b;
    			
    				// we use deterministic random number sequences as packet numbers to
    				// help avoid bit twiddling attacks
    			
       			sequence_in 	= new Random( bytesToLong( a_key ));
       			sequence_out	= new Random( bytesToLong( b_key ));
       			
    		}else{
    			
       			cipher_out	= rc4_engine_b;
    			cipher_in	= rc4_engine_a;
 	
      			sequence_in 	= new Random( bytesToLong( b_key ));
       			sequence_out	= new Random( bytesToLong( a_key ));
    		}
    		
    		crypto_done	= true;
		}
	}
	
	public void
	receive(
		ByteBuffer			data )
	{
		if ( received_packet_count > 0 ){
			
			int	packet_number = data.getInt();
			
			int	pos = data.position();
			
			data.position( data.limit()-4 );
			
			int	crc = data.getInt();
			
			data.position( pos );
			
			data.limit( data.limit()-4);
			
			System.out.println( "Got packet: " + packet_number + "/" + crc );
		}
		
		received_packet_count++;
		
		System.out.println( local_port + ":" + remote_address + " received " + data.remaining());
		
		UDPConnection	connection = null;
		
		boolean	new_connection = false;
		
		synchronized( connections ){

			if ( connections.size() == 0 ){
				
				new_connection	= true;
				
				connection	= new UDPConnection( this );
	
				connections.add( connection );
				
				lead_connection	= connection;
				
			}else{
				
					// TODO:
				
				connection = (UDPConnection)connections.get(0);
			}
		}
		
		if ( new_connection ){
			
			manager.accept( local_port, remote_address, connection );
		}
		
		connection.receive( data );
	}
	
	protected int 
	write( 
		UDPConnection	connection,
		ByteBuffer 		data ) 
	
		throws IOException
	{
		if ( sent_packet_count == 0 ){
			
			if ( connection != lead_connection ){
				
				return( 0 );
			}
		}
		
		System.out.println( local_port + ":" + remote_address + " sent " + data.remaining());

			// TODO: make this take a bytebuffer[] and aggregate
		
			// TODO: retry timer for crypto first packet until reply received...
		
		ByteBuffer	packet;
		
		int	header_size	= 0;
		
		if ( sent_packet_count > 0 ){
		
			int	packet_number = sequence_out.nextInt();
			
			CRC32	checksum = new CRC32();
			
				// packet format
			
				// int packet number
				// TBD other header info: variable length
				// optional payload
				// int checksum of packet number, header + payload
			
				// crypto
				// packet + checksum use rc4_1
				// other header use rc4_2
				// payload is encrypted by higher layer
			
				// because rc4 is non CBC we can predict the encrypted packet numbers into the future
			
			byte[]	packet_bytes = new byte[ 4 + data.remaining() + 4 ];
			
			packet = ByteBuffer.wrap( packet_bytes );
			
			packet.putInt( packet_number );
			packet.put( data );
			
			checksum.update( packet_bytes, 0, packet.position());
			
			int	cs = (int)checksum.getValue();
			
			packet.putInt( cs );
			
			header_size	= 8;
			
			System.out.println( "Sent packet: " + packet_number + "/" + cs );

		}else{
			
			packet = ByteBuffer.allocate( data.remaining());
			
			packet.put( data );
		}
		
		packet.position( 0 );
		
			// cache packet for resend
		
		sent_packet_count++;
		
		return( manager.send( local_port, remote_address, packet ) - header_size );
	}
	
	protected boolean
	canWrite(
		UDPConnection	connection )
	{
		if ( !crypto_done ){
			
			if ( connection != lead_connection ){
				
				return( false );
			}
		}
		
		return( true );	// TODO:!
	}
	
	public void
	close(
		UDPConnection	connection )
	{
		System.out.println( "UDPConnection::close" );
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
