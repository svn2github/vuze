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

public class 
TOTorrentXMLSerialiser 
{
	protected static final int			INDENT_AMOUNT	= 4;
	
	protected TOTorrentImpl		torrent;
	protected PrintWriter		writer = null;
	
	protected String		current_indent_string;
	
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
			
			writer = new PrintWriter( new FileWriter( file ));
			
			writeRoot();
			
		}catch( IOException e ){
			
		}finally{
			
			if ( writer != null ){
				
				try{					
					writer.flush();
					
					writer.close();
					
				}catch( Throwable e ){
			
					throw( new TOTorrentException( "TOTorrentXMLSerialiser: file close fails: " + e.toString(),
													TOTorrentException.RT_WRITE_FAILS ));	
				}
			}
		}
	}
	
	protected void
	writeRoot()
	
		throws TOTorrentException
	{
		writeLine( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		writeLine( "<tor:TORRENT" );
		writeLine( "\txmlns:tor=\"http://localhost:81/azureus\"" );
		writeLine( "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" );
		writeLine( "\txsi:schemaLocation=\"http://localhost:81/azureus http://localhost:81/azureus/torrent.xsd\">" );
			
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
		
			String comment = torrent.getComment();
			
			if ( comment != null ){
				
				writeTag( "COMMENT", comment );	
			}
		
			long creation_date = torrent.getCreationDate();
			
			if ( creation_date != 0 ){
			
				writeTag( "CREATION_DATE", creation_date );	
			}
		
			String	created_by = torrent.getCreatedBy();
			
			if ( created_by != null ){
			
				writeTag( "CREATED_BY", created_by );					
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
			
			writeTag( "PIECE_LENGTH", torrent.getPieceLength());
		
			writeTag( "NAME", torrent.getName());		
		
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
	
									writeTag( "COMPONENT", path_comps[j] );							
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
			
			Map additional_properties = torrent.getAdditionalInfoProperties();
		
			Iterator it = additional_properties.keySet().iterator();
		
			while( it.hasNext()){
		
				String	key = (String)it.next();
			
				writeGenericMapEntry( key, additional_properties.get( key ));	
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
		long		content )
	{
		writeLine( "<" + tag + ">" + content + "</" + tag + ">" );	
	}
	
	protected void
	writeTag(
		String		tag,
		String		content )
	{
		writeLine( "<" + tag + ">" + escapeXML( content ) + "</" + tag + ">" );	
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
	writeLine(
		String	str )
	{
		writer.println( current_indent_string + str );
	}
	
	protected void
	resetIndent()
	{
		current_indent_string	= "";
	}
	
	protected void
	indent()
	{
		for (int i=0;i<INDENT_AMOUNT;i++){
		
			current_indent_string += " ";
		}
	}
	
	protected void
	exdent()
	{
		current_indent_string = current_indent_string.substring(0,current_indent_string.length()-4);
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
	escapeXML(
		String	str )
	{
		str = str.replaceAll( "&", "&amp;" );
		str = str.replaceAll( ">", "&gt;" );
		str = str.replaceAll( "<", "&lt;" );
		str = str.replaceAll( "\"", "&quot;" );
		str = str.replaceAll( "--", "&#45;&#45;" );
		
		return( str );
	}
}
