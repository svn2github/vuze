/*
 * File    : BufferedTableItem.java
 * Created : 24-Nov-2003
 * By      : parg
 * 
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

package org.gudy.azureus2.ui.swt.components;

/**
 * @author parg
 *
 */

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Color;

public class 
BufferedTableItem 
{
	public static final int VALUE_SIZE_INC	= 8;
	
	protected TableItem	item;
	
	protected String[]	values	= new String[VALUE_SIZE_INC];
	
	public
	BufferedTableItem(
		Table		_table,
		int			_i )
	{
		item = new TableItem( _table, _i );
	}

		// prefer not to have this one but need to introduce BufferedTable to do it...
		
	public TableItem
	getItem()
	{
		return( item );	
	}
	
	public void
	dispose()
	{
		item.dispose();
	}
	
	public void
	setImage(
		Image	_image )
	{
		item.setImage( _image );
	}
	
	public boolean
	isDisposed()
	{
		return( item.isDisposed());
	}
	
	public Color
	getForeground()
	{
		return( item.getForeground());
	}
	
	public void
	setForeground(
		Color	c )
	{
		item.setForeground(c);
	}
	
	public synchronized void
	setText(
		int			index,
		String		new_value )
	{
		if ( item.isDisposed()){
			return;
		}
				
		if ( index >= values.length ){
			
			int	new_size = Math.max( index+1, values.length+VALUE_SIZE_INC );
			
			String[]	new_values = new String[new_size];
			
			System.arraycopy( values, 0, new_values, 0, values.length );
			
			values = new_values;
		}
		
		String	value = values[index];
		
		if ( new_value == value ){
			
			return;
		}
		
		if (	new_value != null && 
				value != null &&
				new_value.equals( value )){
					
			return;
		}
		
		values[index] = new_value;
		
		item.setText( index, new_value==null?"":new_value );
	}
}
