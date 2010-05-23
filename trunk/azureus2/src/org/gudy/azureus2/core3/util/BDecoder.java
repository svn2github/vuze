/*
 * BeDecoder.java
 *
 * Created on May 30, 2003, 2:44 PM
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.core3.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A set of utility methods to decode a bencoded array of byte into a Map.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 * 
 * @author TdC_VgA
 *
 */
public class BDecoder 
{
	private static final int MAX_BYTE_ARRAY_SIZE	= 16*1024*1024;
	private static final int MAX_MAP_KEY_SIZE		= 64*1024;
	
	private static final boolean TRACE	= false;
	
	private boolean recovery_mode;

	public static Map
	decode(
		byte[]	data )

		throws IOException
	{
		return( new BDecoder().decodeByteArray( data ));
	}

	public static Map
	decode(
		byte[]	data,
		int		offset,
		int		length )

		throws IOException
	{
		return( new BDecoder().decodeByteArray( data, offset, length ));
	}

	public static Map
	decode(
		BufferedInputStream	is  )

		throws IOException
	{
		return( new BDecoder().decodeStream( is ));
	}


	public 
	BDecoder() 
	{	
	}

	public Map 
	decodeByteArray(
		byte[] data) 

		throws IOException 
	{ 
		return( decode(new BDecoderInputStreamArray(data),true));
	}

	public Map 
	decodeByteArray(
		byte[] 	data,
		int		offset,
		int		length )

		throws IOException 
	{ 
		return( decode(new BDecoderInputStreamArray(data, offset, length ),true));
	}
	
	public Map 
	decodeByteArray(
		byte[] 	data,
		int		offset,
		int		length,
		boolean internKeys)

		throws IOException 
	{ 
		return( decode(new BDecoderInputStreamArray(data, offset, length ),internKeys));
	}
	
	public Map decodeByteBuffer(ByteBuffer buffer, boolean internKeys) throws IOException {
		return decode(new BDecoderInputStreamArray(buffer),internKeys);
	}
	
	public Map 
	decodeStream(
		BufferedInputStream data )  

		throws IOException 
	{
		return decodeStream(data, true);
	}

	public Map 
	decodeStream(
		BufferedInputStream data,
		boolean internKeys)  

		throws IOException 
	{
		Object	res = decodeInputStream(data, 0, internKeys);

		if ( res == null ){

			throw( new BEncodingException( "BDecoder: zero length file" ));

		}else if ( !(res instanceof Map )){

			throw( new BEncodingException( "BDecoder: top level isn't a Map" ));
		}

		return((Map)res );
	}

	private Map 
	decode(
		InputStream data, boolean internKeys ) 

		throws IOException 
	{
		Object res = decodeInputStream(data, 0, internKeys);

		if ( res == null ){

			throw( new BEncodingException( "BDecoder: zero length file" ));

		}else if ( !(res instanceof Map )){

			throw( new BEncodingException( "BDecoder: top level isn't a Map" ));
		}

		return((Map)res );
	}
	
	// reuseable buffer for keys, recursion is not an issue as this is only a temporary buffer that gets converted into a string immediately
	private ByteBuffer keyBytesBuffer = ByteBuffer.allocate(32);

	private Object 
	decodeInputStream(
		InputStream dbis,
		int			nesting,
		boolean internKeys) 

		throws IOException 
	{
		if (nesting == 0 && !dbis.markSupported()) {

			throw new IOException("InputStream must support the mark() method");
		}

			//set a mark
		
		dbis.mark(Integer.MAX_VALUE);

			//read a byte
		
		int tempByte = dbis.read();

			//decide what to do
		
		switch (tempByte) {
		case 'd' :
				//create a new dictionary object
			
			Map tempMap = new LightHashMap();

			try{
					//get the key   
				
				while (true) {
					
					dbis.mark(Integer.MAX_VALUE);

					tempByte = dbis.read();
					if(tempByte == 'e' || tempByte == -1)
						break; // end of map

					dbis.reset();
					
					// decode key strings manually so we can reuse the bytebuffer

					int keyLength = (int)getNumberFromStream(dbis, ':');

					ByteBuffer keyBytes;
					if(keyLength < keyBytesBuffer.capacity())
					{
						keyBytes = keyBytesBuffer;
						keyBytes.position(0).limit(keyLength);
					} else {
						keyBytes = keyBytesBuffer = ByteBuffer.allocate(keyLength);
					}
					
					getByteArrayFromStream(dbis, keyLength, keyBytes.array());						

					if ( keyLength > MAX_MAP_KEY_SIZE ){
						String msg = "dictionary key is too large, max=" + MAX_MAP_KEY_SIZE + ": value=" + new String( keyBytes.array(), 0, 128 );
						System.err.println( msg );
						throw( new IOException( msg ));
					}
					
					CharBuffer	cb = Constants.BYTE_CHARSET.decode(keyBytes);
					String key = new String(cb.array(),0,cb.limit());
					
					// keys often repeat a lot - intern to save space
					if (internKeys)
						key = StringInterner.intern( key );
					
					

					//decode value

					Object value = decodeInputStream(dbis,nesting+1,internKeys);
					
					// value interning is too CPU-intensive, let's skip that for now
					/*if(value instanceof byte[] && ((byte[])value).length < 17)
					value = StringInterner.internBytes((byte[])value);*/

					if ( TRACE ){
						System.out.println( key + "->" + value + ";" );
					}
					
						// recover from some borked encodings that I have seen whereby the value has
						// not been encoded. This results in, for example, 
						// 18:azureus_propertiesd0:e
						// we only get null back here if decoding has hit an 'e' or end-of-file
						// that is, there is no valid way for us to get a null 'value' here
					
					if ( value == null ){
						
						System.err.println( "Invalid encoding - value not serialsied for '" + key + "' - ignoring" );
						
						break;
					}
				
					if ( tempMap.put( key, value) != null ){
						
						Debug.out( "BDecoder: key '" + key + "' already exists!" );
					}
				}

				/*	
	        if ( tempMap.size() < 8 ){

	        	tempMap = new CompactMap( tempMap );
	        }*/

				dbis.mark(Integer.MAX_VALUE);
				tempByte = dbis.read();
				dbis.reset();
				if ( nesting > 0 && tempByte == -1 ){

					throw( new BEncodingException( "BDecoder: invalid input data, 'e' missing from end of dictionary"));
				}
			}catch( Throwable e ){

				if ( !recovery_mode ){

					if ( e instanceof IOException ){

						throw((IOException)e);
					}

					throw( new IOException( Debug.getNestedExceptionMessage(e)));
				}
			}

			if (tempMap instanceof LightHashMap)
				((LightHashMap) tempMap).compactify(-0.9f);


				//return the map
			
			return tempMap;

		case 'l' :
				//create the list
			
			ArrayList tempList = new ArrayList();

			try{
					//create the key
				
				Object tempElement = null;
				while ((tempElement = decodeInputStream(dbis, nesting+1, internKeys)) != null) {
						//add the element
					tempList.add(tempElement);
				}

				tempList.trimToSize();
				dbis.mark(Integer.MAX_VALUE);
				tempByte = dbis.read();
				dbis.reset();
				if ( nesting > 0 && tempByte == -1 ){

					throw( new BEncodingException( "BDecoder: invalid input data, 'e' missing from end of list"));
				}
			}catch( Throwable e ){

				if ( !recovery_mode ){

					if ( e instanceof IOException ){

						throw((IOException)e);
					}

					throw( new IOException( Debug.getNestedExceptionMessage(e)));
				}
			}
				//return the list
			return tempList;

		case 'e' :
		case -1 :
			return null;

		case 'i' :
			return Long.valueOf(getNumberFromStream(dbis, 'e'));

		case '0' :
		case '1' :
		case '2' :
		case '3' :
		case '4' :
		case '5' :
		case '6' :
		case '7' :
		case '8' :
		case '9' :
				//move back one
			dbis.reset();
				//get the string
			return getByteArrayFromStream(dbis);

		default :{

			int	rem_len = dbis.available();

			if ( rem_len > 256 ){

				rem_len	= 256;
			}

			byte[] rem_data = new byte[rem_len];

			dbis.read( rem_data );

			throw( new BEncodingException(
					"BDecoder: unknown command '" + tempByte + ", remainder = " + new String( rem_data )));
		}
		}
	}

	/*
  private long getNumberFromStream(InputStream dbis, char parseChar) throws IOException {
    StringBuffer sb = new StringBuffer(3);

    int tempByte = dbis.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
    	sb.append((char)tempByte);
      tempByte = dbis.read();
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    String str = sb.toString();

    	// support some borked impls that sometimes don't bother encoding anything

    if ( str.length() == 0 ){

    	return( 0 );
    }

    return Long.parseLong(str);
  }
	 */

	/** only create the array once per decoder instance (no issues with recursion as it's only used in a leaf method)
	 */
	private final char[] numberChars = new char[32];
	
	private long 
	getNumberFromStream(
		InputStream 	dbis, 
		char 					parseChar) 

		throws IOException 
	{
		

		int tempByte = dbis.read();

		int pos = 0;

		while ((tempByte != parseChar) && (tempByte >= 0)) {
			numberChars[pos++] = (char)tempByte;
			if ( pos == numberChars.length ){
				throw( new NumberFormatException( "Number too large: " + new String(numberChars,0,pos) + "..." ));
			}
			tempByte = dbis.read();
		}

		//are we at the end of the stream?

		if (tempByte < 0) {

			return -1;

		}else if ( pos == 0 ){
			// support some borked impls that sometimes don't bother encoding anything

			return(0);
		}

		try{
			return( parseLong( numberChars, 0, pos ));
			
		}catch( NumberFormatException e ){
			
			String temp = new String( numberChars, 0, pos );
			
			try{
				double d = Double.parseDouble( temp );
				
				long l = (long)d;
				
				Debug.out( "Invalid number '" + temp + "' - decoding as " + l + " and attempting recovery" );
					
				return( l );
				
			}catch( Throwable f ){
			}
			
			throw( e );
		}
	}

	// This is similar to Long.parseLong(String) source
	// It is also used in projects external to azureus2/azureus3 hence it is public
	public static long
	parseLong(
		char[]	chars,
		int		start,
		int		length )
	{
		if ( length > 0 ){
			// Short Circuit: We don't support octal parsing, so if it 
			// starts with 0, it's 0
			if (chars[start] == '0') {

				return 0;
			}

			long result = 0;

			boolean negative = false;

			int 	i 	= start;

			long limit;

			if ( chars[i] == '-' ){

				negative = true;

				limit = Long.MIN_VALUE;

				i++;

			}else{
				// Short Circuit: If we are only processing one char,
				// and it wasn't a '-', just return that digit instead
				// of doing the negative junk
				if (length == 1) {
					int digit = chars[i] - '0';

					if ( digit < 0 || digit > 9 ){

						throw new NumberFormatException(new String(chars,start,length));

					}else{

						return digit;
					}
				}

				limit = -Long.MAX_VALUE;
			}

			int	max = start + length;

			if ( i < max ){

				int digit = chars[i++] - '0';

				if ( digit < 0 || digit > 9 ){

					throw new NumberFormatException(new String(chars,start,length));

				}else{

					result = -digit;
				}
			}

			long multmin = limit / 10;

			while ( i < max ){

				// Accumulating negatively avoids surprises near MAX_VALUE

				int digit = chars[i++] - '0';

				if ( digit < 0 || digit > 9 ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				if ( result < multmin ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				result *= 10;

				if ( result < limit + digit ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				result -= digit;
			}

			if ( negative ){

				if ( i > start+1 ){

					return result;

				}else{	/* Only got "-" */

					throw new NumberFormatException(new String(chars,start,length));
				}
			}else{

				return -result;
			}  
		}else{

			throw new NumberFormatException(new String(chars,start,length));
		}

	}



	// This one causes lots of "Query Information" calls to the filesystem
	/*
  private long getNumberFromStreamOld(InputStream dbis, char parseChar) throws IOException {
    int length = 0;

    //place a mark
    dbis.mark(Integer.MAX_VALUE);

    int tempByte = dbis.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
      tempByte = dbis.read();
      length++;
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    //reset the mark
    dbis.reset();

    //get the length
    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;

    //get the string
    while (count != length && (len = dbis.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    //jump ahead in the stream to compensate for the :
    dbis.skip(1);

    //return the value

    CharBuffer	cb = Constants.DEFAULT_CHARSET.decode(ByteBuffer.wrap(tempArray));

    String	str_value = new String(cb.array(),0,cb.limit());

    return Long.parseLong(str_value);
  }
	 */

	private byte[] 
	getByteArrayFromStream(
		InputStream dbis )
		
		throws IOException 
	{
		int length = (int) getNumberFromStream(dbis, ':');

		if (length < 0) {
			return null;
		}

		// note that torrent hashes can be big (consider a 55GB file with 2MB pieces
		// this generates a pieces hash of 1/2 meg

		if ( length > MAX_BYTE_ARRAY_SIZE ){

			throw( new IOException( "Byte array length too large (" + length + ")"));
		}
		
		byte[] tempArray = new byte[length];
		
		getByteArrayFromStream(dbis, length, tempArray);		
		
		return tempArray; 
	}

	private void getByteArrayFromStream(InputStream dbis, int length, byte[] targetArray) throws IOException {

		int count = 0;
		int len = 0;
		//get the string
		while (count != length && (len = dbis.read(targetArray, count, length - count)) > 0)
			count += len;

		if (count != length)
			throw (new IOException("BDecoder::getByteArrayFromStream: truncated"));
	}	

	public void
	setRecoveryMode(
		boolean	r )
	{
		recovery_mode	= r;
	}

	public static void
	print(
		Object		obj )
	{
		StringWriter 	sw = new StringWriter();
		
		PrintWriter		pw = new PrintWriter( sw );
		
		print( pw, obj );
		
		pw.flush();
		
		System.out.println( sw.toString());
	}
	
	public static void
	print(
		PrintWriter	writer,
		Object		obj )
	{
		print( writer, obj, "", false );
	}

	private static void
	print(
		PrintWriter	writer,
		Object		obj,
		String		indent,
		boolean		skip_indent )
	{
		String	use_indent = skip_indent?"":indent;

		if ( obj instanceof Long ){

			writer.println( use_indent + obj );

		}else if ( obj instanceof byte[]){

			byte[]	b = (byte[])obj;

			if ( b.length==20 ){
				writer.println( use_indent + " { "+ ByteFormatter.nicePrint( b )+ " }" );
			}else if ( b.length < 64 ){
				writer.println( new String(b) + " [" + ByteFormatter.encodeString( b ) + "]" );
			}else{
				writer.println( "[byte array length " + b.length );
			}

		}else if ( obj instanceof String ){

			writer.println( use_indent + obj );

		}else if ( obj instanceof List ){

			List	l = (List)obj;

			writer.println( use_indent + "[" );

			for (int i=0;i<l.size();i++){

				writer.print( indent + "  (" + i + ") " );

				print( writer, l.get(i), indent + "    ", true );
			}

			writer.println( indent + "]" );

		}else{

			Map	m = (Map)obj;

			Iterator	it = m.keySet().iterator();

			while( it.hasNext()){

				String	key = (String)it.next();

				if ( key.length() > 256 ){
					writer.print( indent + key.substring(0,256) + "... = " );
				}else{
					writer.print( indent + key + " = " );
				}

				print( writer, m.get(key), indent + "  ", true );
			}
		}
	}

	/**
	 * Converts any byte[] entries into UTF-8 strings
	 * @param map
	 * @return
	 */

	public static Map
	decodeStrings(
		Map	map )
	{
		if (map == null ){

			return( null );
		}

		Iterator it = map.entrySet().iterator();

		while( it.hasNext()){

			Map.Entry	entry = (Map.Entry)it.next();

			Object	value = entry.getValue();

			if ( value instanceof byte[]){

				try{
					entry.setValue( new String((byte[])value,"UTF-8" ));

				}catch( Throwable e ){

					System.err.println(e);
				}
			}else if ( value instanceof Map ){

				decodeStrings((Map)value );
			}else if ( value instanceof List ){

				decodeStrings((List)value );
			}
		}

		return( map );
	}

	public static List
	decodeStrings(
		List	list )
	{
		if ( list == null ){

			return( null );
		}

		for (int i=0;i<list.size();i++){

			Object value = list.get(i);

			if ( value instanceof byte[]){

				try{
					String str = new String((byte[])value, "UTF-8" );

					list.set( i, str );

				}catch( Throwable e ){

					System.err.println(e);
				}
			}else if ( value instanceof Map ){

				decodeStrings((Map)value );

			}else if ( value instanceof List ){

				decodeStrings((List)value );		 
			}
		}

		return( list );
	}

	private static void
	print(
		File		f,
		File		output )
	{
		try{
			BDecoder	decoder = new BDecoder();

			decoder.setRecoveryMode( false );

			PrintWriter	pw = new PrintWriter( new FileWriter( output ));

			print( pw, decoder.decodeStream( new BufferedInputStream( new FileInputStream( f ))));

			pw.flush();

		}catch( Throwable e ){

			e.printStackTrace();
		}
	}
/*
	private interface
	BDecoderInputStream
	{
		public int
		read()

			throws IOException;

		public int
		read(
			byte[] buffer )

			throws IOException;

		public int
		read(
			byte[] 	buffer,
			int		offset,
			int		length )

			throws IOException;

		public int
		available()

			throws IOException;

		public boolean
		markSupported();

		public void
		mark(
				int	limit );

		public void
		reset()

			throws IOException;
	}

	private class
	BDecoderInputStreamStream
	
		implements BDecoderInputStream
	{
		final private BufferedInputStream		is;

		private
		BDecoderInputStreamStream(
			BufferedInputStream	_is )
		{
			is	= _is;
		}

		public int
		read()

		throws IOException
		{
			return( is.read());
		}

		public int
		read(
			byte[] buffer )

		throws IOException
		{
			return( is.read( buffer ));
		}

		public int
		read(
			byte[] 	buffer,
			int		offset,
			int		length )

			throws IOException
		{
			return( is.read( buffer, offset, length ));  
		}

		public int
		available()

			throws IOException
		{
			return( is.available());
		}

		public boolean
		markSupported()
		{
			return( is.markSupported());
		}

		public void
		mark(
			int	limit )
		{
			is.mark( limit );
		}

		public void
		reset()

			throws IOException
		{
			is.reset();
		}
	}
*/
	private class
	BDecoderInputStreamArray
	
		extends InputStream
	{
		final private ByteBuffer	buffer;

		public BDecoderInputStreamArray(ByteBuffer buffer) {
			this.buffer = buffer;
		}
		
		private
		BDecoderInputStreamArray(
			byte[]		_buffer )
		{
			buffer	= ByteBuffer.wrap(_buffer);
		}

		private
		BDecoderInputStreamArray(
			byte[]		_buffer,
			int			_offset,
			int			_length )
		{
			buffer = ByteBuffer.wrap(_buffer, _offset, _length);

		}
		
		public int
		read()

			throws IOException
		{
			if(buffer.remaining() > 0)
				return buffer.get() & 0xFF;
			else
				return -1;
		}

		public int
		read(
			byte[] buffer )

			throws IOException
		{
			return( read( buffer, 0, buffer.length ));
		}

		public int
		read(
			byte[] 	b,
			int		offset,
			int		length )

			throws IOException
		{
			
			if (!buffer.hasRemaining() )
				return -1;
			
			int toRead = Math.min(length, buffer.remaining());
			
			buffer.get(b, offset, toRead);


			return( toRead );
		}

		public int
		available()

			throws IOException
		{
			return buffer.remaining();
		}

		public boolean
		markSupported()
		{
			return( true );
		}

		public void
		mark(
			int	limit )
		{
			buffer.mark();
		}

		public void
		reset()

			throws IOException
		{
			buffer.reset();
		}
	}


	public static void
	main(
			String[]	args )
	{	  
		print( 	new File( "C:\\Temp\\tables.config" ),
				new File( "C:\\Temp\\tables.txt" ));
	}
}
