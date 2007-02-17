/*
 * Created on Feb 15, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.util;

import java.util.HashMap;
import java.util.Map;

public class 
HTTPUtils 
{
	private static final String[][]	type_map = {
			{ "html",		"text/html" },
			{ "htm",		"text/html" },
			{ "css",		"text/css" },
			{ "xml",		"text/xml" },
			{ "xsl",		"text/xml" },
			{ "jpg",		"image/jpeg" },
			{ "jpeg",		"image/jpeg" },
			{ "gif",		"image/gif" },
			{ "tiff",		"image/tiff" },
			{ "bmp",		"image/bmp" },
			{ "png",		"image/png" },
			{ "torrent",	"application/x-bittorrent" },
			{ "tor",		"application/x-bittorrent" },
			{ "zip",		"application/zip" },
			{ "txt",		"text/plain" },
			{ "jar",		"application/java-archive" },
			{ "jnlp",		"application/x-java-jnlp-file" },
			{ "mp3",		"audio/x-mpeg" },
			{ "flv",		"video/x-flv" },
	};
	
	private static final String default_type = "application/octet-stream";
	
	private static final Map	map = new HashMap();
	
	static{
		
		for (int i=0;i<type_map.length;i++){
			
			map.put( type_map[i][0], type_map[i][1] );
		}
	}
	
	public static String
	guessContentTypeFromFileType(
		String	file_type )
	{
		if ( file_type != null ){
			
			String	type = (String)map.get( file_type.toLowerCase());

			if ( type != null ){
				
				return( type );
			}
		}
		
		return( default_type );
	}
}
