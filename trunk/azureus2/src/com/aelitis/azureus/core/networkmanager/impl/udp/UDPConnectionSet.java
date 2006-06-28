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

import java.text.DateFormat;
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

import com.aelitis.net.udp.uc.PRUDPPacketReply;

public class 
UDPConnectionSet 
{
	private static final boolean	LOG = true;
	
	private static final byte	PROTOCOL_VERSION	= 1;
	
	private static final byte	COMMAND_CRYPTO		= 0;
	private static final byte	COMMAND_DATA		= 1;
	private static final byte	COMMAND_ACK			= 2;
	
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
		
	private RC4Engine		header_cipher_out;
	private RC4Engine		header_cipher_in;
	private RC4Engine		seq_cipher_out;
	private RC4Engine		seq_cipher_in;
	
	private Random			sequence_in;
	private Random			sequence_out;
	
	private volatile boolean	crypto_done;
	
	
	private Map	connections = new HashMap();
	
	private long	total_tick_count;

		// transmit

	private int	total_data_sent		= 0;
	private int	total_data_resent	= 0;
	
	private int	total_protocol_sent		= 0;
	private int	total_protocol_resent	= 0;

	
	private static final int RETRANSMIT_TIMER	= 500;
	private static final int RETRANSMIT_TICKS	= Math.max( 1, RETRANSMIT_TIMER / UDPConnectionManager.TIMER_TICK_MILLIS );
	private int retransmit_ticks = 0;
	
	private static final int MIN_RETRANSMIT_TIMER	= 100;
	private static final int MIN_RETRANSMIT_TICKS	= Math.max( 1, MIN_RETRANSMIT_TIMER / UDPConnectionManager.TIMER_TICK_MILLIS );

	
	private static final int 	MAX_TRANSMIT_UNACK_PACKETS	= 10;
	private List				transmit_unack_packets = new ArrayList();
		
	private static final int	MAX_CONTIGUOUS_RETRANS	= 3;
	
		// receive
		
	private int		receive_last_inorder_sequence		= -1;
	private int		receive_last_inorder_alt_sequence	= -1;
	
	private static final int RECEIVE_UNACK_IN_SEQUENCE_LIMIT	= 3;
	private long	current_receive_unack_in_sequence_count	= 0;
	private long	sent_receive_unack_in_sequence_count	= 0;
	
	private static final int RECEIVE_OUT_OF_ORDER_ACK_LIMIT		= 3;
	private long	current_receive_out_of_order_count		= 0;
	private long	sent_receive_out_of_order_count			= 0;
	
	
	private static final int RECEIVE_DONE_SEQ_MAX	= 128;
	private LinkedList	receive_done_sequences	= new LinkedList();
	
	private static final int RECEIVE_OUT_OF_ORDER_PACKETS_MAX	= 32;
	private List	receive_out_of_order_packets	= new LinkedList();
	
	private static final int EXPLICITACK_TIMER	= 300;
	private static final int EXPLICITACK_TICKS	= Math.max( 1, EXPLICITACK_TIMER / UDPConnectionManager.TIMER_TICK_MILLIS );
	private int explicitack_ticks = 0;

	
	
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
					
				log( connection.getTransport().getAddress() + ": crypto done" );
				
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
	timerTick()
	
		throws IOException
	{
		synchronized( this ){
			
			total_tick_count++;
			
			if ( retransmit_ticks > 0 ){
				
				retransmit_ticks--;
				
				if ( retransmit_ticks == 0 ){
					
					retransmitExpired();
				}
			}
			
			if ( explicitack_ticks > 0 ){
				
				explicitack_ticks--;
				
				if ( explicitack_ticks == 0 ){
					
					sendAckCommand();
				}
			}
		}
	}
	
	protected void
	retransmitExpired()
	
		throws IOException
	{
		synchronized( this ){
			
				// only ever consider the first outstanding non-received packet for auto-retransmit
			
			for (int i=0;i<transmit_unack_packets.size();i++){
				
				UDPPacket packet = (UDPPacket)transmit_unack_packets.get(i);
				
				if ( !packet.hasBeenReceived()){
					
					log( "Retransmit " + getName() + ":" + packet.getString());
				
					send( packet );
					
					break;
				}
			}
		}
	}
	
	protected void
	remoteLastInSequence(
		int			sequence,
		boolean		main_sequence )
	{
			// if we find this packet then we can also discard any prior to it as they are implicitly
			// ack too
	
		synchronized( this ){

			for (int i=0;i<transmit_unack_packets.size();i++){
				
				UDPPacket	packet = (UDPPacket)transmit_unack_packets.get(i);
				
				if (  	(  main_sequence && packet.getSequence() == sequence ) ||
						( !main_sequence && packet.getAlternativeSequence() == sequence )){
										
					for (int j=0;j<=i;j++){
						
						transmit_unack_packets.remove(0);
					}
					
					return;
				}
			}
		}
	}
	
	protected synchronized void
	dumpState()
	{
		if ( LOG ){
			String	str = getName() + ": State:";
			
			String	unack = "";
			
			for (int i=0;i<transmit_unack_packets.size();i++){
				
				UDPPacket	packet = (UDPPacket)transmit_unack_packets.get(i);
				
				unack += (i==0?"":",") + packet.getString();
			}
			
			str += "unack=" + unack + ",last_in_order=" + receive_last_inorder_sequence +
				 	",current_in_seq=" + current_receive_unack_in_sequence_count +
				 	",sent_in_seq=" + sent_receive_unack_in_sequence_count +
				 	",current_oo=" + current_receive_out_of_order_count +
				 	",sent_oo=" + sent_receive_out_of_order_count;
			
			/*
			String	done = "";
			
			for (int i=0;i<receive_done_sequences.size();i++){
				
				done += (i==0?"":",") + receive_done_sequences.get(i);
			}
			
			str += ",done=" + done;
			*/
			
			String	oo = "";
			
			for (int i=0;i<receive_out_of_order_packets.size();i++){
				
				Object[] entry = (Object[])receive_out_of_order_packets.get(i);
				
				oo += (i==0?"":",") + entry[0] + "/" + entry[1] + "/" + entry[2];
			}
			
			str += ",sent_data=" + total_data_sent +"/" + total_data_resent + ",sent_prot=" + total_protocol_sent + "/" + total_protocol_resent;
			
			log( str );
		}
	}
		
		
	
	
	protected void
	send(
		UDPPacket		packet )
	
		throws IOException
	{
		byte[]	payload = packet.getBuffer();
				
		log( "Connection::write(" + packet.getConnection().getID() + ") loc="+local_port + " - " + remote_address + ": " + packet.getString());

			// cache packet for resend
		
		sent_packet_count++;
		
		synchronized( this ){
			
				// all packets carry an implicit ack, pick up the corresponding count here

			long	unackin = packet.getUnAckInSequenceCount();
			
			if ( unackin > 	sent_receive_unack_in_sequence_count ){
				
				sent_receive_unack_in_sequence_count	= unackin;
			}
			
				// ALL packets have to be received by the receiver else the crypto breakifies

			boolean	new_first_packet = false;
			
			if ( !transmit_unack_packets.contains( packet )){
				
				transmit_unack_packets.add( packet );
				
				new_first_packet	= transmit_unack_packets.size() == 1;
			}
			
				// trigger the retransmit timer if any sent packets have the auto-retransmit property 
			
			boolean	retransmit_active = false;
			
			Iterator	it = transmit_unack_packets.iterator();
			
			while( it.hasNext()){
				
				UDPPacket	p = (UDPPacket)it.next();
			
				if ( p.autoRetransmit()){
				
					retransmit_active = true;
					
					break;
				}
			}
			
			if ( retransmit_active ){
				
					// if this is a newly added packet and the first in the queue then we reset the
					// retransmit timer if it is running
				
				if ( new_first_packet ){
					
					retransmit_ticks = RETRANSMIT_TICKS;
					
				}else{
					
					if ( retransmit_ticks == 0 ){
						
							// TODO: back off based on resend_count + fail on limit
						
						retransmit_ticks = RETRANSMIT_TICKS;
					}
				}
			}else{
				
				retransmit_ticks	= 0;
			}
			
			

				// splice in the latest received in sequence alternative seq
			
			if ( receive_last_inorder_alt_sequence != -1 ){
				
				byte[]	alt = intToBytes( receive_last_inorder_alt_sequence );
				
				payload[0] = alt[0];
				payload[1] = alt[1];
				payload[8] = alt[2];
				payload[9] = alt[3];
			}
		
			int	count = packet.sent( total_tick_count );

			if ( count == 1 ){
				
				if ( packet.getCommand() == COMMAND_DATA ){
			
					total_data_sent++;
					
				}else{

					total_protocol_sent++;
				}
			}else{
				
				if ( packet.getCommand() == COMMAND_DATA ){
					
					total_data_resent++;
					
				}else{

					total_protocol_resent++;
				}
			}
	
				// TODO: remove closed connection's packets
		}
		
		
		manager.send( local_port, remote_address, payload );
	}
	
	public void
	receive(
		byte[]			initial_data )
	
		throws IOException
	{
		dumpState();
		
		log( "Connection::read loc=" + local_port + " - " + remote_address + ",total=" + initial_data.length );
		
		ByteBuffer	initial_buffer = ByteBuffer.wrap( initial_data );
		
		if ( !crypto_done ){
			
				// first packet - connection setup and crypto handshake
			
				// derive the sequence number in the normal way so that if a retranmission occurs
				// after crypto has been setup then it'll get handled correctly as a dupliate packet
				// below
			
			initial_buffer.position( 4 );
			
			Integer	pseudo_seq = new Integer( initial_buffer.getInt());
			
			initial_buffer.position( 0 );
			
			if ( !receive_done_sequences.contains( pseudo_seq )){
				
				receive_done_sequences.addFirst( pseudo_seq );
			
				if ( receive_done_sequences.size() > RECEIVE_DONE_SEQ_MAX ){
					
					receive_done_sequences.removeLast();
				}
			}
			
			receiveCrypto( initial_buffer );
			
		}else{
			
				// pull out the alternative last-in-order seq
			
			byte[]	alt_seq = new byte[4];
			
			alt_seq[0]	= initial_data[0];
			alt_seq[1]	= initial_data[1];
			alt_seq[2]	= initial_data[8];
			alt_seq[3]	= initial_data[9];
			
			int	alt = bytesToInt( alt_seq, 0 );
			
			remoteLastInSequence( alt, false );
			
			try{
				initial_buffer.getInt();	// seq1
				
				Integer	seq2 = new Integer( initial_buffer.getInt());
				
				initial_buffer.getInt();	// seq3
					
					// first see if we know about this sequence number already
				
				if ( receive_done_sequences.contains( seq2 )){
					
					log( "Duplicate processed packet: " + seq2 );
										
					return;
				}
				
				boolean	oop = false;
				
				for (int i=0;i<receive_out_of_order_packets.size();i++){
					
					Object[]	entry = (Object[])receive_out_of_order_packets.get(i);
					
					Integer	oop_seq = (Integer)entry[0];
					
					ByteBuffer	oop_buffer = (ByteBuffer)entry[2];
					
					if ( oop_seq.equals( seq2 )){
						
						if ( oop_buffer != null ){
						
							log( "Duplicate out-of-order packet: " + seq2 );
													
							return;
						}
						
							// got data matching out-of-order-entry, add it in!
						
						byte[]	copy = new byte[initial_data.length];
						
						System.arraycopy( initial_data, 0, copy, 0, copy.length );
						
						ByteBuffer	copy_buffer = ByteBuffer.wrap(copy);
						
						copy_buffer.position( 12 );
						
						entry[2] = copy_buffer;
						
						oop = true;
						
						break;
					}
				}
				
				if ( !oop ){
					
						// not a known out-of-order packet. If our oop list is full then all we can do is drop
						// the packet
					
					boolean	added = false;
					
					while ( receive_out_of_order_packets.size() < RECEIVE_OUT_OF_ORDER_PACKETS_MAX ){
									
						int[]	seq_in = getNextSequenceNumber( sequence_in, seq_cipher_in );
								
						if ( seq2.intValue() == seq_in[1] ){
										
							receive_out_of_order_packets.add( new Object[]{ seq2, new Integer( seq_in[3]), initial_buffer } );
							
							added = true;
							
							break;
							
						}else{
							
							log( "Out-of-order packet: adding spacer for seq " + seq_in[1] );
							
							receive_out_of_order_packets.add( new Object[]{ new Integer( seq_in[1]), new Integer( seq_in[3]), null } );
						}
					}
					
					if ( !added ){
					
							// drop the packet, we have no room to store it
						
						log( "Out-of-order packet dropped as too many pending" );
												
						return;
					}
				}
				
				boolean	this_is_oop = true;
				
					// process any ready packets
				
				Iterator	it = receive_out_of_order_packets.iterator();
				
				while( it.hasNext()){
					
					Object[]	entry = (Object[])it.next();
					
					ByteBuffer	buffer = (ByteBuffer)entry[2];
					
					if ( buffer == null ){
						
						break;
					}
				
					it.remove();
				
					byte[]	data = buffer.array();
					
					if ( buffer == initial_buffer ){
						
						this_is_oop = false;
					}
					
					synchronized( this ){
						
						current_receive_unack_in_sequence_count++;
					}
					
					Integer	seq = (Integer)entry[0];
					
					receive_last_inorder_sequence		= seq.intValue();
					receive_last_inorder_alt_sequence	= ((Integer)entry[1]).intValue();
					
					if ( !receive_done_sequences.contains( seq )){
						
						receive_done_sequences.addFirst( seq );
					
						if ( receive_done_sequences.size() > RECEIVE_DONE_SEQ_MAX ){
							
							receive_done_sequences.removeLast();
						}
					}
					
					header_cipher_in.processBytes( data, 12, 2, data, 12 );
		
					int	header_len = buffer.getShort()&0xffff;
					
					if ( header_len > data.length ){
						
						throw( new IOException( "Header length too large" ));
					}
					
					header_cipher_in.processBytes( data, 14, header_len-14, data, 14 );
					
					SHA1Hasher	hasher = new SHA1Hasher();
					
					hasher.update( data, 4, 4 );
					hasher.update( data, 12, header_len - 4 - 12 );
									
					byte[]	hash = hasher.getDigest();
							
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
					
					int		their_last_inorder_sequence = buffer.getInt();
					
					remoteLastInSequence( their_last_inorder_sequence, true );
										
					byte	command = buffer.get();
					
					if ( command == COMMAND_DATA ){				
	
						receiveDataCommand( seq.intValue(), buffer, header_len );
							
					}else if ( command == COMMAND_ACK ){
	
						receiveAckCommand( buffer, their_last_inorder_sequence );
	
					}else{
					
						throw( new IOException( "Invalid command" ));
					}
				}
				
				if ( this_is_oop ){
					
					synchronized( this ){
					
						current_receive_out_of_order_count++;
					}
				}
			}finally{
			
				synchronized( this ){
					
					long	unack_diff 	= current_receive_unack_in_sequence_count  - sent_receive_unack_in_sequence_count;
					long	oos_diff	= current_receive_out_of_order_count - sent_receive_out_of_order_count;
					
					if ( 	unack_diff > RECEIVE_UNACK_IN_SEQUENCE_LIMIT || 
							oos_diff  > RECEIVE_OUT_OF_ORDER_ACK_LIMIT ){
						
						sendAckCommand();
					}
					
						// TODO: only start timer if unack packet queue has something in it requesting ack?  
					
					if ( explicitack_ticks == 0 ){
							
						explicitack_ticks = EXPLICITACK_TICKS;
					}
				}
			}
		}
	}
	
	
	protected int
	sendCrypto(
		ByteBuffer[]		buffers,
		int					offset,
		int					length )
	
		throws IOException
	{
		// regardless of mss we have to get the first phe handshake messages into a single packet 

		int	payload_to_send	= 0;

		for (int i=offset;i<offset+length;i++){
			
			payload_to_send += buffers[i].remaining();
		}
		
		// first packet, cram it all in
		
		byte[]	packet_bytes = new byte[ payload_to_send ];
		
		ByteBuffer packet_buffer = ByteBuffer.wrap( packet_bytes );
		
		for (int i=offset;i<offset+length;i++){
			
			packet_buffer.put( buffers[i] );
		}
			
		UDPPacket packet = new UDPPacket( lead_connection, new int[]{ -1, -1, -1, -1 }, COMMAND_CRYPTO, packet_bytes, 0 );	
		
		log( "sendCrypto: seq=" + packet.getSequence() + ", len=" + payload_to_send );

		send( packet );
	
		return( payload_to_send );
	}
	
	protected void
	receiveCrypto(
		ByteBuffer		buffer )
	{
		boolean	new_connection = false;
		
		UDPConnection	connection = null;
		
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
					
		if ( new_connection ){
			
			manager.accept( local_port, remote_address, connection );
		}
			
		log( "readCrypto(" + connection.getID() + ") loc="+local_port + " - " + remote_address + ",rem="+ buffer.remaining() + "[" + Debug.getCompressedStackTrace());

		connection.receive( buffer );
	}

	
	protected int
	sendDataCommand(
		UDPConnection		connection,
		ByteBuffer[]		buffers,
		int					offset,
		int					length )
	
		throws IOException
	{
		int	payload_to_send	= 0;

		for (int i=offset;i<offset+length;i++){
			
			payload_to_send += buffers[i].remaining();
		}
		
		byte[]	header = new byte[256];
		
		ByteBuffer	header_buffer = ByteBuffer.wrap( header );
		
		long	unack_in_sequence_count	= current_receive_unack_in_sequence_count;
		
		int[]	sequence_numbers = writeHeaderStart( header_buffer, COMMAND_DATA );
		
		header_buffer.putInt( connection.getID());
		
		int header_size = writeHeaderEnd( header_buffer );
					
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
			
		UDPPacket packet = new UDPPacket( connection, sequence_numbers, COMMAND_DATA, packet_bytes, unack_in_sequence_count );
		

		log( "sendData(" + connection.getID() + ") loc="+local_port + " - " + remote_address + ",seq=" + packet.getSequence() + ",data="+ payload_to_send );

		send( packet );
	
		return( payload_to_send );
	}
	
	protected void
	receiveDataCommand(
		int				sequence,
		ByteBuffer		buffer,
		int				header_length )
	
		throws IOException
	{
		int	connection_id = buffer.getInt();
		
		UDPConnection	connection 		= null;
		boolean			new_connection 	= false;
		
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
		
		buffer.position( header_length );
		
		if ( new_connection ){
			
			manager.accept( local_port, remote_address, connection );
		}
			
		log( "receiveData(" + connection.getID() + ") loc="+local_port + " - " + remote_address + ",seq=" + sequence + ",data="+ buffer.remaining());

		connection.receive( buffer );
	}
	
	
	
	protected void
	sendAckCommand()
	
		throws IOException
	{
		synchronized( this ){
			
				// if there's already an ACK packet outstanding then we just resend that one
			
			Iterator	it = transmit_unack_packets.iterator();
			
			while( it.hasNext()){
				
				UDPPacket	packet = (UDPPacket)it.next();
				
				if ( packet.getCommand() == COMMAND_ACK ){
					
					if ( total_tick_count - packet.getSendTickCount() >= EXPLICITACK_TICKS ){
					
						log( "retransAck " + getName() + ":" + packet.getString());
					
						send( packet );
					}
					
					return;
				}
			}
			
			sent_receive_out_of_order_count		= current_receive_out_of_order_count;
			
			byte[]	header_bytes = new byte[256 + (RECEIVE_OUT_OF_ORDER_PACKETS_MAX+1)*4];
			
			ByteBuffer	header = ByteBuffer.wrap( header_bytes );
			
			long	unack_in_sequence_count	= current_receive_unack_in_sequence_count;

			int[]	sequences = writeHeaderStart( header, COMMAND_ACK );
			
			it = receive_out_of_order_packets.iterator();
			
			String	oos_str = "";
			
			while( it.hasNext()){
				
				Object[]	entry = (Object[])it.next();
			
				if ( entry[2] != null ){
				
					int	out_of_order_seq = ((Integer)entry[0]).intValue();
					int	out_of_rep_seq = ((Integer)entry[1]).intValue();
								
					oos_str += (oos_str.length()==0?"":",") + out_of_order_seq + "/" + out_of_rep_seq;
					
					header.putInt(out_of_order_seq);
				}
			}
			
			header.putInt( -1 );
			
			int	size = writeHeaderEnd( header );
			
			byte[]	packet_bytes = new byte[size];
			
			System.arraycopy( header_bytes, 0, packet_bytes, 0, size );
			
			log( "sendAck: in_seq=" + receive_last_inorder_sequence + ",out_of_seq=" + oos_str );
			
			send( new UDPPacket( lead_connection, sequences, COMMAND_ACK, packet_bytes, unack_in_sequence_count ));
		}
	}

	protected void
	receiveAckCommand(
		ByteBuffer		buffer,
		int				in_order_seq )
	
		throws IOException
	{
		synchronized( this ){
			
			String	oos_str = "";
			
			List	resend_list = new ArrayList();
			
			while( resend_list.size() < MAX_CONTIGUOUS_RETRANS ){
				
				int	out_of_order_seq = buffer.getInt();
				
				if ( out_of_order_seq == -1 ){
					
					break;
					
				}else{
										
					Iterator it = transmit_unack_packets.iterator();
					
					while( it.hasNext()){
						
						UDPPacket	packet = (UDPPacket)it.next();
						
						if ( packet.getSequence() == out_of_order_seq ){
							
								// can't remove the packet here as its presence is required to allow an in-order
								// ack to correctly remove prior packets
							
							packet.setHasBeenReceived();
							
							break;
						}
								
						if ( total_tick_count - packet.getSendTickCount() >= MIN_RETRANSMIT_TICKS ){
							
							if ( !resend_list.contains( packet )){
									
								resend_list.add( packet );
							}
						}
					}
										
					oos_str += (oos_str.length()==0?"":",") + out_of_order_seq;
	
				}
			}
	
			for (int i=0;i<resend_list.size();i++){
				
				send((UDPPacket)resend_list.get(i));
			}
			
			log( "receiveAck: in_seq=" + in_order_seq + ",out_of_seq=" + oos_str );
		}
	}
	
	
	
	protected int[]
	writeHeaderStart(
		ByteBuffer	buffer,
		byte		command )
	
		throws IOException
	{		
		// packet format
	
		// int[3] sequence numbers
		// short header length starting from version
		// byte version
		// byte flags
		// int	last in-order sequence received
		// byte command: data, close, re-request
		// 
		// command specific data
		//     	data: connection_id
		//		close: connection_id....
		// header checksum
		// payload
	
		int[]	sequence_numbers = getNextSequenceNumber( sequence_out, seq_cipher_out );
		
		int	seq = sequence_numbers[1];
		
		buffer.putInt( sequence_numbers[0] );
		buffer.putInt( seq );
		buffer.putInt( sequence_numbers[2] );
		
			// insert space for length added later
				
		buffer.putShort((short)0 );
				
		buffer.put((byte)PROTOCOL_VERSION);
		buffer.put((byte)0);
		
		buffer.putInt( receive_last_inorder_sequence );
		
		buffer.put((byte)command);

		return( sequence_numbers );
	}

	protected int
	writeHeaderEnd(
		ByteBuffer	buffer )
	
		throws IOException
	{
		short	total_length = (short)buffer.position();
		
		buffer.position( 12 );
		
		buffer.putShort((short)( total_length + 4 ));
		
			// hash includes real sequence + header content but obviously not including the hash
		
		byte[] buffer_bytes = buffer.array();
		
		SHA1Hasher	hasher = new SHA1Hasher();
		
		hasher.update( buffer_bytes, 4, 4 );
		hasher.update( buffer_bytes, 12, total_length - 12 );
			
		byte[] hash = hasher.getDigest();

		buffer.position( total_length );
		
		buffer.put( hash, 0, 4 );
		
		total_length += 4;
		
			// don't encrypt the sequence numbers
		
		header_cipher_out.processBytes( buffer_bytes, 12, total_length-12, buffer_bytes, 12 );
		
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
		
		if ( sent_packet_count == 0 ){
			
			return( sendCrypto( buffers, offset, length ));
			
		}else{
		
			return( sendDataCommand( connection, buffers, offset, length ));
		}
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
		
		return( transmit_unack_packets.size() < MAX_TRANSMIT_UNACK_PACKETS );
	}
	
	public void
	close(
		UDPConnection	connection,
		String			reason )
	{
		log( "UDPConnection::close(" + connection.getID() + "): " + reason );
		
		manager.remove( this, connection );
	}
	
	protected void
	failed(
		Throwable e )
	{
		synchronized( connections ){

			List	conns = new ArrayList( connections.values());
		
			for (int i=0;i<conns.size();i++){
				
				((UDPConnection)conns.get(i)).close( Debug.getNestedExceptionMessage(e));
			}
		}
	}
	
	static void
	forDocumentation()
	{
			// this is here to draw attention to the fact that there's a dependency between packet formats...
		
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
		
			// a secondary identifier for the sequence is also generated to be used in header position
			// 0-2 and 8-10 when reporting last sequences in the clear
		
			// TODO: add some history to prevent duplicates within, say, 512 period
		
		final int	mask = 0xfffff800;
		
		while( true ){
			
			int	seq1 = generator.nextInt();
			int	seq2 = generator.nextInt();
			int	seq3 = generator.nextInt();
			int	seq4 = generator.nextInt();
			
			seq1 = cipherInt( cipher, seq1 );
			seq2 = cipherInt( cipher, seq2 );
			seq3 = cipherInt( cipher, seq3 );
			seq4 = cipherInt( cipher, seq4 );
			
			if (( seq1 & mask ) != 0 && seq2 != -1 && ( seq3 & mask ) != 0){
				
				if ( (seq4 & 0xffff0000) != 0 && (seq4 & 0x0000ffff) != 0 ){
					
					return( new int[]{ seq1, seq2, seq3, seq4 });
				}
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
	
	protected String
	getName()
	{
		return( "loc="+local_port + " - " + remote_address );
	}
	
	protected void
	log(
		String		str )
	{
		if ( LOG ){
			String timeStamp =
				"[" +  DateFormat.getDateTimeInstance().format(new Date()) + "] ";        
	
			System.out.println( timeStamp + ": " + str );
		}
	}
}
