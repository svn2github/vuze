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
import java.util.*;

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
	
	public byte[]
	getHash()
	
		throws TOTorrentException;

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
	setAdditionalLongProperty(
		String		name,
		Long		value );
		
	public Long
	getAdditionalLongProperty(
		String		name );
		
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value );
		
	public List
	getAdditionalListProperty(
		String		name );
		
	public void
	setAdditionalMapProperty(
		String		name,
		Map		value );
		
	public Map
	getAdditionalMapProperty(
		String		name );
		
	public void
	serialiseToFile(
		File		file )
		  
		throws TOTorrentException;

	public void
	print();
}
