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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

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
		
		int orderedIndex = 0;
		
		text = text.replaceAll("<ol>","");
		text = text.replaceAll("</ol>","");
		text = text.replaceAll("<ul>","");
		text = text.replaceAll("</ul>","");
		text = text.replaceAll("</li>","");
		text = text.replaceAll("<li>","\n\t*");
		
		String lc_text = text.toLowerCase();
		
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
	
	public static String convertListToString(List list) {
	  
	  StringBuffer result = new StringBuffer();
	  String separator = "";
	  Iterator iter = list.iterator();
	  while(iter.hasNext()) {
	    String line = iter.next().toString();
	    result.append(separator);
	    result.append(line);
	    separator = "\n";
	  }
	  
	  return result.toString();
	}
	
	public static String
	convertHTMLToText2(
		String		content )
	{
		int	pos	= 0;

		String	res = "";
			
		content = removeTagPairs( content, "script" );
		
		content = content.replaceAll( "&nbsp;", " " );

		content = content.replaceAll( "[\\s]+", " " );

		while(true){
			
			int	p1 = content.indexOf( "<",  pos );
			
			if ( p1 == -1 ){
				
				res += content.substring(pos);
				
				break;
			}
			
			int	p2 = content.indexOf( ">", p1 );
			
			if ( p2 == -1 ){
				
				res += content.substring(pos);
				
				break;
			}

			String	tag = content.substring(p1+1,p2).toLowerCase();
				
			res += content.substring(pos,p1);
			
			if ( tag.equals("p") || tag.equals("br")){
				
				if ( res.length() > 0 && res.charAt(res.length()-1) != '\n' ){
				
					res += "\n";
				}
			}
		
			pos	= p2+1;
		}
		
		res = res.replaceAll( "[ \\t\\x0B\\f\\r]+", " " );
		res = res.replaceAll( "[ \\t\\x0B\\f\\r]+\\n", "\n" );
		res = res.replaceAll( "\\n[ \\t\\x0B\\f\\r]+", "\n" );
		
		if ( res.length() > 0 && Character.isWhitespace(res.charAt(0))){
			
			res = res.substring(1);
		}
		
		return( res );
	}
	
	public static String
	splitWithLineLength(
		String		str,
		int			length )
	{
		String	res = "";
		
		StringTokenizer tok = new StringTokenizer(str, "\n");
		
		while( tok.hasMoreTokens()){
			
			String	line = tok.nextToken();
			
			while( line.length() > length ){
			
				if ( res.length() > 0 ){
					
					res += "\n";
				}
	
				boolean	done = false;
				
				for (int i=length-1;i>=0;i--){
					
					if ( Character.isWhitespace( line.charAt(i))){
						
						done	= true;
				
						res += line.substring(0,i);
						
						line = line.substring(i+1);
						
						break;
					}
				}
				
				if ( !done ){
					
					res += line.substring(0,length);
					
					line = line.substring( length );
				}
			}
		
			if ( res.length() > 0 && line.length() > 0 ){
				
				res += "\n";

				res += line;
			}
		}
		
		return( res );
	}
	
	public static String
	removeTagPairs(
		String	content,
		String	tag_name )
	{
		tag_name = tag_name.toLowerCase();
		
		String	lc_content = content.toLowerCase();
		
		int	pos	= 0;

		String	res = "";
		
		int	level 		= 0;
		int	start_pos	= -1;
		
		while(true){
			
			int	start_tag_start = lc_content.indexOf( "<" + tag_name,  pos );
			int end_tag_start	= lc_content.indexOf( "</" + tag_name, pos );
			
			if ( level == 0 ){
				
				if ( start_tag_start == -1 ){
					
					res += content.substring(pos);

					break;
				}
				
				res += content.substring(pos,start_tag_start);						

				start_pos = start_tag_start;
				
				level	= 1;
				
				pos		= start_pos+1;
				
			}else{
				
				if ( end_tag_start == -1 ){
					
					res += content.substring(pos);

					break;
				}
				
				if ( start_tag_start == -1 || end_tag_start < start_tag_start ){
					
					level--;
					
					int	end_end = lc_content.indexOf( '>', end_tag_start );
					
					if( end_end == -1 ){
						
						break;
					}
					
					pos	= end_end + 1;
					
				}else{
					
					if ( start_tag_start == -1 ){
						
						res += content.substring(pos);

						break;
					}
					
					level++;
					
					pos = start_tag_start+1;
				}
			}
		}
			
		return( res );
	}
}
