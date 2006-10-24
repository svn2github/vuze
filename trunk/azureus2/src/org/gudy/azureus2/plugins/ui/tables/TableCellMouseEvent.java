/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.plugins.ui.tables;

/**
 * Mouse event information for 
 * {@link org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener}
 * 
 * @author TuxPaper
 * @created Jan 10, 2006
 * @since 2.3.0.7
 */
public class TableCellMouseEvent {
	/** 
	 * eventType is triggered when mouse is pressed down
	 *  
	 * @since 2.3.0.7
	 */
	public final static int EVENT_MOUSEDOWN = 0;

	/** 
	 * eventType is triggered when mouse is let go 
	 *  
	 * @since 2.3.0.7
	 */
	public final static int EVENT_MOUSEUP = 1;

	/** 
	 * eventType is trigggered when mouse is double clicked 
	 *  
	 * @since 2.3.0.7
	 */
	public final static int EVENT_MOUSEDOUBLECLICK = 2;

	/**
	 * EVENT_* constant specifying the type of event that has been triggered 
	 *  
	 * @since 2.3.0.7
	 */
	public int eventType;

	/**
	 * TableCell that the mouse trigger applies to
	 *  
	 * @since 2.3.0.7
	 */
	public TableCell cell;

	/**
	 * x position of mouse relative to table cell
	 *  
	 * @since 2.3.0.7
	 */
	public int x;

	/**
	 * y position of mouse relative to table cell
	 */
	public int y;

	/**
	 * Which button was pressed.  1 = first button (left),
	 * 2 = second button (middle), 3 = third button (right) 
	 *  
	 * @since 2.3.0.7
	 */
	public int button;

	/**
	 * Keyboard state when the mouse event was triggered.
	 * 
	 * @TODO Define state constants
	 *  
	 * @since 2.3.0.7
	 */
	public int keyboardState;

	/**
	 * Not implemented
	 */
	public boolean skipCoreFunctionality;
}
