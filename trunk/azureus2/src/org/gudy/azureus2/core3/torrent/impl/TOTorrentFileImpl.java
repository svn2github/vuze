/*
 * File    : TOTorrentFileImpl.java
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
TOTorrentFileImpl
	implements TOTorrentFile
{
	protected TOTorrent	torrent;
	protected long		file_length;
	protected byte[][]	path_components;
	
	protected Map		additional_properties = new HashMap();
	
	protected boolean	is_utf8;

	protected
	TOTorrentFileImpl(
		TOTorrent		_torrent,
		long			_len,
		String			_path )
		
		throws TOTorrentException
	{
		torrent	= _torrent;
		
		is_utf8	= true;
		
		try{
			
			file_length			= _len;
			
			Vector	temp = new Vector();
			
			int	pos = 0;
			
			while(true){
				
				int	p1 = _path.indexOf( File.separator, pos );
				
				if ( p1 == -1 ){
					
					temp.add( _path.substring( pos ).getBytes( Constants.DEFAULT_ENCODING ));
					
					break;
				}
				
				temp.add( _path.substring( pos, p1 ).getBytes( Constants.DEFAULT_ENCODING ));
				
				pos = p1+1;
			}
			
			path_components		= new byte[temp.size()][];
			
			temp.copyInto( path_components );
			
			checkComponents();
			
		}catch( UnsupportedEncodingException e ){
	
			throw( new TOTorrentException( 	"Unsupported encoding for '" + _path + "'",
											TOTorrentException.RT_UNSUPPORTED_ENCODING));
		}
	}
	
	protected
	TOTorrentFileImpl(
		TOTorrent		_torrent,
		long			_len,
		byte[][]		_path_components )
	
		throws TOTorrentException
	{
		torrent				= _torrent;
		file_length			= _len;
		path_components		= _path_components;
		
		checkComponents();
	}
	
	protected void
	checkComponents()
	
		throws TOTorrentException
	{
		for (int i=0;i<path_components.length;i++){
			
			byte[]	comp = path_components[i];
			
			if (	comp.length == 2 && 
					comp[0] == (byte)'.' &&
					comp[1] == (byte)'.' ){
				
				throw( 	new TOTorrentException( 
						"Torrent file contains illegal '..' component",
						TOTorrentException.RT_DECODE_FAILS ));
			}
		}
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public long
	getLength()
	{
		return( file_length );
	}
	
	public byte[][]
	getPathComponents()
	{
		return( path_components );
	}
	
	protected boolean
	isUTF8()
	{
		return( is_utf8 );
	}
	
	protected void
	setAdditionalProperty(
		String		name,
		Object		value )
	{
		additional_properties.put( name, value );
	}
	
	protected Map
	getAdditionalProperties()
	{
		return( additional_properties );
	}
}
