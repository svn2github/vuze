/*
 * File    : TOTorrentImpl.java
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
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentImpl
	implements TOTorrent
{
	protected static final String TK_ANNOUNCE			= "announce";
	protected static final String TK_ANNOUNCE_LIST		= "announce-list";
	protected static final String TK_COMMENT			= "comment";
	protected static final String TK_CREATION_DATE		= "creation date";
	
	protected static final String TK_INFO				= "info";
	protected static final String TK_NAME				= "name";
	protected static final String TK_LENGTH				= "length";
	protected static final String TK_PATH				= "path";
	protected static final String TK_FILES				= "files";
	protected static final String TK_PIECE_LENGTH		= "piece length";
	protected static final String TK_PIECES				= "pieces";
		
	private String							torrent_name;
	private String							comment;
	private URL								announce_url;
	private TOTorrentAnnounceURLGroupImpl	announce_group = new TOTorrentAnnounceURLGroupImpl();
	
	private long		piece_length;
	private byte[][]	pieces;
	
	private byte[]		torrent_hash;
	
	private boolean				simple_torrent;
	private TOTorrentFile[]		files;

	private long				creation_date;
	
	private Map					additional_properties = new HashMap();
	
	/** 
	 * Constructor for deserialisation
	 */
	
	protected
	TOTorrentImpl()
	{
	}

	/** 
	 * Constructor for creation
	 */
	
	protected
	TOTorrentImpl(
		String		_torrent_name,
		URL			_announce_url,
		boolean		_simple_torrent )
	{
		torrent_name		= _torrent_name;
		announce_url		= _announce_url;
		simple_torrent		= _simple_torrent;
	}
	
	public void
	serialiseToFile(
		File		output_file )
	
		throws TOTorrentException
	{				
		byte[]	res = serialiseToByteArray();
		
		FileOutputStream os = null;
		
		try{
			os = new FileOutputStream( output_file );
			
			os.write( res );
		
			os.close();
			
		}catch( Throwable e){
			
			throw( new TOTorrentException( 	"TOTorrent::serialise: fails '" + e.toString() + "'",
											TOTorrentException.RT_WRITE_FAILS ));
			
		}finally{
			
			if ( os != null ){
				
				try{
					os.close();
					
				}catch( IOException e ){
				
					e.printStackTrace();
				}
			}
		}
	}
	
	protected byte[]
	serialiseToByteArray()
	
		throws TOTorrentException
	{
		Map	root = serialiseToMap();
				
		return( BEncoder.encode( root ));
	}		

	public Map
	serialiseToMap()
	
		throws TOTorrentException
	{		
		Map	root = new HashMap();
		
		writeStringToMetaData( root, TK_ANNOUNCE, announce_url.toString());
		
		TOTorrentAnnounceURLSet[] sets = announce_group.getAnnounceURLSets();
		
		if (sets.length > 0 ){
			
			List	announce_list = new ArrayList();
			
			for (int i=0;i<sets.length;i++){
				
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				if ( urls.length == 0 ){
					
					continue;
				}
				
				List sub_list = new ArrayList();
				
				announce_list.add( sub_list );
				
				for (int j=0;j<urls.length;j++){
					
					sub_list.add( writeStringToMetaData( urls[j].toString())); 
				}
			}
			
			if ( announce_list.size() > 0 ){
				
				root.put( TK_ANNOUNCE_LIST, announce_list );
			}
		}
		
		if ( comment != null ){
			
			writeStringToMetaData( root, TK_COMMENT, comment );			
		}
		
		if ( creation_date != 0 ){
			
			root.put( TK_CREATION_DATE, new Long( creation_date ));
		}
		
		Map info = new HashMap();
		
		root.put( TK_INFO, info );
		
		info.put( TK_PIECE_LENGTH, new Long( piece_length ));
		
		byte[]	flat_pieces = new byte[pieces.length*20];
		
		for (int i=0;i<pieces.length;i++){
			
			System.arraycopy( pieces[i], 0, flat_pieces, i*20, 20 );
		}
		
		info.put( TK_PIECES, flat_pieces );
		
		writeStringToMetaData( info, TK_NAME, torrent_name );
		
		if ( simple_torrent ){
		
			TOTorrentFile	file = files[0];
			
			info.put( TK_LENGTH, new Long( file.getLength()));
			
		}else{
	
			List	meta_files = new ArrayList();
		
			info.put( TK_FILES, meta_files );
		
			for (int i=0;i<files.length;i++){
				
				Map	file = new HashMap();
		
				meta_files.add( file );
				
				file.put( TK_LENGTH, new Long( files[i].getLength()));
				
				List path = new ArrayList();
				
				file.put( TK_PATH, path );
				
				String	str_path = files[i].getPath();
				
				int	pos = 0;
				
				while(true){
					
					int	p1 = str_path.indexOf( File.separator, pos );
					
					if ( p1 == -1 ){
						
						path.add( writeStringToMetaData( str_path.substring(pos)));
						
						break;
						
					}else{
						
						path.add( writeStringToMetaData( str_path.substring(pos,p1)));
						
						pos	= p1+1;
					}
				}
			}
		}
		
		Iterator it = additional_properties.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			Object	value = additional_properties.get( key );
			
			if ( value != null ){
				
				root.put( key, value );
			}
		}
		
		return( root );
	}
	
	public String
	getName()
	{
		return( torrent_name );
	}
	
	public boolean
	isSimpleTorrent()
	{
		return( simple_torrent );
	}
	
	public String
	getComment()
	{
		return( comment );
	}
	
	public void
	setComment(
		String	_comment )
	{
		comment = _comment;
	}
	
	public void
	setName(
		String	_name )
	{
		torrent_hash		= null;
		
		torrent_name		= _name;
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	public long
	getCreationDate()
	{
		return( creation_date );
	}
	
	public void
	setCreationDate(
		long		_creation_date )
	{
		creation_date 	= _creation_date;
	}
	
	public byte[]
	getHash()
	
		throws TOTorrentException
	{
		if ( torrent_hash == null ){
			
			Map	root = serialiseToMap();
				
			Map info = (Map)root.get( TK_INFO );
				
			setHashFromInfo( info );		
		}
		
		return( torrent_hash );
	}
	
	protected void
	setHashFromInfo(
		Map		info )
		
		throws TOTorrentException
	{	
		try{
			SHA1Hasher s = new SHA1Hasher();
				
			torrent_hash = s.calculateHash(BEncoder.encode(info));
	
		}catch( Throwable e ){
				
			throw( new TOTorrentException( 	"TOTorrent::setHashFromInfo: fails '" + e.toString() + "'",
											TOTorrentException.RT_HASH_FAILS ));
		}
	}
	
	protected void
	setAnnounceURL(
		URL		_url )
	{
		announce_url		= _url;
	}
	
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup()
	{
		return( announce_group );
	}

	protected void
	addTorrentAnnounceURLSet(
		URL[]		urls )
	{
		announce_group.addSet( new TOTorrentAnnounceURLSetImpl( urls ));
	}
	
	public long
	getPieceLength()
	{
		return( piece_length );
	}
	
	protected void
	setPieceLength(
		long	_length )
	{
		piece_length	= _length;
	}
	
	public byte[][]
	getPieces()
	{
		return( pieces );
	}
	
	protected void
	setPieces(
		byte[][]	_pieces )
	{
		pieces = _pieces;
	}
	
	public TOTorrentFile[]
	getFiles()
	{
		return( files );
	}
	
	protected void
	setFiles(
		TOTorrentFile[]		_files )
	{
		files	= _files;
	}
	
	protected boolean
	getSimpleTorrent()
	{
		return( simple_torrent );
	}
	
	protected void
	setSimpleTorrent(
		boolean	_simple_torrent )
	{
		simple_torrent	= _simple_torrent;
	}
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
	{
		try{
		
			setAdditionalByteArrayProperty( name, writeStringToMetaData( value ));
			
		}catch( TOTorrentException e ){
			
				// hide encoding exceptions as default encoding must be available
			
			e.printStackTrace();
		}
	}
		
	public String
	getAdditionalStringProperty(
		String		name )
	{	
		try{			
		
			return( readStringFromMetaData( getAdditionalByteArrayProperty(name)));
			
		}catch( TOTorrentException e ){
			
				// hide encoding exceptions as default encoding must be available
			
			e.printStackTrace();
			
			return( null );
		}
	}
	
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value )
	{
		additional_properties.put( name, value );
	}
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name )
	{
		return((byte[])additional_properties.get( name ));
	}
	
	public void
	setAdditionalLongProperty(
		String		name,
		Long		value )
	{
		additional_properties.put( name, value );
	}
		
	public Long
	getAdditionalLongProperty(
		String		name )
	{
		return((Long)additional_properties.get( name ));
	}
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value )
	{
		additional_properties.put( name, value );
	}
		
	public List
	getAdditionalListProperty(
		String		name )
	{
		return((List)additional_properties.get( name ));
	}
	
	public void
	setAdditionalMapProperty(
		String		name,
		Map 		value )
	{
		additional_properties.put( name, value );
	}
		
	public Map
	getAdditionalMapProperty(
		String		name )
	{
		return((Map)additional_properties.get( name ));
	}
	
	protected String
	readStringFromMetaData(
		Map		meta_data,
		String	name )
		
		throws TOTorrentException
	{
		return(readStringFromMetaData((byte[])meta_data.get(name)));			
	}
	
	protected String
	readStringFromMetaData(
		byte[]		value )
		
		throws TOTorrentException
	{
		try{
			if ( value == null ){
				
				return( null );
			}
			
			return(	new String(value, Constants.DEFAULT_ENCODING ));
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new TOTorrentException( 	"TOTorrentDeserialise: unsupported encoding for '" + new String(value) + "'",
											TOTorrentException.RT_UNSUPPORTED_ENCODING));
		}
	}
	
	protected void
	writeStringToMetaData(
		Map		meta_data,
		String	name,
		String	value )
		
		throws TOTorrentException
	{
		meta_data.put( name, writeStringToMetaData( value ));	
	}
	
	protected byte[]
	writeStringToMetaData(
		String		value )
		
		throws TOTorrentException
	{
		try{
			
			return(	value.getBytes( Constants.DEFAULT_ENCODING ));
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new TOTorrentException( 	"TOTorrent::writeStringToMetaData: unsupported encoding for '" + new String(value) + "'",
											TOTorrentException.RT_UNSUPPORTED_ENCODING));
		}
	}
	
	public void
	print()
	{
		try{
			byte[]	hash = getHash();
			
			System.out.println( "name = " + torrent_name );
			System.out.println( "announce url = " + announce_url );
			System.out.println( "announce group = " + announce_group.getAnnounceURLSets().length );
			System.out.println( "creation date = " + creation_date );
			System.out.println( "comment = " + comment );
			System.out.println( "hash = " + ByteFormatter.nicePrint( hash ));
			System.out.println( "piece length = " + getPieceLength() );
			System.out.println( "pieces = " + getPieces().length );
			
			Iterator it = additional_properties.keySet().iterator();
			
			while( it.hasNext()){
			
				String	key = (String)it.next();
				Object	value = additional_properties.get( key );
				
				try{
				
					System.out.println( "prop '" + key + "' = '" + 
										( value instanceof byte[]?new String((byte[])value, Constants.DEFAULT_ENCODING):value.toString()) + "'" );
				}catch( UnsupportedEncodingException e){
				
					System.out.println( "prop '" + key + "' = unsupported encoding!!!!");	
				}
			}
			
			for (int i=0;i<pieces.length;i++){
				
				System.out.println( "\t" + ByteFormatter.nicePrint(pieces[i]));
			}
											 
			for (int i=0;i<files.length;i++){
				
				System.out.println( "\t" + files[i].getPath() + " (" + files[i].getLength() + ")" );
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
}