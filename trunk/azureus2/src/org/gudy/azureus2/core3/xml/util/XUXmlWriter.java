/*
 * File    : XUXmlWriter.java
 * Created : 23-Oct-2003
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
 
package org.gudy.azureus2.core3.xml.util;

/**
 * @author parg
 */

import java.io.*;

import org.gudy.azureus2.core3.util.*;

public class 
XUXmlWriter 
{
	private static final int			INDENT_AMOUNT	= 4;
	
	private String						current_indent_string;

	private PrintWriter					writer;
		
	protected
	XUXmlWriter()
	{
		resetIndent();
	}
	
	protected
	XUXmlWriter(
		OutputStream	_output_stream )
	{
		setOutputStream( _output_stream );
		
		resetIndent();
	}

	protected void
	setOutputStream(
		OutputStream	_output_stream )
	{
		try{
		
			writer	= new PrintWriter( new OutputStreamWriter(_output_stream , Constants.DEFAULT_ENCODING ));

		}catch( UnsupportedEncodingException e ){
			
			e.printStackTrace();
			
			writer = new PrintWriter( _output_stream );
		}
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
		long		content )
	{
		writeLine( "<" + tag + ">" + content + "</" + tag + ">" );	
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
		if ( current_indent_string.length() >= INDENT_AMOUNT ){
		
			current_indent_string = current_indent_string.substring(0,current_indent_string.length()-INDENT_AMOUNT);
		}else{
			
			current_indent_string	= "";
		}
	}
	
	protected String
	escapeXML(
		String	str )
	{
		if ( str == null ){
			
			return( "" );
			
		}
		str = str.replaceAll( "&", "&amp;" );
		str = str.replaceAll( ">", "&gt;" );
		str = str.replaceAll( "<", "&lt;" );
		str = str.replaceAll( "\"", "&quot;" );
		str = str.replaceAll( "--", "&#45;&#45;" );
		
		return( str );
	}
	
	protected void
	closeOutputStream()
	{
		if ( writer != null ){
								
			writer.flush();
					
			writer.close();
			
			writer	= null;
		}
	}
}
