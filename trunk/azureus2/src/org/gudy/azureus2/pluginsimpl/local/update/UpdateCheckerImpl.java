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

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
UpdateCheckerImpl
	implements UpdateChecker
{
	protected UpdateCheckInstanceImpl		check_instance;
	protected UpdatableComponentImpl		component;
	protected Semaphore						semaphore;
	
	protected boolean						completed;
	protected boolean						failed;
	protected boolean						sem_released;
	
	protected
	UpdateCheckerImpl(
		UpdateCheckInstanceImpl	_check_instance,
		UpdatableComponentImpl	_component,
		Semaphore				_sem )
	{
		check_instance		= _check_instance;
		component			= _component;
		semaphore			= _sem;
	}
	
	public Update
	addUpdate(
		String				name,
		String[]			description,
		String				new_version,
		ResourceDownloader	downloader,
		int					restart_required )
	{
		return( addUpdate(
					name, description, new_version,
					new ResourceDownloader[]{ downloader },
					restart_required ));
	}
	
	public Update
	addUpdate(
		String					name,
		String[]				description,
		String					new_version,
		ResourceDownloader[]	downloaders,
		int						restart_required )
	{
		return( check_instance.addUpdate( 
					component, name, description, new_version,
					downloaders, restart_required ));
	}
	
	public synchronized void
	completed()
	{
		if ( !sem_released ){
			
			completed	= true;
			
			semaphore.release();
		}
	}
		
	public synchronized void
	failed()
	{
		if ( !sem_released ){
			
			failed	= true;
			
			semaphore.release();
		}		
	}
	
	protected boolean
	getFailed()
	{
		return( failed );
	}
}
