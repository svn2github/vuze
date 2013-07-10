/*
 * Created on 21-Mar-2006
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

package com.aelitis.azureus.core.util;

import java.util.*;
import java.io.*;

public class 
LinkFileMap 
{
	private Map<wrapper,Entry>	name_map = new HashMap<wrapper,Entry>();
	
	public File
	get(
		int			index,
		File		from_file )
	{
		Entry entry = name_map.get( new wrapper( from_file ));
		
		if ( entry == null ){
			
			return( null );
		}
		
		return( entry.getToFile());
	}
	
	public void
	put(
		int			index,
		File		from_file,
		File		to_file )
	{
		name_map.put( new wrapper( from_file ), new Entry( index, from_file, to_file ));
	}
	
	public void
	remove(
		int			index,
		File		key )
	{
		name_map.remove( new wrapper( key ));
	}
	
	public Iterator<Entry>
	entryIterator()
	{
		return( name_map.values().iterator());
	}
	
	public static class
	Entry
	{
		private int		index;
		private File	from_file;
		private File	to_file;
		
		private
		Entry(
			int		_index,
			File	_from_file,
			File	_to_file )
		{
			index		= _index;
			from_file	= _from_file;
			to_file		= _to_file;
		}
		
		public int
		getIndex()
		{
			return( index );
		}
		
		public File
		getFromFile()
		{
			return( from_file );
		}
		
		public File
		getToFile()
		{
			return( to_file );
		}
	}
	
	private static class
	wrapper
	{
		private String		file_str;
		
		protected
		wrapper(
			File	file )
		{
			file_str	= file.toString();
		}
		
		
		public boolean
		equals(
			Object	other )
		{
			if ( other instanceof wrapper ){
				
				return( file_str.equals(((wrapper)other).file_str ));
			}
			
			return( false );
		}
		
		public int
		hashCode()
		{
			return( file_str.hashCode());
		}
	}
}
