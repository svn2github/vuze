/*
 * File    : TOTorrentXMLSerialiser.java
 * Created : 13-Oct-2003
 * By      : stuff
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
import java.util.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.*;

public class 
TOTorrentXMLSerialiser
	extends XUXmlWriter
{
	protected TOTorrentImpl		torrent;
	
	protected
	TOTorrentXMLSerialiser(
		TOTorrentImpl		_torrent )
	{
		torrent = _torrent;
	}
	
	protected void
	serialiseToFile(
		File		file )
		
		throws TOTorrentException
	{
		resetIndent();
		
		try{
			
			setOutputStream( new FileOutputStream( file ));
			
			writeRoot();
			
		}catch( IOException e ){
	
			throw( new TOTorrentException( "TOTorrentXMLSerialiser: file write fails: " + e.toString(),
											TOTorrentException.RT_WRITE_FAILS ));	
			
		}finally{
			
			try{
				
				closeOutputStream();
					
			}catch( Throwable e ){
			
				throw( new TOTorrentException( "TOTorrentXMLSerialiser: file close fails: " + e.toString(),
												TOTorrentException.RT_WRITE_FAILS ));	
			}
		}
	}
	
	protected void
	writeRoot()
	
		throws TOTorrentException
	{
		writeLine( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		writeLine( "<tor:TORRENT" );
		writeLine( "\txmlns:tor=\"http://azureus.sourceforge.net/files\"" );
		writeLine( "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" );
		writeLine( "\txsi:schemaLocation=\"http://azureus.sourceforge.net/files http://azureus.sourceforge.net/files/torrent.xsd\">" );
			
		try{
			indent();
			
			writeTag( "ANNOUNCE_URL",  torrent.getAnnounceURL().toString());
		
			TOTorrentAnnounceURLSet[] sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
		
			if (sets.length > 0 ){

				writeLine( "<ANNOUNCE_LIST>");
				
				try{			
					indent();
					
					for (int i=0;i<sets.length;i++){
					
						TOTorrentAnnounceURLSet	set = sets[i];
					
						URL[]	urls = set.getAnnounceURLs();
															
						writeLine( "<ANNOUNCE_ENTRY>");
					
						try{
							indent();
						
							for (int j=0;j<urls.length;j++){
							
								writeTag( "ANNOUNCE_URL",  urls[j].toString());
							}
						}finally{
							
							exdent();
						}
						
						writeLine( "</ANNOUNCE_ENTRY>");
					}
				}finally{
					exdent();
				}
				
				writeLine( "</ANNOUNCE_LIST>");
			}
		
			byte[] comment = torrent.getComment();
			
			if ( comment != null ){
									
				writeLocalisableTag( "COMMENT", comment );	
			}
		
			long creation_date = torrent.getCreationDate();
			
			if ( creation_date != 0 ){
			
				writeTag( "CREATION_DATE", creation_date );	
			}
		
			byte[]	created_by = torrent.getCreatedBy();
			
			if ( created_by != null ){
						
				writeLocalisableTag( "CREATED_BY", created_by );					
			}
			
			writeTag( "TORRENT_HASH", torrent.getHash());
			
			writeInfo();
			
			Map additional_properties = torrent.getAdditionalProperties();
		
			Iterator it = additional_properties.keySet().iterator();
		
			while( it.hasNext()){
		
				String	key = (String)it.next();
			
				writeGenericMapEntry( key, additional_properties.get( key ));	
			}
			
		}finally{
			
			exdent();
		}
		writeLine( "</tor:TORRENT>");
	}
	
	protected void
	writeInfo()
	
		throws TOTorrentException
	{
		writeLine( "<INFO>" );
		
		try{
			indent();
			
			writeLocalisableTag( "NAME", torrent.getName());		
		
			writeTag( "PIECE_LENGTH", torrent.getPieceLength());
		
			TOTorrentFileImpl[] files = (TOTorrentFileImpl[])torrent.getFiles();
			
			if ( torrent.isSimpleTorrent()){
					
				TOTorrentFile	file = files[0];
			
				writeTag( "LENGTH", files[0].getLength());
			
			}else{

				writeLine( "<FILES>");
		
				try{
					indent();
				
					for (int i=0;i<files.length;i++){
					
						writeLine( "<FILE>");
						
						try{
							
							indent();
							
							TOTorrentFileImpl	file	= files[i];
											
							writeTag( "LENGTH", file.getLength());
						
							writeLine( "<PATH>");
							
							try{				
								
								indent();
								
								byte[][]	path_comps = file.getPathComponents();
							
								for (int j=0;j<path_comps.length;j++){
	
									writeLocalisableTag( "COMPONENT", path_comps[j] );							
								}
						
							}finally{
								
								exdent();
							}
							
							writeLine( "</PATH>");
				
							Map additional_properties = file.getAdditionalProperties();
						
							Iterator prop_it = additional_properties.keySet().iterator();
							
							while( prop_it.hasNext()){
								
								String	key = (String)prop_it.next();
							
								writeGenericMapEntry( key, additional_properties.get( key ));
							}
						}finally{
							
							exdent();
						}
						
						writeLine( "</FILE>");
					}
				}finally{
					
					exdent();
				}
				
				writeLine( "</FILES>");
			}
			
			writeLine( "<PIECES>");
					
			try{
				indent();
				
				byte[][]	pieces = torrent.getPieces();
				
				for (int i=0;i<pieces.length;i++){
				
					writeGeneric( pieces[i] );
				}
			}finally{
				exdent();
			}
		
			writeLine( "</PIECES>");

			Map additional_properties = torrent.getAdditionalInfoProperties();
		
			Iterator it = additional_properties.keySet().iterator();
		
			while( it.hasNext()){
		
				String	key = (String)it.next();
			
				writeGenericMapEntry( key, additional_properties.get( key ));	
			}
			
			
		}finally{
			exdent();
		}
		
		writeLine( "</INFO>");
	}
	
	protected void
	writeGenericMapEntry(
		String	name,
		Object	value )
		
		throws TOTorrentException
	{
		writeLine( "<KEY name=\"" + escapeXML( name ) + "\">");
		
		try{
			indent();
			
			writeGeneric( value );
		}finally{
			
			exdent();
		}
		
		writeLine( "</KEY>");
	}
	
	protected void
	writeGeneric(
		Object	obj )
		
		throws TOTorrentException
	{
		if ( obj instanceof Map ){
			
			writeGeneric((Map)obj);
			
		}else if( obj instanceof List ){
			
			writeGeneric((List)obj);
			
		}else if ( obj instanceof byte[] ){
		
			writeGeneric((byte[])obj);
			
		}else{
			
			writeGeneric((Long)obj);
		}
	}
	
	protected void
	writeGeneric(
		Map		map )
		
		throws TOTorrentException
	{
		writeLine( "<MAP>" );
		
		try{
			indent();
			
			Iterator it = map.keySet().iterator();
			
			while(it.hasNext()){
				
				String	key = (String)it.next();
				
				writeGenericMapEntry( key, map.get( key ));
			}
		}finally{
			
			exdent();
		}	

		writeLine( "</MAP>" );
	}
	
	protected void
	writeGeneric(
		List	list )
		
		throws TOTorrentException
	{
		writeLine( "<LIST>" );
		
		try{
			indent();
			
			for (int i=0;i<list.size();i++){
				
				writeGeneric( list.get(i));
			}
		}finally{
			
			exdent();
		}
		
		writeLine( "</LIST>" );
	}
	
	protected void
	writeGeneric(
		byte[]		bytes )
		
		throws TOTorrentException
	{
		writeTag( "BYTES", encodeBytes( bytes ));
	}
	
	protected void
	writeGeneric(
		Long		l )
	{
		writeTag( "LONG", ""+l );
	}
		
	protected void
	writeTag(
		String		tag,
		byte[]		content )
		
		throws TOTorrentException
	{
		writeLine( "<" + tag + ">" + encodeBytes( content ) + "</" + tag + ">" );	
	}
		
	protected void
	writeLocalisableTag(
		String		tag,
		byte[]		content )
		
		throws TOTorrentException
	{
		boolean	use_bytes = true;
		
		String	utf_string = null;
		
		try{
			utf_string = new String(content,Constants.DEFAULT_ENCODING);
			
			if ( Arrays.equals(
					content,
					utf_string.getBytes( Constants.DEFAULT_ENCODING))){

				use_bytes = false;					
			}
		}catch( UnsupportedEncodingException e ){
		}
		
		writeLine( "<" + tag + " encoding=\""+(use_bytes?"bytes":"utf8") + "\">" + 
					(use_bytes?encodeBytes( content ):escapeXML(utf_string)) + "</" + tag + ">" );	
	}
	
	protected String
	encodeBytes(
		byte[]	bytes )
		
		throws TOTorrentException
	{
		String data = ByteFormatter.nicePrint( bytes, true );
			
		return( data );

		/*
		try{
		
			return( URLEncoder.encode(new String( bytes, Constants.DEFAULT_ENCODING ), Constants.DEFAULT_ENCODING));
			
		}catch( UnsupportedEncodingException e ){

			throw( new TOTorrentException( 	"TOTorrentXMLSerialiser: unsupported encoding for '" + new String(bytes) + "'",
										TOTorrentException.RT_UNSUPPORTED_ENCODING));
		}
		*/
	}
	
	protected String
	getUTF(
		byte[]	bytes )
	{
		try{
			return( new String(bytes,Constants.DEFAULT_ENCODING));
			
		}catch( UnsupportedEncodingException e ){
			
			e.printStackTrace();
			
			return( "" );
		}
	}
}
