/*
 * Created on 19-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.ui.tables.mytracker;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.plugins.ui.menus.ContextMenuItem;
import org.gudy.azureus2.plugins.ui.menus.ContextMenuItemListener;


public class
MyTrackerContextMenuItemImpl
	implements ContextMenuItem
{
	protected MyTrackerImpl		my_tracker;
	protected String			name;
	protected List				listeners	= new ArrayList();
	
	protected
	MyTrackerContextMenuItemImpl(
		MyTrackerImpl		_my_tracker,
		String				_name )
	{
		my_tracker	= _my_tracker;
		name		= _name;
	}
	
	public String
	getResourceKey()
	{
		return( name );
	}
	
	public void
	fire(
		TRHostTorrent		torrent )
	{
		MyTrackerRowImpl	target = new MyTrackerRowImpl( torrent );
		
		for (int i=0;i<listeners.size();i++){
			
			((ContextMenuItemListener)listeners.get(i)).selected( this, target );
		}
	}
	
	public void
	addlistener(
		ContextMenuItemListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removelistener(
		ContextMenuItemListener	l )
	{
		listeners.remove(l);
	}
}