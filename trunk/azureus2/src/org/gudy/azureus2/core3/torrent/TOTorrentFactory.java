/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.impl.*;

public class 
TOTorrentFactory 
{
	public static TOTorrent
	deserialiseFromFile(
		File		file )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( file ));
	}
	
	public static TOTorrent
	createFromFileOrDir(
		File						file,
		long						piece_length,
		URL							announce_url )
		
		throws TOTorrentException
	{
		return( createFromFileOrDir( file, piece_length, announce_url, null ));
	}
	
	public static TOTorrent
	createFromFileOrDir(
		File						file,
		long						piece_length,
		URL							announce_url,
		TOTorrentProgressListener	progress_listener )
		
		throws TOTorrentException
	{
		return( new TOTorrentCreateImpl( file, piece_length, announce_url, progress_listener ));
	}
}
