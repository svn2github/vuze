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

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;

public class 
TOTorrentFileImpl
	implements TOTorrentFile
{
	protected long		file_length;
	protected String[]	path_components;
	
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
}
