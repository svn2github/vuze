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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier Chalouhi
 *
 */
public class TestWindow implements AnimableShell {
  
  Shell shell;
  int nbAnimation = 0;
  int x0,y0,x1,y1;
  
  public TestWindow(Display display) {
    shell = new Shell(display,SWT.ON_TOP | SWT.BORDER);    
    shell.setBackground(MainWindow.white);
        
    shell.setSize(200,100);
    Rectangle bounds = display.getClientArea();
    x0 = bounds.width - 200;
    x1 = bounds.width;
    
    y0 = bounds.height;
    y1 = bounds.height - 100;
    
    shell.setLocation(x0,y0);
    shell.open();
    new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),20,20).start();
  }

  
  public void animationEnded() {
    if(nbAnimation == 0) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x0,y1),50,20).start();
      return;
    }
    if(nbAnimation == 1) {
      nbAnimation++;
      new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),20,20).start();
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
