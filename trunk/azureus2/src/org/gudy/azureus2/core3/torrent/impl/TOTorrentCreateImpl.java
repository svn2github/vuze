/*
 * File    : TOTorrentCreateImpl.java
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

package org.gudy.azureus2.core3.torrent.impl;

import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentCreateImpl
	extends		TOTorrentImpl
	implements	TOTorrentFileHasherListener
{	
	protected TOTorrentFileHasher			file_hasher;
	
	protected long	total_file_size		= -1;
	protected long	total_file_count	= 0;
	
	protected long	piece_count;
	
	protected TOTorrentProgressListener		progress_listener;
	
	protected int	reported_progress;
		
	public
	TOTorrentCreateImpl(
		File						_torrent_base,
		URL							_announce_url,
		long						_piece_length,
		TOTorrentProgressListener	_progress_listener )
		
		throws TOTorrentException
	{
		super( _torrent_base.getName(), _announce_url, _torrent_base.isFile());
		
		progress_listener = _progress_listener;
		
		constructFixed( _torrent_base, _piece_length );
	}
	
	public
	TOTorrentCreateImpl(	
		File						_torrent_base,
		URL							_announce_url,
		long						_piece_min_size,
		long						_piece_max_size,
		long						_piece_num_lower,
		long						_piece_num_upper,
		TOTorrentProgressListener	_progress_listener )
		
		throws TOTorrentException
	{
		super( _torrent_base.getName(), _announce_url, _torrent_base.isFile());
		
		progress_listener = _progress_listener;
		
		long	total_size = calculateTotalFileSize( _torrent_base );
		
		long	piece_length = getComputedPieceSize( total_size, _piece_min_size, _piece_max_size, _piece_num_lower, _piece_num_upper );
		
		constructFixed( _torrent_base, piece_length );
	}
	
	protected void
	constructFixed(
		File		_torrent_base,
		long		_piece_length )
	
		throws TOTorrentException
	{
		setCreationDate( System.currentTimeMillis() / 1000);
		
		setCreatedBy( Constants.AZUREUS_NAME + "/" + Constants.AZUREUS_VERSION );
		
		setPieceLength( _piece_length );
		
		report( MessageText.getString("Torrent.create.progress.piecelength") + DisplayFormatters.formatByteCountToKBEtc(_piece_length ));
		
		TOTorrentFileHasher	hasher = new TOTorrentFileHasher((int)_piece_length, progress_listener==null?null:this );
		
		piece_count = calculateNumberOfPieces( _torrent_base,_piece_length );
		
		if ( piece_count == 0 ){
			
			throw( new TOTorrentException( "TOTorrentCreate: specified files have zero total length",
											TOTorrentException.RT_ZERO_LENGTH ));
		}
		
		report( MessageText.getString("Torrent.create.progress.hashing"));

		if ( getSimpleTorrent()){
							
			long length = hasher.add( _torrent_base );
		
			setFiles( new TOTorrentFileImpl[]{ new TOTorrentFileImpl( length, new byte[][]{ getName()})});
			
		
			setPieces( hasher.getPieces());

		}else{
		
			Vector	encoded = new Vector();
		
			processDir( hasher, _torrent_base, encoded, "" );
		
			TOTorrentFileImpl[] files = new TOTorrentFileImpl[ encoded.size()];
		
			encoded.copyInto( files );
		
			setFiles( files );
		}
										 
		setPieces( hasher.getPieces());
	}
	
	protected void
	processDir(
		TOTorrentFileHasher	hasher,
		File				dir,
		Vector				encoded,
		String				root )
		
		throws TOTorrentException
	{
		File[]	dir_files = dir.listFiles();
		
		for (int i=0;i<dir_files.length;i++){
			
			File	file = dir_files[i];
			
			if ( !file.getName().startsWith( "." )){
				
				if ( file.isDirectory()){
					
					String	dir_name = file.getName();
					
					if ( root.length() > 0 ){
						
						dir_name = root + File.separator + dir_name ;
					}
					
					processDir( hasher, file, encoded, dir_name );
					
				}else{
					
					String	file_name = file.getName();
					
					if ( root.length() > 0 ){
					
						file_name = root + File.separator + file_name;
					}
					
					long length = hasher.add( file );
										
					encoded.addElement( new TOTorrentFileImpl( length, file_name));
				}
			}
		}
	}
	
	public void
	pieceHashed(
		int		piece_number )
	{
		if ( progress_listener != null ){
		
			int	this_progress = (int)((piece_number*100)/piece_count );
			
			if ( this_progress != reported_progress ){
				
				reported_progress = this_progress;
				
				progress_listener.reportProgress( reported_progress );
			}
		}
	}
	
	protected long
	calculateNumberOfPieces(
		File				file,
		long				piece_length )
		
		throws TOTorrentException
	{
		long	res = getPieceCount(calculateTotalFileSize( file ), piece_length );
		
		report( MessageText.getString("Torrent.create.progress.piececount") + res );
		
		return( res );
	}
	
	protected long
	calculateTotalFileSize(
		File				file )
		
		throws TOTorrentException
	{
		if ( total_file_size == -1 ){
			
			total_file_size = getTotalFileSize( file );		
		}
		
		return( total_file_size );
	}
	
	protected long
	getTotalFileSize(
		File				file )
		
		throws TOTorrentException
	{
		report( MessageText.getString("Torrent.create.progress.parsingfiles"));
		
		long res = getTotalFileSizeSupport( file );
		
		report( MessageText.getString("Torrent.create.progress.totalfilesize") + DisplayFormatters.formatByteCountToKBEtc(res));

		report( MessageText.getString("Torrent.create.progress.totalfilecount") + total_file_count );
		
		return( res );
	}
	
	protected long
	getTotalFileSizeSupport(
		File				file )
		
		throws TOTorrentException
	{
		String	name = file.getName();
		
		if ( name.equals( "." ) || name.equals( ".." )){
																				
			return( 0 );
		}
		
		if ( !file.exists()){
			
			throw( new TOTorrentException( "TOTorrentCreate: file '" + file.getName() + "' doesn't exist",
											TOTorrentException.RT_FILE_NOT_FOUND ));
		}
		
		if ( file.isFile()){
			
			total_file_count++;
			
			return( file.length());
			
		}else{
			
			File[]	dir_files = file.listFiles();
		
			long	length = 0;
			
			for (int i=0;i<dir_files.length;i++){
				
				length += getTotalFileSizeSupport( dir_files[i] );
			}
			
			return( length );
		}
	}
	
	protected void
	report(
		String	str )
	{
		if ( progress_listener != null ){
			
			progress_listener.reportCurrentTask( str );		
		}
	}
	
	protected static long
	getTorrentDataSizeFromFileOrDirSupport(
		File				file )
	{
		String	name = file.getName();
		
		if ( name.equals( "." ) || name.equals( ".." )){
			
			return( 0 );
		}
		
		if ( !file.exists()){
		
			return(0);
		}
		
		if ( file.isFile()){
						
			return( file.length());
			
		}else{
			
			File[]	dir_files = file.listFiles();
			
			long	length = 0;
			
			for (int i=0;i<dir_files.length;i++){
				
				length += getTorrentDataSizeFromFileOrDirSupport( dir_files[i] );
			}
			
			return( length );
		}
	}
	
	public static long
	getTorrentDataSizeFromFileOrDir(
		File			file_or_dir )
	{
		return( getTorrentDataSizeFromFileOrDirSupport( file_or_dir ));
	}	
	
	public static long
	getComputedPieceSize(
		long 	total_size,
		long	_piece_min_size,
		long	_piece_max_size,
		long	_piece_num_lower,
		long	_piece_num_upper )
	{
		long	piece_length = -1;
		
		long	current_piece_size = _piece_min_size;
		
		while( current_piece_size <= _piece_max_size ){
			
			long	pieces = total_size / current_piece_size;
			
			if ( pieces <= _piece_num_upper ){
				
				piece_length = current_piece_size;
				
				break;
			}
			
			current_piece_size = current_piece_size << 1;
		}
		
		// if we haven't set a piece length here then there are too many pieces even
		// at maximum piece size. Go for largest piece size
		
		if ( piece_length == -1 ){
			
			// just go for the maximum piece size
			
			piece_length = 	_piece_max_size;
		}
		
		return( piece_length );
	}
	
	public static long
	getPieceCount(
		long		total_size,
		long		piece_size )
	{
		return( (total_size + (piece_size-1))/piece_size );
	}
}
