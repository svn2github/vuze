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

import org.gudy.azureus2.core3.torrent.*;

public class 
TOTorrentFileImpl
	implements TOTorrentFile
{
	protected long		file_length;
	protected String	path;
	
	protected
	TOTorrentFileImpl(
		long		_len,
		String		_path )
	{
		file_length	= _len;
		path		= _path;
	}
	
	public long
	getLength()
	{
		return( file_length );
	}
	
	public String
	getPath()
	{
		return( path );
	}
}
