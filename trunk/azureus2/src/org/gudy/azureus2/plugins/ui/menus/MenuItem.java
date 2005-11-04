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

package org.gudy.azureus2.plugins.ui.menus;

import org.gudy.azureus2.plugins.ui.Graphic;

/** Menu item access for the UI.
 *
 * @author parg (Original ContextMenuItem code)
 * @author TuxPaper (Generic-izing, commenting)
 */
public interface MenuItem 
{
		/**
		 * normal selection menu, no Data value required
		 */
	public static final int STYLE_PUSH				= 1;
	
		/**
		 * check box style menu item - data must be of type Boolean
		 */
	
	public static final int STYLE_CHECK				= 2;
	
		/**
		 * radio style - data must be Boolean
		 */
	
	public static final int STYLE_RADIO				= 3;
	
		/**
		 * separator line
		 */
	
	public static final int STYLE_SEPARATOR			= 4;
	
  /** Retrieve the resource key ("name") of this menu item
   *
   * @return resource key for this menu
   */
	public String
	getResourceKey();
	
	/** Retrieve the parent menu's resource key.
	 *
	 * @return parent menu's resource key, or null if no parent
	 *
	public Menu
	getParent();
   */
	
		/**
		 * Get the type of the menu item
		 */
	
	public int
	getStyle();
	
		/**
		 * Set the style of the menu item (see STYLE_ constants)
		 * @param style
		 */
	
	public void
	setStyle(
		int		style );
	
		/**
		 * Get the current data value associated with the menu: Boolean for CHECK style
		 * @return
		 */
	
	public Object
	getData();
	
		/**
		 * Set the current data value associated with the menu: Boolean for CHECK style
		 * @param data
		 */
	
	public void
	setData(
		Object	data );
	
		/**
		 * Whether or not this item is enabled or not
		 * @return
		 */
	
	public boolean
	isEnabled();
	
		/**
		 * Set the enabled status of the menu item
		 * @param enabled
		 */
	
	public void
	setEnabled(
		boolean	enabled );
	
		/**
		 * set the menu item's icon
		 * @param graphic
		 */
	
	public void
	setGraphic(
		Graphic		graphic );
	
		/**
		 * get the menu's graphic
		 * @return
		 */
	
	public Graphic
	getGraphic();
		
	public void
	addFillListener(
		MenuItemFillListener	listener );
	
	public void
	removeFillListener(
		MenuItemFillListener	listener );

	/** Add a Selection Listener for this menu item
	 *
	 * @param l listener to be notified when user has selected menu item.
	 */
	public void
	addListener(
		MenuItemListener	l );
	
	/**
   * Remove a Selection Listener from this menu item
   *
   * @param l listener to remove
   */
	public void
	removeListener(
		MenuItemListener	l );
}
