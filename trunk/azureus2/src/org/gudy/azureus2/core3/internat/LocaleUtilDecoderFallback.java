/*
 * Created on 21-Jun-2004
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

package org.gudy.azureus2.core3.internat;

import java.io.UnsupportedEncodingException;
import org.gudy.azureus2.core3.util.*;

/**
 * @author parg
 *
 */

public class 
LocaleUtilDecoderFallback 
	implements LocaleUtilDecoder
{
	protected static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890_-.";
	
	public String
	getName()
	{
		return( "Fallback" );
	}

	public String
	tryDecode(
		byte[]		bytes,
		boolean		lax )
	{
		return( decode( bytes ));
	}
	
	public String
	decodeString(
		byte[]		bytes )
		
		throws UnsupportedEncodingException
	{
		return( decode( bytes ));
	}
	
	protected String
	decode(
		byte[]	data )
	{
		if ( data == null ){
			
			return( null );
		}
		
		String	res = "";
		
		for (int i=0;i<data.length;i++){
			
			byte	c = data[i];
			
			if ( VALID_CHARS.indexOf( Character.toLowerCase((char)c)) != -1 ){
				
				res += (char)c;
				
			}else{
				
				res += "_" + ByteFormatter.nicePrint(c);
			}
		}
		
		return( res );
	}
}
