/*
 * Created on Sep 13, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.AERunStateHandler;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.IUIIntializer;

public class 
MainWindowFactory 
{
	private static final boolean
	isImmediate()
	{
		return( !AERunStateHandler.isDelayedUI());
	}
	
	public static MainWindow
	create(
		AzureusCore 			core, 
		Display 				display,
		IUIIntializer			uiInitializer )
	{
		if ( isImmediate()){
			
			return( new MainWindowImpl( core, display, uiInitializer ));
			
		}else{
			
			return( new MainWindowDelayStub( core, display, uiInitializer ));
		}
	}
	
	public static MainWindowInitStub
	createAsync(
		Display 		display, 
		IUIIntializer 	uiInitializer )
	{
		final MainWindow	window;
		
		if ( isImmediate()){
		
			window = new MainWindowImpl( display, uiInitializer );
			
		}else{
		
			window = new MainWindowDelayStub( display, uiInitializer );
		}
		
		return(
			new MainWindowInitStub()
			{
				public void
				init(
					AzureusCore		core )
				{
					window.init( core );
				}
			});
	}
	
	public static interface
	MainWindowInitStub
	{
		public void
		init(
			AzureusCore		core );
	}
}
