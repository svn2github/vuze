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

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentDeserialiseImpl
	extends TOTorrentImpl
{
	protected Map	meta_data;
	
	public
	TOTorrentDeserialiseImpl(
		File		file )
		
		throws TOTorrentException
	{
		FileInputStream fis = null;
		
		try{
			
			if(!file.isFile()) {
				
				throw( new TOTorrentException( "Torrent must be a file" ));
			}

			byte[] buf = new byte[1024];
			
			int nbRead;
			
			ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
			
			fis = new FileInputStream(file);
			
			while ((nbRead = fis.read(buf)) > 0){
				
				metaInfo.write(buf, 0, nbRead);
			}
			
			meta_data = BDecoder.decode(metaInfo.toByteArray());
			
			if ( meta_data == null) {
				
				throw( new TOTorrentException( "Torrent decode fails" ));
			}
			
			printMap();
			
				// decode the stuff
			
			setAnnounceURL( new URL( readStringFromMetaData( meta_data, TK_ANNOUNCE )));

			List	announce_list = (List)meta_data.get( TK_ANNOUNCE_LIST );
			
			if ( announce_list != null ){
				
				for (int i=0;i<announce_list.size();i++){
					
					List	set = (List)announce_list.get(i);
					
					URL[]	urls = new URL[set.size()];
					
					for (int j=0;j<urls.length;j++){
						
						urls[j] = new URL( readStringFromMetaData((byte[])set.get(j)));		
					}
					
					addTorrentAnnounceURLSet( urls );
				}
			}
			Map	info = (Map)meta_data.get( TK_INFO );
			
			setName( readStringFromMetaData( info, TK_NAME ));
			
			Long simple_file_length = (Long)info.get( TK_LENGTH );
			
			if ( simple_file_length != null ){
			
				setSimpleTorrent( true );
				
				setFiles( new TOTorrentFileImpl[]{ new TOTorrentFileImpl( simple_file_length.longValue(), getName())});
				
			}else{
				
				setSimpleTorrent( false );  

				List	meta_files = (List)info.get( TK_FILES );
			
				TOTorrentFile[] files = new TOTorrentFile[ meta_files.size()];
			
				for (int i=0;i<files.length;i++){
					
					Map	file_map = (Map)meta_files.get(i);
					
					long	len = ((Long)file_map.get( TK_LENGTH )).longValue();
					
					List	paths = (List)file_map.get( TK_PATH );
					
					String	path = "";
					
					for (int j=0;j<paths.size();j++){
					
						path += (j==0?"":File.separator) + new String(readStringFromMetaData((byte[])paths.get(j)));
					}
					
					files[i] = new TOTorrentFileImpl( len, path );
				}
				
				setFiles( files );
			}
											 
			setPieceLength( ((Long)info.get( TK_PIECE_LENGTH )).longValue());
						
			byte[]	flat_pieces = (byte[])info.get( TK_PIECES );
			
			byte[][]pieces = new byte[flat_pieces.length/20][20];
			
			for (int i=0;i<pieces.length;i++){
				
				System.arraycopy( flat_pieces, i*20, pieces[i], 0, 20 );
			}	
				
			setPieces( pieces );
			
		}catch( TOTorrentException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			 e.printStackTrace();
			 
			 throw( new TOTorrentException( "Torrent decode fails: " + e.toString()));
		}
	}
	

	public void
	printMap()
	{
		print( "", "root", meta_data );
	}
	
	protected void
	print(
		String		indent,
		String		name,
		Map			map )
	{
		System.out.println( indent + name + "{map}" );
		
		Set	keys = map.keySet();
		
		Iterator it = keys.iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			Object	value =  map.get( key );
			
			if ( value instanceof Map ){
				
				print( indent+"  ", key, (Map)value);
				
			}else if ( value instanceof List ){
				
				print( indent+"  ", key, (List)value );
				
			}else if ( value instanceof Long ){
				
				print( indent+"  ", key, (Long)value );
				
			}else{
				
				print( indent+"  ", key, (byte[])value);
			}
		}
	}
	
	protected void
	print(
		String		indent,
		String		name,
		List		list )
	{
		System.out.println( indent + name + "{list}" );
		
		Iterator it = list.iterator();
		
		int	index = 0;
		
		while( it.hasNext()){
			
			Object	value =  it.next();
			
			if ( value instanceof Map ){
				
				print( indent+"  ", "[" + index + "]", (Map)value);
				
			}else if ( value instanceof List ){
				
				print( indent+"  ", "[" + index + "]", (List)value );
				
			}else if ( value instanceof Long ){
				
				print( indent+"  ", "[" + index + "]", (Long)value );
				
			}else{
				
				print( indent+"  ", "[" + index + "]", (byte[])value);
			}
			
			index++;
		}
	}
	protected void
	print(
		String		indent,
		String		name,
		Long		value )
	{
		System.out.println( indent + name + "{long} = " + value.longValue());
	}
	
	protected void
	print(
		String		indent,
		String		name,
		byte[]		value )
	{		
		String	x = new String(value);
		
		boolean	print = true;
		
		for (int i=0;i<x.length();i++){
			
			char	c = x.charAt(i);
			
			if ( c < 128 ){
							
			}else{
				
				print = false;
				
				break;
			}
		}
		
		if ( print ){
			
			System.out.println( indent + name + "{byte[]} = " + x );
			
		}else{
				 
			System.out.println( indent + name + "{byte[], length " + value.length + "}" );
		}
	}
}