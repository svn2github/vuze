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
import org.eclipse.swt.widgets.Table;

/**
 * @author Olivier
 *
 */
public abstract class BufferedTableItem {
  
  private BufferedTableRow row;
  private int position;
  
  public BufferedTableItem(BufferedTableRow row,int position) {
    this.row = row;
    this.position = position;
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
  
  public void setForeground(Color color) {
    row.setForeground(color);
  }
  
  public Rectangle getBounds() {
    if(position != -1)
      return row.getBounds(position);
    return null;
  }
  
  public Table getTable() {
    return row.getTable();
  }
  
  public abstract void refresh();
  
  public void dispose() {}
  
  public boolean isShown() {
    return position != -1;
  }
  
  public boolean needsPainting() {
  	return false; 
  }
  
  public void doPaint(Rectangle clipping) {    
  } 
}
