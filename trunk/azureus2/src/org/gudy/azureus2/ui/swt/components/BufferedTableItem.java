/*
 * File    : BufferedTableItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.util.Debug;

/**
 * @author Olivier
 *
 */
public abstract class BufferedTableItem {
  
  private BufferedTableRow row;
  private int position;
  private boolean bOurFGColor = false;
  
  public BufferedTableItem(BufferedTableRow row,int position) {
    this.row = row;
    this.position = position;
  }
  
  public String getText() {
    if (position != -1)
      return row.getText(position);
    return "";
  }

  public boolean setText(String text) {
    if(position != -1)
      return row.setText(position,text);
    return false;
  }
  
  public void setImage(Image img) {
    if(position != -1)
      row.setImage(position,img);
  }
  
  public void setRowForeground(Color color) {
    row.setForeground(color);
  }
  
  public boolean setItemForeground(Color color) {
    if (position == -1)
      return false;

    boolean ok;
    if (bOurFGColor) {
      Color oldColor = row.getForeground(position);
      ok = row.setForeground(position, color);
      if (ok) {
        if (!color.isDisposed())
          color.dispose();
        bOurFGColor = false;
      }
    } else {
      ok = row.setForeground(position, color);
    }
    return ok;
  }

  public boolean setItemForeground(int red, int green, int blue) {
    if (position == -1)
      return false;

    Color oldColor = row.getForeground(position);
    RGB newRGB = new RGB(red, green, blue);
		if (oldColor.getRGB().equals(newRGB))
		  return false;

    
    Color newColor = new Color(row.getItem().getDisplay(), newRGB);
    boolean ok = row.setForeground(position, newColor);
    if (ok) {
      if (bOurFGColor) {
        if (!oldColor.isDisposed())
          oldColor.dispose();
      } else {
        bOurFGColor = true;
      }
    } else {
      if (!newColor.isDisposed())
        newColor.dispose();
    }
    
    return ok;
  }
  
  public Color getBackground() {
    return row.getBackground();
  }

  public Rectangle getBounds() {
    if(position != -1)
      return row.getBounds(position);
    return null;
  }
  
  public Table getTable() {
    return row.getTable();
  }

  public TableItem getTableItem() {
    return row.getItem();
  }
  
  public abstract void refresh();
  
  public void dispose() {
    if (bOurFGColor && position != -1) {
      Color oldColor = row.getForeground(position);
      if (oldColor != null && !oldColor.isDisposed())
        oldColor.dispose();
    }
  }
  
  public boolean isShown() {
    return position != -1;
  }
  
  public boolean needsPainting() {
  	return false; 
  }
  
  /** Paint the image only (no update needed)
   */
  public void doPaint(GC gc) {
  } 
  
  /** Column location (not position) changed.  Usually due to a resize of
   * a column in a position prior to this one.
   */
  public void locationChanged() {
  }

  public int getPosition() {
    return position;
  }
  
  public String getColumnName() {
    if (!isShown())
      return null;
    Table table = row.getTable();
    if (table != null && !table.isDisposed() && 
        position >= 0 && position < table.getColumnCount())
      return table.getColumn(position).getText();

    return null;
  }

  public void debugOut(String s, boolean bStackTrace) {
    Table table = row.getTable();
    if (table == null || table.isDisposed())
      return;
    TableItem[] ti = table.getSelection();
    for (int i = 0; i < ti.length; i++) {
      if (ti[i] == row.getItem()) {
        System.out.println(i + "-" + ti[i] + ": " + s);
        if (bStackTrace) Debug.outStackTrace(3);
        break;
      }
    }
  }
}
