/*
 * File    : XMLRequestProcessor.java
 * Created : 15-Mar-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.xml.server;

/**
 * @author parg
 *
 */

import java.io.*;
import java.lang.reflect.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;

public class 
XMLRequestProcessor
	extends XUXmlWriter
{
	protected RPRequestHandler				request_handler;
	protected SimpleXMLParserDocument		request;
	
	protected
	XMLRequestProcessor(
		RPRequestHandler		_request_handler,
		InputStream				_request,
		OutputStream			_reply )
	{
		super( _reply );
			
		request_handler		= _request_handler;
		
		try{
			writeLine( "<RESPONSE>" );
			
			indent();
						
			request = SimpleXMLParserDocumentFactory.create( _request );
				
			process();
			
		}catch( Throwable e ){
					
			e.printStackTrace();
			
			writeTag("ERROR", e.toString());

		}finally{
			
			exdent();
			
			writeLine( "</RESPONSE>" );
			
			flushOutputStream();
		}
	}
	
	protected void
	process()
	{
		request.print();
			
		RPRequest	req_obj = (RPRequest)deserialiseObject( request, RPRequest.class, "" );
		
		RPReply reply = request_handler.processRequest( req_obj );

		try{
			Object	response = reply.getResponse();
		
			serialiseObject( response, "" );
			
		}catch( RPException e ){
			
			e.printStackTrace();
			
			writeTag("ERROR", e.toString());
			
		}
	}
	
	protected Object
	deserialiseObject(
		SimpleXMLParserDocumentNode		node,
		Class							cla,
		String							indent )
	{
		System.out.println(indent + "deser:" + cla.getName());
		try{
			Object	obj = cla.newInstance();
			
			Field[] fields = cla.getDeclaredFields();
			
			for (int i=0;i<fields.length;i++){
				
				Field	field = fields[i];
				
				int	modifiers = field.getModifiers();
				
				if (( modifiers & ( Modifier.TRANSIENT | Modifier.STATIC )) == 0 ){
					
					String	name = field.getName();
					
					Class	type = field.getType();
					
					SimpleXMLParserDocumentNode child = node.getChild( name );
				
					System.out.println( indent + "  field:" + field.getName() + " -> " + child );
					
					if ( child != null ){
						
						if ( type == String.class ){
							
							field.set( obj, child.getValue().trim());
							
						}else if ( type == long.class ){
							
							field.setLong( obj,Long.parseLong( child.getValue().trim()));
							
						}else if ( type == boolean.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == byte.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == char.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == double.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == float.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == int.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
							
						}else if ( type == short.class ){
							
							//field.set( obj, new Long(Long.parseLong( child.getValue())));
					
						}else if ( type == Long.class || type == long.class ){
							
							field.set( obj, new Long(Long.parseLong( child.getValue().trim())));
							
						}else{
							
							field.set( obj, deserialiseObject( child, type, indent + "    " ));
						}
					}
				}
			}
			
			return( obj );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new RuntimeException( e.toString()));
		}
	}
	
	protected void
	serialiseObject(
		Object		obj,
		String		indent )
	{
		Class	cla = obj.getClass();
		
		System.out.println(indent + "ser:" + cla.getName());
		
		if ( cla.isArray()){
			
			int	len = Array.getLength( obj );
			
			for (int i=0;i<len;i++){
				
				Object	entry = Array.get( obj, i );
				
				try{
					writeLine( "<ENTRY index=\""+i+"\">" );
					
					indent();
				
					serialiseObject( entry, indent+"  ");
					
				}finally{
					
					exdent();
					
					writeLine( "</ENTRY>");
				}
			}
			
			return;
		}
		
		while( cla != null ){
			try{
				Field[] fields = cla.getDeclaredFields();
				
				for (int i=0;i<fields.length;i++){
					
					Field	field = fields[i];
					
					int	modifiers = field.getModifiers();
					
					if (( modifiers & ( Modifier.TRANSIENT | Modifier.STATIC )) == 0 ){
						
						String	name = field.getName();
						
						Class	type = field.getType();
										
						System.out.println( indent + "  field:" + field.getName() + ", type = " + type );
						
						try{
							writeLine( "<" + name + ">" );
							
							indent();
													
							if ( type == String.class ){
								
								writeLine( (String)field.get( obj ));
								
							}else if ( type == long.class ){
								
								writeLine( ""+field.getLong( obj ));
								
							}else if ( type == boolean.class ){
								
								writeLine( ""+field.getBoolean( obj ));
								
							}else if ( type == byte.class ){
								
								writeLine( ""+field.getByte( obj ));
								
							}else if ( type == char.class ){
								
								writeLine( ""+field.getChar( obj ));
								
							}else if ( type == double.class ){
								
								writeLine( ""+field.getDouble( obj ));
								
							}else if ( type == float.class ){
								
								writeLine( ""+field.getFloat( obj ));
								
							}else if ( type == int.class ){
								
								writeLine( ""+field.getInt( obj ));
								
							}else if ( type == short.class ){
								
								writeLine( ""+field.getShort( obj ));
								
							}else if ( type == Long.class ){
								
								writeLine( ""+field.get( obj ));
								
							}else{
								
								serialiseObject( field.get(obj), indent + "    " );
							}
							
						}finally{
							
							exdent();
							
							writeLine( "</" + name + ">" );
						}
					}
				}
								
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				throw( new RuntimeException( e.toString()));
			}
			
			cla = cla.getSuperclass();
		}
	}
}
