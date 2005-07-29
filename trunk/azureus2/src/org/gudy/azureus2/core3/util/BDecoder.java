/*
 * BeDecoder.java
 *
 * Created on May 30, 2003, 2:44 PM
 */

package org.gudy.azureus2.core3.util;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;

/**
 * A set of utility methods to decode a bencoded array of byte into a Map.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 * 
 * @author TdC_VgA
 *
 */
public class BDecoder {
	
	private boolean recovery_mode;
		
	private Charset	byte_charset;
	private Charset	default_charset;

	
	public static Map
	decode(
		byte[]	data )
	
		throws IOException
	{
		return( new BDecoder().decodeByteArray( data ));
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
  	try{
  		byte_charset 	= Charset.forName( Constants.BYTE_ENCODING );
 		default_charset = Charset.forName( Constants.DEFAULT_ENCODING );

	}catch( Throwable e ){
		
		Debug.printStackTrace( e );
	}
  }

  public Map 
  decodeByteArray(
	byte[] data) 
  
  	throws IOException 
  { 
    return( decode(new ByteArrayInputStream(data)));
  }

  public Map 
  decodeStream(
	BufferedInputStream data )  
  
  	throws IOException 
  {
      return((Map)decodeInputStream(data, 0));
  }

  private Map 
  decode(
	ByteArrayInputStream data ) 
  
  	throws IOException 
  {
      return (Map) decodeInputStream(data, 0);
  }

  private Object 
  decodeInputStream(
  	InputStream bais,
	int			nesting ) 
  
  	throws IOException 
  {
    if (!bais.markSupported()) {
    	
      throw new IOException("InputStream must support the mark() method");
    }

    //set a mark
    bais.mark(Integer.MAX_VALUE);

    //read a byte
    int tempByte = bais.read();

    //decide what to do
    switch (tempByte) {
      case 'd' :
        //create a new dictionary object
        Map tempMap = new HashMap();

        try{
	        //get the key   
	        byte[] tempByteArray = null;
	        
	        while ((tempByteArray = (byte[]) decodeInputStream(bais, nesting+1)) != null) {
	        	
	        	//decode some more
	        	
	          Object value = decodeInputStream(bais,nesting+1);
	          
	          	//add the value to the map
	          
	          CharBuffer	cb = byte_charset.decode(ByteBuffer.wrap(tempByteArray));
	          
	          String	key = new String(cb.array(),0,cb.limit());
	                    
	          tempMap.put( key, value);
	        }
	
	        if ( bais.available() < nesting ){
	        		        		
	        	throw( new IOException( "BDecoder: invalid input data, 'e' missing from end of dictionary"));
	        }
        }catch( Throwable e ){
        	
        	if ( !recovery_mode ){

        		if ( e instanceof IOException ){
        			
        			throw((IOException)e);
        		}
        		
        		throw( new IOException( Debug.getNestedExceptionMessage(e)));
        	}
        }
        
        //return the map
        return tempMap;

      case 'l' :
        //create the list
        List tempList = new ArrayList();

        try{
	        //create the key
	        Object tempElement = null;
	        while ((tempElement = decodeInputStream(bais, nesting+1)) != null) {
	          //add the element
	          tempList.add(tempElement);
	        }
	        
	        if ( bais.available() < nesting ){
	        		        		
	        	throw( new IOException( "BDecoder: invalid input data, 'e' missing from end of list"));
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
        return new Long(getNumberFromStream(bais, 'e'));

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
        bais.reset();
        //get the string
        return getByteArrayFromStream(bais);

      default :{
    	  
    	  int	rem_len = bais.available();
    	  
    	  byte[] rem_data = new byte[rem_len];
    	  
    	  bais.read( rem_data );
    	  
    	  throw( new IOException(
        	"BDecoder: unknown command '" + tempByte + ", remainder = " + new String( rem_data )));
      }
    }
  }

  private long getNumberFromStream(InputStream bais, char parseChar) throws IOException {
    int length = 0;

    //place a mark
    bais.mark(Integer.MAX_VALUE);

    int tempByte = bais.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
      tempByte = bais.read();
      length++;
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    //reset the mark
    bais.reset();

    //get the length
    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;

    //get the string
    while (count != length && (len = bais.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    //jump ahead in the stream to compensate for the :
    bais.skip(1);

    //return the value
    
    CharBuffer	cb = default_charset.decode(ByteBuffer.wrap(tempArray));
    
    String	str_value = new String(cb.array(),0,cb.limit());

    return Long.parseLong(str_value);
  }

  private byte[] getByteArrayFromStream(InputStream bais) throws IOException {
    int length = (int) getNumberFromStream(bais, ':');

    if (length < 0) {
      return null;
    }
    
    	// note that torrent hashes can be big (consider a 55GB file with 2MB pieces
    	// this generates a pieces hash of 1/2 meg
    
    if ( length > 8*1024*1024 ){
    	
    	throw( new IOException( "Byte array length too large (" + length + ")"));
    }
    
    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;
    //get the string
    while (count != length && (len = bais.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    if ( count != tempArray.length ){
     	throw( new IOException( "BDecoder::getByteArrayFromStream: truncated"));
    }
    
    return tempArray;
  }
  
  public void
  setRecoveryMode(
		boolean	r )
  {
	  recovery_mode	= r;
  }
  
  public void
  print(
	Object	obj )
  {
	  print( obj, "", false );
  }
  
  private void
  print(
	Object	obj,
	String	indent,
	boolean	skip_indent )
  {
	  String	use_indent = skip_indent?"":indent;
	  
	  if ( obj instanceof Long ){
		  
		  System.out.println( use_indent + obj );
		  
	  }else if ( obj instanceof byte[]){
		  
		  byte[]	b = (byte[])obj;
		  
		  System.out.println( use_indent + (b.length==20?(" { "+ ByteFormatter.nicePrint( b )+ " }"):new String(b) ));
		
	  }else if ( obj instanceof String ){
		  
		  System.out.println( use_indent + obj );

	  }else if ( obj instanceof List ){
		  
		  List	l = (List)obj;
		  
		  System.out.println( use_indent + "[" );
		  
		  for (int i=0;i<l.size();i++){
			
			  System.out.print( indent + "  (" + i + ") " );
			  
			  print( l.get(i), indent + "    ", true );
		  }
		  
		  System.out.println( indent + "]" );

	  }else{
		  
		  Map	m = (Map)obj;
		  
		  Iterator	it = m.keySet().iterator();
		  
		  while( it.hasNext()){
			  
			  String	key = (String)it.next();
			  
			  System.out.print( indent + key + " = " );
			  
			  print( m.get(key), indent + "  ", true );
		  }
	  }
  }
  
  public void
  print(
		File		f )
  
  	throws IOException
  {
	  print( decodeStream( new BufferedInputStream( new FileInputStream( f ))));
  }
  
  public static void
  main(
	 String[]	args )
  {	  
	  try{
		  
		  BDecoder decoder = new BDecoder();
		  
		  decoder.setRecoveryMode( true );
		  
		  Map res = decoder.decodeStream( new BufferedInputStream( new FileInputStream( new File( "C:\\temp\\scrape.php" ))));
		  
		  decoder.print( res );
		  
		 // byte[] zzz = BEncoder.encode( res );
		 
		  //FileOutputStream fos  = new FileOutputStream( new File( "C:\\temp\\downloads.config.630.recov" ));
		 
		  //fos.write( zzz );
		 
		  //fos.close();
		 
	  }catch( Throwable e ){
		  
		  e.printStackTrace();
	  }
  }
}
