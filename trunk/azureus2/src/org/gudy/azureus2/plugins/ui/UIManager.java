/*
 * Created on 19-Apr-2004
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

package org.gudy.azureus2.plugins.ui;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.tables.mytracker.*;

public interface 
UIManager 
{
	public MyTracker
	getMyTracker();
	
		/**
		 * Gets a basic plugin view model that supports simple plugin requirements
		 * After getting the model create the view using createPluginView
		 * @return
		 */
	
	public BasicPluginViewModel
	getBasicPluginViewModel(
		String			name );

		/**
		 * Creates a view from the model. It is then necessary to add it to the plugin
		 * as any other PluginView
		 * @param model
		 * @return
		 */
	
	public PluginView
	createPluginView(
		PluginViewModel	model );
	
	
	public BasicPluginConfigModel
	createBasicPluginConfigModel(
		String		section_name );
	
	
	public BasicPluginConfigModel
	createBasicPluginConfigModel(
		String		parent_section,
		String		section_name );
	
	public void
	copyToClipBoard(
		String		data )
	
		throws UIException;
}
