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
import org.gudy.azureus2.core3.logging.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateCheckInstanceImpl
	implements UpdateCheckInstance
{
	protected List	listeners	= new ArrayList();
	protected List	updates 	= new ArrayList();
	
	protected Semaphore	sem 	= new Semaphore();

	protected UpdatableComponentImpl[]		components;
	protected UpdateCheckerImpl[]			checkers;
	
	protected boolean		completed;
	protected boolean		cancelled;
	
	protected
	UpdateCheckInstanceImpl(
		UpdatableComponentImpl[]	_components )
	{
		components	= _components;
		
		checkers	= new UpdateCheckerImpl[components.length];
		
 		for (int i=0;i<components.length;i++){
			
			UpdatableComponentImpl	comp = components[i];
			
			UpdateCheckerImpl checker = checkers[i] = 
				new UpdateCheckerImpl( 
						this,
						comp,
						sem );
		}
	}

	public void
	start()
	{
		for (int i=0;i<components.length;i++){
			
			final UpdateCheckerImpl			checker = checkers[i];
			
			Thread	t = 
				new Thread( "UpdatableComponent Checker:" + i )
				{
					public void
					run()
					{					
						try{		
							checker.getComponent().checkForUpdate( checker );
							
						}catch( Throwable e ){
							
							checker.failed();
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
		}
		
		Thread	t = 
			new Thread( "UpdatableComponent Completion Waiter" )
			{
				public void
				run()
				{
					for (int i=0;i<components.length;i++){
			
						sem.reserve();
					}
					
					synchronized( UpdateCheckInstanceImpl.this ){
						
						if ( cancelled ){
							
							return;
						}
					
						completed	= true;
					}	
					
						// If there are any manadatory updates then we just go ahead with them and drop the rest
					
					List	target_updates = new ArrayList();
					
					boolean	mandatory_only	= false;
					
					for (int i=0;i<updates.size();i++){
						
						UpdateImpl	update = (UpdateImpl)updates.get(i);
						
						if ( update.isMandatory()){
							
							mandatory_only	= true;
							
							break;
						}
					}
					
					for (int i=0;i<updates.size();i++){
						
						UpdateImpl	update = (UpdateImpl)updates.get(i);
													
						if ( update.isMandatory() || !mandatory_only ){
							
							target_updates.add( update );
							
						}else{
							
							LGLogger.log("Dropping update '" + update.getName() + "' as non-mandatory and mandatory updates found" );
						}
					}

					updates	= target_updates;
					
					for (int i=0;i<listeners.size();i++){
					
						((UpdateCheckInstanceListener)listeners.get(i)).complete( UpdateCheckInstanceImpl.this );
					}
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
		
	protected synchronized UpdateImpl
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
		
		boolean	cancel_it = false;
		
		synchronized( this ){
			
			if ( cancelled ){
				
				cancel_it	= true;
			}
		}
		
		if ( cancel_it ){
			
			update.cancel();
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
	
	public UpdateChecker[]
	getCheckers()
	{
		return( checkers );
	}
	
	public void
	cancel()
	{
		boolean	just_do_updates = false;
		
		synchronized( this ){
			
			if ( completed ){
				
				just_do_updates = true;
			}
		
			cancelled	= true;
		}
			
		
		for (int i=0;i<updates.size();i++){
			
			((UpdateImpl)updates.get(i)).cancel();
		}

		if ( !just_do_updates ){
			
			for (int i=0;i<checkers.length;i++){
				
				if ( checkers[i] != null ){
					
					checkers[i].cancel();
				}
			}
			
			for (int i=0;i<listeners.size();i++){
					
				((UpdateCheckInstanceListener)listeners.get(i)).cancelled( this );
			}
		}
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
