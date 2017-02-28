/*
 * Created on Aug 26, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package com.aelitis.azureus.core.dht.router;

import java.util.List;

public class 
DHTRouterWrapper
	implements DHTRouter
{
	private final DHTRouter		delegate;
	
	public
	DHTRouterWrapper(
		DHTRouter		_delegate )
	{
		delegate	= _delegate;
	}
	
	protected DHTRouter
	getDelegate()
	{
		return( delegate );
	}
	
	public int
	getK()
	{
		return( delegate.getK());
	}
	
	public byte[]
	getID()
	{
		return( delegate.getID());
	}
	
	public boolean
	isID(
		byte[]	node_id )
	{
		return( delegate.isID(node_id));
	}
	
	public DHTRouterContact
	getLocalContact()
	{
		return( delegate.getLocalContact());
	}
	
	public void
	setAdapter(
		DHTRouterAdapter	_adapter )
	{
		delegate.setAdapter(_adapter);
	}

	public void
	seed()
	{
		delegate.seed();
	}
	
	public void
	contactKnown(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment,
		boolean						force )
	{
		delegate.contactKnown(node_id, attachment, force);
	}
	
	public void
	contactAlive(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment )
	{
		delegate.contactAlive(node_id, attachment);
	}

	public DHTRouterContact
	contactDead(
		byte[]						node_id,
		boolean						force )
	{
		return( delegate.contactDead(node_id, force));	
	}
	
	public DHTRouterContact
	findContact(
		byte[]	node_id )
	{
		return( delegate.findContact(node_id));
	}

	public List<DHTRouterContact>
	findClosestContacts(
		byte[]		node_id,
		int			num_to_return,
		boolean		live_only )
	{
		return( delegate.findClosestContacts(node_id, num_to_return, live_only));
	}
		
	public void
	recordLookup(
		byte[]	node_id )
	{
		delegate.recordLookup(node_id);
	}
	
	public boolean
	requestPing(
		byte[]	node_id )
	{
		return( delegate.requestPing(node_id));
	}
	
	public void
	refreshIdleLeaves(
		long	idle_max )
	{
		delegate.refreshIdleLeaves(idle_max);
	}
	
	public byte[]
	refreshRandom()
	{
		return( delegate.refreshRandom());
	}
	
	public List<DHTRouterContact>
	findBestContacts(
		int		max )
	{
		return( delegate.findBestContacts(max));
	}
	
	public List<DHTRouterContact>
	getAllContacts()
	{
		return( delegate.getAllContacts());
	}
	
	public DHTRouterStats
	getStats()
	{
		return( delegate.getStats());
	}
	
	public void
	setSleeping(
		boolean	sleeping )
	{
		delegate.setSleeping(sleeping);
	}
	
	public void
	setSuspended(
		boolean			susp )
	{
		delegate.setSuspended(susp);
	}
	
	public void
	destroy()
	{
		delegate.destroy();
	}
	
	public void
	print()
	{
		delegate.print();
	}

	public boolean addObserver(DHTRouterObserver rto)
	{
		return( delegate.addObserver(rto));
	}
	
	public boolean containsObserver(DHTRouterObserver rto)
	{
		return( delegate.containsObserver(rto));
	}

	public boolean removeObserver(DHTRouterObserver rto)
	{
		return( delegate.removeObserver(rto));
	}
}
