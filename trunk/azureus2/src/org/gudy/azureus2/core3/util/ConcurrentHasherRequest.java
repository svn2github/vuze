/*
 * Created on 09-Sep-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util;

import java.nio.ByteBuffer;

/**
 * @author parg
 *
 */

public class 
ConcurrentHasherRequest 
{
	protected ConcurrentHasher						concurrent_hasher;
	protected ByteBuffer							buffer;
	protected long									priority;
	protected ConcurrentHasherRequestListener		listener;

	protected byte[]								result;
	protected boolean								cancelled;
	
	protected AESemaphore	sem = new AESemaphore("ConcHashRequest");
	
	protected
	ConcurrentHasherRequest(
		ConcurrentHasher					_concurrent_hasher,
		ByteBuffer							_buffer,
		long								_priority,
		ConcurrentHasherRequestListener		_listener )
	{
		concurrent_hasher	= _concurrent_hasher;
		buffer				= _buffer;
		priority			= _priority;
		listener			= _listener;
	}
	
		/**
		 * synchronously get the result of the hash - null returned if it is cancelled
		 * @return
		 */
	
	public byte[]
	getResult()
	{
		sem.reserve();
		
		return( result );
	}
	
		/**
		 * cancel the hash request. If it is cancelled before it is completed then
		 * a subsequent call to getResult will return null
		 */
	
	public void
	cancel()
	{	
		cancelled	= true;
		
		if ( listener != null ){
			
			listener.complete( this );
		}
		
		sem.releaseForever();
	}
	
	public boolean
	getCancelled()
	{
		return( cancelled );
	}
	public long
	getPriority()
	{
		return( priority );
	}
	
	protected void
	run(
		SHA1Hasher	hasher )
	{
		if ( !cancelled ){
			
			result = hasher.calculateHash( buffer );
		
			if ( listener != null ){
				
				listener.complete( this );
			}
			
			sem.releaseForever();
		}
	}
}
