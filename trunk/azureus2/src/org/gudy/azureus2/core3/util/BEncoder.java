/*
 * BEncoder.java
 *
 * Created on June 4, 2003, 10:17 PM
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
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.xml.util.XUXmlWriter;

/**
 * A set of utility methods to encode a Map into a bencoded array of byte.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 *
 * @author  TdC_VgA
 */
public class 
BEncoder 
{    
	private static final int BUFFER_DOUBLE_LIMIT	= 256*1024;
	
    public static byte[] 
    encode(
    	Map object ) 
    
    	throws IOException
    {
       return( encode( object, false ));
    }    
    
    public static byte[] 
    encode(
    	Map 	object, 
    	boolean url_encode ) 
    
    	throws IOException
    {
    	BEncoder encoder = new BEncoder(url_encode);
    	
    	encoder.encodeObject( object);
    	
    	return( encoder.toByteArray());
    }  
    
    private byte[]		current_buffer		= new byte[256];
    private int			current_buffer_pos	= 0;
    private byte[][]	old_buffers;		
    
    private boolean	url_encode;
    
    private
    BEncoder(
    	boolean	_url_encode )
    {
    	url_encode	= _url_encode;
    }
    
    private void 
	encodeObject(
		Object 					object) 
    
    	throws IOException
	{
    	
        if ( object instanceof String || object instanceof Float){
        	
            String tempString = (object instanceof String) ? (String)object : String.valueOf((Float)object);

            ByteBuffer	bb 	= Constants.DEFAULT_CHARSET.encode( tempString );           
            
            write(Constants.DEFAULT_CHARSET.encode(String.valueOf(bb.limit())));
            
            write(':');
            
            write(bb );
            
        }else if(object instanceof Map){
        	
            Map tempMap = (Map)object;
            
            SortedMap tempTree = null;
            
            	// unfortunately there are some occasions where we want to ensure that
            	// the 'key' of the map is not mangled by assuming its UTF-8 encodable.
            	// In particular the response from a tracker scrape request uses the
            	// torrent hash as the KEY. Hence the introduction of the type below
            	// to allow the constructor of the Map to indicate that the keys should
            	// be extracted using a BYTE_ENCODING 
            	
            boolean	byte_keys = object instanceof ByteEncodedKeyHashMap;
            
            //write the d            
            write('d');
            
            //are we sorted?
            if ( tempMap instanceof TreeMap ){
            	
                tempTree = (TreeMap)tempMap;
                
            }else{
            	
                	//do map sorting here
            	
            	if ( tempMap instanceof CompactMap ){
            		
            		tempTree = new TreeMap();
            		
            		((CompactMap)tempMap).putAllTo( tempTree );
            		
            	}else{
                
            		tempTree = new TreeMap(tempMap);
            	}
            }            
                   
            Iterator	it = tempTree.entrySet().iterator();
            
            while( it.hasNext()){
            	
            	Map.Entry	entry = (Map.Entry)it.next();
			
            	Object o_key = entry.getKey();
   			   		           	
   			   	Object value = entry.getValue();

   			   	if ( value != null ){
   			   	
					if ( o_key instanceof byte[]){
						
						encodeObject( o_key );
	      				
						encodeObject( value );

					}else{
						
						String	key = (String)o_key;
					
		                if ( byte_keys ){
		                		   		
		   					try{
		  					
		   						encodeObject( Constants.BYTE_CHARSET.encode(key));
		      				
		   						encodeObject( tempMap.get(key));
		      		
		    				}catch( UnsupportedEncodingException e ){
		                		
		    					throw( new IOException( "BEncoder: unsupport encoding: " + e.getMessage()));
		    				}
		
		                }else{                 
	
		                	encodeObject( key );	// Key goes in as UTF-8
		      				
		                	encodeObject( value);
	    				}   
					}
                }     
            }
            
            write('e');
            
            
        }else if(object instanceof List){
        	
            List tempList = (List)object;
            
            	//write out the l
            
            write('l');                                   
            
            for(int i = 0; i<tempList.size(); i++){
                
            	encodeObject( tempList.get(i));                            
            }   
            
            write('e');                          
            
        }else if(object instanceof Long){
        	
            Long tempLong = (Long)object;         
            //write out the l       
               write('i');
               write(Constants.DEFAULT_CHARSET.encode(tempLong.toString()));
               write('e');
         }else if(object instanceof Integer){
         	
			Integer tempInteger = (Integer)object;         
			//write out the l       
			write('i');
			write(Constants.DEFAULT_CHARSET.encode(tempInteger.toString()));
			write('e');
			
       }else if(object instanceof byte[]){
       	
            byte[] tempByteArray = (byte[])object;
            write(Constants.DEFAULT_CHARSET.encode(String.valueOf(tempByteArray.length)));
            write(':');
            if ( url_encode ){
            	write(URLEncoder.encode(new String(tempByteArray, Constants.BYTE_ENCODING), Constants.BYTE_ENCODING ).getBytes());
            }else{
            	write(tempByteArray);
            }
            
       }else if(object instanceof ByteBuffer ){
       	
       		ByteBuffer  bb = (ByteBuffer)object;
       		write(Constants.DEFAULT_CHARSET.encode(String.valueOf(bb.limit())));
            write(':');
            write(bb);
            
       }else if ( object == null ){
    	   
    	   	// ideally we'd bork here but I don't want to run the risk of breaking existing stuff so just log
    	   
    	   Debug.out( "Attempt to encode a null value" );
    	   
       }else{
        	
    	   Debug.out( "Attempt to encode an unsupported entry type: " + object.getClass() + ";value=" + object);
       }
    }
    
    private void
    write(
    	char		c )
   	{
    	int rem = current_buffer.length - current_buffer_pos;
    	
    	if ( rem > 0 ){
    		
    		current_buffer[current_buffer_pos++] = (byte)c;
    		
    	}else{
    		
       		int	next_buffer_size = current_buffer.length < BUFFER_DOUBLE_LIMIT?(current_buffer.length << 1):(current_buffer.length + BUFFER_DOUBLE_LIMIT );

    		byte[]	new_buffer = new byte[ next_buffer_size ];
       		
    		new_buffer[ 0 ] = (byte)c;

    		if ( old_buffers == null ){
    			
    			old_buffers = new byte[][]{ current_buffer };
    			
    		}else{
    			
    			byte[][] new_old_buffers = new byte[old_buffers.length+1][];
    			
    			System.arraycopy( old_buffers, 0, new_old_buffers, 0, old_buffers.length );
    			
    			new_old_buffers[ old_buffers.length ] = current_buffer;
    			
    			old_buffers = new_old_buffers;
    		}
    		
    		current_buffer		= new_buffer;
    		current_buffer_pos 	= 1;
     	}
   	}
   	
    private void
    write(
    	byte[]			bytes )
    {
    	write( bytes, bytes.length );
    }
    
    private void
    write(
    	byte[]			bytes,
    	int				length )
    {
    	int rem = current_buffer.length - current_buffer_pos;
    	
    	if ( rem >= length ){
    		
    		System.arraycopy( bytes, 0, current_buffer, current_buffer_pos, length );
    		
    		current_buffer_pos += length;
    		
    	}else{
    		
    		if ( rem > 0 ){
    			
	    		System.arraycopy( bytes, 0, current_buffer, current_buffer_pos, rem );
	
	    		length -= rem;
    		}
    		    
    		int	next_buffer_size = current_buffer.length < BUFFER_DOUBLE_LIMIT?(current_buffer.length << 1):(current_buffer.length + BUFFER_DOUBLE_LIMIT );
    				
    		byte[]	new_buffer = new byte[ Math.max( next_buffer_size, length + 512 ) ];
       		   		
    		System.arraycopy( bytes, rem, new_buffer, 0, length );

    		if ( old_buffers == null ){
    			
    			old_buffers = new byte[][]{ current_buffer };
    			
    		}else{
    			
    			byte[][] new_old_buffers = new byte[old_buffers.length+1][];
    			
    			System.arraycopy( old_buffers, 0, new_old_buffers, 0, old_buffers.length );
    			
    			new_old_buffers[ old_buffers.length ] = current_buffer;
    			
    			old_buffers = new_old_buffers;
    		}
    		
    		current_buffer		= new_buffer;
    		current_buffer_pos 	= length;
     	}   
    }
    
    private void
	write(
		ByteBuffer		bb )
    {
    	write( bb.array(), bb.limit());
    }

    private byte[]
    toByteArray()
    {
    	if ( old_buffers == null ){
    		
    		byte[]	res = new byte[current_buffer_pos];
    		
    		System.arraycopy( current_buffer, 0, res, 0, current_buffer_pos );
    		
    		// System.out.println( "-> " + current_buffer_pos );
    		
    		return( res );
    		
    	}else{
    		
    		int	total = current_buffer_pos;
    		
    		for (int i=0;i<old_buffers.length;i++){
    			
    			total += old_buffers[i].length;
    		}
    		
    		byte[] res = new byte[total];
    		
    		int	pos = 0;
    		
    		//String str = "";
    		
    		for (int i=0;i<old_buffers.length;i++){

    			byte[] buffer = old_buffers[i];
    			
    			int	len = buffer.length;
    			
    			System.arraycopy( buffer, 0, res, pos, len );
    			
    			pos += len;
    			
    			//str += (str.length()==0?"":",") + len;
    		}
    		
      		System.arraycopy( current_buffer, 0, res, pos, current_buffer_pos );
      		 
    		//System.out.println( "-> " + str + "," + current_buffer_pos );

      		return( res );
    	}
    }
                   
    
    private static Object
    normaliseObject(
    	Object		o )
    {
    	if ( o instanceof Integer ){
       		o = new Long(((Integer)o).longValue());
      	}else if ( o instanceof Boolean ){
       		o = new Long(((Boolean)o).booleanValue()?1:0);
      	}else if ( o instanceof Float ){
       		o = String.valueOf((Float)o);
       	}else if ( o instanceof byte[] ){    		
       		try{
       			o = new String((byte[])o,"UTF-8");			
       		}catch( Throwable e ){
       		}
       	}
    	
    	return( o );
    }
    
    public static boolean
    objectsAreIdentical(
    	Object		o1,
    	Object		o2 )
    {
    	if ( o1 == null && o2 == null ){
    		
    		return( true );
    		
    	}else if ( o1 == null || o2 == null ){
    		
    		return( false );
    	}
    	
      	if ( o1.getClass() != o2.getClass()){
      		 
    		if ( 	( o1 instanceof Map && o2 instanceof Map ) ||
    				( o1 instanceof List && o2 instanceof List )){
    			
    			// things actually OK
    			
    		}else{
    			
		    	o1 = normaliseObject( o1 );
		    	o2 = normaliseObject( o2 );
	       	
		    	if ( o1.getClass() != o2.getClass()){
	    				    			
			    	Debug.out( "Failed to normalise classes " + o1.getClass() + "/" + o2.getClass());
			    		
			    	return( false );
		    	}
      		}
    	}
    	
    	if ( 	o1 instanceof Long ||
    			o1 instanceof String ){		
    		
    		return( o1.equals( o2 ));
    		
     	}else if ( o1 instanceof byte[] ){
     		
     		return( Arrays.equals((byte[])o1,(byte[])o2 ));
     		    		
    	}else if ( o1 instanceof List ){
    		
    		return( listsAreIdentical((List)o1,(List)o2));
    		
       	}else if ( o1 instanceof Map ){
       	    		
    		return( mapsAreIdentical((Map)o1,(Map)o2));
    		
       	}else if ( 	o1 instanceof Integer ||
	    			o1 instanceof Boolean ||
	    			o1 instanceof Float ||
	    			o1 instanceof ByteBuffer ){
    		
    		return( o1.equals( o2 ));

    	}else{
    		
    		Debug.out( "Invalid type: " + o1 );
    		
    		return( false );
    	}
    }
    
    public static boolean
	listsAreIdentical(
		List	list1,
		List	list2 )
    {
    	if ( list1 == null && list2 == null ){
    		
    		return( true );
    		
    	}else if ( list1 == null || list2 == null ){
    		
    		return( false );
    	}
    	
    	if ( list1.size() != list2.size()){
    		
    		return( false );
    	}
    	
    	for ( int i=0;i<list1.size();i++){
    		
    		if ( !objectsAreIdentical( list1.get(i), list2.get(i))){
    			
    			return( false );
    		}
    	}
    	
    	return( true );
    }
    
    public static boolean
	mapsAreIdentical(
		Map	map1,
		Map	map2 )
	{
    	if ( map1 == null && map2 == null ){
    		
    		return( true );
    		
    	}else if ( map1 == null || map2 == null ){
    		
    		return( false );
    	}
    	
    	if ( map1.size() != map2.size()){
    		
    		return( false );
    	}
    	
    	Iterator	it = (map1 instanceof CompactMap )?((CompactMap)map1).getKeySetIterator():map1.keySet().iterator();
    	
    	while( it.hasNext()){
    		
    		Object	key = it.next();
    		
    		Object	v1 = map1.get(key);
    		Object	v2 = map2.get(key);
    		
    		if ( !objectsAreIdentical( v1, v2 )){
    			
    			return( false );
    		}
    	}
    	
    	return( true );
    }	
    
    public static Map
    cloneMap(
    	Map		map )
    {
    	if ( map == null ){
    		
    		return( null );
    	}
    	
    	Map res = new TreeMap();
    	
    	Iterator	it = map.entrySet().iterator();
    	
    	while( it.hasNext()){
    		
    		Map.Entry	entry = (Map.Entry)it.next();
    		
    		Object	key 	= entry.getKey();
    		Object	value	= entry.getValue();

    			// keys must be String (or very rarely byte[])
    		
    		if ( key instanceof byte[] ){
    			
    			key = ((byte[])key).clone();
    		}
    		
    		res.put( key, clone( value ));
    	}
    	
    	return( res );
    }
    
    public static List
    cloneList(
    	List		list )
    {
    	if ( list == null ){
    		
    		return( null );
    	}
    	
    	List	res = new ArrayList(list.size());
    	
    	Iterator	it = list.iterator();
    	
    	while( it.hasNext()){
    		
    		res.add( clone( it.next()));
    	}
    	
    	return( res );
    }
    
    public static Object
    clone(
    	Object	obj )
    {
    	if ( obj instanceof List ){
    		
    		return( cloneList((List)obj));
    		
    	}else if ( obj instanceof Map ){
    		
    		return( cloneMap((Map)obj));
    		
    	}else if ( obj instanceof byte[]){
    		
    		return(((byte[])obj).clone());
    		
    	}else{
    			// assume immutable - String,Long etc
    		
    		return( obj );
    	}
    }
    
    public static StringBuffer
    encodeToXML(
    	Map			map,
    	boolean		simple )
    {
     	XMLEncoder writer = new XMLEncoder();
  
     	return( writer.encode( map, simple ));
    }    
    
    protected static class
    XMLEncoder
    	extends XUXmlWriter
    {
    	protected
    	XMLEncoder()
    	{
    	}
    	
    	protected StringBuffer
    	encode(
    		Map		map,
    		boolean	simple )
    	{
    		StringWriter	writer = new StringWriter(1024);
    		
    		setOutputWriter( writer );
    		
    		setGenericSimple( simple );
    		
    		writeGeneric( map );
    		
    		flushOutputStream();
    		
    		return( writer.getBuffer());
    	}
    }
}
