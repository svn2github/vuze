/*
 * Created on Jun 9, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.util;

public class 
GeneralUtils 
{
		/**
		 * string.replaceAll does \\ and $ expansion in replacement, this doesn't, in fact it
		 * doesn't do any pattern matching at all, it is a literal replacement
		 * @param str
		 * @param from_str		= NOTE, no regex support
		 * @param replacement
		 * @return
		 */
	
	public static String
	replaceAll(
		String	str,
		String	from_str,
		String	replacement )
	{
		StringBuffer	res = null;
				
		int	pos = 0;
		
		while( true ){
		
			int	p1 = str.indexOf( from_str, pos );
			
			if ( p1 == -1 ){
				
				if ( res == null ){
				
					return( str );
				}
				
				res.append( str.substring( pos ));
				
				return( res.toString());
				
			}else{
				
				if ( res == null ){
					
					res = new StringBuffer( str.length() * 2 );
				}
				
				if ( p1 > pos ){
					
					res.append( str.substring( pos, p1 ));
				}
				
				res.append( replacement );
				
				pos = p1 + from_str.length();
			}
		}
	}
}
