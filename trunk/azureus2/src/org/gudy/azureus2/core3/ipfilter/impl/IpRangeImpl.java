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
import java.util.*;

// import java.util.*;

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
   private Object endIp;		// Integer if value, String/null otherwise
          
   private boolean sessionOnly;
   private boolean added_to_range_list;
   
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
   		if (startIp instanceof Integer && 
   			endIp instanceof Integer ){
   			
   		   	long start_address 	= ((Integer)startIp).intValue(); 
   	     	long end_address 	= ((Integer)endIp).intValue();
   	     	
   	    	if ( start_address < 0 ){
   	     		
   	    		start_address += 0x100000000L;
   	     	}
   	       	if ( end_address < 0 ){
   	     		
   	       		end_address += 0x100000000L;
   	     	}
   	       	
   	       	return( end_address >= start_address);
   		}

   		return( false );
   }
    
   public boolean 
   isInRange(
     String ipAddress ) 
   {
     if(!isValid()){
     	
       return false;
     }
     
     try{
     	long	int_address = PRHelpers.addressToInt( ipAddress );
     	
     	if ( int_address < 0 ){
     		
     		int_address += 0x100000000L;
     	}
     	
     	long start_address 	= ((Integer)startIp).intValue(); 
     	long end_address 	= ((Integer)endIp).intValue();
     	
    	if ( start_address < 0 ){
     		
    		start_address += 0x100000000L;
     	}
       	if ( end_address < 0 ){
     		
       		end_address += 0x100000000L;
     	}
     	
       	return( int_address >= start_address && int_address <= end_address );
       	
     }catch( UnknownHostException e ){
     	
     	return( false );
     }
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
   
   // private static Map word_map = new HashMap();
   
   private final static byte[][]	frequent_words =
   	{
   		"ap2p".getBytes(),
		"city".getBytes(),
		"of".getBytes(),
		"proxy".getBytes(),
		"spider".getBytes(),
		".com".getBytes(),
		"ads".getBytes(),
		"customer".getBytes(),
		"software".getBytes(),
		"government".getBytes(),
		"gmbh".getBytes(),
		"interconnexion".getBytes(),
		"backbone".getBytes(),
   	};
			
   
    protected void
	internDescription(
		String	description_str )
    {
    	if ( description_str == null ){
    		
    		description = null;
    		
    		return;
    	}
    	
    	byte[]	bytes = description_str.toLowerCase().getBytes();
    	
    	int	word_start = 0;
    	
      	description = new byte[bytes.length];
    	
    	int	desc_pos = 0;
    	
       	for (int i=0;i<bytes.length+1;i++){

       		byte	b = i==bytes.length?(byte)' ':bytes[i];
       		
       			// kill any existing characters in the space we're using for frequent words
       		
       		if ( b < 0 ){
       			
       			b = (byte)'_';
       		}
       		
       		if ( b == ' ' || b == '.' || b == '-' ){
       			
       			int	word_len = i - word_start;
      
       			boolean	hit = false;
       			
       			if ( word_len > 1 ){
      
       				for (int j=0;j<frequent_words.length;j++){
       					
       					byte[]	fw = frequent_words[j];
       					
       					if ( fw.length == word_len ){
       					
       						hit	= true;
       						
       						for (int k=word_start;k<i;k++){
       						
       							if ( bytes[k] != fw[k-word_start]){
       								
       								hit	= false;
       								
       								break;
       							}
       						}
       					}
       					
       					if ( hit ){

       						description[desc_pos++] = (byte)(128+j);
       						      						
       						break;
       					}
       				}
       				
       				/*
       				if ( false ){
       
	       				String	w = new String(bytes,word_start,word_len);
	       				
	       				Integer	x = (Integer)word_map.get(w);
	       				
	       				if ( x == null ){
	       					
	       					x = new Integer(1);
	       				}else{
	       					
	       					int	ff = x.intValue() + 1;
	       					
	       					x = new Integer( ff );
	       					
	       					if ( ff % 100 == 0 && ff > 500 ){
	       						
	       						System.out.println(w + " -> " + ff );
	       					}
	       				}
	       				
	       				word_map.put( w, x );
       				}
       				*/
       			}
       			
       			if ( !hit ){
       				
       				for (int j=word_start;j<i;j++){
       					
       					description[ desc_pos++ ] = bytes[j]<0?(byte)'_':bytes[j];
       				}
       			}
       			
       			if ( i < bytes.length ){
       				
       				description[ desc_pos++ ] = b;
       				
       			}
       			
       			word_start = i+1;
       		}
       	}
       	
       	if ( desc_pos < description.length ){
       		
       		byte[]	d = new byte[desc_pos];
       		
       		System.arraycopy( description, 0, d, 0, desc_pos );
       		
       		description	= d;
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
    		
    		if ( b < 0 ){
    			
    			res.append( new String(frequent_words[128+b]));
    			
    		}else{
    			
    			res.append((char)b);
    		}
    	}
    	
    	return( res.toString());
    }
    
   public String
   getStartIp()
   {
   	return( startIp instanceof Integer?PRHelpers.intToAddress(((Integer)startIp).intValue()):(String)startIp);
   }
	
   protected long
   getStartIpLong()
   {
   	if ( startIp instanceof Integer ){
   		
   		long	val = ((Integer)startIp).intValue();
   		
   		if ( val < 0 ){
   			
   			val += 0x100000000L;
   		}
   		
   		return( val );
   	}else{
   		return( -1 );
   	}
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
   
   protected long
   getEndIpLong()
   {
   	if ( endIp instanceof Integer ){
   		
   		long	val = ((Integer)endIp).intValue();
   		
   		if ( val < 0 ){
   			
   			val += 0x100000000L;
   		}
   		
   		return( val );
   	}else{
   		return( -1 );
   	}
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

	public int
	compareStartIpTo(
		IpRange	other )
	{
		long l = getStartIpLong() - ((IpRangeImpl)other).getStartIpLong();
		
		if ( l < 0 ){
			return( -1 );
		}else if ( l > 0 ){
			return( 1 );
		}else{
			return(0);
		}
	}
	
	public int
	compareEndIpTo(
		IpRange	other )
	{
		long l = getEndIpLong() - ((IpRangeImpl)other).getEndIpLong();
		
		if ( l < 0 ){
			return( -1 );
		}else if ( l > 0 ){
			return( 1 );
		}else{
			return(0);
		}
	}
	
	public int
	compareDescription(
		IpRange	other )
	{
		byte[]	other_description = ((IpRangeImpl)other).description;
		
		if ( description == null || other_description == null ){
			
			return(0);
		}
				
		int	pos = 0;
		
		byte[]	this_b	= description;
		byte[]	other_b	= other_description;
		
		while(true){
			if ( pos == this_b.length || pos == other_b.length ){
				
				break;
			}
			
			byte	b1 = this_b[pos];
			byte	b2 = other_b[pos];
						
			pos++;
			
			if ( b1 != b2 ){
			
				if ( b1 < 0 ){
					
					pos--;
					
					byte[]	word 		= frequent_words[128+b1];
					int		word_len 	= word.length;
					
					byte[]	new_b = new byte[ word_len + this_b.length ];
					
					System.arraycopy( word,		0, 		new_b, pos, word_len );
					System.arraycopy( this_b, 	pos, 	new_b, pos+word_len, this_b.length - pos );
					
					this_b = new_b;
					
					b1 = this_b[pos++];
				}
				
				if ( b2 < 0 ){
					
					pos--;
					
					byte[]	word 		= frequent_words[128+b2];
					int		word_len 	= word.length;
					
					byte[]	new_b = new byte[ word_len + other_b.length ];
					
					System.arraycopy( word,		0, 		new_b, pos, word_len );
					System.arraycopy( other_b, 	pos, 	new_b, pos+word_len, other_b.length - pos );
					
					other_b = new_b;
					
					b2 = other_b[pos++];
				}
				
				if ( b1 != b2 ){
					
					return( b1 - b2 );
				}
			}
		}
		
		return( this_b.length - other_b.length );
	}
	
	protected void
	setAddedToRangeList(
		boolean		b )
	{
		added_to_range_list = b;
	}
	
	protected boolean
	getAddedToRangeList()
	{
		return( added_to_range_list );
	}
 }
