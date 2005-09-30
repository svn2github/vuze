/*
 * Created on 28-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.diskmanager.file.impl;

import java.io.File;
import java.io.RandomAccessFile;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class 
FMFileAccessCompact
	implements FMFileAccess
{
	private File				control_file;
	private FMFileAccess		delegate;
	
	protected
	FMFileAccessCompact(
		File			_control_file,
		FMFileAccess	_delegate )
	
		throws FMFileManagerException
	{
		control_file	= _control_file;
		delegate		= _delegate;
		
		try{
			if ( !control_file.exists()){
				
				control_file.getParentFile().mkdirs();
			
				control_file.createNewFile();
			}
		}catch( Throwable e ){
			
			throw( new FMFileManagerException( "createNewFile fails", e ));
		}
	}
	
	public long
	getLength(
		RandomAccessFile		raf )
	
		throws FMFileManagerException
	{
		long	length = delegate.getLength( raf );
		
		System.out.println( "compact: getLength - " + length );

		return( length );
	}
	
	public void
	setLength(
		RandomAccessFile		raf,
		long					length )
	
		throws FMFileManagerException
	{
		System.out.println( "compact: setLength - " + length );

		delegate.setLength( raf, length );
	}
	
	public void
	read(
		RandomAccessFile	raf,
		DirectByteBuffer	buffer,
		long				position )
	
		throws FMFileManagerException
	{
		System.out.println( "compact: read - " + position );

		delegate.read( raf, buffer, position );
	}
	
	public void
	write(
		RandomAccessFile		raf,
		DirectByteBuffer[]		buffers,
		long					position )
	
		throws FMFileManagerException
	{
		System.out.println( "compact: write - " + position );
		
		delegate.write( raf, buffers, position );
	}
	
	public void
	flush()
	
		throws FMFileManagerException
	{
		// save control file!
		
		System.out.println( "compact: flush" );
	}
}
