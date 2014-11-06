/*
 * Created on 24-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package org.gudy.azureus2.pluginsimpl.local.utils.xml.simpleparser;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;

public class 
SimpleXMLParserDocumentFactoryImpl 
	implements SimpleXMLParserDocumentFactory
{
	public SimpleXMLParserDocument
	create(
		File		file )
		
		throws SimpleXMLParserDocumentException
	{
		return( new SimpleXMLParserDocumentImpl( file ));
	}
	
	public SimpleXMLParserDocument
	create(
		InputStream		is )
		
		throws SimpleXMLParserDocumentException
	{
		return( new SimpleXMLParserDocumentImpl( null, is ));
	}
	
	public SimpleXMLParserDocument
	create(
		URL				source_url,
		InputStream		is )
		
		throws SimpleXMLParserDocumentException
	{
		return( new SimpleXMLParserDocumentImpl( source_url, is ));
	}
	
	public SimpleXMLParserDocument
	create(
		String		data )
		
		throws SimpleXMLParserDocumentException
	{
		return( new SimpleXMLParserDocumentImpl( data ));
	}
}
