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
UpdateImpl 
	implements Update
{
	protected UpdateManagerImpl			manager;
	protected String					name;
	protected ResourceDownloader[]		downloaders;
	protected int						restart_required;

	protected List						listeners = new ArrayList();
	
	protected
	UpdateImpl(
		UpdateManagerImpl		_manager,
		String					_name,
		ResourceDownloader[]	_downloaders,
		int						_restart_required )
	{
		manager				= _manager;
		name				= _name;
		downloaders			= _downloaders;
		restart_required	= _restart_required;
	}
	
	public String
	getName()
	{
		return( name );
	}

	public ResourceDownloader[]
	getDownloaders()
	{
		return( downloaders );
	}
	
	public void
	setRestartRequired(
		int	_restart_required )
	{
		restart_required	= _restart_required;
	}
	
	public int
	getRestartRequired()
	{
		return( restart_required );
	}
	

	public void
	cancel()
	{
		for (int i=0;i<listeners.size();i++){
			
			((UpdateListener)listeners.get(i)).cancelled( this );
		}
	}
	
	public void
	addListener(
		UpdateListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		UpdateListener		l )
	{
		listeners.remove( l );
	}
}
