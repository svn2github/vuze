/*
 * File    : ED2KHasher.java
 * Created : 16-Feb-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

public class 
ED2KHasher 
{
	public static final int	BLOCK_SIZE = 0x947000;
	
	protected MD4Hasher	current_hasher	= new MD4Hasher();
	protected MD4Hasher	block_hasher;
	
	protected int	current_bytes;
	
	public
	ED2KHasher()
	{
		
	}
	
	public void
	update(
		byte[]		data )
	{
		int		rem = data.length;
		int		pos	= 0;
		
		while( rem > 0 ){
			
			int	space = BLOCK_SIZE - current_bytes;
			
			if ( rem <= space ){
				
				current_hasher.update( data, pos, rem );
				
				current_bytes += rem;
				
				break;
				
			}else{
				
				if ( block_hasher == null ){
					
					block_hasher = new MD4Hasher();
				}
				
				if ( space == 0 ){
			
					block_hasher.update( current_hasher.getDigest());
					
					current_hasher = new MD4Hasher();
					
					current_bytes = 0;
					
				}else{
					
					current_hasher.update( data, pos, space );
					
					pos 			+= space;
					rem				-= space;
					current_bytes	+= space;
				}
			}
		}
	}
	
	public byte[]
	getDigest()
	{
		if ( block_hasher == null ){
			
			return( current_hasher.getDigest());
			
		}else{
		
			if ( current_bytes > 0 ){
				
				block_hasher.update( current_hasher.getDigest());
			}
			
			return( block_hasher.getDigest());
		}
	}
}
