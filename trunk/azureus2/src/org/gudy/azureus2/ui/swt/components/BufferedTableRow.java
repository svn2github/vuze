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

import org.eclipse.swt.SWT;
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
	protected Color[]	foreground_colors	= new Color[0];
	
	protected Color		foreground;
	
	/** Must be initialized from the Display thread */
	public BufferedTableRow(Table _table)
	{
	  this(_table, -1);
	}

	/** Must be initialized from the Display thread */
	public BufferedTableRow(Table _table, int index)
	{
    if (index >= 0 && index < _table.getItemCount())
      item = new TableItem( _table, SWT.NULL, index );
    else
      item = new TableItem( _table, SWT.NULL );
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
		if (item == null || item.isDisposed()){
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
		return (item == null || item.isDisposed());
	}
	
	public Color
	getForeground()
	{
  	if (item == null || item.isDisposed())
  	  return null;
		return( item.getForeground());
	}
	
	public void
	setForeground(
		Color	c )
	{
  	if (item == null || item.isDisposed())
  	  return;
		if (foreground != null && foreground.equals(c))
		  return;
		
		foreground = c;
		
		item.setForeground(foreground);
	}

	public boolean
	setForeground(
	  int index,
		Color	new_color )
	{
		if (item == null || item.isDisposed()){
			return false;
		}
				
		if ( index >= foreground_colors.length ){
			
			int	new_size = Math.max( index+1, foreground_colors.length+VALUE_SIZE_INC );
			
			Color[]	new_colors = new Color[new_size];
			
			System.arraycopy( foreground_colors, 0, new_colors, 0, foreground_colors.length );
			
			foreground_colors = new_colors;
		}

		Color value = foreground_colors[index];
		
		if ( new_color == value ){
			
			return false;
		}
		
		if (	new_color != null && 
				value != null &&
				new_color.equals( value )){
					
			return false;
		}
		
		foreground_colors[index] = new_color;

    try {
      item.setForeground(index, new_color);
    } catch (NoSuchMethodError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    
    return true;
	}

	public Color getForeground(int index)
	{
  	if (item == null || item.isDisposed())
  	  return null;
		if (index >= foreground_colors.length)
		  return item.getForeground();

		return foreground_colors[index];
	}
	
	public String
	getText(
		int		index )
	{
		if ( index >= text_values.length ){
			
			return "";
		}
		
		return( text_values[index] == null ? "" : text_values[index]);
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
		if (item == null || item.isDisposed()){
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
    if (item == null || item.isDisposed())
      return null;
    Table parent = item.getParent();
    if (parent == null || parent.isDisposed())
      return null;
    return parent;
  }
  
  public Color getBackground() {
    if(item == null || item.isDisposed())
      return null;
    return item.getBackground();
  }

  /* The Index is this item's the position in list.
   *
   * @return Item's Position
   */
  public int getIndex() {
    Table table = getTable();
    if (table == null)
      return -1;
    return table.indexOf(item);
  }
  
  public boolean setIndex(int index) {
    Table table = getTable();
    if (table == null)
      return false;
    int oldIndex = table.indexOf(item);
    if (oldIndex == index)
      return false;
    
    if (index > oldIndex)
       index--;
    //System.out.println(this+": oldIndex="+oldIndex+"; index="+ index);
		TableItem newItem = new TableItem( table, SWT.NULL, index );
		copyToItem(newItem);

		// don't use oldIndex
		table.remove(table.indexOf(item));
		item = newItem;
		return true;
  }
  
  private void copyToItem(TableItem newItem) {
    Table table = getTable();
    if (table == null)
      return;

    newItem.setText(text_values);
		newItem.setImage(image_values);
		Color colorFG = item.getForeground();
		Color colorBG = item.getBackground();
		newItem.setForeground(colorFG);
		newItem.setBackground(colorBG);
		int numColumns = table.getColumnCount();
		for (int i = 0; i < numColumns; i++) {
      try {
        Color colorColumnFG = item.getForeground(i);
        Color colorColumnBG = item.getBackground(i);
        if (!colorColumnFG.equals(colorFG))
          newItem.setForeground(i, colorColumnFG);
        if (!colorColumnBG.equals(colorBG))
          newItem.setBackground(i, colorColumnBG);
      } catch (NoSuchMethodError e) {
        /* Ignore for Pre 3.0 SWT.. */
      }
		}
    if (getSelected())
      table.select(table.indexOf(newItem));
    else
      table.deselect(table.indexOf(newItem));
	}
  
  public boolean getSelected() {
    Table table = getTable();
    if (table == null)
      return false;
    return table.isSelected(table.indexOf(item));
  }

  public void setSelected(boolean bSelected) {
    Table table = getTable();
    if (table == null)
      return;
    if (bSelected)
      table.select(getIndex());
    else
      table.deselect(getIndex());
  }
  
  public void setTableItem(TableItem ti, boolean bCopyFromOld) {
    if (bCopyFromOld) {
      copyToItem(ti);
    } else {
  		ti.setForeground(null);
  		ti.setBackground(null);
      Table table = getTable();
      if (table == null)
        return;
  		int numColumns = table.getColumnCount();
  		for (int i = 0; i < numColumns; i++) {
        try {
  		    ti.setForeground(i, null);
    	    ti.setBackground(i, null);
        } catch (NoSuchMethodError e) {
          /* Ignore for Pre 3.0 SWT.. */
        }
  		}
 		}
	  text_values		= new String[0];
	  image_values	= new Image[0];
	  foreground_colors	= new Color[0];
    foreground = null;

    item = ti;
  }
}
