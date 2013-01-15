/*
 * Created on Jan 15, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.magnet.swt;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;


public class 
MagnetPluginUISWT 
{
	public
	MagnetPluginUISWT(
		UIInstance					instance,
		TableContextMenuItem[]		menus )
	{
		UISWTInstance	swt = (UISWTInstance)instance;
		
		Image	image = swt.loadImage( "com/aelitis/azureus/plugins/magnet/icons/magnet.gif" );

		for ( TableContextMenuItem menu: menus ){
			
			menu.setGraphic( swt.createGraphic( image ));
		}	
	}
}
