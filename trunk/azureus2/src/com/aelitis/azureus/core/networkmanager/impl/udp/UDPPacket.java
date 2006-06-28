/*
 * Created on 26 Jun 2006
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


public class 
UDPPacket 
{
	private final UDPConnection		connection;
	private final int				sequence;
	private final int				alt_sequence;
	private final byte				command;
	private final byte[]			buffer;
	private final long				unack_in_sequence_count;
	
	private boolean auto_retransmit			= true;
	private int		sent_count;
	private boolean received;
	private long	send_tick_count;
	
	protected
	UDPPacket(
		UDPConnection	_connection,
		int[]			_sequences,
		byte			_command,
		byte[]			_buffer,
		long			_unack_in_sequence_count )
	{
		connection		= _connection;
		sequence		= _sequences[1];
		alt_sequence	= _sequences[3];
		command			= _command;
		buffer			= _buffer;
		
		unack_in_sequence_count	= _unack_in_sequence_count;
	}
		
	protected UDPConnection
	getConnection()
	{
		return( connection );
	}
	
	protected int
	getSequence()
	{
		return( sequence );
	}
	
	protected int
	getAlternativeSequence()
	{
		return( alt_sequence );
	}
	
	protected byte
	getCommand()
	{
		return( command );
	}
	
	protected byte[]
	getBuffer()
	{
		return( buffer );
	}
	
	protected long
	getUnAckInSequenceCount()
	{
		return( unack_in_sequence_count );
	}
	
	protected boolean
	autoRetransmit()
	{
		return( auto_retransmit );
	}
	
	protected int
	sent(
		long	tick_count )
	{
		sent_count++;
		
		send_tick_count = tick_count;
		
		return( sent_count );
	}
	
	protected long
	getSendTickCount()
	{
		return( send_tick_count );
	}
	
	protected void
	setHasBeenReceived()
	{
		received	= true;
	}
	
	protected boolean
	hasBeenReceived()
	{
		return( received );
	}
	
	protected int
	getSentCount()
	{
		return( sent_count );
	}
	
	protected String
	getString()
	{
		return( "seq=" + sequence + ",type=" + command + ",sent=" + sent_count +",len=" + buffer.length );
	}
}
