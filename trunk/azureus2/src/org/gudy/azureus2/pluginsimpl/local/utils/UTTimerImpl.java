/*
 * Created on 29-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.*;

import org.gudy.azureus2.core3.util.*;

public class 
UTTimerImpl
	implements UTTimer
{
	protected Timer		timer;

	protected boolean		destroyed;
	
	protected
	UTTimerImpl(
		PluginInterface		pi,
		String				name )
	{
		timer = new Timer( "Plugin " + pi.getPluginID() + ":" + name );
	}
	
	public UTTimerEvent
	addPeriodicEvent(
		long						periodic_millis,
		final UTTimerEventPerformer	performer )
	{
		if ( destroyed ){
			
			throw( new RuntimeException( "Timer has been destroyed" ));
			
		}
		
		final timerEvent	res = new timerEvent();
			
		TimerEventPeriodic ev = timer.addPeriodicEvent( 
			periodic_millis,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent		ev )
				{
					res.perform( performer );
				}
			});
		
		res.setEvent( ev );
		
		return( res );
	}
	
	public void
	destroy()
	{
		destroyed	= true;
		
		timer.destroy();
	}
	
	protected class
	timerEvent
		implements UTTimerEvent
	{
		protected TimerEventPeriodic		ev;
		
		protected void
		setEvent(
			TimerEventPeriodic	_ev )
		{
			ev		= _ev;
		}
		
		protected void
		perform(
			UTTimerEventPerformer	p )
		{
			p.perform( this );
		}
		
		public void
		cancel()
		{
			ev.cancel();
		}
	}
}
