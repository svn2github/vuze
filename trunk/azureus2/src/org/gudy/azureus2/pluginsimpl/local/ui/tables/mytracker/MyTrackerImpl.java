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

package org.gudy.azureus2.pluginsimpl.local.ui.tables.mytracker;

/**
 * @author parg
 *
 * XXX DeleteMe
 */

import java.util.*;

import org.gudy.azureus2.plugins.ui.tables.mytracker.*;
import org.gudy.azureus2.plugins.ui.menus.*;

public class 
MyTrackerImpl 
	implements MyTracker
{
	protected static MyTrackerImpl	singleton;
	
	public synchronized static MyTrackerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new MyTrackerImpl();
		}
		
		return( singleton );
	}
	
	protected List		menus = new ArrayList();
	
	public ContextMenuItem
	addContextMenu(
		String		name )
	{
		ContextMenuItem	menu = new MyTrackerContextMenuItemImpl( this, name );
		
		menus.add( menu );
		
		return( menu );
	}
	
	public ContextMenuItem[]
	getContextMenus()
	{
		MyTrackerContextMenuItemImpl[]	res = new MyTrackerContextMenuItemImpl[menus.size()];

		menus.toArray( res );
		
		return( res );
	}
}
