/*
 * File    : TOTorrentFactory.java
 * Created : 5 Oct. 2003
 * By      : Parg 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.torrent;


import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.impl.*;

public class 
TOTorrentFactory 
{	
	public static final long	TO_DEFAULT_FIXED_PIECE_SIZE = 256*1024;
	
	public static final long	TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN = 32*1024;
	public static final long	TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX = 4*1024*1024;
	
	public static final long	TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER = 1024;
	public static final long	TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER = 2048;
	
		// deserialisation methods
		
	public static TOTorrent
	deserialiseFromBEncodedFile(
		File		file )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( file ));
	}
	
	public static TOTorrent
	deserialiseFromMap(
		Map			data )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( data ));
	}
	
	public static TOTorrent
	deserialiseFromXMLFile(
		File		file )
		
		throws TOTorrentException
	{
		return( new TOTorrentXMLDeserialiser().deserialise( file ));
	}

		// construction methods: fixed piece size
		
	public static TOTorrent
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url )
		
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, TO_DEFAULT_FIXED_PIECE_SIZE, null ));
	}
	
	public static TOTorrent
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_length )
		
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, piece_length, null ));
	}
	
	
	public static TOTorrent
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		TOTorrentProgressListener	progress_listener )
		
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, TO_DEFAULT_FIXED_PIECE_SIZE, null ));
	}	
	
	public static TOTorrent
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_length,
		TOTorrentProgressListener	progress_listener )
		
		throws TOTorrentException
	{
		return( new TOTorrentCreateImpl( file, announce_url, piece_length, progress_listener ));
	}
	
	// construction methods: variable piece size

	public static TOTorrent
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url )
			
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength( 
					file, 
					announce_url,
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN, 
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX,
					TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER,
					TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER,
					null ));
	}
		
	public static TOTorrent
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_min_size,
		long						piece_max_size,
		long						piece_num_lower,
		long						piece_num_upper )
			
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength( 
					file, announce_url, piece_min_size, piece_max_size,
					piece_num_lower, piece_num_upper, null ));
	}
		
		
	public static TOTorrent
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		TOTorrentProgressListener	progress_listener )
			
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength( 
					file, 
					announce_url,
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN, 
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX,
					TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER,
					TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER,
					progress_listener ));	
	}	
		
	public static TOTorrent
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_min_size,
		long						piece_max_size,
		long						piece_num_lower,
		long						piece_num_upper,
		TOTorrentProgressListener	progress_listener )
			
		throws TOTorrentException
	{
		return( new TOTorrentCreateImpl(
						file, announce_url, piece_min_size, piece_max_size,
						piece_num_lower, piece_num_upper, progress_listener ));

	}
}
