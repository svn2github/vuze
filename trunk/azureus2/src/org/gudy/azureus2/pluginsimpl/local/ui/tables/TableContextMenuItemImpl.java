/*
 * Azureus - a Java Bittorrent client
 * 2004/May/16 TuxPaper
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.tables;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.ui.menus.*;

public class TableContextMenuItemImpl 
       implements TableContextMenuItem
{
  private String 	sTableID;
  private String 	sName;
  private int		style		= STYLE_PUSH;
  private boolean	enabled		= true;
  private Object	data;
  private Graphic	graphic;
  
  private List 	listeners 		= new ArrayList();
  private List	fill_listeners	= new ArrayList();
  
  public TableContextMenuItemImpl(String tableID, String key) {
    sTableID = tableID;
    sName = key;
  }

  public String getTableID() {
    return sTableID;
  }

  public String getResourceKey() {
    return sName;
  }
  
	public int
	getStyle()
	{
		return( style );
	}
	
	public void
	setStyle(
		int		_style )
	{
		style	= _style;
	}
	
	public Object
	getData()
	{
		return( data );
	}
	
	public void
	setData(
		Object	_data )
	{
		data	= _data;
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public void
	setEnabled(
		boolean	_enabled )
	{
		enabled = _enabled;
	}
	
	public void
	setGraphic(
		Graphic		_graphic )
	{
		graphic	= _graphic;
	}
	
	public Graphic
	getGraphic()
	{
		return( graphic );
	}
	
	public void
	invokeMenuWillBeShownListeners(
		TableRow[]		rows )
	{
		  for (int i = 0; i < fill_listeners.size(); i++){
		    	
		    	try{
		    		((MenuItemFillListener)(fill_listeners.get(i))).menuWillBeShown(this, rows);
		    		
		    	}catch( Throwable e ){
		    		
		    		Debug.printStackTrace(e);
		    	}
		    }
	}

	public void
	addFillListener(
		MenuItemFillListener	listener )
	{
		fill_listeners.add( listener );
	}
	
	public void
	removeFillListener(
		MenuItemFillListener	listener )
	{
		fill_listeners.remove( listener );
	}
	
  public void invokeListeners(TableRow row) {
    for (int i = 0; i < listeners.size(); i++){
    	
    	try{
    		((MenuItemListener)(listeners.get(i))).selected(this, row);
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    }
  }

  public void addListener(MenuItemListener l) {
    listeners.add(l);
  }
  
  public void removeListener(MenuItemListener l) {
    listeners.remove(l);
  }
}