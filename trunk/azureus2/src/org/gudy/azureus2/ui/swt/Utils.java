/*
 * File    : Utils.java
 * Created : 25 sept. 2003 16:15:07
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
 
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Olivier
 * 
 */
public class Utils {
  
  public static void disposeComposite(Composite composite) {
    if(composite == null || composite.isDisposed())
        return;
    Control[] controls = composite.getChildren();
    for(int i = 0 ; i < controls.length ; i++) {
      Control control = controls[i];                
      if(control != null && ! control.isDisposed()) {
        if(control instanceof Composite) {
          disposeComposite((Composite) control);
        }
        control.dispose();
      }
    }    
    composite.dispose();
  }
  
  public static void changeBackgroundComposite(Composite composite,Color color) {
    if(1==1)
      return;  
    if(composite == null || composite.isDisposed())
          return;
      Control[] controls = composite.getChildren();
      for(int i = 0 ; i < controls.length ; i++) {
        Control control = controls[i];                
        if(control != null && ! control.isDisposed()) {
          if(control instanceof Composite) {
            changeBackgroundComposite((Composite) control,color);
          }
          control.setBackground(color);
        }
      }    
      composite.setBackground(color);
    }

}
