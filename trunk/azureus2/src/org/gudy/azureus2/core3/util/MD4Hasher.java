
package org.gudy.azureus2.core3.util;

import java.security.*;


public class 
MD4Hasher
{    
	protected MessageDigest md4;
    
		/**
		 * Uses bouncy castle provider
		 *
		 */
	
    public 
	MD4Hasher()
    {
    	try{
    		md4 = MessageDigest.getInstance("MD4", "BC");
    		  		
    	}catch( Throwable e ){
    		
    			// should never get here
    		
    		e.printStackTrace();
    	}
    }
    
    public void 
	reset()
    {
    	md4.reset();
    }
    
    public void
    update(
    	byte[]		data,
		int			pos,
		int			len )
    {
    	md4.update( data, pos, len );
    }    
    
    public void
    update(
    	byte[]		data )
    {
    	update( data, 0, data.length );
    }
    
    public byte[]
    getDigest()
    {
    	return( md4.digest());  	
    }
}
