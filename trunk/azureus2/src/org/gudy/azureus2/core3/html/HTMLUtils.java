/*
 * Created on 30-Nov-2004
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

package org.gudy.azureus2.core3.html;

import java.util.ArrayList;
import java.util.List;

/**
 * @author parg
 *
 */
public class 
HTMLUtils 
{
		/**
		 * returns a list of strings for each line in a basic text representation
		 * @param indent
		 * @param text
		 * @return
		 */
	
	public static List
	convertHTMLToText(
		String		indent,
		String		text )
	{
		int		pos = 0;
		
		String	lc_text = text.toLowerCase();
		
		List	lines = new ArrayList();
		
		while( true ){
			
			String	line;
			
			String[]	tokens = new String[]{ "<br>", "<p>" };
			
			String	token 	= null;
			int		p1		= -1;
			
			for (int i=0;i<tokens.length;i++){
				
				int	x = lc_text.indexOf( tokens[i], pos );
				
				if ( x != -1 ){
					if ( p1 == -1 || x < p1 ){
						token	= tokens[i];
						p1		= x;
					}
				}
			}

			if ( p1 == -1 ){
				
				line = text.substring(pos);
				
			}else{
				
				line = text.substring(pos,p1);
				
				pos = p1+token.length();
			}
			
			lines.add( indent + line );
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( lines );
	}
}
