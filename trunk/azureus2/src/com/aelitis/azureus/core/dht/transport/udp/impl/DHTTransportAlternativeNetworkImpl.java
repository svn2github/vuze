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
	private static final int	MAX_AGE_SECS 	= 5*60;
	private static final int	MAX_CONTACTS	= 64;
	
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
					int res = o1.getLastAlive() - o2.getLastAlive();
					
					if ( res == 0 ){
						
						res = o1.getID() - o2.getID();
					}
					
					return( res );
				}
			});
	
	private TreeSet<DHTTransportAlternativeContact> contacts_reverse =
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
		int		max )
	{
		List<DHTTransportAlternativeContact> result = new ArrayList<DHTTransportAlternativeContact>( max );
	
		Set<Integer>	used_ids = new HashSet<Integer>();
		
		synchronized( contacts ){
			
			Iterator<DHTTransportAlternativeContact> it = contacts.iterator();
			
			while( it.hasNext()){
				
				if ( max == 0 ){
					
					break;
				}
				
				DHTTransportAlternativeContact contact = it.next();
				
				Integer id = contact.getID();
				
				if ( used_ids.contains( id )){
					
					continue;
				}
				
				used_ids.add( id );
				
				result.add( contact );
				
				max--;
			}
		}
		
		return( result );
	}
	
	protected void
	addContacts(
		List<DHTTransportAlternativeContact>	new_contacts )
	{
		synchronized( contacts ){
			
			for ( DHTTransportAlternativeContact contact: new_contacts ){
				
				contacts.add( contact );
				
				contacts_reverse.add( contact );
			}
			
			Iterator<DHTTransportAlternativeContact> it = contacts_reverse.iterator();
			
			while( it.hasNext()){
				
				DHTTransportAlternativeContact contact = it.next();
				
				if ( contacts.size() > MAX_CONTACTS || contact.getAge() > MAX_AGE_SECS ){
			
					it.remove();
					
					contacts.remove( contact );
				}
			}
		}
	}
	
	protected void
	addContact(
		DHTTransportAlternativeContact		new_contact )
	{
		synchronized( contacts ){
							
			contacts.add( new_contact );
				
			contacts_reverse.add( new_contact );
			
			Iterator<DHTTransportAlternativeContact> it = contacts_reverse.iterator();
			
			while( it.hasNext()){
				
				DHTTransportAlternativeContact contact = it.next();
				
				if ( contacts.size() > MAX_CONTACTS || contact.getAge() > MAX_AGE_SECS ){
			
					it.remove();
					
					contacts.remove( contact );
				}
			}
		}		
	}
	
	protected int
	getRequiredContactCount()
	{
		synchronized( contacts ){
			
			int rem = MAX_CONTACTS - contacts.size();
			
			if ( rem > 0 ){
				
				return( rem );
			}
						
			Iterator<DHTTransportAlternativeContact> it = contacts_reverse.iterator();
				
			while( it.hasNext()){
				
				DHTTransportAlternativeContact contact = it.next();
				
				if ( contact.getAge() > MAX_AGE_SECS ){
			
					it.remove();
					
					contacts.remove( contact );
				}
			}
			
			return( MAX_CONTACTS - contacts.size());
		}
	}
}
