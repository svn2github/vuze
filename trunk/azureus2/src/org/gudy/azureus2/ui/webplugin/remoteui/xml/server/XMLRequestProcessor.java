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

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.pluginsimpl.remote.*;

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
		
		writeLine( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		
		try{
			writeLine( "<RESPONSE>" );
			
			indent();
						
			request = SimpleXMLParserDocumentFactory.create( _request );
				
			process();
			
		}catch( Throwable e ){
					
			e.printStackTrace();
			
			if ( e instanceof SimpleXMLParserDocumentException ){
				
				writeTag("ERROR", "Invalid XML Plugin request received - " + exceptionToString(e));
				
			}else{
				
				writeTag("ERROR", exceptionToString(e));
				
			}

		}finally{
			
			exdent();
			
			writeLine( "</RESPONSE>" );
			
			flushOutputStream();
		}
	}
	
	protected void
	process()
	{
		// request.print();
			
		RPRequest	req_obj = (RPRequest)deserialiseObject( request, RPRequest.class, "" );
		
		RPReply reply = request_handler.processRequest( req_obj );

			// void methods result in null return

		if ( reply != null ){
			
			Map	props = reply.getProperties();
			
			Iterator it = props.keySet().iterator();
			
			while( it.hasNext()){
				
				String	name 	= (String)it.next();
				String	value 	= (String)props.get(name);
				
				writeTag( name, value );
			}
			
			try{
				Object	response = reply.getResponse();
			
					// null responses are represented by no response
				
				if ( response != null ){
					
					serialiseObject( response, "" );
				}
				
			}catch( RPException e ){
				
				e.printStackTrace();
				
				writeTag("ERROR", exceptionToString(e));
				
			}
		}
	}
	
	protected String
	exceptionToString(
		Throwable e )
	{
		Throwable cause = e.getCause();
		
		if ( cause != null ){
			
			String	m = cause.getMessage();
			
			if ( m != null ){
				
				return( m );
			}
			
			return( cause.toString());
		}
		
		String	m = e.getMessage();
		
		if ( m != null ){
			
			return( m );
		}
		
		return( e.toString());
	}
	
	protected Object
	deserialiseObject(
		SimpleXMLParserDocumentNode		node,
		Class							cla,
		String							indent )
	{
		boolean	request_class = cla == RPRequest.class;
		
		// System.out.println(indent + "deser:" + cla.getName());
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
					
						// System.out.println( indent + "  field:" + field.getName() + " -> " + child );
						
					if ( child != null ){

						if ( type.isArray()){
						
							Class	sub_type = type.getComponentType();
					
							SimpleXMLParserDocumentNode[] entries = child.getChildren();
							
							Object array = Array.newInstance( sub_type, entries.length );
						
								// hack. for request objects we deserialise the parameters
								// array by using the method signature
							
							String	sig 	= request_class?((RPRequest)obj).getMethod():null;
							int		sig_pos = sig==null?0:sig.indexOf( '[' )+1;
							
							for (int j=0;j<entries.length;j++){
								
								SimpleXMLParserDocumentNode	array_child = entries[j];
								
								if ( sub_type == String.class ){
									
									Array.set( array, j, child.getValue().trim());
									
								}else if ( sub_type == Object.class && request_class ){
									
									int	p1 = sig.indexOf(',');
									
									String	bit;
									
									if ( p1 == -1 ){
										
										bit = sig.substring( sig_pos, sig.length()-1);
										
									}else{
										
										bit = sig.substring( sig_pos, p1 );
										
										sig_pos = p1+1;
									}
									
									String	sub_value = array_child.getValue().trim();
									
									if ( bit.equalsIgnoreCase("url")){
										
										Array.set( array, j, new URL(sub_value));
										
									}else{
										
										throw( new RuntimeException( "not implemented"));
									}
								}else{
									
									throw( new RuntimeException( "not implemented"));
								}
							}
							
							field.set( obj, array );
						}else{
							
							String	value = child.getValue().trim();
							
							if ( type == String.class ){
								
								field.set( obj, value );
								
							}else if ( type == long.class ){
								
								field.setLong( obj,Long.parseLong( value ));
								
							}else if ( type == boolean.class ){
								
								throw( new RuntimeException( "not implemented"));

								//field.set( obj, new Long(Long.parseLong( value )));
								
							}else if ( type == byte.class ){

								throw( new RuntimeException( "not implemented"));

								//field.set( obj, new Long(Long.parseLong( value )));
								
							}else if ( type == char.class ){
								
								field.setChar( obj, value.charAt(0));
								
							}else if ( type == double.class ){
								
								field.setDouble( obj, Double.parseDouble( value));
								
							}else if ( type == float.class ){
								
								field.setFloat( obj, Float.parseFloat( value));
								
							}else if ( type == int.class ){
								
								field.setInt( obj, Integer.parseInt( value));
								
							}else if ( type == short.class ){
								
								field.setShort( obj, (Short.parseShort( value )));
						
							}else if ( type == Long.class || type == long.class ){
								
								field.set( obj, new Long(Long.parseLong( value )));
								
							}else{
								
								field.set( obj, deserialiseObject( child, type, indent + "    " ));
							}
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
		
		// System.out.println(indent + "ser:" + cla.getName());
		
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
										
						// System.out.println( indent + "  field:" + field.getName() + ", type = " + type );
						
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
