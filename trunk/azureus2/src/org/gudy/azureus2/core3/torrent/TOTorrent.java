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

import java.io.File;
import java.net.URL;

public interface 
TOTorrent
{
	public String
	getName();

	public URL
	getAnnounceURL();

	public byte[][]
	getPieces();

	public long
	getPieceLength();

	public TOTorrentFile[]
	getFiles();
	
	public void
	serialiseToFile(
		File		file )
		  
		throws TOTorrentException;

	public void
	print();
}
