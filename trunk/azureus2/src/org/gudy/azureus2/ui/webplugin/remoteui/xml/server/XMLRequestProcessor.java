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

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentAttribute;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;
import org.gudy.azureus2.pluginsimpl.remote.*;

public class 
XMLRequestProcessor
	extends XUXmlWriter
{
	protected RPRequestHandler				request_handler;
	protected SimpleXMLParserDocument		request;
	
	protected
	XMLRequestProcessor(
		RPRequestHandler			_request_handler,
		RPRequestAccessController	_access_controller,
		String						_client_ip,
		InputStream					_request,
		OutputStream				_reply )
	{
		super( _reply );
			
		request_handler		= _request_handler;
		
		writeLineRaw( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		
		try{
			writeLineRaw( "<RESPONSE>" );
			
			indent();
						
			request = SimpleXMLParserDocumentFactory.create( _request );
				
			process( _client_ip , _access_controller);
			
		}catch( Throwable e ){
					
			Debug.printStackTrace( e );
			
			if ( e instanceof SimpleXMLParserDocumentException ){
				
				writeTag("ERROR", "Invalid XML Plugin request received - " + exceptionToString(e));
				
			}else{
				
				writeTag("ERROR", e );
				
			}

		}finally{
			
			exdent();
			
			writeLineRaw( "</RESPONSE>" );
			
			flushOutputStream();
		}
	}
	
	protected void
	process(
		String						client_ip,
		RPRequestAccessController	access_controller )
	{
		// request.print();
			
		RPRequest	req_obj = (RPRequest)deserialiseObject( request, RPRequest.class, "" );
		
		req_obj.setClientIP( client_ip );
		
		RPReply reply = request_handler.processRequest( req_obj, access_controller );

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
					
					serialiseObject( response, "", 0xffffffff );
				}
				
			}catch( RPException e ){
				
				Debug.printStackTrace( e );
				
				writeTag("ERROR", e );
				
			}
		}
	}
	
	protected void
	writeTag(
		String		tag,
		Throwable 	e )
	{
		writeLineRaw( "<ERROR>");

		writeLineEscaped( exceptionToString(e));
		
		if ( e instanceof RPException && e.getCause() != null ){
			
			e = e.getCause();
		}
		
		serialiseObject( e, "    ", ~Modifier.PRIVATE );
		
		writeLineRaw( "</ERROR>" );
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
			// hack I'm afraid, when deserialising request objects we need to use the
			// method to correctly deserialise parameters
		
		String	request_method = null;
		
		if ( cla == RPRequest.class ){
			
			request_method = node.getChild( "METHOD" ).getValue().trim();

		}
		
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
							
							if ( request_method != null  ){
								
									// must be params
						
								String[]	bits = new String[entries.length];
								
								int		method_pos = request_method.indexOf( '[' )+1;

								for (int j=0;j<entries.length;j++){
																												
									int	p1 = request_method.indexOf(',',method_pos);
										
									String	bit;
										
									if ( p1 == -1 ){
											
										bits[j] = request_method.substring( method_pos, request_method.length()-1).toLowerCase();
											
										break;
									}else{
											
										bits[j] = request_method.substring( method_pos, p1 ).toLowerCase();
											
										method_pos = p1+1;
									}
								}
					
								for (int j=0;j<entries.length;j++){
									
									SimpleXMLParserDocumentNode	array_child = entries[j];
										
									SimpleXMLParserDocumentAttribute index_attr = array_child.getAttribute("index");
									
									String	index_str = index_attr==null?null:index_attr.getValue().trim();
									
									int	array_index = index_str==null?j:Integer.parseInt(index_str);
									
									String	bit = bits[array_index];
									
									String	sub_value = array_child.getValue().trim();
									
									if ( bit.equals("string")){
										
										Array.set( array, array_index, sub_value );
									
									}else if ( bit.equals("int")){
										
										Array.set( array, array_index, Integer.valueOf( sub_value));
									
									}else if ( bit.equals("boolean")){
										
										Array.set( array, array_index, Boolean.valueOf( sub_value ));
									
									}else if ( bit.equals("url")){
										
										Array.set( array, array_index, new URL(sub_value));
									
									}else if ( bit.equals("byte[]")){
										
										Array.set( array, array_index, ByteFormatter.decodeString( sub_value ));
									
									}else{
											// see if its an object
										
										SimpleXMLParserDocumentNode	obj_node = array_child.getChild("OBJECT");
										
										if ( obj_node != null ){
											
											String	oid_str = obj_node.getChild("_object_id").getValue().trim();
											
											long oid = Long.parseLong( oid_str );
											
											RPObject	local_obj = RPObject._lookupLocal( oid );
											
											Array.set( array, array_index, local_obj );
																			
										}else{
										
											throw( new RuntimeException( "not implemented"));										
										}
									}
								}

							}else{
							
								for (int j=0;j<entries.length;j++){
									
									SimpleXMLParserDocumentNode	array_child = entries[j];
									
									if ( sub_type == String.class ){
										
										Array.set( array, j, child.getValue().trim());
										
									}else{
									
										throw( new RuntimeException( "not implemented"));
									}
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
			
			Debug.printStackTrace( e );
			
			throw( new RuntimeException( e.toString()));
		}
	}
	
	protected void
	serialiseObject(
		Object		obj,
		String		indent,
		int			original_modifier_filter )
	{
		int modifier_filter = original_modifier_filter & (~(Modifier.TRANSIENT | Modifier.STATIC));
		
		Class	cla = obj.getClass();
		
		// System.out.println(indent + "ser:" + cla.getName());
		
		if ( cla.isArray()){
			
			int	len = Array.getLength( obj );
			
				// byte[] -> hex chars
			
			if ( cla.getComponentType() == byte.class ){
			
				byte[] data = (byte[])obj;
				
				writeLineEscaped( ByteFormatter.nicePrint( data, true ));
				
			}else{
				
				for (int i=0;i<len;i++){
					
					Object	entry = Array.get( obj, i );
					
					try{
						writeLineRaw( "<ENTRY index=\""+i+"\">" );
						
						indent();
					
						serialiseObject( entry, indent+"  ", original_modifier_filter);
						
					}finally{
						
						exdent();
						
						writeLineRaw( "</ENTRY>");
					}
				}
			}
			
			return;
		}
		
		if ( cla == String.class ){
			
			writeLineEscaped( "" + (String)obj);
				
		}else if ( cla == Integer.class ){
			
			writeLineEscaped( "" + ((Integer)obj).intValue());
				
		}else if ( cla == Boolean.class ){
		
			writeLineEscaped( "" + ((Boolean)obj).booleanValue());
				
		}else{
			
			while( cla != null ){
				try{
					Field[] fields = cla.getDeclaredFields();
					
					for (int i=0;i<fields.length;i++){
						
						Field	field = fields[i];
						
						int	modifiers = field.getModifiers();
						
						if ((( modifiers | modifier_filter ) == modifier_filter )){
							
							String	name = field.getName();
							
							Class	type = field.getType();
											
							// System.out.println( indent + "  field:" + field.getName() + ", type = " + type );
							
							try{
								writeLineRaw( "<" + name + ">" );
								
								indent();
														
								if ( type == String.class ){
									
									writeLineEscaped( (String)field.get( obj ));
									
								}else if ( type == Integer.class ){
									
									writeLineEscaped( ""+((Integer)field.get( obj )).intValue());
								
								}else if ( type == Boolean.class ){
									
									writeLineEscaped( ""+((Boolean)field.get( obj )).booleanValue());
								
								}else if ( type == long.class ){
									
									writeLineEscaped( ""+field.getLong( obj ));
									
								}else if ( type == boolean.class ){
									
									writeLineEscaped( ""+field.getBoolean( obj ));
									
								}else if ( type == byte.class ){
									
									writeLineEscaped( ""+field.getByte( obj ));
									
								}else if ( type == char.class ){
									
									writeLineEscaped( ""+field.getChar( obj ));
									
								}else if ( type == double.class ){
									
									writeLineEscaped( ""+field.getDouble( obj ));
									
								}else if ( type == float.class ){
									
									writeLineEscaped( ""+field.getFloat( obj ));
									
								}else if ( type == int.class ){
									
									writeLineEscaped( ""+field.getInt( obj ));
									
								}else if ( type == short.class ){
									
									writeLineEscaped( ""+field.getShort( obj ));
									
								}else if ( type == Long.class ){
									
									writeLineEscaped( ""+field.get( obj ));
									
								}else{
									
									serialiseObject( field.get(obj), indent + "    ", original_modifier_filter );
								}
								
							}finally{
								
								exdent();
								
								writeLineRaw( "</" + name + ">" );
							}
						}
					}
									
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
					
					throw( new RuntimeException( e.toString()));
				}
				
				cla = cla.getSuperclass();
			}
		}
	}
}
