/*
 * File    : SplashWindow.java
 * Created : 11 nov. 2003
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Olivier
 *
 */
public class SplashWindow {
  
  Display display;
  Shell splash;
  Label currentTask;
  ProgressBar percentDone;
  Color white;
  
  
  public void show(Display display) {
    this.display = display;
    white = new Color(display,255,255,255);
    splash = new Shell(display, SWT.ON_TOP);
    splash.setText("Azureus");
    splash.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    splash.setLayout(layout);
    Label label = new Label(splash, SWT.NONE);
    label.setImage(ImageRepository.getImage("azureus_splash"));

    currentTask = new Label(splash,SWT.BORDER);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    currentTask.setLayoutData(gridData);
    currentTask.setBackground(white);

    this.percentDone = new ProgressBar(splash,SWT.HORIZONTAL);
    this.percentDone.setMinimum(0);
    this.percentDone.setMaximum(100);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    this.percentDone.setLayoutData(gridData);

    splash.pack();
    Rectangle splashRect = splash.getBounds();
    Rectangle displayRect = display.getBounds();
    int x = (displayRect.width - splashRect.width) / 2;
    int y = (displayRect.height - splashRect.height) / 2;
    splash.setLocation(x, y);
    splash.open();    
  }
  
  public void close() {
    if(splash != null && !splash.isDisposed())
      splash.dispose();
    if(white != null && ! white.isDisposed())
      white.dispose();
  }
  
  public void setPercentDone(int percentDone) {
    this.percentDone.setSelection(percentDone);
  }
  
  public void setCurrentTask(String task) {
    currentTask.setText(task);
  }

}
