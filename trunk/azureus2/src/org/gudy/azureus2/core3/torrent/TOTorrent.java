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

	public String
	getComment();
	
	public URL
	getAnnounceURL();

	/**
	 * When a group of sets of trackers is defined their URLs are accessed via this method
	 * @return the group, always present, which may have 0 members
	 */
	
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup();
	 
	public byte[][]
	getPieces();

	public long
	getPieceLength();

	public TOTorrentFile[]
	getFiles();
	
	/**
	 * The additional properties are used for holding non-core data for Azureus' own user
	 * @param name		name of the property (e.g. "encoding")
	 * @param value		value. This will be encoded with default encoding
	 */
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
		
		throws TOTorrentException;
		
	public String
	getAdditionalStringProperty(
		String		name )
		
		throws TOTorrentException;
		
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value );
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name );
		
	public void
	serialiseToFile(
		File		file )
		  
		throws TOTorrentException;

	public void
	print();
}
