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


import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;

public class 
TOTorrentFileImpl
	implements TOTorrentFile
{
	protected long		file_length;
	protected String[]	path_components;
	
	protected Map		additional_properties = new HashMap();
	
	protected
	TOTorrentFileImpl(
		long			_len,
		String			_path )
	{
		file_length			= _len;
		
		Vector	temp = new Vector();
		
		int	pos = 0;
		
		while(true){
			
			int	p1 = _path.indexOf( File.separator, pos );
			
			if ( p1 == -1 ){
				
				temp.add( _path.substring( pos ));
				
				break;
			}
			
			temp.add( _path.substring( pos, p1 ));
			
			pos = p1+1;
		}
		path_components		= new String[temp.size()];
		
		temp.copyInto( path_components );
	}
	
	protected
	TOTorrentFileImpl(
		long			_len,
		String[]		_path_components )
	{
		file_length			= _len;
		path_components		= _path_components;
	}
	
	public long
	getLength()
	{
		return( file_length );
	}
	
	public String
	getPath()
	{
		String	res = "";
		
		for (int i=0;i<path_components.length;i++){
			
			res += (i==0?"":File.separator) + path_components[i];
		}
		
		return( res );
	}
	
	public String[]
	getPathComponents()
	{
		return( path_components );
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
