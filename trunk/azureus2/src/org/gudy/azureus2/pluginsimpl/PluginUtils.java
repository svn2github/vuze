/*
 * Created on 28-Apr-2004
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

package org.gudy.azureus2.pluginsimpl;

/**
 * @author parg
 *
 */

public class 
PluginUtils 
{
		/**
		 * compare two version strings of form n.n.n.n (e.g. 1.2.3.4)
		 * @param version_1	
		 * @param version_2
		 * @return -ve -> version_1 higher, 0 = same, +ve -> version_2 higher
		 */
	
	public static int
	comparePluginVersions(
		String		version_1,
		String		version_2 )
	{
		boolean	done = false;
		
		for (int j=0;j<Math.min(version_2.length(), version_1.length());j++){
			
			char	v1_c	= version_1.charAt(j);
			char	v2_c	= version_2.charAt(j);
			
			if ( v1_c == v2_c ){
				
				continue;
			}
			
			if ( v2_c == '.' ){
				
					// version1 higher (e.g. 10.2 -vs- 1.2)
				
				return( -1 );
				
			}else if ( v1_c == '.' ){
				
					// version2 higher ( e.g. 1.2 -vs- 10.2 )
				
				return( 1 );
								
			}else{
				
				return( v2_c - v1_c );
			}
		}
		
			// longest one wins. e.g. 1.2.1 -vs- 1.2
		
		return( version_2.length() - version_1.length());
	}
}
