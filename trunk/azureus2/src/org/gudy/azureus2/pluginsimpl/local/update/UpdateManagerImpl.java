/*
 * Created on 07-May-2004
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

package org.gudy.azureus2.pluginsimpl.local.update;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateManagerImpl
	implements UpdateManager
{
	protected static UpdateManagerImpl		singleton = new UpdateManagerImpl();
	
	public static UpdateManager
	getSingleton()
	{
		return( singleton );
	}
	
	protected List	listeners	= new ArrayList();
	protected List	updates 	= new ArrayList();
	
	protected 
	UpdateManagerImpl()
	{
	}
	
	public Update
	addUpdate(
		String				name,
		String[]			desc,
		String				new_version,
		ResourceDownloader	downloader,
		boolean				mandatory,
		int					restart_required )
	{
		return( addUpdate( name, desc, new_version, new ResourceDownloader[]{ downloader }, mandatory, restart_required ));
	}
	
	public synchronized Update
	addUpdate(
		String					name,
		String[]				desc,
		String					new_version,
		ResourceDownloader[]	downloaders,
		boolean					mandatory,
		int						restart_required )
	{
		UpdateImpl	update = new UpdateImpl( this, name, desc, new_version, downloaders, mandatory, restart_required );
		
		updates.add( update );
		
		for (int i=0;i<listeners.size();i++){
			
			((UpdateManagerListener)listeners.get(i)).updateAdded( this, update );
		}
		
		return( update );
	}
	
	public synchronized Update[]
	getUpdates()
	{
		Update[]	res = new Update[updates.size()];
		
		updates.toArray( res );
		
		return( res );
	}
	
	public void
	addListener(
		UpdateManagerListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
			UpdateManagerListener	l )
	{
		listeners.remove(l);
	}
}
