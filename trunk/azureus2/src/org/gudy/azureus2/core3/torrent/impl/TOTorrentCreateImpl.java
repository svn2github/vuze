/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent.impl;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.net.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;

public class 
TOTorrentCreateImpl
	extends		TOTorrentImpl
	implements	TOTorrentFileHasherListener
{	
	protected TOTorrentFileHasher			file_hasher;
	
	protected long	piece_count;
	
	protected TOTorrentProgressListener		progress_listener;
	
	protected int	reported_progress;
		
	public
	TOTorrentCreateImpl(
		File						_torrent_base,
		long						_piece_length,
		URL							_announce_url,
		TOTorrentProgressListener	_progress_listener )
		
		throws TOTorrentException
	{
		super( _torrent_base.getName(), _announce_url, _piece_length, _torrent_base.isFile());
		
		progress_listener = _progress_listener;
		
		TOTorrentFileHasher	hasher = new TOTorrentFileHasher((int)_piece_length, progress_listener==null?null:this );
		
		piece_count = countPieces( _torrent_base,_piece_length );
		
		if ( piece_count == 0 ){
			
			throw( new TOTorrentException( "TOTorrentCreate: specified files have zero total length" ));
		}
		
		if ( getSimpleTorrent()){
							
			long length = hasher.add( _torrent_base );
		
			setFiles( new TOTorrentFileImpl[]{ new TOTorrentFileImpl( length, getName())});
		
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
	countPieces(
		File				file,
		long				piece_length )
	{
		return( (getTotalFileLength( file ) + (piece_length-1))/piece_length );
	}
	
	protected long
	getTotalFileLength(
		File				file )
	{
		String	name = file.getName();
		
		if ( name.equals( "." ) || name.equals( ".." )){
																				
			return( 0 );
		}
		
		if ( file.isFile()){
			
			return( file.length());
			
		}else{
			
			File[]	dir_files = file.listFiles();
		
			long	length = 0;
			
			for (int i=0;i<dir_files.length;i++){
				
				length += getTotalFileLength( dir_files[i] );
			}
			
			return( length );
		}
	}
}
