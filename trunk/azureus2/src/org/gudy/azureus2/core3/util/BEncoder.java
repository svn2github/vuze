/*
 * BEncoder.java
 *
 * Created on June 4, 2003, 10:17 PM
 */

package org.gudy.azureus2.core3.util;

import java.io.*;
import java.nio.charset.Charset;
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
	 Charset	default_charset;
	 Charset	byte_charset;

	 /** Creates a new instance of BEncoder */
	 
    public 
	BEncoder() 
    {
     	try{
     		default_charset = Charset.forName( Constants.DEFAULT_ENCODING );
     		byte_charset 	= Charset.forName( Constants.BYTE_ENCODING );
      		
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace( e );
    	}
    }
    
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

            ByteBuffer	bb 	= default_charset.encode( tempString );           
            
            write(baos,default_charset.encode(String.valueOf(bb.limit())));
            
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
			
            	String key = (String)entry.getKey();
   			   		           	
   			   	Object value = entry.getValue();

   			   	if ( value != null ){
   			   		
	                if ( byte_keys ){
	                		   		
	   					try{
	  					
	   				 		encode( baos, byte_charset.encode(key));
	      				
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
               write(baos,default_charset.encode(tempLong.toString()));
               baos.write('e');
         }else if(object instanceof Integer){
         	
			Integer tempInteger = (Integer)object;         
			//write out the l       
			baos.write('i');
			write(baos,default_charset.encode(tempInteger.toString()));
			baos.write('e');
			
       }else if(object instanceof byte[]){
       	
            byte[] tempByteArray = (byte[])object;
            write(baos,default_charset.encode(String.valueOf(tempByteArray.length)));
            baos.write(':');
            baos.write(tempByteArray);
            
       }else if(object instanceof ByteBuffer ){
       	
       		ByteBuffer  bb = (ByteBuffer)object;
       		write(baos,default_charset.encode(String.valueOf(bb.limit())));
            baos.write(':');
            write(baos,bb);
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
    	
    	try{
    		return(  Arrays.equals( encode(map1), encode(map2)));
    		
    	}catch( IOException e ){
    		
    		Debug.printStackTrace( e );
    		
    		return( false );
    	}
    }		
}
