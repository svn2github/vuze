/*
 * File    : StatsView.java
 * Created : 15 déc. 2003}
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
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.graphics.SpeedGraphic;

/**
 * @author Olivier
 *
 */
public class SpeedView extends AbstractIView {

  GlobalManager manager;
  GlobalManagerStats stats;
  
  Composite panel;
  
  Canvas downSpeedCanvas;
  SpeedGraphic downSpeedGraphic;
  
  Canvas upSpeedCanvas;
  SpeedGraphic upSpeedGraphic;
  
  UpdateThread updateThread;
  
  public SpeedView(GlobalManager manager) {
    this.manager = manager;
    this.stats = manager.getStats();
  }
  
  private class UpdateThread extends Thread {
    boolean bContinue;
    
    public void run() {
      try {
        bContinue = true;
        while(bContinue) {
          downSpeedGraphic.addIntValue(manager.getStats().getDownloadAverage());
          upSpeedGraphic.addIntValue(manager.getStats().getUploadAverage());
          Thread.sleep(1000);
        }
      } catch(Exception e) {
        e.printStackTrace();        
      }
    }
    
    public void stopIt() {
      bContinue = false;
    }
  }
  
  public void initialize(Composite composite) {
    panel = new Composite(composite,SWT.NULL);
    panel.setLayout(new GridLayout());
    GridData gridData;
        
    Group gDownSpeed = new Group(panel,SWT.NULL);
    Messages.setLanguageText(gDownSpeed,"SpeedView.downloadSpeed.title");
    gridData = new GridData(GridData.FILL_BOTH);
    gDownSpeed.setLayoutData(gridData);    
    gDownSpeed.setLayout(new GridLayout());
    
    downSpeedCanvas = new Canvas(gDownSpeed,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    downSpeedCanvas.setLayoutData(gridData);
    downSpeedGraphic = SpeedGraphic.getInstance();
    downSpeedGraphic.initialize(downSpeedCanvas);
    
    Group gUpSpeed = new Group(panel,SWT.NULL);
    Messages.setLanguageText(gUpSpeed,"SpeedView.uploadSpeed.title");
    gridData = new GridData(GridData.FILL_BOTH);
    gUpSpeed.setLayoutData(gridData);
    gUpSpeed.setLayout(new GridLayout());
    
    upSpeedCanvas = new Canvas(gUpSpeed,SWT.NULL);
    gridData = new GridData(GridData.FILL_BOTH);
    upSpeedCanvas.setLayoutData(gridData);
    upSpeedGraphic = SpeedGraphic.getInstance();
    upSpeedGraphic.initialize(upSpeedCanvas);
    
    updateThread = new UpdateThread(); 
    updateThread.setDaemon(true);
    updateThread.start();
  }
  
  public void delete() {
    updateThread.stopIt();
    MainWindow.getWindow().setStats(null);
    Utils.disposeComposite(panel);
    downSpeedGraphic.dispose();
    upSpeedGraphic.dispose();
  }

  public String getFullTitle() {
    return MessageText.getString("SpeedView.title.full"); //$NON-NLS-1$
  }
  
  public Composite getComposite() {
    return panel;
  }
  
  public void refresh() {
    downSpeedGraphic.refresh();
    upSpeedGraphic.refresh();
  }
  
}
