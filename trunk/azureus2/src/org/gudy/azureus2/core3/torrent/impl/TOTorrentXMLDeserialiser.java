/*
 * File    : TOTorrentXMLDeserialiser.java
 * Created : 14-Oct-2003
 * By      : parg
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

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentXMLDeserialiser 
{
	public
	TOTorrentXMLDeserialiser()
	{
	}
	
	public TOTorrent
	deserialise(
		File		file )
		
		throws TOTorrentException
	{
		try{
			
			SimpleXMLParserDocument	doc = SimpleXMLParserDocumentFactory.create( file );
			
			TOTorrent res = decodeRoot( doc );
			
			res.print();
			
		
			throw( new TOTorrentException( "parp", TOTorrentException.RT_DECODE_FAILS ));
			
		}catch( SimpleXMLParserDocumentException e ){
		
			throw( new TOTorrentException( "parp", TOTorrentException.RT_DECODE_FAILS ));
		}
	}
	
	protected TOTorrent
	decodeRoot(
		SimpleXMLParserDocument		doc )
		
		throws TOTorrentException
	{
		String root_name = doc.getName();
		
		if ( root_name.equalsIgnoreCase("TORRENT")){
			
			TOTorrentImpl	torrent = new TOTorrentImpl();
			
			SimpleXMLParserDocumentNode[] kids = doc.getChildren();
			
			URL		announce_url 					= null;
			
			SimpleXMLParserDocumentNode	info_node 	= null;
			
			for (int i=0;i<kids.length;i++){
				
				SimpleXMLParserDocumentNode	kid = kids[i];
				
				String	name = kid.getName();
				
				if ( name.equalsIgnoreCase( "ANNOUNCE_URL")){
					
					try{
					
						announce_url = new URL(kid.getValue());
						
					}catch( MalformedURLException e ){
				
						throw( new TOTorrentException( "ANNOUNCE_URL malformed", TOTorrentException.RT_DECODE_FAILS));
					}
					
				}else if ( name.equalsIgnoreCase( "ANNOUNCE_LIST ")){
					
					// todo
					
				}else if ( name.equalsIgnoreCase( "COMMENT ")){
					
					torrent.setComment( kid.getValue());
					
				}else if ( name.equalsIgnoreCase( "CREATED_BY ")){
					
					torrent.setCreatedBy( kid.getValue());
					
				}else if ( name.equalsIgnoreCase( "CREATION_DATE ")){
					
					try{
					
						torrent.setCreationDate( Long.parseLong(kid.getValue()));
						
					}catch(Throwable e){
						
						throw( new TOTorrentException( "CREATION_DATE invalid - must be 'long' value", TOTorrentException.RT_DECODE_FAILS));
					}
				}else if ( name.equalsIgnoreCase( "INFO" )){
					
					decodeInfo( kid, torrent );
					
				}else{
						// generic entry
				}
			}

			if ( announce_url == null ){
				
				throw( new TOTorrentException( "ANNOUNCE_URL missing", TOTorrentException.RT_DECODE_FAILS));
			}
			
			torrent.setAnnounceURL( announce_url );
			
			return( torrent );
		}else{
			
			throw( new TOTorrentException( "Invalid root element", TOTorrentException.RT_DECODE_FAILS));
		}
	}
	
	protected void
	decodeInfo(
		SimpleXMLParserDocumentNode		doc,
		TOTorrentImpl					torrent )
		
		throws TOTorrentException
	{
		SimpleXMLParserDocumentNode[] kids = doc.getChildren();
			
		byte[]	torrent_name 	= null;
		long	torrent_length	= 0;
					
		for (int i=0;i<kids.length;i++){
			
			SimpleXMLParserDocumentNode	kid = kids[i];
				
			String	name = kid.getName();
				
				System.out.println( "info:" + name );
				
			if ( name.equalsIgnoreCase( "PIECE_LENGTH")){
				
				try{
				
					torrent.setPieceLength( Long.parseLong( kid.getValue()));
					
				}catch( Throwable e ){

					throw( new TOTorrentException( "PIECE_LENGTH invalid - must be 'long' value", TOTorrentException.RT_DECODE_FAILS));
				}
				
			}else if ( name.equalsIgnoreCase( "LENGTH")){
	
				torrent.setSimpleTorrent( true );
				
				try{
				
					torrent_length = Long.parseLong( kid.getValue());
					
				}catch( Throwable e ){

					throw( new TOTorrentException( "LENGTH invalid - must be 'long' value", TOTorrentException.RT_DECODE_FAILS));
				}
				
			}else if ( name.equalsIgnoreCase( "NAME")){
				
				try{
				
					torrent.setName( URLDecoder.decode( kid.getValue(), Constants.BYTE_ENCODING ).getBytes( Constants.BYTE_ENCODING ));
				
				}catch( UnsupportedEncodingException e ){

					throw( new TOTorrentException( "NAME invalid - unsupported encoding", TOTorrentException.RT_DECODE_FAILS));				
				}
			}else if ( name.equalsIgnoreCase( "FILES" )){
				
				torrent.setFiles( new TOTorrentFileImpl[]{});			// !!!!
			// todo
			}else if ( name.equalsIgnoreCase( "PIECES" )){
				
				torrent.setPieces( new byte[][]{} );
				// todo
			}else{
				
				// todo generic
			}
		}
		
		if ( torrent.isSimpleTorrent()){
	
			torrent.setFiles( new TOTorrentFileImpl[]{});			// !!!!
		}
	
	}
}
