/*
 * Created on 4 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.Iterator;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.views.IView;

/**
 * @author Olivier Chalouhi
 *
 */
public class GUIUpdater extends Thread implements ParameterListener {
  
  private MainWindow mainWindow;
  private Display display;
  
  boolean finished = false;
  boolean refreshed = true;
  
  int waitTime = COConfigurationManager.getIntParameter("GUI Refresh");   
  
  public GUIUpdater(MainWindow mainWindow) {       
    super("GUI updater"); //$NON-NLS-1$
    this.mainWindow = mainWindow;
    this.display = mainWindow.getDisplay();
    
    setPriority(Thread.MAX_PRIORITY);
    COConfigurationManager.addParameterListener("GUI Refresh", this);
  }

  public void run() {
    while (!finished) {
      if(refreshed)
        update();
      try {
        Thread.sleep(waitTime);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @param parameterName the name of the parameter that has changed
   * @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
   */
  public void parameterChanged(String parameterName) {
    waitTime = COConfigurationManager.getIntParameter("GUI Refresh");
  }

  private void update() {
    refreshed = false;
    if (display != null && !display.isDisposed())
      display.asyncExec(new Runnable() {
      public void run() {
        try {
          IView view = null;
          if (!mainWindow.getShell().isDisposed() && mainWindow.isVisible() && !mainWindow.getShell().getMinimized()) {

            view = mainWindow.getCurrentView();
            
            if (view != null) {
              view.refresh();
              Tab.refresh();
            }

            mainWindow.ipBlocked.setText( "{"+DisplayFormatters.formatDateShort(IpFilter.getInstance().getLastUpdateTime()) + "} IPs: " + IpFilter.getInstance().getNbRanges() + " - " + IpFilter.getInstance().getNbIpsBlocked());
            mainWindow.statusDown.setText(MessageText.getString("ConfigView.download.abbreviated") + " " + DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getDownloadAverage())); //$NON-NLS-1$
            mainWindow.statusUp.setText(MessageText.getString("ConfigView.upload.abbreviated") + " " + DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getUploadAverage())); //$NON-NLS-1$
          }
          
          if(mainWindow.systemTraySWT != null)
            mainWindow.systemTraySWT.update();
          
          synchronized (mainWindow.downloadBars) {
            Iterator iter = mainWindow.downloadBars.values().iterator();
            while (iter.hasNext()) {
              MinimizedWindow mw = (MinimizedWindow) iter.next();
              mw.refresh();
            }
          }
        } catch (Exception e) {
          LGLogger.log(LGLogger.ERROR, "Error while trying to update GUI");
          e.printStackTrace();
        } finally {
          refreshed = true;
        }
      }        
    });
  }

  public void stopIt() {
    finished = true;
    COConfigurationManager.removeParameterListener("GUI Refresh", this);
    COConfigurationManager.removeParameterListener("config.style.refreshMT", this);
  }
}
