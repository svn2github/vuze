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

package org.gudy.azureus2.platform.win32;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.logging.*;

public class 
PlatformManagerUpdateChecker 
{
	protected
	PlatformManagerUpdateChecker(
		final PlatformManagerImpl		platform )
	{
		Thread	t = 
			new Thread("Win32PlatformManager:updateChecker")
			{
				public void
				run()
				{
					check( platform );
				}
			};
		
		t.setDaemon( true );
		
		t.start();
	}
	
	protected void
	check(
		PlatformManagerImpl		platform )
	{
		String	current_version = platform==null?"1.0":platform.getVersion();
		
		LGLogger.log( "PlatformManager:Win32 update check starts: current = " + current_version );
	}
}
