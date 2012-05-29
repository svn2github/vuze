/*
 * Created on 1 Nov 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
NetUtils 
{
	private static final int MIN_NI_CHECK_MILLIS 	= 30*1000;
	private static final int INC1_NI_CHECK_MILLIS 	= 2*60*1000;
	private static final int INC2_NI_CHECK_MILLIS 	= 15*60*1000;
	
	private static int	current_check_millis = MIN_NI_CHECK_MILLIS;
	
	private static long	last_ni_check	= -1;
	
	private static volatile List<NetworkInterface>		current_interfaces = new ArrayList<NetworkInterface>();
	
	private static boolean						check_in_progress;
	
	private static AESemaphore					ni_sem = new AESemaphore( "NetUtils:ni" );
	
	public static List<NetworkInterface>
	getNetworkInterfaces()
	
		throws SocketException
	{
		long	now = SystemTime.getMonotonousTime();
		
		boolean	do_check = false;
		
		synchronized( NetUtils.class ){
			
			if ( !check_in_progress ){
				
				if ( last_ni_check < 0 || now - last_ni_check > current_check_millis ){
									
					do_check 			= true;
					check_in_progress	= true;
				}
			}
		}
		
		if ( do_check ){

			List<NetworkInterface> result = new ArrayList<NetworkInterface>();

			try{
					// got some major CPU issues on some machines with crap loads of NIs
				
				long	start 	= SystemTime.getHighPrecisionCounter();
				
				Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
				
				long	elapsed_millis = ( SystemTime.getHighPrecisionCounter() - start ) / 1000000;
					
				long	old_period = current_check_millis;
				
				if ( elapsed_millis > 1000 && current_check_millis <  INC2_NI_CHECK_MILLIS ){
										
					current_check_millis = INC2_NI_CHECK_MILLIS;
					
				}else if ( elapsed_millis > 250 && current_check_millis < INC1_NI_CHECK_MILLIS ){
					
					current_check_millis = INC1_NI_CHECK_MILLIS;
				}
				
				if ( old_period != current_check_millis ){
					
					Debug.out( "Network interface enumeration took " + elapsed_millis + ": decreased refresh frequency to " + current_check_millis + "ms" );
				}
				
				if ( nis != null ){
					
					while( nis.hasMoreElements()){
						
						result.add( nis.nextElement());
					}
				}
				
				// System.out.println( "getNI: elapsed=" + elapsed_millis + ", result=" + result.size());

			}finally{
				
				synchronized( NetUtils.class ){
				
					check_in_progress	= false;
					current_interfaces 	= result;
					
					last_ni_check	= SystemTime.getMonotonousTime();
				}
	
				ni_sem.releaseForever();
			}			
		}
		
		ni_sem.reserve();
		
		return( current_interfaces );
	}
	
	public static InetAddress
	getLocalHost()
	
		throws UnknownHostException
	{
		try{
			return( InetAddress.getLocalHost());
			
		}catch( Throwable e ){
			
				// sometimes get this when changing host name
				// return first non-loopback one
			
			try{
				List<NetworkInterface> 	nis = getNetworkInterfaces();

				for ( NetworkInterface ni: nis ){
						
					Enumeration addresses = ni.getInetAddresses();
					
					while( addresses.hasMoreElements()){
						
						InetAddress address = (InetAddress)addresses.nextElement();
						
						if ( address.isLoopbackAddress() || address instanceof Inet6Address ){
							
							continue;
						}
						
						return( address );
					}
				}
			}catch( Throwable f ){
			}
			
			return( InetAddress.getByName( "127.0.0.1" ));
		}
	}
}
