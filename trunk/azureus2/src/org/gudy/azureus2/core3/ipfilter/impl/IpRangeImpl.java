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

import java.util.StringTokenizer;

import org.gudy.azureus2.core3.ipfilter.*;

/**
 * @author Olivier
 * 
 */

public class 
IpRangeImpl
	implements IpRange 
{
    
   public String description;
    
   public String startIp;
   public String endIp;
    
   private int[] start;
   private int[] end;
    
   private boolean valid;
   
   private boolean sessionOnly;
    
  public IpRangeImpl(String description, String startIp, String endIp) {
    this(description,startIp,endIp,true);
  }
   
   public IpRangeImpl(String description, String startIp, String endIp,boolean sessionOnly) {
     this.valid = false;
     this.description = description;
     this.sessionOnly = sessionOnly;
     if(startIp == null || endIp == null) {        
       return;
     }
     this.startIp = startIp.trim();
     this.endIp = endIp.trim();
     checkValid(); 
   }
    
   public void checkValid() {
     this.valid = false;   
     StringTokenizer stStart = new StringTokenizer(startIp,".");
     StringTokenizer stEnd = new StringTokenizer(endIp,".");
     if(stStart.countTokens() != 4 || stEnd.countTokens() != 4) {      
       return;
     }   
     start = new int[4];
     end = new int[4];
     int i = 0;
     while(stStart.hasMoreTokens() && i < start.length) {
       try {
         start[i++] = Integer.parseInt(stStart.nextToken());
       } catch(Exception e) {
         return;
       }
     }
     i = 0;
     while(stEnd.hasMoreTokens() && i < end.length) {
       try {
         end[i++] = Integer.parseInt(stEnd.nextToken());
       } catch(Exception e) {
         return;
       }
     }
     this.valid = true;
   }
    
   public boolean isValid() {
     return this.valid;
   }
    
   public boolean isInRange(String ipAddress) {
     if(!valid)
       return false;
     StringTokenizer st = new StringTokenizer(ipAddress,".");
     if(st.countTokens() != 4)
       return false;
     int n[] = new int[4];
     for(int i = 0 ; i < 4 ; i++) {       
       try {
         n[i] = Integer.parseInt(st.nextToken());
       } catch(Exception e) {
         return false;
       }
     }
     
     boolean testStart,testEnd;
     testStart = true; testEnd = true;
     for(int i = 0 ; i < 4 ; i++) {
       //Outside range is immediately wrong     
       if(testStart && n[i] < start[i])
         return false;
       if(testEnd && n[i] > end[i])
         return false;
       //Strictly inside a range is immediately right
       if(n[i] > start[i])
         testStart = false;
       if(n[i] < end[i])
         testEnd = false;     
       }          
     return true;
   }
    
   public String
   getDescription()
   {
   	return( description );
   }

   public void
   setDescription(
   	String	str )
   {
   	description = str;
   }
   
   public String
   getStartIp()
   {
   	return( startIp );
   }
	
	public void
	setStartIp(
		String	str )
	{
		startIp	= str;
	}
	
   public String
   getEndIp()
   {
   	return( endIp );
   }
   
   public void
   setEndIp(
	   String	str )
   {
	   endIp	= str;
   }
   
   public String toString() {
     return description + " : " + startIp + " - " + endIp; 
   }

  public boolean isSessionOnly() {
    return this.sessionOnly;
  }

  public void setSessionOnly(boolean sessionOnly) {
    this.sessionOnly = sessionOnly;
  }

 }
