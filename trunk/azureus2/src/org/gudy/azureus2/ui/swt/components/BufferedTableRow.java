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
import org.eclipse.swt.graphics.Rectangle;

public class 
BufferedTableRow 
{
	public static final int VALUE_SIZE_INC	= 8;
	
	protected TableItem	item;
	
	protected String[]	text_values		= new String[0];
	protected Image[]	image_values	= new Image[0];
	
	protected Color		foreground;
	
	public
	BufferedTableRow(
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
   		int 	index,
		Image	new_image )
	{
		if ( item.isDisposed()){
			return;
		}
				
		if ( index >= image_values.length ){
			
			int	new_size = Math.max( index+1, image_values.length+VALUE_SIZE_INC );
			
			Image[]	new_images = new Image[new_size];
			
			System.arraycopy( image_values, 0, new_images, 0, image_values.length );
			
			image_values = new_images;
		}
		
		Image	image = image_values[index];
		
		if ( new_image == image ){
			
			return;
		}
		
		image_values[index] = new_image;
		
		item.setImage( index, new_image );	
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
		if ( 	foreground != null &&
				foreground.equals( c )){
				
			return;
		}
		
		foreground = c;
		
		item.setForeground(foreground);
	}
	
	public String
	getText(
		int		index )
	{
		if ( index >= text_values.length ){
			
			return( null );
		}
		
		return( text_values[index]);
	}
  /**
   * @param index
   * @param new_value
   * @return true if the item has been updated
   */
	public boolean
	setText(
		int			index,
		String		new_value )
	{
		if ( item.isDisposed()){
			return false;
		}
				
		if ( index >= text_values.length ){
			
			int	new_size = Math.max( index+1, text_values.length+VALUE_SIZE_INC );
			
			String[]	new_values = new String[new_size];
			
			System.arraycopy( text_values, 0, new_values, 0, text_values.length );
			
			text_values = new_values;
		}
		
		String	value = text_values[index];
		
		if ( new_value == value ){
			
			return false;
		}
		
		if (	new_value != null && 
				value != null &&
				new_value.equals( value )){
					
			return false;
		}
		
		text_values[index] = new_value;
		
		item.setText( index, new_value==null?"":new_value );
    
    return true;
	}
  
  public Rectangle getBounds(int index) {
    if(item == null || item.isDisposed())
      return null;
    return item.getBounds(index);
  }
  
  public Table getTable() {
    if(item == null || item.isDisposed())
      return null;
    return item.getParent();
  }
}
