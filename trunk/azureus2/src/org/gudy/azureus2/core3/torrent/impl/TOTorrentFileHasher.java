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

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentFileHasher 
{
	protected int		piece_length;
	
	protected Vector	pieces = new Vector();
	
	protected byte[]	buffer;
	protected int		buffer_pos;
	 
	protected TOTorrentFileHasherListener	listener;
		
	protected
	TOTorrentFileHasher(
		int								_piece_length,
		TOTorrentFileHasherListener		_listener )
	{
		piece_length	= _piece_length;
		listener		= _listener;
		
		buffer = new byte[piece_length];
	}
		
	long
	add(
		File		_file )
		
		throws TOTorrentException
	{
		try{
			long		file_length = 0;
			
			FileInputStream is = new FileInputStream( _file );

			while(true){
				
				int	len = is.read( buffer, buffer_pos, piece_length - buffer_pos );
				
				if ( len > 0 ){
					
					file_length += len;
					
					buffer_pos += len;
					
					if ( buffer_pos == piece_length ){
						
						// hash this piece
						
						byte[] hash = new SHA1Hasher().calculateHash(buffer);

						pieces.add( hash );
						
						if ( listener != null ){
							
							listener.pieceHashed( pieces.size() );
						}
						
						buffer_pos = 0;					
					}
				}else{
					
					break;
				}		
			}
			
			return( file_length );
			
		}catch( Throwable e ){
			
			throw( new TOTorrentException( "TOTorrentFileHasher: file read fails '" + e.toString() + "'" ));
		}
	}
	
	protected byte[][]
	getPieces()
		
		throws TOTorrentException
	{
		try{
			if ( buffer_pos > 0 ){
								
				byte[] rem = new byte[buffer_pos];
				
				System.arraycopy( buffer, 0, rem, 0, buffer_pos );
				
				pieces.addElement(new SHA1Hasher().calculateHash(rem));
				
				if ( listener != null ){
							
					listener.pieceHashed( pieces.size() );
				}
				
				buffer_pos = 0;
			}
		
			byte[][] res = new byte[pieces.size()][];
		
			pieces.copyInto( res );
		
			return( res );
			
		}catch( Throwable e ){
			
			throw( new TOTorrentException( "TOTorrentFileHasher: file read fails '" + e.toString() + "'" ));
		}
	}
}
