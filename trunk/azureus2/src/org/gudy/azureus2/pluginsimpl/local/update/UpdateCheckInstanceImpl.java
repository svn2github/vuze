/*
 * Created on 12-May-2004
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

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateCheckInstanceImpl
	implements UpdateCheckInstance
{
	protected List	listeners	= new ArrayList();
	protected List	updates 	= new ArrayList();
	
	protected UpdatableComponentImpl[]		components;
	
	protected
	UpdateCheckInstanceImpl(
		UpdatableComponentImpl[]	_components )
	{
		components	= _components;
	}

	public void
	start()
	{
		final	Semaphore	sem = new Semaphore();
		
		for (int i=0;i<components.length;i++){
			
			final UpdatableComponentImpl	comp = components[i];
			
			Thread	t = 
				new Thread()
				{
					public void
					run()
					{
						UpdateCheckerImpl	checker = 
							new UpdateCheckerImpl( 
									UpdateCheckInstanceImpl.this,
									comp,
									sem );
						
						try{
							
							comp.getComponent().checkForUpdate( checker );
							
						}catch( Throwable e ){
							
							checker.failed();
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
		}
		
		for (int i=0;i<components.length;i++){

			sem.reserve();
		}
		
		for (int i=0;i<listeners.size();i++){
			
			((UpdateCheckInstanceListener)listeners.get(i)).complete( this );
		}
	}
		
	protected synchronized Update
	addUpdate(
		UpdatableComponentImpl	comp,
		String					name,
		String[]				desc,
		String					new_version,
		ResourceDownloader[]	downloaders,
		int						restart_required )
	{
		UpdateImpl	update = 
			new UpdateImpl( name, desc, new_version, 
							downloaders, comp.isMandatory(), restart_required );
		
		updates.add( update );
		
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
		UpdateCheckInstanceListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		UpdateCheckInstanceListener	l )
	{
		listeners.remove(l);
	}
}
