/*
 * File    : IpRange.java
 * Created : 8 oct. 2003 13:02:23
 * By      : Olivier 
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
 
package org.gudy.azureus2.core3.ipfilter.impl;

import java.net.UnknownHostException;

import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.tracker.protocol.PRHelpers;

/**
 * @author Olivier
 * 
 */

public class 
IpRangeImpl
	implements IpRange 
{
   private byte[] description;
    
   private Object startIp;		// Integer if value, String/null otherwise
   private Object endIp;			// Integer if value, String/null otherwise
          
   private boolean sessionOnly;
    
  public 
  IpRangeImpl(
  	String _description, 
	String _startIp, 
	String _endIp) 
  {
    this(_description,_startIp,_endIp,true);
  }
   
   public 
   IpRangeImpl(
   	String 	_description, 
	String 	_startIp, 
	String 	_endIp,
	boolean _sessionOnly) 
   {     
     sessionOnly = _sessionOnly;
     
     if(_startIp == null || _endIp == null) {
     	
     	throw( new RuntimeException( "Invalid start/end values - null not supported" ));
     }
     
     startIp = _startIp.trim();
     
     endIp = _endIp.trim();
     
     internDescription(_description);
     
     checkValid(); 
   }
    
   public void 
   checkValid() 
   {  	
	if ( startIp != null && endIp != null ){
				
		if ( startIp instanceof String ){

			try{
				startIp = new Integer( PRHelpers.addressToInt((String)startIp ));
				
			}catch( UnknownHostException e ){
				
			}
		}
		
		if ( endIp instanceof String ){

			try{
				endIp = new Integer( PRHelpers.addressToInt((String)endIp ));
				
			}catch( UnknownHostException e ){
				
			}
		}
	}
	
   	((IpFilterImpl)IpFilterImpl.getInstance()).setValidOrNot( this, isValid());
   	
   }
    
   public boolean 
   isValid() 
   {
   	return(	startIp instanceof Integer && 
   			endIp instanceof Integer &&
			((Integer)startIp).compareTo( endIp ) <= 0 );
   }
    
   public boolean 
   isInRange(
     String ipAddress ) 
   {
     if(!isValid()){
     	
       return false;
     }
     
     return(((IpFilterImpl)IpFilterImpl.getInstance()).isInRange( this, ipAddress));
   }
    
   public String
   getDescription()
   {
   	return( externDescription());
   }

   public void
   setDescription(
   	String	str )
   {  	
   	internDescription(str);
   }
   
    protected void
	internDescription(
		String	desc )
    {
    	if ( desc == null ){
    		
    		description = null;
    		
    		return;
    	}
    	
    	char[]	chars = desc.toLowerCase().toCharArray();
    	
    	description = new byte[chars.length];
    	
    	int	pos = 0;
    	
    	for (int i=0;i<chars.length;i++){
    		
    		byte	b = (byte)chars[i];
    		
    		if ( b > 200 ){
    			
    			b = (byte)'_';
    		}
    		
    		description[pos++] = b;
    	}
    }
   
    protected String
	externDescription()
    {
    	if ( description == null ){
    		
    		return( null );
    	}
    	
    	StringBuffer	res = new StringBuffer();
    	
    	for (int i=0;i<description.length;i++){
    		
    		byte	b = description[i];
    		
    		res.append((char)b);
    	}
    	
    	return( res.toString());
    }
    
   public String
   getStartIp()
   {
   	return( startIp instanceof Integer?PRHelpers.intToAddress(((Integer)startIp).intValue()):(String)startIp);
   }
	
	public void
	setStartIp(
		String	str )
	{
	  	if ( str == null ){
	   		throw( new RuntimeException( "Invalid start value - null not supported" ));
	   	}

	   	if ( str.equals( getStartIp())){
	   		
	   		return;
	   	}
	   	
		startIp	= str;
		
		checkValid();
	}
	
   public String
   getEndIp()
   {
   	return( endIp instanceof Integer?PRHelpers.intToAddress(((Integer)endIp).intValue()):(String)endIp);
   }
   
   public void
   setEndIp(
	   String	str )
   
   {
   	if ( str == null ){
   		
   		throw( new RuntimeException( "Invalid end value - null not supported" ));
   	}

   	if ( str.equals( getEndIp())){
   		
   		return;
   	}
   	
	endIp	= str;
	   
	checkValid();
   }
   
   public String 
   toString() 
   {
     return( getDescription() + " : " + getStartIp() + " - "+ getEndIp()); 
   }

  public boolean 
  isSessionOnly() 
  {
    return( sessionOnly );
  }

  public void 
  setSessionOnly(
  	boolean _sessionOnly) 
  {
    sessionOnly = _sessionOnly;
  }

 }
