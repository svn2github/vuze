/*
 * File    : LocaleUtilEncoding.java
 * Created : 18-Nov-2003
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

package org.gudy.azureus2.core3.internat;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;

import org.gudy.azureus2.core3.util.*;

public class 
LocaleUtilDecoder
{
	protected CharsetDecoder	decoder;
	protected String			name;
	
	protected
	LocaleUtilDecoder(
		CharsetDecoder	_decoder,
		String			_name )
	{
		decoder		= _decoder;
		name		= _name;
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public CharsetDecoder
	getDecoder()
	{
		return( decoder );
	}
	
	public String
	decodeString(
		byte[]		bytes )
		
		throws UnsupportedEncodingException
	{
		if ( bytes == null ){
			
			return( null );
		}
		
		ByteBuffer bb = ByteBuffer.wrap(bytes);
      		
		CharBuffer cb = CharBuffer.allocate(bytes.length);
      		
		CoderResult cr = decoder.decode(bb,cb, true);
			
		try{
			
			if ( !cr.isError() ){
								
				cb.flip();
					
				String	str = cb.toString();
					
				byte[]	b2 = str.getBytes(decoder.charset().name());
					
					// make sure the conversion is symetric (there are cases where it appears
					// to work but in fact converting back to bytes leads to a different
					// result
						
				/*
				for (int k=0;k<str.length();k++){
					System.out.print( Integer.toHexString(str.charAt(k)));
				}
				System.out.println("");
				*/
					
				if ( Arrays.equals( bytes, b2 )){
					
					return( str );
				}
			}
		}catch( UnsupportedEncodingException e ){
			
				// ignore
		}
		
		try{
		
				// no joy, default
		
			return( new String( bytes, Constants.DEFAULT_ENCODING ));
			
		}catch( UnsupportedEncodingException e ){
			
			e.printStackTrace();
			
			return( new String( bytes ));
		}
	}
}
