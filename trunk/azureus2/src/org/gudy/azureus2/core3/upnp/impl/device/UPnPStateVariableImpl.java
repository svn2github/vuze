/*
 * Created on 15-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.upnp.impl.device;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.xml.simpleparser.*;

public class 
UPnPStateVariableImpl
	implements UPnPStateVariable
{
	protected UPnPServiceImpl		service;
	protected String				name;
	
	protected 
	UPnPStateVariableImpl(
		UPnPServiceImpl					_service,
		SimpleXMLParserDocumentNode		node )
	{
		service	= _service;
		
		name	= node.getChild( "name" ).getValue();
	}

	public String
	getName()
	{
		return( name );
	}
	
	public UPnPService
	getService()
	{
		return( service );
	}
	
	public String
	getValue()
	
		throws UPnPException
	{
		try{
			String	soap_action = "urn:schemas-upnp-org:control-1-0#QueryStateVariable";
			
			String	request =
				"<?xml version=\"1.0\"?>\n"+
				"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"+
					"<s:Body>\n";
													
			request += 	"<u:QueryStateVariable xmlns:u=\"urn:schemas-upnp-org:control-1-0\">\n" +
							"<u:varName>" + name + "</u:varName>\n" +
						"</u:QueryStateVariable>\n";

			request += "</s:Body\n>"+
						"</s:Envelope>\n";
							
				// try standard POST
			
			InputStream	is;
			
			URL	control = service.getControlURL();
			
			HttpURLConnection	con = (HttpURLConnection)control.openConnection();
			
			con.setRequestProperty( "SOAPACTION", "\""+ soap_action + "\"");
			
			con.setRequestProperty( "Content-Type", "text/xml; charset=\"utf-8\"" );
			
			con.setRequestMethod( "POST" );
			
			con.setDoInput( true );
			con.setDoOutput( true );
			
			OutputStream	os = con.getOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter(os, "UTF-8" ));
						
			pw.println( request );
			
			pw.flush();

			con.connect();
			
			if ( con.getResponseCode() == 405 ){
				
					// gotta retry with M-POST method
								
				con = (HttpURLConnection)control.openConnection();
				
				con.setRequestProperty( "Content-Type", "text/xml; charset=\"utf-8\"" );
				
				con.setRequestMethod( "M-POST" );
				
				con.setRequestProperty( "MAN", "\"http://schemas.xmlsoap.org/soap/envelope/\"; ns=01" );

				con.setRequestProperty( "01-SOAPACTION", "\""+ soap_action + "\"");
				
				con.setDoInput( true );
				con.setDoOutput( true );
				
				os = con.getOutputStream();
				
				pw = new PrintWriter( new OutputStreamWriter(os, "UTF-8" ));
							
				pw.println( request );
				
				pw.flush();
	
				con.connect();
			
				is = con.getInputStream();	
				
			}else{
				
				is = con.getInputStream();
			}
			
			SimpleXMLParserDocument resp_doc = service.getDevice().getUPnP().parseXML( is );
			
			SimpleXMLParserDocumentNode	body = resp_doc.getChild( "Body" );
			
			SimpleXMLParserDocumentNode fault = body.getChild( "Fault" );
			
			if ( fault != null ){
				
				throw( new UPnPException( "Invoke fails - fault reported: " + fault.getValue()));
			}
			
			SimpleXMLParserDocumentNode	resp_node = body.getChild( "QueryStateVariableResponse" );
			
			if ( resp_node == null ){
				
				throw( new UPnPException( "Invoke fails - response missing: " + body.getValue()));
			}
			
			SimpleXMLParserDocumentNode	value_node = resp_node.getChild( "return" );
			
			return( value_node.getValue());
			
		}catch( Throwable e ){
			
			if ( e instanceof UPnPException ){
				
				throw((UPnPException)e);
			}
			
			throw( new UPnPException( "Invoke fails", e ));	
		}
	}
}