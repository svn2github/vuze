/*
 * File    : PopupShell.java
 * Created : 14 mars 2004
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
package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * @author Olivier Chalouhi
 *
 */
public class PopupShell {
  
  protected Shell shell;
      
  public PopupShell(Display display) { 
    
    if ( display.isDisposed()){          
      return;
    }
    
    shell = new Shell(display,SWT.NO_TRIM | SWT.ON_TOP );            
    
    Region region = new Region();
    int[] border = {0,0 , 0,144 , 244,144 , 244,0}; 
    region.add(border);
    int[] corner1 = { 0,0  , 0,6 , 1,6 , 1,4 , 4,1 , 6,1 , 6,0};
    region.subtract(corner1);
    int[] corner2 = { 0,144  , 0,138 , 1,138 , 1,139  , 5,143 , 7,143 , 7,144};
    region.subtract(corner2);
    int[] corner3 = { 244,144  , 244,138 , 243,138 , 243,139 , 239,143, 237,143, 237,144};
    region.subtract(corner3);
    int[] corner4 = { 244,0  , 244,6 , 243,6 , 243,4 , 239,1, 237,1, 237,0};
    region.subtract(corner4);
    
    shell.setRegion(region);
    
    Rectangle size = region.getBounds();
    shell.setSize(size.width, size.height);
    shell.setImage(ImageRepository.getImage("azureus"));
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0;
    layout.marginWidth= 0;
    layout.spacing = 0;
    
    shell.setLayout(layout);
  }
  
  protected void layout() {
    Label label = new Label(shell,SWT.NULL);
    label.setImage(ImageRepository.getImage("popup"));
    
    FormData formData = new FormData();
    formData.left = new FormAttachment(0,-1);
    formData.top = new FormAttachment(0,-1);
    
    label.setLayoutData(formData); 
    
    shell.layout();
  }
}
