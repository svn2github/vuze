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

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.views.IView;

/**
 * @author Olivier Chalouhi
 *
 */
public class GUIUpdater extends AEThread implements ParameterListener {
  
  private AzureusCore		azureus_core;
  private MainWindow 		mainWindow;
  private Display 			display;
  
  boolean finished = false;
  boolean refreshed = true;
  
  int waitTime = COConfigurationManager.getIntParameter("GUI Refresh");   
  
  public 
  GUIUpdater(
  	AzureusCore		_azureus_core,
	MainWindow 		mainWindow) 
  {       
    super("GUI updater");
    azureus_core		= _azureus_core;
    this.mainWindow = mainWindow;
    this.display = mainWindow.getDisplay();
    
    setPriority(Thread.MAX_PRIORITY);
    COConfigurationManager.addParameterListener("GUI Refresh", this);
  }

  public void runSupport() {
    while (!finished) {
      if(refreshed)
        update();
      try {
        Thread.sleep(waitTime);
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
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

            IpFilter ip_filter = azureus_core.getIpFilterManager().getIPFilter();
            
            mainWindow.ipBlocked.setText( 
            		"{"+
					DisplayFormatters.formatDateShort(ip_filter.getLastUpdateTime()) + 
					"} IPs: " + 
					ip_filter.getNbRanges() + 
					" - " + 
					ip_filter.getNbIpsBlocked() + 
					"/" +
					ip_filter.getNbBannedIps() +
					"/" + 
					azureus_core.getIpFilterManager().getBadIps().getNbBadIps());
					
            int	ul_limit = COConfigurationManager.getIntParameter("Max Upload Speed KBs");
            int	dl_limit = COConfigurationManager.getIntParameter("Max Download Speed KBs");
            
       
		    mainWindow.statusDown.setText(
		    		MessageText.getString("ConfigView.download.abbreviated") + " " + 
					(dl_limit==0?"":"[" + dl_limit + "K] " ) +
					DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getDownloadAverage()));
		    
            mainWindow.statusUp.setText(
            		MessageText.getString("ConfigView.upload.abbreviated") + " " + 
					(ul_limit==0?"":"[" + ul_limit + "K] " ) +
					DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.globalManager.getStats().getUploadAverage()));
          }
          
          if(mainWindow.systemTraySWT != null)
            mainWindow.systemTraySWT.update();
          
          try{
          	mainWindow.downloadBars_mon.enter();
          
            Iterator iter = mainWindow.downloadBars.values().iterator();
            while (iter.hasNext()) {
              MinimizedWindow mw = (MinimizedWindow) iter.next();
              mw.refresh();
            }
          }finally{
          	
          	mainWindow.downloadBars_mon.exit();
          }
        } catch (Exception e) {
          LGLogger.log(LGLogger.ERROR, "Error while trying to update GUI");
          Debug.printStackTrace( e );
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
