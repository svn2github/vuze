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

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentImpl
	implements TOTorrent
{
	private String	torrent_name;
	private URL		announce_url;
	
	private long		piece_length;
	private byte[][]	pieces;
	
	private boolean				simple_torrent;
	private TOTorrentFile[]		files;

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
		long		_piece_length,
		boolean		_simple_torrent )
	{
		torrent_name		= _torrent_name;
		announce_url		= _announce_url;
		piece_length		= _piece_length;
		simple_torrent		= _simple_torrent;
	}
	
	public void
	serialiseToFile(
		File		output_file )
	
		throws TOTorrentException
	{
		Map	root = new HashMap();
		
		root.put( "announce", announce_url.toString().getBytes());
		
		Map info = new HashMap();
		
		root.put( "info", info );
		
		info.put( "piece length", new Long( piece_length ));
		
		byte[]	flat_pieces = new byte[pieces.length*20];
		
		for (int i=0;i<pieces.length;i++){
			
			System.arraycopy( pieces[i], 0, flat_pieces, i*20, 20 );
		}
		
		info.put( "pieces", flat_pieces );
		
		info.put( "name", torrent_name.getBytes());
		
		if ( simple_torrent ){
		
			TOTorrentFile	file = files[0];
			
			info.put( "length", new Long( file.getLength()));
			
		}else{
	
			List	meta_files = new ArrayList();
		
			info.put( "files", meta_files );
		
			for (int i=0;i<files.length;i++){
				
				Map	file = new HashMap();
		
				meta_files.add( file );
				
				file.put( "length", new Long( files[i].getLength()));
				
				List path = new ArrayList();
				
				file.put( "path", path );
				
				String	str_path = files[i].getPath();
				
				int	pos = 0;
				
				while(true){
					
					int	p1 = str_path.indexOf( File.separator, pos );
					
					if ( p1 == -1 ){
						
						path.add( str_path.substring(pos).getBytes());
						
						break;
						
					}else{
						
						path.add( str_path.substring(pos,p1).getBytes());
						
						pos	= p1+1;
					}
				}
			}
		}
		
		byte[]	res = BEncoder.encode( root );
		
		try{
			FileOutputStream os = new FileOutputStream( output_file );
			
			os.write( res );
		
			os.close();
			
		}catch( Throwable e){
			
			throw( new TOTorrentException( "TOTorrent::serialise: fails '" + e.toString() + "'" ));
		}
	}
	
	public String
	getName()
	{
		return( torrent_name );
	}
	
	protected void
	setName(
		String	_name )
	{
		torrent_name		= _name;
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	protected void
	setAnnounceURL(
		URL		_url )
	{
		announce_url		= _url;
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
	print()
	{
		System.out.println( "name = " + torrent_name );
		System.out.println( "announce url = " + announce_url );
		System.out.println( "piece length = " + piece_length );
		System.out.println( "pieces = " + pieces.length );
		
		for (int i=0;i<pieces.length;i++){
			
			System.out.println( "\t" + pieces[i][0] + pieces[i][1] + pieces[i][2] + pieces[i][3] + "..." );
		}
										 
		for (int i=0;i<files.length;i++){
			
			System.out.println( "\t" + files[i].getPath() + " (" + files[i].getLength() + ")" );
		}
	}
}