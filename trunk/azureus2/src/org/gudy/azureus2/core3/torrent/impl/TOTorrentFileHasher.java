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
import java.security.NoSuchAlgorithmException;
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
	 
	protected SHA1Hasher					overall_sha1_hash;
	protected MD4Hasher						overall_md4_hash;
	protected Md5Hasher						overall_md5_hash;
	protected ED2KHasher					overall_ed2k_hash;
	
	protected byte[]						md4_digest;
	protected byte[]						md5_digest;
	protected byte[]						sha1_digest;
	protected byte[]						ed2k_digest;
	
	protected TOTorrentFileHasherListener	listener;
		
	protected
	TOTorrentFileHasher(
		int								_piece_length,
		TOTorrentFileHasherListener		_listener )
	{
		try{
			overall_sha1_hash 	= new SHA1Hasher();
			
			overall_md4_hash 	= new MD4Hasher();
			
			overall_md5_hash 	= new Md5Hasher();
			
			overall_ed2k_hash 	= new ED2KHasher();
			
		}catch( NoSuchAlgorithmException e ){
			
			e.printStackTrace();
		}
		
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

						overall_sha1_hash.update( buffer );
						overall_md4_hash.update( buffer );
						overall_md5_hash.update( buffer );
						overall_ed2k_hash.update( buffer );
						
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
				
				overall_sha1_hash.update( rem );
				overall_md4_hash.update( rem );
				overall_md5_hash.update( rem );
				overall_ed2k_hash.update( rem );
				
				if ( listener != null ){
							
					listener.pieceHashed( pieces.size() );
				}
				
				buffer_pos = 0;
			}
		
			if ( md5_digest == null ){
				
				md5_digest	= overall_md5_hash.getDigest();
				md4_digest	= overall_md4_hash.getDigest();			
				sha1_digest	= overall_sha1_hash.getDigest();
				ed2k_digest	= overall_ed2k_hash.getDigest();
			}
			
			byte[][] res = new byte[pieces.size()][];
		
			pieces.copyInto( res );
		
			return( res );
			
		}catch( Throwable e ){
			
			throw( new TOTorrentException( 	"TOTorrentFileHasher: file read fails '" + e.toString() + "'",
											TOTorrentException.RT_READ_FAILS ));
		}
	}
	
	protected byte[]
	getMD5Digest()
	
		throws TOTorrentException
	{
		if ( md5_digest == null ){
			
			getPieces();
		}
		
		return( md5_digest );
	}
	
	protected byte[]
	getMD4Digest()
	
		throws TOTorrentException
	{
		if ( md4_digest == null ){
			
			getPieces();
		}
		
		return( md4_digest );
	}
	
	protected byte[]
	getED2KDigest()
	
		throws TOTorrentException
	{
		if ( ed2k_digest == null ){
			
			getPieces();
		}
		
		return( ed2k_digest );
	}
	
	protected byte[]
	getSHA1Digest()
	
		throws TOTorrentException
	{
		if ( sha1_digest == null ){
			
			getPieces();
		}
		
		return( sha1_digest );
	}
}
