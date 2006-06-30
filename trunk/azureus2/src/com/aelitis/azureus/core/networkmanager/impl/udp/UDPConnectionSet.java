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
	private static final byte	COMMAND_CLOSE		= 3;
	
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
	private boolean					outgoing;
	
	private UDPConnection	lead_connection;
	private int				sent_packet_count;
		
	private RC4Engine		header_cipher_out;
	private RC4Engine		header_cipher_in;
	
	private SequenceGenerator	in_seq_generator;
	private SequenceGenerator	out_seq_generator;

	
	private volatile boolean	crypto_done;
	
	private volatile boolean	failed;
	
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
	private UDPPacket	current_retransmit_target;
	
	private static final int RETRANSMIT_COUNT_LIMIT	= 5;

	
	private static final int MIN_RETRANSMIT_TIMER	= 100;
	private static final int MIN_RETRANSMIT_TICKS	= Math.max( 1, MIN_RETRANSMIT_TIMER / UDPConnectionManager.TIMER_TICK_MILLIS );
	private static final int MAX_RETRANSMIT_TIMER	= 5000;
	private static final int MAX_RETRANSMIT_TICKS	= Math.max( 1, MAX_RETRANSMIT_TIMER / UDPConnectionManager.TIMER_TICK_MILLIS );

	
	private static final int 	MAX_TRANSMIT_UNACK_DATA_PACKETS	= 10;
	private static final int 	MAX_TRANSMIT_UNACK_PACKETS		= MAX_TRANSMIT_UNACK_DATA_PACKETS + 4;	// + protocol packets
	private List				transmit_unack_packets = new ArrayList();
		
	private static final int	MAX_CONTIGUOUS_RETRANS	= 3;
	
		// receive
		
	private int		receive_last_inorder_sequence		= -1;
	private int		receive_last_inorder_alt_sequence	= -1;
	
	private int		receive_their_last_inorder_sequence		= -1;

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

	private static final int MAX_SEQ_MEMORY = Math.max( RECEIVE_OUT_OF_ORDER_PACKETS_MAX, MAX_TRANSMIT_UNACK_PACKETS );

	
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
	
		throws IOException
	{
		UDPConnection	old_connection = null;
		
		synchronized( connections ){
			
			if ( failed ){
				
				throw( new IOException( "Connection set has failed" ));
			}
			
			old_connection =  (UDPConnection)connections.put( new Integer( connection.getID()), connection );
			
			if ( connections.size() == 1 && lead_connection == null ){
				
				lead_connection = connection;
				
				outgoing		= true;
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
	
			connections.remove( new Integer( connection.getID()));
			
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
					
				log( "crypto done" );
				
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
	
		 			out_seq_generator = new SequenceGenerator( new Random( bytesToLong( d_key )), rc4_engine_c, false );
		 			in_seq_generator  = new SequenceGenerator( new Random( bytesToLong( c_key )), rc4_engine_d, true );
 
	    		}else{
	    			
	       			header_cipher_out	= rc4_engine_b;
	    			header_cipher_in	= rc4_engine_a;
		 			
		 			in_seq_generator  = new SequenceGenerator( new Random( bytesToLong( d_key )), rc4_engine_c, true );
		 			out_seq_generator = new SequenceGenerator( new Random( bytesToLong( c_key )), rc4_engine_d, false );
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
		boolean	retrans_expired = false;
		boolean	ack_expired		= false;
		
		synchronized( this ){
			
			total_tick_count++;
			
			if ( retransmit_ticks > 0 ){
				
				retransmit_ticks--;
				
				if ( retransmit_ticks == 0 ){
					
					retrans_expired = true;
				}
			}
			
			if ( explicitack_ticks > 0 ){
				
				explicitack_ticks--;
				
				if ( explicitack_ticks == 0 ){
					
					ack_expired = true;
				}
			}
		}
		
		if ( retrans_expired ){
			
			retransmitExpired();
		}
			
		if ( ack_expired ){
			
			sendAckCommand();
		}
	}
	
	protected UDPPacket
	getRetransmitPacket()
	{
		Iterator	it = transmit_unack_packets.iterator();
		
		while( it.hasNext()){
			
			UDPPacket	p = (UDPPacket)it.next();
		
			if ( p.autoRetransmit() && !p.hasBeenReceived()){
			
				return( p );
			}
		}
		
		return( null );
	}
	
	protected int
	getRetransmitTicks( 
		int	resend_count )
	{
		int	res;
		
		if ( resend_count == 0 ){
			
			res = RETRANSMIT_TICKS;
			
		}else{
			
			res = RETRANSMIT_TICKS + (( MAX_RETRANSMIT_TICKS - RETRANSMIT_TICKS ) * resend_count ) / ( RETRANSMIT_COUNT_LIMIT-1 ); 
		}
		
		System.out.println( "retry: " + res );
		
		return( res );
	}
	
	protected void
	retransmitExpired()
	
		throws IOException
	{
		UDPPacket	packet_to_send = null;
		
		synchronized( this ){
			
			packet_to_send = getRetransmitPacket();
			
			if ( packet_to_send != null ){
				
				packet_to_send.resent();
			}
		}
		
		if ( packet_to_send != null ){
			
			log( "Retransmit: " + packet_to_send.getString());

			send( packet_to_send );
		}
	}
	
	protected void
	remoteLastInSequence(
		int			alt_sequence )
	{
			// if we find this packet then we can also discard any prior to it as they are implicitly
			// ack too
	
		synchronized( this ){

			for (int i=0;i<transmit_unack_packets.size();i++){
				
				UDPPacket	packet = (UDPPacket)transmit_unack_packets.get(i);
				
				if ( packet.getAlternativeSequence() == alt_sequence ){
							
					receive_their_last_inorder_sequence	= packet.getSequence();
					
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
			String	str = "State:";
			
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
			
			str += ",oo=" + oo;
			
			str += ",sent_data=" + total_data_sent +"/" + total_data_resent + ",sent_prot=" + total_protocol_sent + "/" + total_protocol_resent;
			
			log( str );
		}
	}
		
		
	
	
	protected void
	send(
		UDPPacket		packet )
	
		throws IOException
	{
		if ( failed ){
			
			throw( new IOException( "Connection set has failed" ));
		}
		
		byte[]	payload = packet.getBuffer();
				
		log( packet.getConnection(), "Write: " + packet.getString());

			// cache packet for resend
		
		sent_packet_count++;
				
		synchronized( this ){
			
			int	resend_count = packet.getResendCount();
			
			if ( resend_count > RETRANSMIT_COUNT_LIMIT ){
				
				throw( new IOException( "Packet resend limit exceeded" ));
			}
			
				// all packets carry an implicit ack, pick up the corresponding count here

			long	unackin = packet.getUnAckInSequenceCount();
			
			if ( unackin > 	sent_receive_unack_in_sequence_count ){
				
				sent_receive_unack_in_sequence_count	= unackin;
			}
			
				// trigger the retransmit timer if any sent packets have the auto-retransmit property 
			
			UDPPacket	retransmit_target = getRetransmitPacket();
			
			if ( retransmit_target == null ){
				
					// no auto-retransmit packet, cancel timer
				
				retransmit_ticks			= 0;
				
			}else if ( retransmit_target != current_retransmit_target ){
				
					// auto-retransmit packet has changed, reset timer
				
				retransmit_ticks = getRetransmitTicks( resend_count );

			}else{
				
					// current retry target timer expired, restart it
				
				if ( retransmit_ticks == 0 ){
					
					retransmit_ticks = getRetransmitTicks( resend_count );
				}
			}
			
			current_retransmit_target	= retransmit_target;

				// splice in the latest received in sequence alternative seq if non-crypto packet
			
			if ( packet.getAlternativeSequence() != -1 ){
				
				byte[]	alt = intToBytes( receive_last_inorder_alt_sequence );
				
				payload[0] = alt[0];
				payload[1] = alt[1];
				payload[8] = alt[2];
				payload[9] = alt[3];
			}
		
			int send_count = packet.sent( total_tick_count );

			if ( send_count == 1 ){
				
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
		}
		

		
		manager.send( local_port, remote_address, payload );
	}
	
	public void
	receive(
		byte[]			initial_data )
	
		throws IOException
	{
		if ( failed ){
			
			throw( new IOException( "Connection set has failed" ));
		}
		
		dumpState();
		
		log( "Read: total=" + initial_data.length );
		
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
			
			if ( outgoing ){
				
					// a reply received by the initiator acknowledges that the initial message sent has
					// been received
				
				remoteLastInSequence( -1 );
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
						
			remoteLastInSequence( alt );
			
			try{
				initial_buffer.getInt();	// seq1
				
				Integer	seq2 = new Integer( initial_buffer.getInt());
				
				initial_buffer.getInt();	// seq3
					
					// first see if we know about this sequence number already
				
				if ( receive_done_sequences.contains( seq2 )){
					
					log( "Duplicate processed packet: " + seq2 );
										
					return;
				}
				
				if ( !out_seq_generator.isValidAlterativeSequence( alt )){
									
					log( "Received invalid alternative sequence " + alt + " - dropping packet" );
				
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
						
						log( "Out-of-order packet entry data matched for seq " + seq2 );
						
							// got data matching out-of-order-entry, add it in!
												
						entry[2] = initial_buffer;
						
						oop = true;
						
						break;
					}
				}
				
				if ( !oop ){
					
						// not a known out-of-order packet. If our oop list is full then all we can do is drop
						// the packet
					
					boolean	added = false;
					
					while ( receive_out_of_order_packets.size() < RECEIVE_OUT_OF_ORDER_PACKETS_MAX ){
									
						int[]	seq_in = in_seq_generator.getNextSequenceNumber();
								
						if ( seq2.intValue() == seq_in[1] ){
								
							log( "Out-of-order packet entry adding for seq " + seq_in[1] );
														
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
						
						log( "Header length too large" );
						
						return;
					}
					
					header_cipher_in.processBytes( data, 14, header_len-14, data, 14 );
					
					SHA1Hasher	hasher = new SHA1Hasher();
					
					hasher.update( data, 4, 4 );
					hasher.update( data, 12, header_len - 4 - 12 );
									
					byte[]	hash = hasher.getDigest();
							
					for (int i=0;i<4;i++){
						
						if ( hash[i] != data[header_len-4+i] ){	
						
							log( "hash incorrect" );
							
							return;
						}
					}
					
					byte	version = buffer.get();
					
					if ( version != PROTOCOL_VERSION ){
						
						// throw( new IOException( "Invalid protocol version '" + version + "'" ));
					}
					
					byte	flags = buffer.get();
															
					byte	command = buffer.get();
					
					if ( command == COMMAND_DATA ){				
	
						receiveDataCommand( seq.intValue(), buffer, header_len );
							
					}else if ( command == COMMAND_ACK ){
						
						receiveAckCommand( buffer );
						
					}else if ( command == COMMAND_CLOSE ){
						
						receiveCloseCommand( buffer );
	
					}else{
					
						// ignore unrecognised commands to support future change
					}
				}
				
				if ( this_is_oop ){
					
					synchronized( this ){
					
						current_receive_out_of_order_count++;
					}
				}
			}finally{
			
				boolean	send_ack = false;
				
				synchronized( this ){
					
					long	unack_diff 	= current_receive_unack_in_sequence_count  - sent_receive_unack_in_sequence_count;
					long	oos_diff	= current_receive_out_of_order_count - sent_receive_out_of_order_count;
					
					if ( 	unack_diff > RECEIVE_UNACK_IN_SEQUENCE_LIMIT || 
							oos_diff  > RECEIVE_OUT_OF_ORDER_ACK_LIMIT ){
						
						send_ack = true;
					}
				}
				
				if ( send_ack ){
					
					sendAckCommand();
				}
					
				synchronized( this ){

						// if we have either received in-order packets that we haven't sent an ack for or
						// out-of-order packets start the ack timer
					
					long	unack_diff 	= current_receive_unack_in_sequence_count  - sent_receive_unack_in_sequence_count;
						
					if ( unack_diff > 0 || receive_out_of_order_packets.size() > 0 ){
						
						if ( explicitack_ticks == 0 ){
							
							explicitack_ticks = EXPLICITACK_TICKS;
						}
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
			
		UDPPacket packet_to_send = new UDPPacket( lead_connection, new int[]{ -1, -1, -1, -1 }, COMMAND_CRYPTO, packet_bytes, 0 );	
		
		synchronized( this ){
			
			transmit_unack_packets.add( packet_to_send );
		}
		
		log( "sendCrypto: seq=" + packet_to_send.getSequence() + ", len=" + payload_to_send );

		send( packet_to_send );
	
		return( payload_to_send );
	}
	
	protected void
	receiveCrypto(
		ByteBuffer		buffer )
	
		throws IOException
	{
		boolean	new_connection = false;
		
		UDPConnection	connection = null;
		
		synchronized( connections ){

			if ( failed ){
				
				throw( new IOException( "Connection set has failed" ));
			}
			
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
			
		log( connection, "readCrypto: rem="+ buffer.remaining());

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
		
		UDPPacket	packet_to_send;
		
		synchronized( this ){
			
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
				
			packet_to_send = new UDPPacket( connection, sequence_numbers, COMMAND_DATA, packet_bytes, unack_in_sequence_count );
			
			transmit_unack_packets.add( packet_to_send );
		}

		log( connection, "sendData: seq=" + packet_to_send.getSequence() + ",data="+ payload_to_send );

		send( packet_to_send );
	
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
			
			if ( failed ){
				
				throw( new IOException( "Connection set has failed" ));
			}
			
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
			
		log( connection, "receiveData: seq=" + sequence + ",data="+ buffer.remaining());

		connection.receive( buffer );
	}
	
	
	
	protected void
	sendAckCommand()
	
		throws IOException
	{
		UDPPacket	packet_to_send = null;

		synchronized( this ){
									
				// if there's already an ACK packet outstanding then we just resend that one
			
			Iterator	it = transmit_unack_packets.iterator();
			
			while( it.hasNext()){
				
				UDPPacket	packet = (UDPPacket)it.next();
				
				if ( packet.getCommand() == COMMAND_ACK ){
					
					if ( total_tick_count - packet.getSendTickCount() >= EXPLICITACK_TICKS ){
					
						log( packet.getConnection(), "retransAck:" + packet.getString());
					
						packet_to_send	= packet;
						
						break;
						
					}else{
					
							// sent too recently, bail out
						
						return;
					}
				}
			}
			
			if ( packet_to_send == null ){
			
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
								
				packet_to_send = new UDPPacket( lead_connection, sequences, COMMAND_ACK, packet_bytes, unack_in_sequence_count );
									
				transmit_unack_packets.add( packet_to_send );

				log( lead_connection, "sendAck: in_seq=" + receive_last_inorder_sequence + ",out_of_seq=" + oos_str );
			}
		}
		
		if ( packet_to_send != null ){
				
			send( packet_to_send );
		}
	}

	protected void
	receiveAckCommand(
		ByteBuffer		buffer )
	
		throws IOException
	{
		List	resend_list = new ArrayList();

		String	oos_str = "";

		synchronized( this ){	
				
			Iterator it = transmit_unack_packets.iterator();

			while( resend_list.size() < MAX_CONTIGUOUS_RETRANS ){
				
				int	out_of_order_seq = buffer.getInt();
				
				if ( out_of_order_seq == -1 ){
					
					break;
					
				}else{
						
					oos_str += (oos_str.length()==0?"":",") + out_of_order_seq;

					while( it.hasNext() && resend_list.size() < MAX_CONTIGUOUS_RETRANS ){
						
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
				}
			}
		}
		
		log( "receiveAck: in_seq=" + receive_their_last_inorder_sequence + ",out_of_seq=" + oos_str );

		for (int i=0;i<resend_list.size();i++){
				
			send((UDPPacket)resend_list.get(i));
		}			
	}
	
	
	protected void
	sendCloseCommand(
		UDPConnection		connection )
	
		throws IOException
	{
		if ( crypto_done ){
			
			UDPPacket	packet_to_send;
			
			synchronized( this ){
				
				byte[]	header_bytes = new byte[256];
				
				ByteBuffer	header = ByteBuffer.wrap( header_bytes );
				
				long	unack_in_sequence_count	= current_receive_unack_in_sequence_count;
		
				int[]	sequences = writeHeaderStart( header, COMMAND_CLOSE );
				
				header.putInt( connection.getID());
				
				int	size = writeHeaderEnd( header );
				
				byte[]	packet_bytes = new byte[size];
				
				System.arraycopy( header_bytes, 0, packet_bytes, 0, size );
				
				log( connection, "sendClose" );
				
				packet_to_send = new UDPPacket( lead_connection, sequences, COMMAND_CLOSE, packet_bytes, unack_in_sequence_count );
				
				transmit_unack_packets.add( packet_to_send );
			}
			
			send( packet_to_send );
				
		}else{
			
			log( "sendClose: crypto not setup" );
		}
	}
	
	protected void
	receiveCloseCommand(
		ByteBuffer		buffer )
	
		throws IOException
	{
		int	connection_id = buffer.getInt();
		
		UDPConnection	connection 		= null;
		
		synchronized( connections ){
			
			if ( failed ){
				
				throw( new IOException( "Connection set has failed" ));
			}
			
			connection = (UDPConnection)connections.get( new Integer( connection_id ));
		}
	
		log( "receiveClose: con=" + (connection==null?"<null>":(""+connection.getID())));
		
		if ( connection != null ){
			
			connection.close( "Remote has closed the connection" );
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
	
		int[]	sequence_numbers = out_seq_generator.getNextSequenceNumber();
		
		int	seq = sequence_numbers[1];
		
		buffer.putInt( sequence_numbers[0] );
		buffer.putInt( seq );
		buffer.putInt( sequence_numbers[2] );
		
			// insert space for length added later
				
		buffer.putShort((short)0 );
				
		buffer.put((byte)PROTOCOL_VERSION);
		buffer.put((byte)0);
				
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
		
		return( transmit_unack_packets.size() < MAX_TRANSMIT_UNACK_DATA_PACKETS );
	}
	
	public void
	close(
		UDPConnection	connection,
		String			reason )
	{
		log( connection, "close: " + reason );
		
		boolean	found;
		
		synchronized( connections ){
			
			found = connections.containsValue( connection );
		}
		
		if ( found ){
			
			try{
				sendCloseCommand( connection );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}

			// final poll incase there are ignorant listeners
		
		connection.poll();

		manager.remove( this, connection );
	}
	
	public void
	failed(
		UDPConnection	connection,
		Throwable		reason )
	{
		log( connection, "Failed: " + Debug.getNestedExceptionMessage(reason));
	
			// run a final poll operation to inform any selector listeners of the failure
			
		connection.poll();
		
		manager.remove( this, connection );
	}
	
	protected void
	failed(
		Throwable e )
	{
		List	conns;
		
		synchronized( connections ){
			
			if ( !failed ){
				
				log( "Connection set failed: " + Debug.getNestedExceptionMessage( e ));
			
				failed	= true;
			}

			conns = new ArrayList( connections.values());
		}
		
		for (int i=0;i<conns.size();i++){
			
			try{
				((UDPConnection)conns.get(i)).failed( e );
				
			}catch( Throwable f ){
				
				Debug.printStackTrace(f);
			}
		}
		
		manager.failed( this );
	}
	
	protected boolean
	hasFailed()
	{			
		return( failed );
	}
	
	static void
	forDocumentation()
	{
			// this is here to draw attention to the fact that there's a dependency between packet formats...
		
		PRUDPPacketReply.registerDecoders( new HashMap());
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
	
			System.out.println( timeStamp + ": " + getName() + ": " + str );
		}
	}
	
	protected void
	log(
		UDPConnection	connection,
		String			str )
	{
		if ( LOG ){
			String timeStamp =
				"[" +  DateFormat.getDateTimeInstance().format(new Date()) + "] ";        
	
			System.out.println( timeStamp + ": " + getName() + " (" + connection.getID() + "): " + str );
		}
	}
	protected class
	SequenceGenerator
	{
		private Random		generator;
		private RC4Engine	cipher;
		private boolean		in;
		
		private final int[]	seq_memory;
		private final int[]	alt_seq_memory;
		private int seq_memory_pos;

		private static final boolean	DEBUG_SEQUENCES	= true;
		
		private int debug_seq_in_next	= outgoing?0:1000000;
		private int debug_seq_out_next	= outgoing?1000000:0;

		protected
		SequenceGenerator(
			Random		_generator,
			RC4Engine	_cipher,
			boolean		_in )
		{
			generator	= _generator;
			cipher		= _cipher;
			in			= _in;
			
			seq_memory		= new int[MAX_SEQ_MEMORY];
			alt_seq_memory	= new int[MAX_SEQ_MEMORY];
			
			Arrays.fill( seq_memory, -1 );
			Arrays.fill( alt_seq_memory, -1 );
		}
		
		protected synchronized int[]
      	getNextSequenceNumber()
      	{
      			// damn tracker udp protocol has:
      			// request: long (random connection id) int (action)
      			// reply: int (action) int (random txn id)
      		
      			// now action is always < 2048 so all other uses of udp packets will have either
      			// 0x000007ff in either bytes 9 or 0 onwards. So we're forced to use 12 byte sequence numbers
      		
      			// internally we use the middle integer as the packet sequence
      		
      			// a secondary identifier for the sequence is also generated to be used in header position
      			// 0-2 and 8-10 when reporting last sequences in the clear
      				
      		final int	mask = 0xfffff800;
      		
      		while( true ){
      			
      			int	seq1;
      			int	seq2;
      			int	seq3;
      			int	seq4;
      			
      			if ( DEBUG_SEQUENCES ){
      				
      				if ( in ){
      					seq1 = 0xffffffff;
      					seq2 = debug_seq_in_next;
      					seq3 = 0xffffffff;
      					seq4 = debug_seq_in_next;
      					
      					debug_seq_in_next++;
      				}else{
      					seq1 = 0xffffffff;
      					seq2 = debug_seq_out_next;
      					seq3 = 0xffffffff;
      					seq4 = debug_seq_out_next;
      					
      					debug_seq_out_next++;
      				}
      				
      				
      			}else{
      				seq1 = generator.nextInt();
      				seq2 = generator.nextInt();
      				seq3 = generator.nextInt();
      				seq4 = generator.nextInt();
      			
      				seq1 = cipherInt( cipher, seq1 );
      				seq2 = cipherInt( cipher, seq2 );
      				seq3 = cipherInt( cipher, seq3 );
      				seq4 = cipherInt( cipher, seq4 );
      			}
      			
  				if (( seq1 & mask ) != 0 && seq2 != -1 && ( seq3 & mask ) != 0){
  					
  					if ( (seq4 & 0xffff0000) != 0 && (seq4 & 0x0000ffff) != 0 ){
  						
  						boolean	bad	= false;
  						
  						for (int i=0;i<MAX_SEQ_MEMORY;i++){
  							
  							if ( seq_memory[i] == seq2 || alt_seq_memory[i] == seq4 ){
  								
  								bad	= true;
  								
  								break;
  							}
  						}
  						
  						if ( !bad ){
  							
  							seq_memory[seq_memory_pos]			= seq2;
  							
  							alt_seq_memory[seq_memory_pos++]	= seq4;
  							
  							if ( seq_memory_pos == MAX_SEQ_MEMORY ){
  								
  								seq_memory_pos = 0;
  							}
  							
  							return( new int[]{ seq1, seq2, seq3, seq4 });
  						}
  					}
  				}
  			}
      	}
      	
		protected boolean
      	isValidAlterativeSequence(
      		int		seq )
		{
			for (int i=0;i<MAX_SEQ_MEMORY;i++){
				
				if ( alt_seq_memory[i] == seq ){
					
					return( true );
				}
			}
			
			return( false );
      	}
	}
}
