/*
 * File    : TestWindow.java
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
package org.gudy.azureus2.ui.swt.animations.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * @author Olivier Chalouhi
 *
 */
public class TestWindow implements AnimableShell {
  
  Shell shell;
  int nbAnimation = 0;
  int x0,y0,x1,y1;
  
  public TestWindow(Display display) {
    shell = new Shell(display,SWT.NO_TRIM | SWT.ON_TOP);            
    
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
    
    shell.setLayout(new FillLayout());
    Label label = new Label(shell,SWT.NULL);
    label.setImage(ImageRepository.getImage("popup"));   
    Rectangle bounds = display.getClientArea();    
    x0 = bounds.width - 250;
    x1 = bounds.width;
    
    y0 = bounds.height;
    y1 = bounds.height - 150;
    
    shell.setLocation(x0,y0);
    shell.open();
    new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),30,30).start();
  }

  
  public void animationEnded() {
    if(nbAnimation == 0) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x0,y1),1,3000).start();
      return;
    }
    if(nbAnimation == 1) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),50,30).start();
      return;
    }
    if(nbAnimation == 2) {
     shell.getDisplay().asyncExec(new Runnable() {
      public void run() {
        shell.dispose();
      }
    });
   }
  }

  public void animationStarted() {
    
  }

  public Shell getShell() {
   return shell;
  }

  public void reportPercent(int percent) {
    // TODO Auto-generated method stub
  }
}
