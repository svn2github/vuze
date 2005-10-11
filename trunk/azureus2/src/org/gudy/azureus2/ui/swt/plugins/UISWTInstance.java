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

package org.gudy.azureus2.ui.swt.plugins;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.ui.UIInstance;

public interface 
UISWTInstance 
	extends UIInstance
{
	  /** Retrieve the SWT Display object that Azureus uses (when in SWT mode).
	   * If you have a thread that does some periodic/asynchronous stuff, Azureus 
	   * will crashes with and 'InvalidThreadAccess' exception unless you
	   * embed your calls in a Runnable, and use getDisplay().aSyncExec(Runnable r);
	   *
	   * @return SWT Display object that Azureus uses
	   *
	   * @since 2.1.0.0
	   */
	public Display getDisplay();

	  /** Creates an UIImageSWT object with the supplied SWT Image
	   *
	   * @param img Image to assign to the object
	   * @return a new UIImagetSWT object
	   *
	   * @since 2.1.0.0
	   */
	
	public UISWTGraphic createGraphic(Image img);

	  /**
	   * A Plugin might call this method to add a View to Azureus's views
	   * The View will be accessible from View > Plugins > View name
	   * @param view The PluginView to be added
	   * @param autoOpen Whether the plugin should auto-open at startup
	   *
	   * @since 2.1.0.2
	   */
	
	public void 
	addView( 
		UISWTPluginView 	view, 
		boolean 			autoOpen );
	
		/**
		 * Remove a view
		 * @param view
		 */

	public void
	removeView(
		UISWTPluginView		view );
	
		/**
		 * Add an AWT panel as the plugin view
		 * @param view
		 * @param auto_open
		 */
	  
	public void
	addView(
		UISWTAWTPluginView	view,
		boolean				auto_open );
	
		/**
		 * Remove a view
		 * @param view
		 */

	public void
	removeView(
		UISWTAWTPluginView		view );

}
