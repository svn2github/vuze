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

import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;

public class 
XMLRequestProcessor
	extends XUXmlWriter
{
	protected PluginInterface				plugin_interface;
	protected SimpleXMLParserDocument		request;
	
	protected
	XMLRequestProcessor(
		PluginInterface			_plugin_interface,
		InputStream				_request,
		OutputStream			_reply )
	{
		super( _reply );
		
		plugin_interface	= _plugin_interface;
		
		try{
			System.out.println( "got:" + _request );
			
			request = SimpleXMLParserDocumentFactory.create( _request );
				
			process();
					
		}catch( Throwable e ){
			
				// TODO: need new outputstream here
			
			writeLine( "<RESPONSE>" );
			
			indent();
			
			e.printStackTrace();
			
			writeTag("ERROR", e.toString());
			
			exdent();
			
			writeLine( "</RESPONSE>" );

		}finally{
			
			flushOutputStream();
		}
	}
	
	protected void
	process()
	{
		try{
			writeLine( "<RESPONSE>" );
			
			indent();
			
			request.print();
					
			SimpleXMLParserDocumentNode oid_node 	= request.getChild( "OID" );
			SimpleXMLParserDocumentNode method_node = request.getChild( "METHOD" );
			
			String	oid		= oid_node.getValue();
			String	method	= method_node.getValue();
			
			System.out.println( "oid=" + oid + ", method = " + method );
			
			if ( method.equals( "getSingleton")){
				
				RPObject	obj = RPPluginInterface.create(plugin_interface);
				
				writeTag( "OID", obj._getOID());
				
			}
		}finally{
		
			exdent();
			
			writeLine( "</RESPONSE>" );
		}
	}
}
