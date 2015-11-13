/*
 * Created on May 23, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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
 */


package com.aelitis.azureus.core.util;

import java.io.IOException;
import java.io.InputStream;

public class 
UncloseableInputStream 
	extends InputStream
{
	private InputStream		is;
	
	private boolean			closed;
	
	public
	UncloseableInputStream(
		InputStream		_is )
	{
		is		= _is;
	}
	
	@Override
	public int 
	read() 
		throws IOException 
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
	
		return( is.read());
	}

	public int 
	read(
		byte b[] )

		throws IOException 
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
	
		return( is.read( b ));
	}

	public int 
	read(
		byte b[], 
		int off, 
		int len) 

		throws IOException 
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
		
		return( is.read( b, off, len ));
	}

	public void 
	close() 
			
		throws IOException
	{
		closed	= true;
	}

	public long 
	skip(
		long n )
		
		throws IOException
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
		
		return( is.skip( n ));
	}

	public int 
	available()
	
		throws IOException
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
		
		return( is.available());
	}

	public void
	mark(
		int readlimit )
	{
		is.mark( readlimit );
	}

	public void 
	reset()
	
		throws IOException
	{
		if ( closed ){
			throw( new IOException( "Stream Closed" ));
		}
		
		is.reset();
	}

	public boolean 
	markSupported() 
	{
		return( false );	// lets say no to prevent interaction with delegated stream...
	}
	
	public boolean
	isClosed()
	{
		return( closed );
	}
}
