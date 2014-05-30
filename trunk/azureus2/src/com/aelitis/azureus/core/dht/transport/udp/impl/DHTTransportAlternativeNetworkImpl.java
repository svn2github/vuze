/*
 * Created on May 29, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.aelitis.azureus.core.dht.transport.DHTTransportAlternativeContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportAlternativeNetwork;

public class 
DHTTransportAlternativeNetworkImpl 
	implements DHTTransportAlternativeNetwork
{
	private static final int	LIVE_AGE_SECS 		= 20*60;
	private static final int	LIVEISH_AGE_SECS 	= 40*60;
	private static final int	MAX_CONTACTS		= 64;
	
	private int	network;
	
	private TreeSet<DHTTransportAlternativeContact> contacts =
		new TreeSet<DHTTransportAlternativeContact>(
			new Comparator<DHTTransportAlternativeContact>() 
			{
				public int 
				compare(
					DHTTransportAlternativeContact o1,
					DHTTransportAlternativeContact o2 ) 
				{
					int res = o2.getLastAlive() - o1.getLastAlive();
					
					if ( res == 0 ){
						
						res = o2.getID() - o1.getID();
					}
					
					return( res );
				}
			});
	
	
	protected
	DHTTransportAlternativeNetworkImpl(
		int		_net )
	{
		network	= _net;
	}
	
	public int
	getNetworkType()
	{
		return( network );
	}
	
	public List<DHTTransportAlternativeContact>
	getContacts(
		int			max )
	{
		return( getContacts( max, false ));
	}
	
	protected List<DHTTransportAlternativeContact>
	getContacts(
		int			max,
		boolean		live_only )
	{
		if ( max == 0 ){
			
			max = MAX_CONTACTS;
		}
		
		List<DHTTransportAlternativeContact> result = new ArrayList<DHTTransportAlternativeContact>( max );
	
		Set<Integer>	used_ids = new HashSet<Integer>();
		
		synchronized( contacts ){
			
			Iterator<DHTTransportAlternativeContact> it = contacts.iterator();
			
			while( it.hasNext()){
								
				DHTTransportAlternativeContact contact = it.next();
				
				if ( live_only && contact.getAge() > LIVEISH_AGE_SECS ){
					
					break;
				}
				
				Integer id = contact.getID();
				
				if ( used_ids.contains( id )){
					
					continue;
				}
				
				used_ids.add( id );
				
				result.add( contact );
				
				if ( result.size() == max ){
					
					break;
				}

			}
		}
		
		return( result );
	}
	
	private void
	trim()
	{
		Iterator<DHTTransportAlternativeContact> it = contacts.iterator();
		
		int	pos = 0;
		
		while( it.hasNext()){
				
			it.next();
			
			pos++;
			
			if(  pos > MAX_CONTACTS ){
				
				it.remove();
			}
		}
	}
	
	protected void
	addContacts(
		List<DHTTransportAlternativeContact>	new_contacts )
	{
		synchronized( contacts ){
			
			for ( DHTTransportAlternativeContact new_contact: new_contacts ){
				
				//System.out.println( "add contact: " + getString(new_contact));
				
				contacts.add( new_contact );
			}
			
			if ( contacts.size() > MAX_CONTACTS ){
				
				trim();
			}
			
			//System.out.println( "    contacts=" + contacts.size());
		}
	}
	
	protected void
	addContact(
		DHTTransportAlternativeContact		new_contact )
	{
		synchronized( contacts ){
				
			//System.out.println( "add contact: " +  getString(new_contact));
			
			contacts.add( new_contact );
				
			if ( contacts.size() > MAX_CONTACTS ){
				
				trim();
			}
			
			//System.out.println( "    contacts=" + contacts.size());
		}		
	}
	
	protected int
	getRequiredContactCount()
	{
		synchronized( contacts ){
			
			int	num_contacts = contacts.size();
			
			int	result = 0;
			
			if ( num_contacts < MAX_CONTACTS ){
						
				result =  MAX_CONTACTS - num_contacts;
				
			}else{
				
				Iterator<DHTTransportAlternativeContact> it = contacts.iterator();
					
				int	pos = 0;
				
				while( it.hasNext()){
					
					DHTTransportAlternativeContact contact = it.next();
					
					if ( contact.getAge() > LIVE_AGE_SECS ){
				
						result = MAX_CONTACTS - pos;
						
						break;
						
					}else{
						
						pos++;
					}
				}
			}
				
			//System.out.println( network + ": required=" + result );
			
			return( result );
		}
	}
	
	private String
	getString(
		DHTTransportAlternativeContact		contact )
	{
		return( contact.getProperties() + ", age=" + contact.getAge());
	}
}
