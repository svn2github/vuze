/*
 * File    : TOTorrentFileHasher.java
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
		long		file_length = 0;
		
		InputStream is = null;
		
		try{
			
			is = new BufferedInputStream(new FileInputStream( _file ));

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
			
		}catch( Throwable e ){
			
			throw( new TOTorrentException( 	"TOTorrentFileHasher: file read fails '" + e.toString() + "'",
											TOTorrentException.RT_READ_FAILS ));
		}finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (Exception e) {
				}
			}
		}
		
		return( file_length );
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
			
			throw( new TOTorrentException( 	"TOTorrentFileHasher: file read fails '" + e.toString() + "'",
											TOTorrentException.RT_READ_FAILS ));
		}
	}
}
