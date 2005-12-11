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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * A buffered Table Row.
 *<p> 
 * We buffer certain properties of TableRow to save CPU cycles.  For example,
 * foreground_colors is cached because TableItem.getForegroundColor is expensive
 * when there is no color set, and setForegroundColor is always expensive.
 *<p> 
 * Text is not buffered as SWT does a good job of buffering it already.
 *<p>
 * 
 * @note For Virtual tables, we can not set any visual properties until
 *        SWT.SetData has been called.  getData("SD") is set after SetData
 *        call, and the row is invalidated.  Thus, there is no need to set
 *        any visual properties until the row #isVisible()
 * 
 * @author parg<br>
 * @author TuxPaper (SWT.Virtual Stuff)
 */
public class 
BufferedTableRow
{
	private static final int VALUE_SIZE_INC	= 8;
	
	private static Color[] alternatingColors = null;
	
	protected Table table;
	protected TableItem	item;
	
	protected Image[]	image_values	= new Image[0];
	protected Color[]	foreground_colors	= new Color[0];
	
	protected Color		foreground;
	
	// remove when things work
	private static final boolean bDebug = false;
	
	private Point ptIconSize;
	
	/**
	 * Default constructor
	 * 
	 * @param _table
	 */
	public BufferedTableRow(Table _table)
	{
		table = _table;
		item = null;
	}
	
	/**
	 * Create a row in the SWT table
	 *
	 */
	public void createSWTRow() {
    item = new TableItem(table, SWT.NULL);
		item.setData("TableRow", this);
		setAlternatingBGColor();
	}
	
	private void setAlternatingBGColor() {
		if (noTableItem() || Constants.isLinux)
			return;

		if (alternatingColors == null) {
			alternatingColors = new Color[] {
					table.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND),
					Colors.colorAltRow };
		}

		int index = table.indexOf(item);
		Color newColor = alternatingColors[index % alternatingColors.length];
		if (!getBackground().equals(newColor))
			item.setBackground(newColor);
	}

	/**
	 * Disposes of underlying SWT TableItem. If no TableItem has been
	 * assigned to the row yet, an unused TableItem will be disposed of, if
	 * available.
	 * <p>
	 * Disposing of fonts, colors and other resources are the responsibilty of 
	 * the caller.
	 */
	public void
	dispose()
	{
		if (table != null && !table.isDisposed() && Utils.isThisThreadSWT()) {
			if (noTableItem()) {
				// No assigned spot yet, or not our spot:
				// find a row with no TableRow data

				TableItem[] items = table.getItems();
				for (int i = items.length - 1; i >= 0; i--) {
					TableItem item = items[i];
					if (!item.isDisposed()) {
						Object itemRow = item.getData("TableRow");
						if (itemRow == null || itemRow == this) {
							this.item = item;
							break;
						}
					}
				}
			}
			
			if (item != null && !item.isDisposed()) 
				item.dispose();
			else
				System.err.println("No table row was found to dispose");
		} else {
			if (!Utils.isThisThreadSWT()) {
				System.err.println("Calling BufferedTableRow.dispose on non-SWT thread!");
				System.err.println(Debug.getStackTrace(false, false));
			}
		}
		item = null;
	}
	
	/**
	 * Sets the receiver's image at a column.
	 *
	 * @param index the column index
	 * @param new_image the new image
	 */
	public void
	setImage(
   		int 	index,
		Image	new_image )
	{
		if (noTableItem())
			return;

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
	noTableItem()
	{
		return table.isDisposed() || item == null || item.isDisposed()
				|| item.getData("TableRow") != this;
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
		if (noTableItem())
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
		if (noTableItem())
			return false;
				
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
		if (noTableItem())
			return "";

		// SWT >= 3041(Win),3014(GTK),3002(Carbon) and returns "" if range check
		// fails
		return item.getText(index);
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
		if (noTableItem())
			return false;
		
		if (index < 0 || index >= table.getColumnCount())
			return false;
		
		if (new_value == null)
			new_value = "";

		if (item.getText(index).equals(new_value))
			return false;

		item.setText( index, new_value );
    
    return true;
	}
	
  public Rectangle getBounds(int index) {
		if (noTableItem())
			return null;
		return item.getBounds(index);
	}

  protected Table getTable() {
  	return table;
  }
  
  public Color getBackground() {
    if(item == null || item.isDisposed())
      return null;
    return item.getBackground();
  }

  /**
   * The Index is this item's the position in list.
   *
   * @return Item's Position
   */
  public int getIndex() {
  	if (noTableItem())
  		return -1;

    if (table == null)
      return -1;
    return table.indexOf(item);
  }
  
  /** This flickers */
  public boolean setIndex(int index) {
    if (table == null)
      return false;
  	if (item != null) {
	    int oldIndex = table.indexOf(item);
	    if (oldIndex == index)
	      return false;
	    
	    if (index > oldIndex)
	       index--;
  	}
    //if (bDebug) System.out.println(this+": oldIndex="+oldIndex+"; index="+ index);
		TableItem newItem = new TableItem( table, SWT.NULL, index );
		copyToItem(newItem);

		// don't use oldIndex
  	if (item != null) {
  		table.remove(table.indexOf(item));
  	}
		item = newItem;
		if (bDebug) System.out.println("SetIndex=" + index);

		return true;
  }
  
  private void copyToItem(TableItem newItem) {
    Table table = getTable();
    if (table == null || item == null)
      return;

//    newItem.setText(text_values);
		newItem.setImage(image_values);
		Color colorFG = item.getForeground();
		Color colorBG = item.getBackground();
		newItem.setForeground(colorFG);
		newItem.setBackground(colorBG);
		int numColumns = table.getColumnCount();
		for (int i = 0; i < numColumns; i++) {
      try {
      	newItem.setText(i, item.getText(i));
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
    if (isSelected())
      table.select(table.indexOf(newItem));
    else
      table.deselect(table.indexOf(newItem));

    newItem.setData("TableRow", item.getData("TableRow"));
	}
  
  public boolean isSelected() {
  	if (noTableItem())
  		return false;

  	// Invalid Indexes are checked/ignored by SWT.
    return table.isSelected(table.indexOf(item));
  }

  public void setSelected(boolean bSelected) {
  	if (noTableItem())
  		return;

    if (bSelected)
      table.select(getIndex());
    else
      table.deselect(getIndex());
  }

  /**
   * Set the TableItem associated with this row to the TableItem at the
   * specified index.
   * 
   * @param newIndex Index of TableItem that will be associated with this row
   * @param bCopyFromOld True: Copy the visuals from the old TableItem to
   *                            the new TableItem
   * @return success level
   */
  public boolean setTableItem(int newIndex, boolean bCopyFromOld) {
  	TableItem newRow = table.getItem(newIndex); 
  	if (newRow != null && !newRow.isDisposed()) {
  		// This is temporary for SWT 3212, because there are cases where
  		// it says it isn't disposed, when it really almost is
  		try {
  			newRow.setData("Test", "");
  		} catch (NullPointerException e) {
  			return false;
  		}
  	}

  	if (newRow == item) {
  		if (newRow == null || newRow.getData("TableRow") == this) {
     		setAlternatingBGColor();
  			return false;
  		}
  	}

  	if (newRow != null) {
  		if (newRow.getParent() != table)
  			return false;

	    if (bCopyFromOld) {
	      copyToItem(newRow);
	    } else if ((table.getStyle() & SWT.VIRTUAL) > 0
					&& newRow.getData("SD") != null) {
	    	// clear causes too much flicker
	    	//table.clear(table.indexOf(newRow));
	  		newRow.setForeground(null);
	  		//newRow.setBackground(null);

	  		int numColumns = table.getColumnCount();
	  		for (int i = 0; i < numColumns; i++) {
	        try {
        		newRow.setForeground(i, null);
	        } catch (NoSuchMethodError e) {
	          /* Ignore for Pre 3.0 SWT.. */
	        }
	  		}
	 		}

	    try {
	    	newRow.setData("TableRow", this);
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	System.out.println("Disposed? " + newRow.isDisposed());
	    	if (!newRow.isDisposed()) {
		    	System.out.println("TR? " + newRow.getData("TableRow"));
		    	System.out.println("SD? " + newRow.getData("SD"));
	    	}
	    }
  	}
	  image_values	= new Image[0];
	  foreground_colors	= new Color[0];
    foreground = null;

    if (item != null && !item.isDisposed() && item.getData("TableRow") == this)
    	item.setData("TableRow", null);

    item = newRow;
    if (item != null) {
   		setAlternatingBGColor();
	    // XXX Move back to TableView
	    if ((table.getStyle() & SWT.VIRTUAL) == 0)
	    	setIconSize(ptIconSize);
    }
 		invalidate();

    return true;
  }

  public boolean setHeight(int iHeight) {
  	return setIconSize(new Point(1, iHeight));
  }
  
  public boolean setIconSize(Point pt) {
		if (noTableItem())
			return false;
		
    ptIconSize = pt;

    if (pt == null)
      return false;
		
    // set row height by setting image
    Image image = new Image(item.getDisplay(), pt.x, pt.y);
    item.setImage(0, image);
    item.setImage(0, null);
    image.dispose();
    
    return true;
  }

  /**
	 * Whether the row is currently visible to the user
	 * 
	 * @return visibility
	 */
	public boolean isVisible() {
		if (noTableItem())
			return false;

		int index = table.indexOf(item);
		if (index == -1)
			return false;

		int iTopIndex = table.getTopIndex();
		if (index < iTopIndex)
			return false;

		// 2 offset to be on the safe side
		TableItem bottomItem = table.getItem(new Point(2,
				table.getClientArea().height - 1));
		if (bottomItem != null) {
			int iBottomIndex = table.indexOf(bottomItem);
			if (index > iBottomIndex)
				return false;
		}

//   XXX: getItemHeight is very slow on linux!
//		// iBottomIndex may be greater than # of rows, but that doesn't matter
//		// because index is always less than # of rows
//		int iBottomIndex = iTopIndex
//				+ ((table.getClientArea().height - table.getHeaderHeight() - 1) / table
//						.getItemHeight()) + 1;
//
//		if (index > iBottomIndex)
//			return false;

		// Not visible if we haven't setData yet
		if ((table.getStyle() & SWT.VIRTUAL) > 0 && item.getData("SD") == null) {
			//System.out.println("Row " + index + " not SD yet");
			return false;
		}

		return true;
	}
  
  /**
   * Overridable function that is called when row needs invalidation.
   *
   */
  public void invalidate() {
  }
}
