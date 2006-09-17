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
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A set of utility methods to encode a Map into a bencoded array of byte.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 *
 * @author  TdC_VgA
 */
public class 
BEncoder 
{          	
    public static byte[] encode(Map object) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new BEncoder().encode(baos, object);
        return baos.toByteArray();
    }    
    
    private void 
	encode(
		ByteArrayOutputStream 	baos, 
		Object 					object) 
    
    	throws IOException
	{
    	
        if ( object instanceof String || object instanceof Float){
        	
            String tempString = (object instanceof String) ? (String)object : String.valueOf((Float)object);

            ByteBuffer	bb 	= Constants.DEFAULT_CHARSET.encode( tempString );           
            
            write(baos,Constants.DEFAULT_CHARSET.encode(String.valueOf(bb.limit())));
            
            baos.write(':');
            
            write(baos,bb );
            
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
            baos.write('d');
            
            //are we sorted?
            if ( tempMap instanceof TreeMap ){
            	
                tempTree = (TreeMap)tempMap;
                
            }else{
            	
                	//do map sorting here
            	
                tempTree = new TreeMap(tempMap);                
            }            
                   
            Iterator	it = tempTree.entrySet().iterator();
            
            while( it.hasNext()){
            	
            	Map.Entry	entry = (Map.Entry)it.next();
			
            	Object o_key = entry.getKey();
   			   		           	
   			   	Object value = entry.getValue();

   			   	if ( value != null ){
   			   	
					if ( o_key instanceof byte[]){
						
   				 		encode( baos, (byte[])o_key);
	      				
	      				encode( baos, value );

					}else{
						
						String	key = (String)o_key;
					
		                if ( byte_keys ){
		                		   		
		   					try{
		  					
		   				 		encode( baos, Constants.BYTE_CHARSET.encode(key));
		      				
		      					encode( baos, tempMap.get(key));
		      		
		    				}catch( UnsupportedEncodingException e ){
		                		
		    					throw( new IOException( "BEncoder: unsupport encoding: " + e.getMessage()));
		    				}
		
		                }else{                 
	
		                	encode(baos, key );	// Key goes in as UTF-8
		      				
		      				encode(baos, value);
	    				}   
					}
                }     
            }
            
            baos.write('e');
            
            
        }else if(object instanceof List){
        	
            List tempList = (List)object;
            
            	//write out the l
            
            baos.write('l');                                   
            
            for(int i = 0; i<tempList.size(); i++){
                
                encode(baos, tempList.get(i));                            
            }   
            
            baos.write('e');                          
            
        }else if(object instanceof Long){
        	
            Long tempLong = (Long)object;         
            //write out the l       
               baos.write('i');
               write(baos,Constants.DEFAULT_CHARSET.encode(tempLong.toString()));
               baos.write('e');
         }else if(object instanceof Integer){
         	
			Integer tempInteger = (Integer)object;         
			//write out the l       
			baos.write('i');
			write(baos,Constants.DEFAULT_CHARSET.encode(tempInteger.toString()));
			baos.write('e');
			
       }else if(object instanceof byte[]){
       	
            byte[] tempByteArray = (byte[])object;
            write(baos,Constants.DEFAULT_CHARSET.encode(String.valueOf(tempByteArray.length)));
            baos.write(':');
            baos.write(tempByteArray);
            
       }else if(object instanceof ByteBuffer ){
       	
       		ByteBuffer  bb = (ByteBuffer)object;
       		write(baos,Constants.DEFAULT_CHARSET.encode(String.valueOf(bb.limit())));
            baos.write(':');
            write(baos,bb);
        }else{
        	
        	throw( new IOException( "Unsupported entry type: " + object.getClass()));
        }
    }
    
    protected void
	write(
		OutputStream	os,
		ByteBuffer		bb )
    
    	throws IOException
    {
    	os.write( bb.array(), 0, bb.limit());
    }
    
    private static boolean
    objectsAreIdentical(
    	Object		o1,
    	Object		o2 )
    {
    	if ( o1 == null && o2 == null ){
    		
    		return( true );
    		
    	}else if ( o1 == null || o2 == null ){
    		
    		return( false );
    	}
    	
     	if ( o1 instanceof Integer ){
       		o1 = new Long(((Integer)o1).longValue());
       	}
      	if ( o2 instanceof Integer ){
       		o2 = new Long(((Integer)o2).longValue());
       	}
      	
      	if ( o1 instanceof Float ){
       		o1 = String.valueOf((Float)o1);
       	}
       	if ( o2 instanceof Float ){
       		o2 = String.valueOf((Float)o2);
       	}
       	
    	if ( o1.getClass() != o2.getClass()){
    		
    		return( false );
    	}
    	
    	if ( o1 instanceof Long ){
    		
    		return( o1.equals( o2 ));
    		
     	}else if ( o1 instanceof byte[] ){
     		
     		return( Arrays.equals((byte[])o1,(byte[])o2 ));
     		
     	}else if ( o1 instanceof ByteBuffer ){
     		
     		return( o1.equals( o2 ));
     			
    	}else if ( o1 instanceof String ){
    		
    		return( o1.equals(o2 ));
    		
    	}else if ( o1 instanceof List ){
    		
    		return( listsAreIdentical((List)o1,(List)o2));
    		
       	}else if ( o1 instanceof Map ){
       	    		
    		return( mapsAreIdentical((Map)o1,(Map)o2));
    		
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
    	
    	Iterator	it = map1.keySet().iterator();
    	
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
}
