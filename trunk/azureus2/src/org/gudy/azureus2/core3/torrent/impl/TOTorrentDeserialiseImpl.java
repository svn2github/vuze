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
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentDeserialiseImpl
	extends TOTorrentImpl
{
	public
	TOTorrentDeserialiseImpl(
		File		file )
		
		throws TOTorrentException
	{		
		if(!file.isFile()) {
			
			throw( new TOTorrentException( "TOTorrentDeserialise: Torrent must be a file ('" + file.getName() + "')" ));
		}

		ByteArrayOutputStream metaInfo = new ByteArrayOutputStream();
		
		FileInputStream fis = null;
		
		try{
		
			byte[] buf = new byte[2048];
		
			int nbRead;
		
			fis = new FileInputStream(file);
		
			while ((nbRead = fis.read(buf)) > 0){
			
				metaInfo.write(buf, 0, nbRead);
			}
		}catch( IOException e ){
			
			throw( new TOTorrentException( "TOTorrentDeserialise: IO exception reading torrent '" + e.toString()+ "'" ));
			
		}finally{
			
			if ( fis != null ){
				try{
					
					fis.close();
					
				}catch( IOException e ){
					
					e.printStackTrace();
				}
			}
		}
		
		construct( metaInfo.toByteArray());
	}
	
	public
	TOTorrentDeserialiseImpl(
		byte[]		bytes )
		
		throws TOTorrentException
	{
		construct( bytes );
	}
	public
	TOTorrentDeserialiseImpl(
		Map			map )
		
		throws TOTorrentException
	{
		construct( map );
	}

	protected void
	construct(
		byte[]		bytes )
		
		throws TOTorrentException
	{
		Map meta_data = BDecoder.decode(bytes);
		
		if ( meta_data == null) {
			
			throw( new TOTorrentException( "TOTorrentDeserialise: decode fails" ));
		}

		construct( meta_data );
	}
	
	protected void
	construct(
		Map		meta_data )
		
		throws TOTorrentException
	{
		
			// decode the stuff
		
		Iterator root_it = meta_data.keySet().iterator();
		
		while( root_it.hasNext()){
			
			String	key = (String)root_it.next();
			
			if ( key.equalsIgnoreCase( TK_ANNOUNCE )){
						
				String url_str = readStringFromMetaData( meta_data, TK_ANNOUNCE );
				
				url_str.replaceAll( " ", "" );
				
				try{
				
					setAnnounceURL( new URL( url_str ));
					
				}catch( MalformedURLException e ){
					
					e.printStackTrace();
					
					throw( new TOTorrentException( "TOTorrentDeserialise: announce URL malformed ('" + url_str + "'"));
				}
				
			}else if ( key.equalsIgnoreCase( TK_ANNOUNCE_LIST )){

				List	announce_list = (List)meta_data.get( TK_ANNOUNCE_LIST );
				
				if ( announce_list != null ){
					
					for (int i=0;i<announce_list.size();i++){
						
						List	set = (List)announce_list.get(i);
							
						Vector urls = new Vector();
							
						for (int j=0;j<set.size();j++){
				
							try{
		
								String url_str = readStringFromMetaData((byte[])set.get(j));
								
								url_str.replaceAll( " ", "" );
										
								urls.add( new URL( url_str ));		
						
							}catch( MalformedURLException e ){
								
								e.printStackTrace();
							}
						}
						
						if ( urls.size() > 0 ){
						
							URL[]	url_array = new URL[urls.size()];
							
							urls.copyInto( url_array );
							
							addTorrentAnnounceURLSet( url_array );
						}
					}
				}
			}else if ( key.equalsIgnoreCase( TK_COMMENT )){
				
				setComment( readStringFromMetaData( meta_data, TK_COMMENT ));
				
			}else if ( key.equalsIgnoreCase( TK_INFO )){
				
				// processed later
				
			}else{
				
				Object	prop = meta_data.get( key );
				
				if ( prop instanceof byte[] ){
					
					setAdditionalByteArrayProperty( key, (byte[])prop );
					
				}else if ( prop instanceof Long ){
					
					setAdditionalLongProperty( key, (Long)prop );
					
				}else if ( prop instanceof List ){
					
					setAdditionalListProperty( key, (List)prop );
					
				}else{
					
					setAdditionalMapProperty( key, (Map)prop );
				}
			}
		}
		
		Map	info = (Map)meta_data.get( TK_INFO );
		
		setHashFromInfo( info );
		
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
	}
	

	public void
	printMap()
	{
		try{
		
			print( "", "root", serialiseToMap());
			
		}catch( TOTorrentException e ){
		
			e.printStackTrace();
		}
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