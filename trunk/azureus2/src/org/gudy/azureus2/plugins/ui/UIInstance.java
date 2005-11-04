/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.plugins.ui;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * This interface represents a UI running on the core (e.g. the SWT UI). The actual implementation of
 * this will support UI-specific operations - you need to cast this to the appropriate type to access
 * them.
 * This is to allow "native" UI plugin access - for example a plugin that directly accesses SWT functionality
 * would do it via this object (it'll be an instance of 
 * 	org.gudy.azureus2.ui.swt.plugins.UISWTInstance )
 */

public interface 
UIInstance 
{
		/**
		 * Some UI instances need to understand which plugin they are associated with. This method
		 * gives the opportunity to customise the UIInstance returned to a plugin so that operations
		 * on it can take the appropriate actions
		 */
	
	public UIInstance
	getPluginSpecificInstance(
		PluginInterface		plugin_interface );
	
		/**
		 * This method will be called by the UI manager when detaching the UI to permit the action to be
		 * vetoed/any detach logic to occur. It should not be directly called by the plugin code 
		 */
	
	public void
	detach()
	
		throws UIException;
}
