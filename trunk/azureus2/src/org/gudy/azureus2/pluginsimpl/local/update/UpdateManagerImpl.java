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

import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.*;

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

	protected List	components 	= new ArrayList();
	protected List	listeners	= new ArrayList();
	
	protected
	UpdateManagerImpl()
	{
		UpdateInstallerImpl.checkForFailedInstalls();
		
			// cause the platform manager to register any updateable components
		
		try{
			PlatformManagerFactory.getPlatformManager();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public synchronized void
	registerUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory )
	{
		components.add( new UpdatableComponentImpl( component, mandatory ));
	}
	
	
	public synchronized UpdateCheckInstance
	createUpdateCheckInstance()
	{
		UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[components.size()];
		
		components.toArray( comps );
		
		UpdateCheckInstance	res = new UpdateCheckInstanceImpl( comps );
		
		for (int i=0;i<listeners.size();i++){
			
			((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
		}
		
		return( res );
	}
	
	public UpdateInstaller
	createInstaller()
		
		throws UpdateException
	{
		return( new UpdateInstallerImpl());
	}
	
	
	public void
	restart()
	
		throws UpdateException
	{
		try{
			PluginManager.restartAzureus();
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateManager:restart fails", e ));
		}
	}
	
	public void
	addListener(
		UpdateManagerListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		UpdateManagerListener	l )
	{
		listeners.remove(l);
	}
}
